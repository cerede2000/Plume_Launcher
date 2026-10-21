package dev.plume

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.ActivityManager
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

/**
 * Google TV's launcher has to stay enabled (disabling it boot-loops the TV), so we take the Home key before
 * the system sees it and open our home directly; Google TV never flashes up. Holding Home still opens the
 * Google TV dashboard. Covering Google TV whenever it shows stays as the fallback for anything else that
 * opens it (e.g. Back out of the last app).
 */
class HomeOverride : AccessibilityService() {
    override fun onServiceConnected() {
        running = this
    }

    /** Turned off, from Plume's switch or Android's Accessibility settings: the app mustn't turn it back on. */
    override fun onUnbind(intent: Intent?): Boolean {
        running = null
        getSharedPreferences("home", MODE_PRIVATE).edit().putBoolean("home_off", true).apply()
        return super.onUnbind(intent)
    }

    override fun onKeyEvent(e: KeyEvent): Boolean {
        if (stopping) { // the cover is up: no key reaches Settings behind it; Back cancels
            if (e.keyCode == KeyEvent.KEYCODE_BACK && e.action == KeyEvent.ACTION_UP) cancelled = true
            return true
        }
        if (e.keyCode != KeyEvent.KEYCODE_HOME) return remap(e)
        // Decided on release, by how long it was held: this TV's remote doesn't reliably send key repeats.
        if (e.action == KeyEvent.ACTION_UP && !e.isCanceled) {
            if (e.eventTime - e.downTime < LONG_PRESS_MS) goHome() else performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        }
        return true
    }

    /** Keys the user changed in Plume settings (see [Remap]); any other key goes on to the system untouched. */
    private fun remap(e: KeyEvent): Boolean {
        if (Remap.kept(e)) return false
        val id = Remap.id(e)
        if (id == Remap.pending) { // the key just learned: its repeats and release never reach anyone
            if (e.action == KeyEvent.ACTION_UP) Remap.pending = null
            return true
        }
        Remap.learn?.let { learn ->
            if (e.action == KeyEvent.ACTION_DOWN && e.repeatCount == 0) {
                Remap.pending = id
                learn(e)
            }
            return true
        }
        val m = Remap.all(this)[id] ?: return false
        // Decided on release, like Home: this TV's remote doesn't reliably send key repeats.
        if (e.action == KeyEvent.ACTION_UP && !e.isCanceled) {
            val action = if (e.eventTime - e.downTime >= LONG_PRESS_MS && m.long.isNotEmpty()) m.long else m.short
            if (action.isNotEmpty()) Remap.run(this, action)
        }
        return true
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        if (e.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && e.packageName == GOOGLE_TV &&
            e.className?.contains("dashboard", ignoreCase = true) != true
        ) goHome()
    }

    // Marked as Home so the setup check can tell it from Plume being opened from the apps list.
    private fun goHome() = startActivity(Intent(this, MainActivity::class.java).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    override fun onInterrupt() {}

    // ---------- Memory: ending other apps (see Stop) ----------
    // Android 14 lets an app end only itself, so this does what a person would: opens the app's page in Settings,
    // taps Force stop, then OK. A black cover over it all hides the pages going by and shows the progress.

    private val ui = Handler(Looper.getMainLooper())
    private var stopping = false
    private var cancelled = false
    private var cover: View? = null
    private var coverTitle: TextView? = null
    private var coverLine: TextView? = null
    private var coverBar: ProgressBar? = null

    /** The service can read Settings' screens only once the user has (re)enabled it with that capability. */
    fun canTap() = (serviceInfo?.capabilities ?: 0) and AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT != 0

    private fun freeKb() = ActivityManager.MemoryInfo().also { getSystemService(ActivityManager::class.java).getMemoryInfo(it) }.availMem / 1024

    private fun label(pkg: String) = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }

