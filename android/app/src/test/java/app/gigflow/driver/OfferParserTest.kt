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


/** Tests for strict Uber/Lyft request detection and false-positive prevention. */
class StrictRideshareOfferTest {
    private val uber = "com.ubercab.driver"
    private val lyft = "com.lyft.android.driver"

    @Test
    fun navigationFareAndMetricsAreNotEnough() {
        assertFalse(OfferParser.hasExplicitRideshareRequestCue(uber,
            listOf("Monterey Ave", "23 min", "14 mi", "Dropping off John", "Navigate", "$12.50")))
    }

    @Test
    fun acceptanceRateIsNotAnOfferAction() {
        val texts = listOf("Dropping off John", "Navigate", "Acceptance rate 93%", "$12.50", "23 min", "14 mi")
        assertFalse(OfferParser.hasExplicitRideshareRequestCue(uber, texts))
        assertTrue(OfferParser.isActiveTripNavigation(uber, texts))
    }

    @Test
    fun earningsReceiptIsNotAnOffer() {
        assertFalse(OfferParser.hasExplicitRideshareRequestCue(uber,
            listOf("UberX", "Trip details", "Pickup", "Dropoff", "$17.50", "14 mi", "23 min", "Earned")))
    }

    @Test
    fun tripRadarTabWithoutMatchIsNotAnOffer() {
        assertFalse(OfferParser.hasExplicitRideshareRequestCue(uber,
            listOf("Trip Radar", "$25.00", "5 mi", "20 min")))
    }

    @Test
    fun explicitAcceptButtonCounts() {
        assertTrue(OfferParser.hasExplicitRideshareRequestCue(uber,
            listOf("UberX", "Accept", "$16.50", "3 mi", "12 min")))
    }

    @Test
    fun tapAnywhereToAcceptCounts() {
        assertTrue(OfferParser.hasExplicitRideshareRequestCue(uber,
            listOf("Tap anywhere to accept", "UberX", "$12.00", "5 mi")))
    }

    @Test
    fun exclusiveRequestBadgeCounts() {
        assertTrue(OfferParser.hasExplicitRideshareRequestCue(uber,
            listOf("Exclusive", "UberX", "$19.50", "10 min", "3 mi")))
    }

    @Test
    fun newBackToBackRequestCountsDuringActiveTrip() {
        val texts = listOf("Dropping off John", "Navigate", "New trip request", "$24.00", "7 mi")
        assertTrue(OfferParser.hasExplicitRideshareRequestCue(uber, texts))
        assertFalse(OfferParser.isActiveTripNavigation(uber, texts))
    }

    @Test
    fun lyftMatchMeCounts() {
        assertTrue(OfferParser.hasExplicitRideshareRequestCue(lyft,
            listOf("Match me", "$19.50", "10 min", "3 mi")))
    }

    @Test
    fun foodDeliveryBehaviorNotChanged() {
        assertTrue(OfferParser.hasExplicitRideshareRequestCue(
            "com.doordash.driverapp", listOf("$14.00", "4 mi", "12 min")))
    }
}
