package dev.plume

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The language packs in lang/ must not be able to break the app: no unknown key, the same placeholders as English. */
class LangTest {
    private val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "lang").isDirectory }
    private val english = File(root, "app/src/main/res/values/strings.xml").readText()

    private fun placeholders(t: String) = Regex("%(\\d+\\$)?[sd]").findAll(t).map { it.value }.sorted().toList()

    /** key -> raw text of the English strings, with plurals as "name:quantity". */
    private val keys: Map<String, String> = buildMap {
        Regex("<string\\s+name=\"([^\"]+)\"[^>]*>(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
            .findAll(english).forEach { put(it.groupValues[1], it.groupValues[2]) }
        Regex("<plurals\\s+name=\"([^\"]+)\"[^>]*>(.*?)</plurals>", RegexOption.DOT_MATCHES_ALL).findAll(english).forEach { p ->
            Regex("<item\\s+quantity=\"([^\"]+)\"[^>]*>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
                .findAll(p.groupValues[2]).forEach { put(p.groupValues[1] + ":" + it.groupValues[1], it.groupValues[2]) }
        }
    }

    private val packs = File(root, "lang").listFiles { f -> f.name.endsWith(".txt") }!!.sortedBy { it.name }

    @Test fun everyLanguageOfTheAppHasItsPack() {
        assertEquals(Lang.NAMES.keys.sorted(), packs.map { it.name.removeSuffix(".txt") })
    }

    @Test fun noPackKeyIsUnknown() {
        val plurals = keys.keys.filter { ':' in it }.map { it.substringBefore(':') }.toSet()
        for (pack in packs) for (k in Lang.parse(pack.readText()).keys) {
            assertTrue("${pack.name}: unknown key $k", k in keys || (':' in k && k.substringBefore(':') in plurals))
        }
    }

    @Test fun placeholdersMatchEnglish() {
        for (pack in packs) for ((k, v) in Lang.parse(pack.readText())) {
            val en = keys[k] ?: keys[k.substringBefore(':') + ":other"] ?: continue
            if (':' in k) continue // a plural form may drop %d for "one": "une photo"
            assertEquals("${pack.name}: placeholders of $k", placeholders(en), placeholders(v))
        }
    }

    @Test fun escapesAreRead() {
        val m = Lang.parse("a\tone\\ntwo\\\\three\n# comment\n\nb\tx\ty")
        assertEquals("one\ntwo\\three", m["a"])
        assertEquals("x\ty", m["b"])
        assertEquals(2, m.size)
    }
}
