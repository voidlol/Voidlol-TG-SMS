package ru.voidlol.tgsms.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryAlertPolicyTest {
    @Test
    fun alertsAtThresholdWhenDischarging() {
        assertTrue(
            BatteryAlertPolicy.shouldAlert(
                batteryPercent = 30,
                isCharging = false,
                thresholdPercent = 30,
                lastAlertedPercent = null
            )
        )
    }

    @Test
    fun doesNotAlertAboveThreshold() {
        assertFalse(
            BatteryAlertPolicy.shouldAlert(
                batteryPercent = 31,
                isCharging = false,
                thresholdPercent = 30,
                lastAlertedPercent = null
            )
        )
    }

    @Test
    fun doesNotAlertWhileCharging() {
        assertFalse(
            BatteryAlertPolicy.shouldAlert(
                batteryPercent = 30,
                isCharging = true,
                thresholdPercent = 30,
                lastAlertedPercent = null
            )
        )
    }

    @Test
    fun waitsForTwoPercentDropBeforeNextAlert() {
        assertFalse(
            BatteryAlertPolicy.shouldAlert(
                batteryPercent = 29,
                isCharging = false,
                thresholdPercent = 30,
                lastAlertedPercent = 30
            )
        )
        assertTrue(
            BatteryAlertPolicy.shouldAlert(
                batteryPercent = 28,
                isCharging = false,
                thresholdPercent = 30,
                lastAlertedPercent = 30
            )
        )
    }

    @Test
    fun resetsWhenChargingOrBackAboveThreshold() {
        assertTrue(BatteryAlertPolicy.shouldReset(batteryPercent = 28, isCharging = true, thresholdPercent = 30))
        assertTrue(BatteryAlertPolicy.shouldReset(batteryPercent = 31, isCharging = false, thresholdPercent = 30))
        assertFalse(BatteryAlertPolicy.shouldReset(batteryPercent = 28, isCharging = false, thresholdPercent = 30))
    }
}
