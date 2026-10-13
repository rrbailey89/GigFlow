package app.gigflow.driver

import java.util.Locale

/**
 * Conservative, on-device view of a driver's availability.
 *
 * Opening a driver app, viewing a reservation, or seeing "Go online" means
 * NOTHING about being ready to accept rides. Only exact online-state labels
 * in recognized driver apps can start automatic GPS tracking.
 *
 * These strings vary with app versions and locale. Unknown always means
 * "don't start"; manual Start shift remains available everywhere.
 */
enum class DriverAvailability { ONLINE, OFFLINE, UNKNOWN }
enum class MileageTransition { START_AUTO, STOP_AUTO, NONE }

object DriverOnlineStateDetector {
    private val ridesharePackages = setOf("com.ubercab.driver", "com.lyft.android.driver")

    private val onlineLabels = setOf(
        "go offline",
        "tap to go offline",
        "you're online",
        "you are online",
        "you're currently online",
        "finding trips",
        "finding rides",
        "looking for trips",
        "looking for rides",
        "searching for trips",
        "receiving trip requests",
        "accepting trip requests",
    )
    private val offlineLabels = setOf(
        "go online",
        "tap to go online",
        "you're offline",
        "you are offline",
        "currently offline",
        "offline",
    )

    fun detect(pkg: String, texts: List<String>): DriverAvailability {
        if (pkg !in ridesharePackages) return DriverAvailability.UNKNOWN
        // Normalize typographic apostrophes, whitespace and case, but don't
        // match substrings like "Be online by 1:20 PM" or "Go online to ...".
        val labels = texts.map { it.trim().lowercase(Locale.ROOT)
            .replace('’', '\'').replace(Regex("\\s+"), " ") }.toSet()
        return when {
            labels.any { it in offlineLabels } -> DriverAvailability.OFFLINE
            labels.any { it in onlineLabels } -> DriverAvailability.ONLINE
            else -> DriverAvailability.UNKNOWN
        }
    }
}

/**
 * One transition per explicit online/offline change. This prevents an
 * online screen from re-starting GPS after the user presses Stop shift.
 * Unknown screens (trip details, external navigation) never trigger either
 * action, and only automatically started sessions may be auto-stopped.
 */
class DriverOnlineStateTracker {
    private val known = mutableMapOf<String, DriverAvailability>()

    fun reset() = known.clear()

    fun onScreen(
        pkg: String,
        texts: List<String>,
        autoEnabled: Boolean,
        mileageRunning: Boolean,
        mileageAutomatic: Boolean,
    ): MileageTransition {
        val state = DriverOnlineStateDetector.detect(pkg, texts)
        if (state == DriverAvailability.UNKNOWN) return MileageTransition.NONE
        if (known[pkg] == state) return MileageTransition.NONE

        known[pkg] = state
        return when {
            state == DriverAvailability.ONLINE && autoEnabled && !mileageRunning ->
                MileageTransition.START_AUTO
            state == DriverAvailability.OFFLINE && mileageAutomatic &&
                known.values.none { it == DriverAvailability.ONLINE } ->
                MileageTransition.STOP_AUTO
            else -> MileageTransition.NONE
        }
    }
}
