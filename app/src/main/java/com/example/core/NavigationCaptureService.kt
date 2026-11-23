package com.example.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * User-consented, app-window/screen capture transport for mirroring a navigation provider.
 *
 * Android owns the capture consent UI. This service never attempts to capture another app
 * without the one-time MediaProjection grant returned by that system UI.
 */
class NavigationCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var workerThread: HandlerThread? = null
    private var projectionCallback: MediaProjection.Callback? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCapture()
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val resultData = intent?.parcelableIntent<Intent>(EXTRA_RESULT_DATA)
        if (resultCode == 0 || resultData == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startCaptureForeground()
        startProjection(resultCode, resultData)
        return START_NOT_STICKY
    }

    private fun startCaptureForeground() {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("LumaNav navigation mirror")
            .setContentText("Screen capture is active. Tap Stop to end sharing.")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startProjection(resultCode: Int, resultData: Intent) {
        stopCapture()
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mediaProjection = manager.getMediaProjection(resultCode, resultData)
        if (mediaProjection == null) {
            stopSelf()
            return
        }
        projection = mediaProjection

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
        val width = metrics.widthPixels.coerceAtLeast(1)
        val height = metrics.heightPixels.coerceAtLeast(1)
        val density = metrics.densityDpi

        workerThread = HandlerThread("LumaNavScreenCapture").also { it.start() }
        val handler = Handler(workerThread!!.looper)
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3)
        imageReader = reader
        reader.setOnImageAvailableListener({ source ->
            val image = runCatching { source.acquireLatestImage() }.getOrNull() ?: return@setOnImageAvailableListener
            try {
                publishFrame(image)
            } finally {
                image.close()
            }
        }, handler)

        val callback = object : MediaProjection.Callback() {
            override fun onStop() {
                stopCapture()
                stopSelf()
            }
        }
        projectionCallback = callback
        mediaProjection.registerCallback(callback, handler)

        virtualDisplay = mediaProjection.createVirtualDisplay(
            "LumaNavNavigationMirror",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            handler
        )
        _captureActive.value = true
    }

    private fun publishFrame(image: Image) {
        val plane = image.planes.firstOrNull() ?: return
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = (rowStride - pixelStride * image.width) / pixelStride
        val padded = Bitmap.createBitmap(
            image.width + rowPadding.coerceAtLeast(0),
            image.height,
            Bitmap.Config.ARGB_8888
        )
        padded.copyPixelsFromBuffer(buffer)
        val frame = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
        padded.recycle()
        _latestFrame.value?.takeIf { !it.isRecycled }?.recycle()
        _latestFrame.value = frame
    }

    private fun stopCapture() {
        _captureActive.value = false
        _latestFrame.value?.takeIf { !it.isRecycled }?.recycle()
        _latestFrame.value = null
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.setOnImageAvailableListener(null, null)
        imageReader?.close()
        imageReader = null
        val activeProjection = projection
        projectionCallback?.let { callback ->
            if (activeProjection != null) {
                runCatching { activeProjection.unregisterCallback(callback) }
            }
        }
        projectionCallback = null
        if (activeProjection != null) {
            runCatching { activeProjection.stop() }
        }
        projection = null
        workerThread?.quitSafely()
        workerThread = null
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Navigation screen sharing",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    private inline fun <reified T> Intent.parcelableIntent(key: String): T? {
        return if (Build.VERSION.SDK_INT >= 33) getParcelableExtra(key, T::class.java)
        else @Suppress("DEPRECATION") (getParcelableExtra(key) as? T)
    }

    companion object {
        private const val CHANNEL_ID = "lumanav_navigation_capture"
        private const val NOTIFICATION_ID = 4107
        private const val EXTRA_RESULT_CODE = "projection_result_code"
        private const val EXTRA_RESULT_DATA = "projection_result_data"
        private const val ACTION_STOP = "com.example.lumanav.STOP_CAPTURE"

        private val _captureActive = MutableStateFlow(false)
        val captureActive: StateFlow<Boolean> = _captureActive.asStateFlow()

        private val _latestFrame = MutableStateFlow<Bitmap?>(null)
        val latestFrame: StateFlow<Bitmap?> = _latestFrame.asStateFlow()

        fun start(context: Context, resultCode: Int, resultData: Intent) {
            val serviceIntent = Intent(context, NavigationCaptureService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, NavigationCaptureService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}
