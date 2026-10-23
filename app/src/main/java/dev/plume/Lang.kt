package dev.plume

import android.content.Context
import android.content.res.Resources
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Language packs: one "key<TAB>text" file per language, fetched from GitHub when the user picks the language. */
internal object Lang {
    val NAMES = linkedMapOf("fr" to "Français", "tr" to "Türkçe", "ar" to "العربية")
    private const val BASE = "https://raw.githubusercontent.com/cerede2000/Plume_Launcher/assets-sources-proposal/lang/"
    private var texts: Map<String, String> = emptyMap()

    val active get() = texts.isNotEmpty()

    private fun file(c: Context, code: String) = File(c.filesDir, "lang-$code.txt")

    fun has(c: Context, code: String) = file(c, code).exists()

    fun use(c: Context, code: String?) {
        texts = try {
            code?.let { read(file(c, it).readText()) } ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun read(t: String): Map<String, String> {
        val m = HashMap<String, String>()
        for (l in t.lineSequence()) {
            val i = l.indexOf('\t')
            if (i > 0) m[l.substring(0, i)] = l.substring(i + 1).replace("\\n", "\n")
        }
        return m
    }

    /** Fetches the pack on a background thread; [done] gets true once it is saved. */
    fun fetch(c: Context, code: String, done: (Boolean) -> Unit) {
        Thread {
            val ok = try {
                val conn = URL(BASE + code + ".txt").openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val body = conn.inputStream.use { it.readBytes() }.toString(Charsets.UTF_8)
                if (read(body).isEmpty()) {
                    false
                } else {
                    val tmp = File(c.filesDir, "lang.part")
                    tmp.writeText(body)
                    tmp.renameTo(file(c, code))
                }
            } catch (e: Exception) {
                false
            }
            done(ok)
        }.start()
    }

    fun text(r: Resources, id: Int): String? = texts[r.getResourceEntryName(id)]
}

/** Looks in the pack first, then in the strings built into the APK. */
@Suppress("DEPRECATION")
internal class PackResources(base: Resources) : Resources(base.assets, base.displayMetrics, base.configuration) {
    override fun getString(id: Int): String = Lang.text(this, id) ?: super.getString(id)

    override fun getString(id: Int, vararg args: Any?): String =
        Lang.text(this, id)?.let { try { String.format(it, *args) } catch (e: Exception) { null } } ?: super.getString(id, *args)

    override fun getText(id: Int): CharSequence = Lang.text(this, id) ?: super.getText(id)
}
