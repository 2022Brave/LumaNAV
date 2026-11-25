package com.example.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * User-consented, app-window/screen capture transport for mirroring a navigation provider.
 * Android owns the capture consent UI. Every projection token is used for one capture session.
 */
class NavigationCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var workerThread: HandlerThread? = null
    private var workerHandler: Handler? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var captureDensityDpi: Int = 0
    private var lastFrameAtMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            releaseCaptureResources(stopProjection = true)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val resultData = intent?.parcelableIntent<Intent>(EXTRA_RESULT_DATA)
        if (resultCode == 0 || resultData == null) {
            _captureError.value = "Screen capture permission was not returned by Android."
            stopSelf()
            return START_NOT_STICKY
        }

        _captureError.value = null
        releaseCaptureResources(stopProjection = true)
        startCaptureForeground()
        runCatching { startProjection(resultCode, resultData) }
            .onFailure { error ->
                releaseCaptureResources(stopProjection = true)
                _captureError.value = error.message?.takeIf(String::isNotBlank)
                    ?: "LumaNav could not start screen capture."
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        return START_NOT_STICKY
    }

    private fun startCaptureForeground() {
        val stopIntent = Intent(this, NavigationCaptureService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this,
            STOP_REQUEST_CODE,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("LumaNav navigation mirror")
            .setContentText("Mirroring the selected app window.")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
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
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val activeProjection = manager.getMediaProjection(resultCode, resultData)
            ?: error("Android did not grant a screen capture session.")
        projection = activeProjection

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
        captureDensityDpi = metrics.densityDpi

        workerThread = HandlerThread("LumaNavScreenCapture").also { it.start() }
        val handler = Handler(workerThread!!.looper)
        workerHandler = handler

        val (width, height) = scaledDimensions(metrics.widthPixels, metrics.heightPixels)
        val reader = createImageReader(width, height, handler)
        imageReader = reader

        val callback = object : MediaProjection.Callback() {
            override fun onStop() {
                releaseCaptureResources(stopProjection = false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            override fun onCapturedContentResize(width: Int, height: Int) {
                resizeCaptureOutput(width, height)
            }
        }
        projectionCallback = callback
        activeProjection.registerCallback(callback, handler)

        virtualDisplay = activeProjection.createVirtualDisplay(
            "LumaNavNavigationMirror",
            width,
            height,
            captureDensityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            handler
        ) ?: error("Android could not create the navigation mirror display.")

        _captureActive.value = true
    }

    private fun createImageReader(width: Int, height: Int, handler: Handler): ImageReader {
        return ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2).also { reader ->
            reader.setOnImageAvailableListener({ source ->
                val image = runCatching { source.acquireLatestImage() }.getOrNull()
                    ?: return@setOnImageAvailableListener
                try {
                    publishFrame(image)
                } finally {
                    image.close()
                }
            }, handler)
        }
    }

    private fun resizeCaptureOutput(contentWidth: Int, contentHeight: Int) {
        val handler = workerHandler ?: return
        val display = virtualDisplay ?: return
        val (width, height) = scaledDimensions(contentWidth, contentHeight)
        if (width <= 0 || height <= 0) return

        val replacement = createImageReader(width, height, handler)
        val previous = imageReader
        display.resize(width, height, captureDensityDpi)
        display.setSurface(replacement.surface)
        imageReader = replacement
        previous?.setOnImageAvailableListener(null, null)
        previous?.close()
    }

    private fun scaledDimensions(sourceWidth: Int, sourceHeight: Int): Pair<Int, Int> {
        val safeWidth = sourceWidth.coerceAtLeast(1)
        val safeHeight = sourceHeight.coerceAtLeast(1)
        val scale = minOf(1f, MAX_CAPTURE_DIMENSION.toFloat() / maxOf(safeWidth, safeHeight))
        return (safeWidth * scale).toInt().coerceAtLeast(1) to
            (safeHeight * scale).toInt().coerceAtLeast(1)
    }

    private fun publishFrame(image: Image) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastFrameAtMs < FRAME_INTERVAL_MS) return
        lastFrameAtMs = now

        val plane = image.planes.firstOrNull() ?: return
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        if (pixelStride <= 0) return
        val rowPadding = ((rowStride - pixelStride * image.width) / pixelStride).coerceAtLeast(0)
        val padded = Bitmap.createBitmap(
            image.width + rowPadding,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        buffer.rewind()
        padded.copyPixelsFromBuffer(buffer)
        val frame = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
        padded.recycle()

        // Do not recycle the previous frame here: Compose may still be drawing it.
        // Replacing the StateFlow reference lets the old bitmap be collected safely.
        _latestFrame.value = frame
    }

    private fun releaseCaptureResources(stopProjection: Boolean) {
        _captureActive.value = false
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
        if (stopProjection && activeProjection != null) {
            runCatching { activeProjection.stop() }
        }
        projection = null

        workerThread?.quitSafely()
        workerThread = null
        workerHandler = null
        lastFrameAtMs = 0L
        getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }

    override fun onDestroy() {
        releaseCaptureResources(stopProjection = true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
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
        private const val STOP_REQUEST_CODE = 4108
        private const val EXTRA_RESULT_CODE = "projection_result_code"
        private const val EXTRA_RESULT_DATA = "projection_result_data"
        private const val ACTION_STOP = "com.example.lumanav.STOP_CAPTURE"
        private const val MAX_CAPTURE_DIMENSION = 1280
        private const val FRAME_INTERVAL_MS = 250L

        private val _captureActive = MutableStateFlow(false)
        val captureActive: StateFlow<Boolean> = _captureActive.asStateFlow()

        private val _latestFrame = MutableStateFlow<Bitmap?>(null)
        val latestFrame: StateFlow<Bitmap?> = _latestFrame.asStateFlow()

        private val _captureError = MutableStateFlow<String?>(null)
        val captureError: StateFlow<String?> = _captureError.asStateFlow()

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
