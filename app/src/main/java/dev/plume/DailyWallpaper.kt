package dev.plume

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** What a source says about its newest picture. [token] changes when the picture does. */
internal class Latest(val token: String, val title: String, val url: String)

/**
 * A place that publishes a new wallpaper every now and then. To add one, write an object like [Bing] and put it in
 * [DailyWallpaper.SOURCES]; give it a name in strings.xml. The rest is shared: the download, one JPEG on disk per
 * source, one lookup a day, the entry in the wallpaper list, the screen-sized picture, the dimming in dark mode
 * and the clean-up when another wallpaper is chosen.
 */
internal interface WallpaperSource {
    val id: String

    /** String resource: the name shown in the wallpaper list. */
    val name: Int

    /** Blocking, for a worker thread. Asks for a picture of about [width]×[height]. Throws if the source fails. */
    fun latest(width: Int, height: Int): Latest
}

/** The wallpaper choice for a source, next to "res:…", "file:…" and "folder:…". */
internal val WallpaperSource.choice get() = "daily:$id"

/** The preference that holds one fact about a source: "hash", "title", "day" (last lookup) or "try". */
internal fun WallpaperSource.pref(what: String) = "daily_${id}_$what"

/** The Bing picture of the day. Asked at the screen's size, Bing sends a 4K picture of ~950 KB. */
internal object Bing : WallpaperSource {
    override val id = "bing"
    override val name = R.string.bing_daily

    override fun latest(width: Int, height: Int): Latest {
        val l = Locale.getDefault()
        val market = if (l.country.isEmpty()) "en-US" else "${l.language}-${l.country}"
        val image = JSONObject(DailyWallpaper.connect("https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&uhd=1&uhdwidth=$width&uhdheight=$height&mkt=$market")
            .inputStream.use { it.readBytes().decodeToString() }).getJSONArray("images").getJSONObject(0)
        // The size is part of the token, so a bigger screen fetches again.
        return Latest(image.getString("hsh") + width, image.optString("title"), "https://www.bing.com" + image.getString("url"))
    }
}

/**
 * The shared part of internet wallpapers. One JPEG per source, replaced in place when the picture changes, so no old
 * picture is ever kept. Nothing runs in the background: the home screen asks for a lookup at most once a day. The work
 * runs on a thread of its own that lives only while there is something to do, and it doesn't depend on the home
 * screen, which Android may recreate (theme change) while a picture downloads.
 */
internal object DailyWallpaper {
    val SOURCES: List<WallpaperSource> = listOf(Bing)

    /** Called on the main thread when a new picture is on disk. The home screen sets it while it is on screen. */
    @Volatile
    var onChanged: (() -> Unit)? = null

    private val main = Handler(Looper.getMainLooper())
    private val worker = ThreadPoolExecutor(0, 1, 5, TimeUnit.SECONDS, LinkedBlockingQueue())
    private val inFlight = HashSet<String>()
    private val lock = Any() // a download finishing and a wallpaper being dropped never overlap

    fun sourceOf(choice: String) = SOURCES.firstOrNull { it.choice == choice }

    /** The sources a theme uses, read from the same preferences as the wallpaper choice itself. */
    fun used(prefs: SharedPreferences) =
        SOURCES.filter { prefs.getString("wp_light", null) == it.choice || prefs.getString("wp_dark", null) == it.choice }

    fun file(c: Context, s: WallpaperSource) = File(c.filesDir, "daily_${s.id}.jpg")

    /** The biggest size that fits the screen: 1080p, 1440p or 4K. */
    fun size(widthPx: Int) = when {
        widthPx >= 3000 -> 3840 to 2160
        widthPx >= 2200 -> 2560 to 1440
        else -> 1920 to 1080
    }

    /**
     * Looks up [s] on the worker thread and saves its picture unless it is the one on disk already. A source that is
     * being looked up is left alone. Failures are only logged: yesterday's picture stays.
     */
    fun schedule(c: Context, prefs: SharedPreferences, s: WallpaperSource, widthPx: Int, today: Long) {
        synchronized(inFlight) { if (!inFlight.add(s.id)) return }
        worker.execute {
            try {
                val got = download(c, s, widthPx, prefs.getString(s.pref("hash"), null))
                synchronized(lock) {
                    if (s !in used(prefs)) file(c, s).delete() // another wallpaper was chosen meanwhile: keep nothing
                    else {
                        prefs.edit().putLong(s.pref("day"), today).apply {
                            if (got != null) putString(s.pref("hash"), got.token).putString(s.pref("title"), got.title)
                        }.apply()
                        if (got != null) main.post { onChanged?.invoke() }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("Plume", "${s.id} picture failed ($e)")
            } finally {
                synchronized(inFlight) { inFlight.remove(s.id) }
            }
        }
    }

    /** A source no theme uses any more keeps nothing: its picture and its preferences go. */
    fun dropUnused(c: Context, prefs: SharedPreferences) = synchronized(lock) {
        val used = used(prefs)
        for (s in SOURCES) if (s !in used) {
            file(c, s).delete()
            prefs.edit().remove(s.pref("hash")).remove(s.pref("title")).remove(s.pref("day")).remove(s.pref("try")).apply()
        }
        synchronized(inFlight) { if (inFlight.isEmpty()) File(c.filesDir, "daily.part").delete() }
    }

    /** Returns the new picture, or null when it is the one on disk. Throws when the network or the source fails. */
    private fun download(c: Context, s: WallpaperSource, widthPx: Int, have: String?): Latest? {
        val (w, h) = size(widthPx)
        val latest = s.latest(w, h)
        if (latest.token == have && file(c, s).exists()) return null
        val part = File(c.filesDir, "daily.part")
        try {
            val conn = connect(latest.url)
            val copied = conn.inputStream.use { input -> part.outputStream().use { input.copyTo(it) } }
            // A body cut short is not a picture, even if its header decodes.
            if (conn.contentEncoding == null && conn.contentLengthLong.let { it >= 0 && it != copied }) throw IOException("cut short")
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(part.path, bounds)
            if (bounds.outWidth <= 0 || !part.renameTo(file(c, s))) throw IOException("not a picture")
        } finally {
            part.delete() // gone already after a successful rename
        }
        return latest
    }

    fun connect(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 8000
        readTimeout = 20000
    }
}
