package dev.hydranet.batman

object BatteryWarningDecision {
    fun thresholdToNotify(
        batteryPercent: Int,
        thresholds: List<Int>,
        alreadyWarned: Set<Int>,
    ): Int? {
        return thresholds
            .map { it.coerceIn(MIN_THRESHOLD, MAX_THRESHOLD) }
            .distinct()
            .filter { threshold -> batteryPercent <= threshold && threshold !in alreadyWarned }
            .minByOrNull { threshold -> threshold - batteryPercent }
    }

    fun resetWarnedForBatteryLevel(
        batteryPercent: Int,
        thresholds: List<Int>,
        alreadyWarned: Set<Int>,
    ): Set<Int> {
        val configured = thresholds.toSet()
        return alreadyWarned.filterTo(mutableSetOf()) { threshold ->
            threshold in configured && batteryPercent <= threshold
        }
    }

    fun warnedAfterNotification(
        batteryPercent: Int,
        thresholds: List<Int>,
        alreadyWarned: Set<Int>,
    ): Set<Int> {
        val warned = resetWarnedForBatteryLevel(batteryPercent, thresholds, alreadyWarned)
            .toMutableSet()

        thresholds
            .filter { threshold -> batteryPercent <= threshold }
            .forEach { threshold -> warned += threshold }

        return warned
    }

    const val MIN_THRESHOLD = 1
    const val MAX_THRESHOLD = 100
}
