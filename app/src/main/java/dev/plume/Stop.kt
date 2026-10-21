package dev.plume

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo

/**
 * Ends other apps to free memory. Android 14 lets an app end only itself (ending others takes a system-only
 * permission), so the work is done the way a person would: [HomeOverride] opens each app's page in Settings and
 * taps Force stop, behind a cover that shows the progress. See [HomeOverride.stopApps].
 */
object Stop {
    private val NAME = Regex("[A-Za-z0-9._]+")

    private fun others(c: Context, vararg categories: String): List<String> = categories.flatMap { cat ->
        c.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(cat), 0).map { it.activityInfo.packageName }
    }.filter { it != c.packageName }.distinct()

    /** Home screens other than Plume: the Google TV one on a Google TV. */
    fun homes(c: Context) = others(c, Intent.CATEGORY_HOME)

    /** Every app Plume could list, home screens included, as (name, package): what can be picked and what "stop all" ends. */
    fun candidates(c: Context): List<Pair<String, String>> = others(c, Intent.CATEGORY_HOME, Intent.CATEGORY_LEANBACK_LAUNCHER).map { p ->
        val name = try {
            c.packageManager.getApplicationLabel(c.packageManager.getApplicationInfo(p, 0)).toString()
        } catch (e: Exception) {
            p
        }
        name to p
    }.sortedBy { it.first.lowercase() }

    /** An app already ended stays ended until someone opens it, so it is left alone. */
    private fun stopped(c: Context, pkg: String) = try {
        c.packageManager.getApplicationInfo(pkg, 0).flags and ApplicationInfo.FLAG_STOPPED != 0
    } catch (e: Exception) {
        true
    }

    /** Hands [pkgs] to the service that does the tapping. False when that service is off or can't read Settings yet. */
    fun stop(c: Context, pkgs: Collection<String>): Boolean {
        val service = HomeOverride.running?.takeIf { it.canTap() } ?: return false
        service.stopApps(pkgs.filter { it != c.packageName && NAME.matches(it) && !stopped(c, it) }.distinct())
        return true
    }

    fun all(c: Context) = stop(c, candidates(c).map { it.second })
}
