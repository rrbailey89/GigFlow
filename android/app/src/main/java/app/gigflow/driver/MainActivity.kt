package app.gigflow.driver

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.ViewGroup
import android.widget.FrameLayout
import app.gigflow.driver.ui.*

/**
 * GigFlow Driver — iOS-styled companion.
 *
 * Five tabs hosted in a single activity (no fragments, no deps):
 *   Dashboard · Offers · Track · Plan · Assistant
 *
 * The accessibility service runs independently of this UI — see
 * GigFlowAccessibilityService.
 */
class MainActivity : Activity() {

    companion object {
        const val REQ_LOCATION = 7
    }

    private lateinit var settings: SettingsRepository
    private lateinit var db: LocalDb
    private lateinit var p: Ios.Palette

    private lateinit var dashboard: DashboardScreen
    private lateinit var offers: OffersScreen
    private lateinit var track: TrackScreen
    private lateinit var plan: PlanScreen
    private lateinit var assist: AssistScreen

    private lateinit var content: FrameLayout
    private lateinit var tabBar: TabBar
    private var current = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsRepository(this)
        db = LocalDb(this)
        p = Ios.palette(this)

        dashboard = DashboardScreen(this, p, db, settings)
        offers = OffersScreen(this, p, settings)
        track = TrackScreen(this, p, db, settings)
        plan = PlanScreen(this, p, db, settings)
        assist = AssistScreen(this, p, settings, db)

        content = FrameLayout(this)
        tabBar = TabBar(this, p)
        tabBar.setTabs(listOf(
            TabBar.Tab("house", "Home"),
            TabBar.Tab("tag", "Offers"),
            TabBar.Tab("plus.circle", "Track"),
            TabBar.Tab("calendar", "Plan"),
            TabBar.Tab("slider", "Assist"),
        ))
        tabBar.onSelect = { show(it) }

        val root = FrameLayout(this).apply {
            addView(content, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            addView(tabBar, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            setBackgroundColor(p.groupedBg)
        }
        setContentView(root)

        // Edge-to-edge content while KEEPING Android's system navigation
        // visible. GigFlow's tab bar is not a substitute for the OS bar.
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.show(WindowInsets.Type.navigationBars())
            root.setOnApplyWindowInsetsListener { _, insets ->
                val sys = insets.getInsets(WindowInsets.Type.systemBars())
                content.setPadding(0, sys.top, 0, 0)
                tabBar.setPadding(0, 0, 0, sys.bottom)
                insets
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }

        show(0)
        UpdateChecker.promptIfNewer(this, p)
    }

    private fun show(i: Int) {
        current = i
        val target = when (i) {
            0 -> dashboard.screen
            1 -> offers.screen
            2 -> track.screen
            3 -> plan.screen
            else -> assist.screen
        }
        if (target.parent == content) return
        content.removeAllViews()
        target.alpha = 0f
        content.addView(target, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        target.animate().alpha(1f).setDuration(Ios.ANIM_FAST).start()
        refresh(i)
    }

    private fun refresh(i: Int): Unit {
        Ios.hapticsEnabled = settings.haptics
        return when (i) {
            0 -> dashboard.refresh()
            1 -> offers.refresh()
            2 -> track.refresh()
            3 -> plan.refresh()
            else -> assist.refresh()
        }
    }

    override fun onResume() {
        super.onResume()
        // Reassert OS navigation visibility after returning from another
        // activity (e.g. the installer, an accessibility settings screen).
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.show(WindowInsets.Type.navigationBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
        if (::tabBar.isInitialized) refresh(current)
    }

    /** Location permission result — granted means the user tapped Start shift. */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LOCATION) {
            if (MileageTracker.hasLocationPermission(this)) {
                startForegroundService(MileageTracker.startIntent(this))
            }
            if (::track.isInitialized) track.refresh()
        }
    }
}
