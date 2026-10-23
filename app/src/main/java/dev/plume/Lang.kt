package dev.plume

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Language packs. English is in the APK (it is the fallback). Every other language is one text file, lang/<code>.txt in
 * this repository, with a "key<TAB>text" line per string and plurals as "name:quantity". Picking a language downloads its
 * pack once and keeps it; the daily check asks GitHub if it changed (ETag), so a fixed text reaches the TV with no release.
 * A key the pack lacks shows in English, and a key the app does not know is ignored, so one pack suits every version:
 * never change what an existing key means, add a new key. Only one pack is kept: picking another deletes the old one.
 */
internal object Lang {
    val NAMES = linkedMapOf("fr" to "Français", "tr" to "Türkçe", "ar" to "العربية")
    private const val BASE = "https://raw.githubusercontent.com/cerede2000/Plume_Launcher/integration/lang/"
    private const val MAX_BYTES = 200_000

    private var texts: Map<String, String> = emptyMap()
    private var locale: Locale = Locale.ENGLISH
    private var plurals: android.icu.text.PluralRules? = null

    val active get() = texts.isNotEmpty()

    private fun file(c: Context, code: String) = File(c.filesDir, "lang-$code.txt")
    private fun tag(c: Context, code: String) = File(c.filesDir, "lang-$code.etag")

    fun has(c: Context, code: String) = file(c, code).exists()

    /** Reads the saved pack of [code] (English when null, missing or damaged). */
    fun use(c: Context, code: String?) {
        texts = try {
            if (code == null || code !in NAMES) emptyMap() else parse(file(c, code).readText())
        } catch (e: Exception) {
            emptyMap()
        }
        locale = if (active) Locale.forLanguageTag(code!!) else Locale.ENGLISH
        plurals = if (active) android.icu.text.PluralRules.forLocale(locale) else null
    }

    internal fun parse(t: String): Map<String, String> {
        val m = HashMap<String, String>()
        for (line in t.lineSequence()) {
            if (line.isEmpty() || line[0] == '#') continue
            val i = line.indexOf('\t')
            if (i > 0) m[line.substring(0, i)] = unescape(line.substring(i + 1))
        }
        return m
    }

    private fun unescape(s: String): String {
        if ('\\' !in s) return s
        val b = StringBuilder()
        var i = 0
        while (i < s.length) {
            val ch = s[i]
            if (ch == '\\' && i + 1 < s.length) {
                b.append(if (s[i + 1] == 'n') '\n' else s[i + 1])
                i += 2
            } else {
                b.append(ch)
                i++
            }
        }
        return b.toString()
    }

    /**
     * Gets the pack of [code] on a background thread. Without [force] the saved ETag is sent, and GitHub answers
     * "not modified" in a few hundred bytes when nothing changed. [done]: true when the pack on the TV is good.
     */
    fun fetch(c: Context, code: String, force: Boolean, done: (Boolean) -> Unit) {
        Thread {
            done(try {
                val saved = has(c, code)
                val conn = URL(BASE + code + ".txt").openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                if (saved && !force) tag(c, code).takeIf { it.exists() }?.readText()?.let { conn.setRequestProperty("If-None-Match", it) }
                when (conn.responseCode) {
                    304 -> true
                    200 -> {
                        val body = conn.inputStream.use { it.readBytes() }
                        if (body.size > MAX_BYTES) return@Thread done(saved)
                        val text = body.toString(Charsets.UTF_8)
                        if (parse(text).isEmpty()) return@Thread done(saved)
                        val part = File(c.filesDir, "lang.part")
                        part.writeText(text)
                        part.renameTo(file(c, code))
                        conn.getHeaderField("ETag")?.let { tag(c, code).writeText(it) }
                        true
                    }
                    else -> saved
                }
            } catch (e: Exception) {
                has(c, code)
            })
        }.start()
    }

    /** Deletes the packs other than [keep]: one language is kept at a time. */
    fun prune(c: Context, keep: String?) {
        for (code in NAMES.keys) if (code != keep) {
            file(c, code).delete()
            tag(c, code).delete()
        }
    }

    /** The language of the TV when Plume has a pack for it. */
    fun systemCode(): String? = Locale.getDefault().language.takeIf { it in NAMES }

    internal fun text(r: Resources, id: Int): String? = texts[r.getResourceEntryName(id)]

    internal fun plural(r: Resources, id: Int, n: Int): String? {
        val name = r.getResourceEntryName(id)
        return texts[name + ":" + (plurals?.select(n.toDouble()) ?: "other")] ?: texts["$name:other"]
    }

    internal fun format(text: String, args: Array<out Any?>): String? = try {
        String.format(locale, text, *args)
    } catch (e: Exception) {
        null // a pack with the wrong placeholders: the English text shows instead of a crash
    }

    /** [base] itself while English shows; otherwise resources that look in the pack first. Rebuilt when the configuration changes. */
    fun resources(base: Resources, kept: PackResources?): Resources {
        if (!active) return base
        if (kept != null && kept.sameAs(base.configuration)) return kept
        return PackResources(base)
    }
}

/** Resources whose texts come from the pack of the language when it has them, and from the APK otherwise. */
@Suppress("DEPRECATION")
internal class PackResources(base: Resources) : Resources(base.assets, base.displayMetrics, base.configuration) {
    private val made = Configuration(base.configuration)

    fun sameAs(c: Configuration) = made == c

    override fun getString(id: Int): String = Lang.text(this, id) ?: super.getString(id)

    override fun getString(id: Int, vararg args: Any?): String =
        Lang.text(this, id)?.let { Lang.format(it, args) } ?: super.getString(id, *args)

    override fun getText(id: Int): CharSequence = Lang.text(this, id) ?: super.getText(id)

    override fun getText(id: Int, def: CharSequence?): CharSequence = Lang.text(this, id) ?: super.getText(id, def)

    override fun getQuantityString(id: Int, quantity: Int): String = Lang.plural(this, id, quantity) ?: super.getQuantityString(id, quantity)

    override fun getQuantityString(id: Int, quantity: Int, vararg args: Any?): String =
        Lang.plural(this, id, quantity)?.let { Lang.format(it, args) } ?: super.getQuantityString(id, quantity, *args)
}
