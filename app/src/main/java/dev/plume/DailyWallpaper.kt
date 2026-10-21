package dev.plume

import android.content.Context
import android.graphics.BitmapFactory
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * The Bing picture of the day as a wallpaper. One JPEG on disk, replaced in place when Bing's picture changes,
 * so no old picture is ever kept. Nothing runs in the background: the home screen calls [refresh] at most once
 * a day. Everything that knows about Bing is in this file.
 */
internal object DailyWallpaper {
    /** The wallpaper choice that means "Bing picture of the day", next to "res:…", "file:…" and "folder:…". */
    const val CHOICE = "daily:bing"
    private const val HOST = "https://www.bing.com"

    fun file(c: Context) = File(c.filesDir, "daily.jpg")

    /** The biggest Bing size that fits the screen. Asked at this size Bing sends a 4K picture of ~950 KB. */
    fun size(widthPx: Int) = when {
        widthPx >= 3000 -> 3840 to 2160
        widthPx >= 2200 -> 2560 to 1440
        else -> 1920 to 1080
    }

    /**
     * Blocking: call it from a worker thread. Asks Bing for today's picture and saves it unless it is the one on
     * disk already ([have] is its hash). Returns the new hash and title, null when nothing changed, and throws
     * when the network or Bing fails, so the caller keeps yesterday's picture.
     */
    fun refresh(c: Context, widthPx: Int, have: String?): Pair<String, String>? {
        val (w, h) = size(widthPx)
        val l = Locale.getDefault()
        val market = if (l.country.isEmpty()) "en-US" else "${l.language}-${l.country}"
        val image = JSONObject(open("$HOST/HPImageArchive.aspx?format=js&idx=0&n=1&uhd=1&uhdwidth=$w&uhdheight=$h&mkt=$market")
            .use { it.readBytes().decodeToString() }).getJSONArray("images").getJSONObject(0)
        val hash = image.getString("hsh") + w // the size is part of it, so a bigger screen fetches again
        if (hash == have && file(c).exists()) return null
        val part = File(c.filesDir, "daily.part")
        try {
            open(HOST + image.getString("url")).use { input -> part.outputStream().use { input.copyTo(it) } }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(part.path, bounds)
            if (bounds.outWidth <= 0 || !part.renameTo(file(c))) throw IOException("not a picture")
        } finally {
            part.delete() // gone already after a successful rename
        }
        return hash to image.getString("title")
    }

    private fun open(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 8000
        readTimeout = 20000
    }.inputStream
}
