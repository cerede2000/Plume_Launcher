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
 * The languages on offer are listed in lang/index.txt ("code<TAB>name" lines), downloaded the same way: a new language is a new
 * file and a line in that index, with no new version of the app.
 */
internal object Lang {
    private val CODE = Regex("[a-z]{2,3}(-[A-Za-z0-9]{2,8})*")
    private const val BASE = "https://raw.githubusercontent.com/cerede2000/Plume_Launcher/integration/lang/"
    private const val MAX_BYTES = 200_000

    private var texts: Map<String, String> = emptyMap()

    /** The languages on offer: code -> the name in that language. From the saved index, empty until it was downloaded once. */
    @Volatile
    var available: Map<String, String> = emptyMap()
        private set
    private var locale: Locale = Locale.ENGLISH
    private var plurals: android.icu.text.PluralRules? = null

    val active get() = texts.isNotEmpty()

    private fun file(c: Context, code: String) = File(c.filesDir, "lang-$code.txt")
    private fun tag(c: Context, code: String) = File(c.filesDir, "lang-$code.etag")
    private fun indexFile(c: Context) = File(c.filesDir, "lang-index.txt")
    private fun indexTag(c: Context) = File(c.filesDir, "lang-index.etag")

    internal fun validCode(code: String) = CODE.matches(code)

    /** "code<TAB>name" lines; a line with an unsafe code or an empty name is dropped. */
    internal fun parseIndex(t: String): Map<String, String> {
        val m = LinkedHashMap<String, String>()
        for (line in t.lineSequence()) {
            if (line.isEmpty() || line[0] == '#') continue
            val i = line.indexOf('\t')
            if (i <= 0) continue
            val code = line.substring(0, i).trim()
            val name = line.substring(i + 1).trim()
            if (validCode(code) && name.isNotEmpty() && code !in m) m[code] = name
        }
        return m
    }

    private fun loadIndex(c: Context) {
        available = try {
            parseIndex(indexFile(c).readText())
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun has(c: Context, code: String) = file(c, code).exists()

    /** Reads the saved pack of [code] (English when null, missing or damaged). */
    fun use(c: Context, code: String?) {
        loadIndex(c)
        texts = try {
            if (code == null || !validCode(code)) emptyMap() else parse(file(c, code).readText())
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
     * Saves [path] of the repository to [store]. Without [force] the saved ETag is sent, and GitHub answers "not modified" in
     * a few hundred bytes when nothing changed. True when a good copy is on the TV afterwards.
     */
    private fun download(path: String, store: File, etag: File, force: Boolean, good: (String) -> Boolean): Boolean = try {
        val saved = store.exists()
        val conn = URL(BASE + path).openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        if (saved && !force) etag.takeIf { it.exists() }?.readText()?.let { conn.setRequestProperty("If-None-Match", it) }
        when (conn.responseCode) {
            304 -> true
            200 -> {
                val body = conn.inputStream.use { it.readBytes() }
                val text = body.toString(Charsets.UTF_8)
                if (body.size > MAX_BYTES || !good(text)) {
                    saved
                } else {
                    val part = File(store.parentFile, "lang.part")
                    part.writeText(text)
                    part.renameTo(store)
                    conn.getHeaderField("ETag")?.let { etag.writeText(it) }
                    true
                }
            }
            else -> saved
        }
    } catch (e: Exception) {
        store.exists()
    }

    /** Gets the pack of [code] on a background thread; [done] gets true when the pack on the TV is good. */
    fun fetch(c: Context, code: String, force: Boolean, done: (Boolean) -> Unit) {
        if (!validCode(code)) return done(false)
        Thread { done(download("$code.txt", file(c, code), tag(c, code), force) { parse(it).isNotEmpty() }) }.start()
    }

    /** Gets the list of languages on offer; [done] gets true when there is a good list on the TV. */
    fun fetchIndex(c: Context, force: Boolean, done: (Boolean) -> Unit) {
        Thread {
            val ok = download("index.txt", indexFile(c), indexTag(c), force) { parseIndex(it).isNotEmpty() }
            if (ok) loadIndex(c)
            done(ok)
        }.start()
    }

    /** Deletes the packs other than [keep]: one language is kept at a time. */
    fun prune(c: Context, keep: String?) {
        c.filesDir.listFiles()?.forEach { f ->
            val n = f.name
            if (n.startsWith("lang-") && !n.startsWith("lang-index") && (keep == null || !n.startsWith("lang-$keep."))) f.delete()
        }
    }

    /** The language of the TV ("pt-BR", then "pt") when the index offers it. */
    fun systemCode(): String? = Locale.getDefault().let { listOf(it.toLanguageTag(), it.language) }.firstOrNull { it in available }

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
