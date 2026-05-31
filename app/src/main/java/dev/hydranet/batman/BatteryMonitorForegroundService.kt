package dev.hydranet.batman

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.content.ContextCompat

class BatteryMonitorForegroundService : Service() {
    private var batteryReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        if (!BatteryWarningStore.isMonitoringEnabled(this)) {
            stopSelf()
            return
        }

        createMonitorChannel()
        startForeground(
            MONITOR_NOTIFICATION_ID,
            monitorNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        registerBatteryReceiver()
        BatteryWarningNotifier.checkAndNotify(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!BatteryWarningStore.isMonitoringEnabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        BatteryWarningNotifier.checkAndNotify(this)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        batteryReceiver?.let(::unregisterReceiver)
        batteryReceiver = null
        super.onDestroy()
    }

    private fun registerBatteryReceiver() {
        if (batteryReceiver != null) return

        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                BatteryWarningNotifier.checkAndNotify(context)
            }
        }

        ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun monitorNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return Notification.Builder(this, MONITOR_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_battery_alert)
            .setContentTitle("Battery Notifier is active")
            .setContentText("Monitoring battery levels in the background.")
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    private fun createMonitorChannel() {
        val channel = NotificationChannel(
            MONITOR_CHANNEL_ID,
            "Background monitor",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Persistent notification for background battery monitoring"
            setShowBadge(false)
        }

        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val MONITOR_CHANNEL_ID = "battery_monitor"
        private const val MONITOR_NOTIFICATION_ID = 1002

        fun start(context: Context): Boolean {
            if (!BatteryWarningStore.isMonitoringEnabled(context)) return false

            return try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, BatteryMonitorForegroundService::class.java),
                )
                true
            } catch (_: IllegalStateException) {
                false
            } catch (_: SecurityException) {
                false
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BatteryMonitorForegroundService::class.java))
        }
    }
}
