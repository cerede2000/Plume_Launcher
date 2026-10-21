package dev.plume

import android.app.ActivityManager
import android.content.Context
import android.content.Intent

/**
 * Ends other apps' background processes to free memory. An app can't force-stop another one (that is a system
 * permission), but KILL_BACKGROUND_PROCESSES, an ordinary permission, ends what Android would end itself when memory
 * runs low. What plays or shows is left alone, and some apps start again by themselves a little later.
 */
object Stop {
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

    fun kill(c: Context, pkgs: Collection<String>) {
        val am = c.getSystemService(ActivityManager::class.java)
        for (p in pkgs) if (p != c.packageName) am.killBackgroundProcesses(p)
    }

    fun all(c: Context) = kill(c, candidates(c).map { it.second })
}
