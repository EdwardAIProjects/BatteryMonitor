package dev.hydranet.batman

import android.content.Context

object BatteryWarningStore {
    private const val PREFS_NAME = "battery_warning_settings"
    private const val KEY_THRESHOLDS = "thresholds"
    private const val KEY_WARNED_THRESHOLDS = "warned_thresholds"
    private const val KEY_MONITORING_ENABLED = "monitoring_enabled"

    private val defaultThresholds = listOf(20, 10)

    fun ensureInitialized(context: Context) {
        val prefs = preferences(context)
        if (!prefs.contains(KEY_THRESHOLDS)) {
            prefs.edit()
                .putString(KEY_THRESHOLDS, encode(defaultThresholds))
                .apply()
        }
    }

    fun getThresholds(context: Context): List<Int> {
        ensureInitialized(context)
        return decode(preferences(context).getString(KEY_THRESHOLDS, null))
            .sortedDescending()
    }

    fun isMonitoringEnabled(context: Context): Boolean {
        return preferences(context).getBoolean(KEY_MONITORING_ENABLED, true)
    }

    fun setMonitoringEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit()
            .putBoolean(KEY_MONITORING_ENABLED, enabled)
            .apply()
    }

    fun addThreshold(context: Context, threshold: Int): List<Int> {
        val next = (getThresholds(context) + threshold.coerceThreshold())
            .distinct()
            .sortedDescending()
        saveThresholds(context, next)
        return next
    }

    fun removeThreshold(context: Context, threshold: Int): List<Int> {
        val next = getThresholds(context)
            .filterNot { it == threshold }
            .sortedDescending()
        val warned = getWarnedThresholds(context) - threshold

        preferences(context).edit()
            .putString(KEY_THRESHOLDS, encode(next))
            .putString(KEY_WARNED_THRESHOLDS, encode(warned))
            .apply()

        return next
    }

    fun getWarnedThresholds(context: Context): Set<Int> {
        return decode(preferences(context).getString(KEY_WARNED_THRESHOLDS, null)).toSet()
    }

    fun saveWarnedThresholds(context: Context, thresholds: Set<Int>) {
        preferences(context).edit()
            .putString(KEY_WARNED_THRESHOLDS, encode(thresholds))
            .apply()
    }

    private fun saveThresholds(context: Context, thresholds: List<Int>) {
        preferences(context).edit()
            .putString(KEY_THRESHOLDS, encode(thresholds.map { it.coerceThreshold() }.distinct()))
            .apply()
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun encode(values: Iterable<Int>): String {
        return values
            .map { it.coerceThreshold() }
            .distinct()
            .sortedDescending()
            .joinToString(separator = ",")
    }

    private fun decode(value: String?): List<Int> {
        return value
            ?.split(",")
            ?.mapNotNull { raw -> raw.toIntOrNull()?.coerceThreshold() }
            ?.distinct()
            .orEmpty()
    }

    private fun Int.coerceThreshold(): Int {
        return coerceIn(BatteryWarningDecision.MIN_THRESHOLD, BatteryWarningDecision.MAX_THRESHOLD)
    }
}
