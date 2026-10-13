package app.gigflow.driver

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class MileageDisplayTest {
    @Test fun milesSelectedConvertsKilometres() {
        assertEquals("1.0 mi so far · Manual shift",
            MileageDisplay.label(1.609344, "MI", false, Locale.US))
    }

    @Test fun kilometresSelectedStayMetric() {
        assertEquals("1.6 km so far · Auto shift",
            MileageDisplay.label(1.609344, "KM", true, Locale.US))
    }

    @Test fun initialNotificationStartsAtZeroInMiles() {
        assertEquals("0.0 mi so far · Auto shift",
            MileageDisplay.label(0.0, "MI", true, Locale.US))
    }

    @Test fun switchingUnitsChangesLabel() {
        val km = 8.04672
        assertEquals("5.0 mi so far · Auto shift",
            MileageDisplay.label(km, "MI", true, Locale.US))
        assertEquals("8.0 km so far · Auto shift",
            MileageDisplay.label(km, "KM", true, Locale.US))
    }
}
