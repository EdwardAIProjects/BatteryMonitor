package dev.hydranet.batman

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import androidx.core.content.ContextCompat

object BatteryWarningNotifier {
    private const val CHANNEL_ID = "battery_warnings"
    private const val CHANNEL_NAME = "Battery Notifier"
    private const val NOTIFICATION_ID = 1001

    fun checkAndNotify(context: Context): Int? {
        BatteryWarningStore.ensureInitialized(context)
        if (!BatteryWarningStore.isMonitoringEnabled(context)) return null

        createNotificationChannel(context)

        val batteryPercent = currentBatteryPercent(context) ?: return null
        val thresholds = BatteryWarningStore.getThresholds(context)
        val warned = BatteryWarningDecision.resetWarnedForBatteryLevel(
            batteryPercent = batteryPercent,
            thresholds = thresholds,
            alreadyWarned = BatteryWarningStore.getWarnedThresholds(context),
        )
        BatteryWarningStore.saveWarnedThresholds(context, warned)

        val threshold = BatteryWarningDecision.thresholdToNotify(
            batteryPercent = batteryPercent,
            thresholds = thresholds,
            alreadyWarned = warned,
        ) ?: return null

        if (!canPostNotifications(context)) return null

        postNotification(
            context = context,
            batteryPercent = batteryPercent,
            threshold = threshold,
        )
        BatteryWarningStore.saveWarnedThresholds(
            context = context,
            thresholds = BatteryWarningDecision.warnedAfterNotification(
                batteryPercent = batteryPercent,
                thresholds = thresholds,
                alreadyWarned = warned,
            ),
        )

        return threshold
    }

    fun currentBatteryPercent(context: Context): Int? {
        val batteryManager = context.getSystemService(BatteryManager::class.java)
        val propertyLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (propertyLevel in 0..100) return propertyLevel

        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

        if (level < 0 || scale <= 0) return null
        return ((level * 100f) / scale).toInt().coerceIn(0, 100)
    }

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Battery level warning notifications"
            setShowBadge(false)
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun canPostNotifications(context: Context): Boolean {
        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        return permissionGranted &&
            context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    }

    private fun postNotification(
        context: Context,
        batteryPercent: Int,
        threshold: Int,
    ) {
        val launchIntent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_battery_alert)
            .setContentTitle("Battery is $batteryPercent%")
            .setContentText("Dropped below your $threshold% warning level.")
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }
}
