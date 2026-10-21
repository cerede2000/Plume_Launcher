package dev.plume

import android.content.Context
import android.graphics.BitmapFactory
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

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
        val image = JSONObject(DailyWallpaper.open("https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&uhd=1&uhdwidth=$width&uhdheight=$height&mkt=$market")
            .use { it.readBytes().decodeToString() }).getJSONArray("images").getJSONObject(0)
        // The size is part of the token, so a bigger screen fetches again.
        return Latest(image.getString("hsh") + width, image.getString("title"), "https://www.bing.com" + image.getString("url"))
    }
}

/**
 * The shared part of internet wallpapers. One JPEG per source, replaced in place when the picture changes, so no old
 * picture is ever kept. Nothing runs in the background: the home screen calls [refresh] at most once a day.
 */
internal object DailyWallpaper {
    val SOURCES: List<WallpaperSource> = listOf(Bing)

    fun sourceOf(choice: String) = SOURCES.firstOrNull { it.choice == choice }

    fun file(c: Context, s: WallpaperSource) = File(c.filesDir, "daily_${s.id}.jpg")

    /** The biggest size that fits the screen: 1080p, 1440p or 4K. */
    fun size(widthPx: Int) = when {
        widthPx >= 3000 -> 3840 to 2160
        widthPx >= 2200 -> 2560 to 1440
        else -> 1920 to 1080
    }

    /**
     * Blocking: call it from a worker thread. Saves [s]'s newest picture unless it is the one on disk already ([have]
     * is its token). Returns it, null when nothing changed, and throws when the network or the source fails, so the
     * caller keeps yesterday's picture.
     */
    fun refresh(c: Context, s: WallpaperSource, widthPx: Int, have: String?): Latest? {
        val (w, h) = size(widthPx)
        val latest = s.latest(w, h)
        if (latest.token == have && file(c, s).exists()) return null
        val part = File(c.filesDir, "daily.part")
        try {
            open(latest.url).use { input -> part.outputStream().use { input.copyTo(it) } }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(part.path, bounds)
            if (bounds.outWidth <= 0 || !part.renameTo(file(c, s))) throw IOException("not a picture")
        } finally {
            part.delete() // gone already after a successful rename
        }
        return latest
    }

    fun open(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 8000
        readTimeout = 20000
    }.inputStream
}
