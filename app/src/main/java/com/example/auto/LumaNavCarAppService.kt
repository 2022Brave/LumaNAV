package com.example.auto

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import androidx.core.app.NotificationCompat

class LumaNavCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            HostValidator.Builder(this)
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build()
        }
    }

    override fun onCreateSession(): Session = LumaNavCarSession()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_NAVIGATION -> {
                val destination = intent.getStringExtra(EXTRA_DESTINATION).orEmpty()
                ensureNotificationChannel()
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_menu_directions)
                    .setContentTitle("LumaNAV navigation active")
                    .setContentText(
                        destination.takeIf { it.isNotBlank() }?.let { "Guidance to $it" }
                            ?: "Turn-by-turn guidance is active"
                    )
                    .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }

            ACTION_STOP_NAVIGATION -> {
                stopForeground(Service.STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "LumaNAV navigation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing notification while LumaNAV provides car navigation."
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }

    companion object {
        const val ACTION_START_NAVIGATION = "com.lumanav.navigation.auto.START_NAVIGATION"
        const val ACTION_STOP_NAVIGATION = "com.lumanav.navigation.auto.STOP_NAVIGATION"
        const val EXTRA_DESTINATION = "destination_name"

        private const val CHANNEL_ID = "lumanav_car_navigation"
        private const val NOTIFICATION_ID = 4101
    }
}
