package dev.plume

import android.content.Context
import java.io.File

/**
 * The "Shows from your apps" wallpaper: the picture of a show that an app publishes (Watch Next, or a home channel),
 * one at a time, changed every so often like a folder of photos. Nothing goes online but what the cards already
 * fetch from the apps' own addresses, and nothing happens unless someone picks it. One picture is kept on disk and
 * replaced by the next; another wallpaper deletes it.
 */
internal object ShowsWall {
    /** The wallpaper choice, next to "res:…", "file:…", "folder:…" and "bing". */
    const val CHOICE = "shows"

    fun file(c: Context) = File(c.filesDir, "shows.jpg")

    /**
     * Whether the app's art is wide enough for a wallpaper. A thumbnail is (apps give 16:9 there). Poster art says its
     * shape: 0 is 16:9, 1 is 3:2 and 2 is 4:3; 1:1 (3), 2:3 (4) and a movie poster (5) are not, they would be cropped to a strip.
     */
    fun wide(thumbnail: Boolean, ratio: Int) = thumbnail || ratio in 0..2

    /** What the picture itself must be: at least 480 px wide, between 1.2:1 and 2.5:1. Checked on the bytes, as the ratio an app gives can be wrong. */
    fun fits(w: Int, h: Int) = w >= 480 && h > 0 && w * 10 >= h * 12 && w * 10 <= h * 25

    /** [keys] in the order to try them: the one after [current] first, then on, and [current] last. An unknown [current] keeps the order. */
    fun order(keys: List<String>, current: String?): List<String> {
        val at = keys.indexOf(current)
        return if (at < 0) keys else keys.drop(at + 1) + keys.take(at + 1)
    }

    private const val TARGET = 0.30f // the mean brightness a picture is brought to in dark mode
    private const val MOST = 0.6f // the most black Auto lays over a picture

    /** Mean brightness, 0 to 1, of ARGB [pixels]. */
    fun meanLuma(pixels: IntArray): Float {
        if (pixels.isEmpty()) return 0f
        var sum = 0.0
        for (p in pixels) sum += 0.2126 * (p shr 16 and 255) + 0.7152 * (p shr 8 and 255) + 0.0722 * (p and 255)
        return (sum / pixels.size / 255.0).toFloat()
    }

    /** The alpha (0 to 255) of black that brings a picture of mean brightness [mean] down to [TARGET]: none for a picture that is darker already. */
    fun autoDim(mean: Float): Int = ((1f - TARGET / mean.coerceAtLeast(0.01f)).coerceIn(0f, MOST) * 255).toInt()

    private const val FLOOR = 0.22f // a picture darker than this is lifted towards it
    private const val LIFT_MAX = 0.2f // the most grey the lift adds to the shadows

    /** The grey (0 to 255) screened over a very dark picture of mean brightness [mean], so that its shadows are not lost: none from [FLOOR] up. */
    fun autoLift(mean: Float): Int = (((FLOOR - mean) / (1f - mean)).coerceIn(0f, LIFT_MAX) * 255).toInt()

    /** How long a picture that can never be used (not found, not wide, too big) is left alone: 6 hours. */
    const val DEAD_MS = 6 * 3_600_000L

    /** How long a picture that failed because of the network is left alone: 5 minutes. */
    const val NETWORK_MS = 5 * 60_000L

    /** Whether [key] is being left alone at [now], given when each failed picture may be tried again. */
    fun skipped(failedUntil: Map<String, Long>, key: String, now: Long) = (failedUntil[key] ?: 0L) > now
}
