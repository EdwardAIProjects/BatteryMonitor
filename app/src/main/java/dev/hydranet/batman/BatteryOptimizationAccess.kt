package dev.hydranet.batman

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

object BatteryOptimizationAccess {
    fun isIgnoringOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java)
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    fun settingsIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }
}