    private fun showCover() {
        val d = resources.displayMetrics.density
        val title = TextView(this).apply {
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            text = getString(R.string.freeing_memory)
        }
        val line = TextView(this).apply {
            textSize = 16f
            setTextColor(0xFFB0B4BA.toInt())
            gravity = Gravity.CENTER
            setPadding(0, (12 * d).toInt(), 0, (18 * d).toInt())
        }
        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(title)
            addView(line)
            addView(bar, LinearLayout.LayoutParams((360 * d).toInt(), (6 * d).toInt()))
        }
        val root = FrameLayout(this).apply {
            setBackgroundColor(0xFA000000.toInt())
            addView(box, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        getSystemService(WindowManager::class.java).addView(root, lp)
        cover = root
        coverTitle = title
        coverLine = line
        coverBar = bar
    }

    private fun hideCover() {
        cover?.let { getSystemService(WindowManager::class.java).removeView(it) }
        cover = null
        stopping = false
    }

    /** Ends each of [pkgs], showing the progress; at the end, how many Mb that freed. Back on the remote cancels. */
    fun stopApps(pkgs: List<String>) {
        if (stopping || pkgs.isEmpty()) return
        stopping = true
        cancelled = false
        val before = freeKb()
        var ended = 0
        showCover()
        coverBar?.max = pkgs.size
        fun finish() {
            goHome()
            coverLine?.text = ""
            ui.postDelayed({ // what an ended app gives back shows a moment later
                val freed = ((freeKb() - before) / 1024).coerceAtLeast(0)
                coverTitle?.text = if (ended == 0) getString(R.string.freed_none) else getString(R.string.freed_result, ended, freed)
                coverBar?.progress = pkgs.size
                ui.postDelayed({ hideCover() }, 3000)
            }, 2500)
        }
        fun step(i: Int) {
            if (cancelled || i >= pkgs.size) return finish()
            coverLine?.text = "${i + 1} / ${pkgs.size} · ${label(pkgs[i])}"
            coverBar?.progress = i
            endOne(pkgs[i]) { ok ->
                if (ok) ended++
                step(i + 1)
            }
        }
        step(0)
    }

    /** The texts Settings itself shows, in the TV's language: read from its own resources. */
    private fun texts(pkg: String, vararg names: String): List<String> = try {
        val r = packageManager.getResourcesForApplication(pkg)
        names.mapNotNull { n -> r.getIdentifier(n, "string", pkg).takeIf { it != 0 }?.let { r.getString(it) } }
    } catch (e: Exception) {
        emptyList()
    }

    private fun endOne(pkg: String, done: (Boolean) -> Unit) {
        val page = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        val settings = packageManager.resolveActivity(page, 0)?.activityInfo?.packageName ?: return done(false)
        val stop = texts(settings, "device_apps_app_management_force_stop", "force_stop") + "Force stop"
        val ok = texts(settings, "ok", "okay") + Resources.getSystem().getString(android.R.string.ok) + "OK"
        try {
            startActivity(page)
        } catch (e: Exception) {
            return done(false)
        }
        poll(34, 150, { tap(settings, stop) }) { clicked ->
            if (clicked != true) return@poll done(false)
            poll(20, 150, { tap(settings, ok) }) { confirmed -> ui.postDelayed({ done(confirmed == true) }, 700) }
        }
    }

    /** Tries [attempt] every [gap] ms: null means not there yet, anything else ends it. */
    private fun poll(tries: Int, gap: Long, attempt: () -> Boolean?, then: (Boolean?) -> Unit) {
        if (cancelled) return then(false)
        val r = attempt()
        if (r != null) return then(r)
        if (tries <= 1) return then(null)
        ui.postDelayed({ poll(tries - 1, gap, attempt, then) }, gap)
    }

    /** Taps the control whose text is exactly one of [texts] in the window of [pkg]: true tapped, false disabled, null not there. */
    private fun tap(pkg: String, texts: Collection<String>): Boolean? {
        val root = rootInActiveWindow?.takeIf { it.packageName == pkg } ?: return null
        for (t in texts) for (n in root.findAccessibilityNodeInfosByText(t)) {
            if (n.text?.toString()?.trim().equals(t.trim(), ignoreCase = true).not()) continue
            var x: AccessibilityNodeInfo? = n
            while (x != null && !x.isClickable) x = x.parent
            if (x == null) continue
            return x.isEnabled && x.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        return null
    }

    companion object {
        /** The bound service, so Plume's switch can turn it off without any permission. */
        var running: HomeOverride? = null
            private set
        private const val GOOGLE_TV = "com.google.android.apps.tv.launcherx"
        private const val LONG_PRESS_MS = 500L
    }
}
