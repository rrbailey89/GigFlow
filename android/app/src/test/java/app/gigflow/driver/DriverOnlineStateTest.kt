package app.gigflow.driver

import org.junit.Assert.assertEquals
import org.junit.Test

class DriverOnlineStateTest {
    private val uber = "com.ubercab.driver"
    private val lyft = "com.lyft.android.driver"

    @Test fun browsingOfflineUberDoesNotStartMileage() {
        val gate = DriverOnlineStateTracker()
        assertEquals(MileageTransition.NONE,
            gate.onScreen(uber, listOf("Discover", "Go online", "Reservations"),
                true, false, false))
    }

    @Test fun openingReservationDoesNotMeanOnline() {
        assertEquals(DriverAvailability.UNKNOWN,
            DriverOnlineStateDetector.detect(uber,
                listOf("Reservation request", "Be online by", "1:20 PM", "Accept")))
    }

    @Test fun confirmedOnlineStartsAutomaticShift() {
        val gate = DriverOnlineStateTracker()
        assertEquals(MileageTransition.START_AUTO,
            gate.onScreen(uber, listOf("Go offline", "Finding trips"), true, false, false))
    }

    @Test fun secondFrameDoesNotStartAgainAfterManualStop() {
        val gate = DriverOnlineStateTracker()
        gate.onScreen(uber, listOf("Go offline"), true, false, false)
        assertEquals(MileageTransition.NONE,
            gate.onScreen(uber, listOf("Go offline"), true, false, false))
    }

    @Test fun offlineTransitionStopsAutomaticallyStartedMileage() {
        val gate = DriverOnlineStateTracker()
        gate.onScreen(uber, listOf("You're online"), true, false, false)
        assertEquals(MileageTransition.STOP_AUTO,
            gate.onScreen(uber, listOf("Go online"), true, true, true))
    }

    @Test fun offlineNeverStopsManualShift() {
        val gate = DriverOnlineStateTracker()
        gate.onScreen(uber, listOf("Go offline"), true, true, false)
        assertEquals(MileageTransition.NONE,
            gate.onScreen(uber, listOf("You're offline"), true, true, false))
    }

    @Test fun onlineScreenNeverOverridesManuallyStartedMileage() {
        val gate = DriverOnlineStateTracker()
        assertEquals(MileageTransition.NONE,
            gate.onScreen(uber, listOf("Go offline"), true, true, false))
    }

    @Test fun autoTrackingDisabledNeverStarts() {
        val gate = DriverOnlineStateTracker()
        assertEquals(MileageTransition.NONE,
            gate.onScreen(uber, listOf("Go offline"), false, false, false))
    }

    @Test fun unknownStatusDoesNotStopAnExistingShift() {
        val gate = DriverOnlineStateTracker()
        gate.onScreen(uber, listOf("Go offline"), true, false, false)
        assertEquals(MileageTransition.NONE,
            gate.onScreen(uber, listOf("Map", "Navigation", "Trip details"), true, true, true))
    }

    @Test fun lyftOnlineKeepsShiftRunningWhenUberGoesOffline() {
        val gate = DriverOnlineStateTracker()
        gate.onScreen(uber, listOf("Go offline"), true, false, false)
        gate.onScreen(lyft, listOf("You’re online"), true, true, true)
        assertEquals(MileageTransition.NONE,
            gate.onScreen(uber, listOf("Go online"), true, true, true))
        assertEquals(MileageTransition.STOP_AUTO,
            gate.onScreen(lyft, listOf("You are offline"), true, true, true))
    }

    @Test fun unsupportedAppsCannotAutoStartFromGenericLabels() {
        val gate = DriverOnlineStateTracker()
        assertEquals(MileageTransition.NONE,
            gate.onScreen("com.amazon.rabbit", listOf("Go offline"), true, false, false))
    }

    @Test fun offlineThenOnlineCanStartANewShift() {
        val gate = DriverOnlineStateTracker()
        gate.onScreen(uber, listOf("Go offline"), true, false, false)
        gate.onScreen(uber, listOf("Go online"), true, true, true)
        assertEquals(MileageTransition.START_AUTO,
            gate.onScreen(uber, listOf("Finding trips"), true, false, false))
    }

    @Test fun resetAllowsRetriggerAfterEnablingSetting() {
        val gate = DriverOnlineStateTracker()
        gate.onScreen(uber, listOf("Go offline"), false, false, false)
        gate.reset()
        assertEquals(MileageTransition.START_AUTO,
            gate.onScreen(uber, listOf("Go offline"), true, false, false))
    }

    @Test fun typographicQuotesAndWhitespaceRecognized() {
        assertEquals(DriverAvailability.ONLINE,
            DriverOnlineStateDetector.detect(lyft, listOf("  You’re   online  ")))
    }
}
