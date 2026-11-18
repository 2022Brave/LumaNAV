package com.example.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
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
 * Mirrors a user-selected navigation app window into AUVIONYX.
 *
 * Android 14+ lets the user choose a single app window in the system capture prompt.
 * The user must select Google Maps, Waze or Mappls (not the entire display) so the
 * capture remains the navigation app while AUVIONYX is in the foreground.
 */
object NavigationScreenMirror {
    private val _latestFrame = MutableStateFlow<Bitmap?>(null)
    val latestFrame: StateFlow<Bitmap?> = _latestFrame.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    internal fun publishFrame(bitmap: Bitmap) {
        _latestFrame.value = bitmap
        _isRunning.value = true
    }

    internal fun setRunning(running: Boolean) {
        _isRunning.value = running
        if (!running) _latestFrame.value = null
    }

    fun start(context: Context, resultCode: Int, resultData: Intent) {
        val intent = Intent(context, NavigationScreenMirrorService::class.java).apply {
            action = NavigationScreenMirrorService.ACTION_START
            putExtra(NavigationScreenMirrorService.EXTRA_RESULT_CODE, resultCode)
            putExtra(NavigationScreenMirrorService.EXTRA_RESULT_DATA, resultData)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        context.startService(
            Intent(context, NavigationScreenMirrorService::class.java).apply {
                action = NavigationScreenMirrorService.ACTION_STOP
            }
        )
    }
}

class NavigationScreenMirrorService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var workerThread: HandlerThread? = null
    private var worker: Handler? = null
    private var lastFrameAtMs = 0L
    private var densityDpi = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCapture()
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action != ACTION_START) return START_NOT_STICKY

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val resultData = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (resultData == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return runCatching {
            startCapture(resultCode, resultData)
            START_NOT_STICKY
        }.getOrElse {
            stopCapture()
            stopSelf()
            START_NOT_STICKY
        }
    }

    private fun startCapture(resultCode: Int, resultData: Intent) {
        if (projection != null) return
        val manager = getSystemService(MediaProjectionManager::class.java)
        val mediaProjection = manager.getMediaProjection(resultCode, resultData)
        projection = mediaProjection

        workerThread = HandlerThread("AuvionyxNavigationMirror").also { it.start() }
        worker = Handler(workerThread!!.looper)

        val metrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getSystemService(WindowManager::class.java).maximumWindowMetrics
        } else {
            @Suppress("DEPRECATION")
            DisplayMetrics().also {
                @Suppress("DEPRECATION")
                (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(it)
            }
        }
        val initialWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            metrics.bounds.width()
        } else {
            @Suppress("DEPRECATION")
            metrics.widthPixels
        }
        val initialHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            metrics.bounds.height()
        } else {
            @Suppress("DEPRECATION")
            metrics.heightPixels
        }
        densityDpi = resources.displayMetrics.densityDpi

        mediaProjection.registerCallback(
            object : MediaProjection.Callback() {
                override fun onStop() {
                    stopCapture()
                    stopSelf()
                }

                override fun onCapturedContentResize(width: Int, height: Int) {
                    if (width > 0 && height > 0) resizeCapture(width, height)
                }
            },
            worker
        )

        createReader(initialWidth, initialHeight)
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "AUVIONYX-Navigation-Window",
            initialWidth,
            initialHeight,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null,
            worker
        )
        NavigationScreenMirror.setRunning(true)
    }

    private fun createReader(width: Int, height: Int) {
        val reader = ImageReader.newInstance(
            width.coerceAtLeast(1),
            height.coerceAtLeast(1),
            PixelFormat.RGBA_8888,
            2
        )
        reader.setOnImageAvailableListener({ source -> consumeFrame(source) }, worker)
        imageReader = reader
    }

    private fun resizeCapture(width: Int, height: Int) {
        val oldReader = imageReader
        val newReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        newReader.setOnImageAvailableListener({ source -> consumeFrame(source) }, worker)
        imageReader = newReader
        virtualDisplay?.resize(width, height, densityDpi)
        virtualDisplay?.setSurface(newReader.surface)
        oldReader?.close()
    }

    private fun consumeFrame(source: ImageReader) {
        val image = runCatching { source.acquireLatestImage() }.getOrNull() ?: return
        try {
            val now = System.currentTimeMillis()
            if (now - lastFrameAtMs < FRAME_INTERVAL_MS) return
            lastFrameAtMs = now

            val plane = image.planes.firstOrNull() ?: return
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = (rowStride - pixelStride * image.width).coerceAtLeast(0)
            val paddedWidth = image.width + rowPadding / pixelStride
            val padded = Bitmap.createBitmap(paddedWidth, image.height, Bitmap.Config.ARGB_8888)
            plane.buffer.rewind()
            padded.copyPixelsFromBuffer(plane.buffer)
            val cropped = if (paddedWidth == image.width) {
                padded
            } else {
                Bitmap.createBitmap(padded, 0, 0, image.width, image.height).also { padded.recycle() }
            }

            val targetWidth = cropped.width.coerceAtMost(MAX_FRAME_WIDTH)
            val targetHeight = (cropped.height * (targetWidth.toFloat() / cropped.width))
                .toInt().coerceAtLeast(1)
            val frame = if (targetWidth != cropped.width || targetHeight != cropped.height) {
                Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true)
                    .also { cropped.recycle() }
            } else cropped
            NavigationScreenMirror.publishFrame(frame)
        } catch (_: Exception) {
            // A dropped frame is recoverable; the next ImageReader callback will retry.
        } finally {
            image.close()
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, NavigationScreenMirrorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("AUVIONYX navigation mirror active")
            .setContentText("Mirroring the navigation app window selected by you.")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Navigation screen mirror",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun stopCapture() {
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.setOnImageAvailableListener(null, null) }
        runCatching { imageReader?.close() }
        imageReader = null
        runCatching { projection?.stop() }
        projection = null
        workerThread?.quitSafely()
        workerThread = null
        worker = null
        NavigationScreenMirror.setRunning(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.auvionyx.action.START_NAVIGATION_MIRROR"
        const val ACTION_STOP = "com.example.auvionyx.action.STOP_NAVIGATION_MIRROR"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL_ID = "auvionyx_navigation_mirror"
        private const val NOTIFICATION_ID = 4107
        private const val FRAME_INTERVAL_MS = 500L
        private const val MAX_FRAME_WIDTH = 1600
    }
}
