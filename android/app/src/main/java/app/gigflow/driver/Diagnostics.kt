package app.gigflow.driver

import android.content.Context
import org.json.JSONObject

/**
 * Per-app diagnostic snapshots — what the assistant last saw in each watched
 * driver app. This is the user-facing debugging surface: when a screen looks
 * like an offer but doesn't parse, we keep the raw texts so the driver can
 * check "why nothing happened" instead of guessing.
 *
 * Stored in SharedPreferences as one JSON object keyed by package name.
 */
object Diagnostics {

    private const val FILE = "gigflow_diag"
    private const val KEY = "snaps"
    private const val LAST_SCORED_KEY = "last_scored"
    private const val MAX_TEXTS = 60

    // Throttle plain "idle" writes — only interesting outcomes and stale
    // snapshots get persisted, not every accessibility event.
    private const val IDLE_WRITE_MS = 30_000L

    data class Snap(
        val at: Long,
        val outcome: String, // "offer" | "miss" | "trip" | "ignored" | "idle"
        val texts: List<String>,
    )

    fun record(ctx: Context, pkg: String, outcome: String, texts: List<String>) {
        val prefs = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val all = JSONObject(prefs.getString(KEY, "{}") ?: "{}")
        val prev = all.optJSONObject(pkg)
        val joined = texts.take(MAX_TEXTS).joinToString("\n")
        // Skip repeat writes: idle is throttled, and an unchanged outcome
        // with identical texts doesn't need re-persisting every frame. An
        // empty stored snapshot (app was still on its splash) never blocks
        // real content, or the first 30s of every launch would look broken.
        if (prev != null &&
            System.currentTimeMillis() - prev.optLong("at") < IDLE_WRITE_MS &&
            (prev.optString("texts").isNotEmpty() || joined.isEmpty()) &&
            (outcome == "idle" ||
                (prev.optString("outcome") == outcome && prev.optString("texts") == joined))
        ) return
        val o = JSONObject()
            .put("at", System.currentTimeMillis())
            .put("outcome", outcome)
            .put("texts", joined)
        val edit = prefs.edit().putString(KEY, all.put(pkg, o).toString())
        // Preserve the last scored screen for false-positive reports.
        if (outcome == "offer") {
            val history = JSONObject(prefs.getString(LAST_SCORED_KEY, "{}") ?: "{}")
            edit.putString(LAST_SCORED_KEY, history.put(pkg, o).toString())
        }
        edit.apply()
    }

    fun getLastScored(ctx: Context, pkg: String): Snap? {
        val all = JSONObject(
            ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .getString(LAST_SCORED_KEY, "{}") ?: "{}",
        )
        val o = all.optJSONObject(pkg) ?: return null
        return Snap(
            at = o.optLong("at"),
            outcome = "offer",
            texts = o.optString("texts").split("\n").filter { it.isNotBlank() },
        )
    }

    fun get(ctx: Context, pkg: String): Snap? {
        val all = JSONObject(
            ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .getString(KEY, "{}") ?: "{}",
        )
        val o = all.optJSONObject(pkg) ?: return null
        return Snap(
            at = o.optLong("at"),
            outcome = o.optString("outcome", "idle"),
            texts = o.optString("texts").split("\n").filter { it.isNotBlank() },
        )
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .remove(KEY).remove(LAST_SCORED_KEY).apply()
    }
}
