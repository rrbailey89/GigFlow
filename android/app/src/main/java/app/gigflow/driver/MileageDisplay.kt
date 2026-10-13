package app.gigflow.driver

import java.util.Locale

/** Single source of truth for mileage notification units and shift source. */
object MileageDisplay {
    fun label(
        distanceKm: Double,
        unitPreference: String,
        automatic: Boolean,
        locale: Locale = Locale.US,
    ): String {
        val useKm = unitPreference == "KM"
        val distance = if (useKm) distanceKm else distanceKm * 0.621371
        val unit = if (useKm) "km" else "mi"
        val source = if (automatic) "Auto" else "Manual"
        return String.format(locale, "%.1f %s so far · %s shift", distance, unit, source)
    }
}
