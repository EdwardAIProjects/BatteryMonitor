package dev.hydranet.batman

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryWarningDecisionTest {
    @Test
    fun thresholdToNotifyChoosesClosestCrossedLevel() {
        val threshold = BatteryWarningDecision.thresholdToNotify(
            batteryPercent = 17,
            thresholds = listOf(50, 20, 10),
            alreadyWarned = emptySet(),
        )

        assertEquals(20, threshold)
    }

    @Test
    fun warnedAfterNotificationSuppressesHigherCrossedLevels() {
        val warned = BatteryWarningDecision.warnedAfterNotification(
            batteryPercent = 17,
            thresholds = listOf(50, 20, 10),
            alreadyWarned = emptySet(),
        )

        assertEquals(setOf(50, 20), warned)
    }

    @Test
    fun resetWarnedForBatteryLevelClearsLevelsAfterChargingPastThem() {
        val warned = BatteryWarningDecision.resetWarnedForBatteryLevel(
            batteryPercent = 25,
            thresholds = listOf(50, 20, 10),
            alreadyWarned = setOf(50, 20, 10),
        )

        assertEquals(setOf(50), warned)
    }

    @Test
    fun thresholdToNotifyReturnsNullForAlreadyWarnedLevels() {
        val threshold = BatteryWarningDecision.thresholdToNotify(
            batteryPercent = 17,
            thresholds = listOf(50, 20, 10),
            alreadyWarned = setOf(50, 20),
        )

        assertNull(threshold)
    }
}
