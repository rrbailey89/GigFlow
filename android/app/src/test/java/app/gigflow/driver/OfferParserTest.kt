package app.gigflow.driver

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression tests for the Uber active-trip false positive. */
class OfferParserTest {
    private val uber = "com.ubercab.driver"
    private val lyft = "com.lyft.android.driver"

    @Test
    fun dropoffNavigationIsNotAnOfferEvenWithVisibleEarnings() {
        val texts = listOf("Monterey Ave", "1000 ft", "23 min", "14 mi",
            "Dropping off John", "Navigate", "$12.50")
        assertTrue(OfferParser.isActiveTripNavigation(uber, texts))
    }

    @Test
    fun pickupNavigationIsNotAnOffer() {
        val texts = listOf("Picking up passenger", "Navigate", "8 min", "3 mi", "$8")
        assertTrue(OfferParser.isActiveTripNavigation(lyft, texts))
    }

    @Test
    fun anotherRequestDuringActiveTripCanStillBeScored() {
        val texts = listOf("Dropping off John", "Navigate", "Trip Radar",
            "Accept", "$18.00", "5 mi", "12 min")
        assertFalse(OfferParser.isActiveTripNavigation(uber, texts))
    }

    @Test
    fun ordinaryOfferScreenIsStillEligible() {
        val texts = listOf("UberX", "$15.50", "4.2 mi", "17 min")
        assertFalse(OfferParser.isActiveTripNavigation(uber, texts))
    }

    @Test
    fun navigationWithoutActiveTripStatusIsNotSuppressed() {
        assertFalse(OfferParser.isActiveTripNavigation(uber, listOf("Navigate", "$12", "14 mi")))
    }

    @Test
    fun deliveryAppParsingIsNotAffected() {
        val texts = listOf("Dropping off John", "Navigate", "23 min", "$12")
        assertFalse(OfferParser.isActiveTripNavigation("com.doordash.driverapp", texts))
    }
}
