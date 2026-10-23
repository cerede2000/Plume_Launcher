package dev.plume

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The language files in lang/ must not be able to break the app. A language is a file lang/<code>.txt and a line in
 * lang/index.txt, and nothing else: every check below runs on every file there, so a new language is checked with no change here.
 */
class LangTest {
    private val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "lang").isDirectory }
    private val english = File(root, "app/src/main/res/values/strings.xml").readText()
    private val indexText = File(root, "lang/index.txt").readText()
    private val index = Lang.parseIndex(indexText)
    private val packs = File(root, "lang").listFiles { f -> f.name.endsWith(".txt") && f.name != "index.txt" }!!.sortedBy { it.name }

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
    private val pluralNames = keys.keys.filter { ':' in it }.map { it.substringBefore(':') }.toSet()
    private val quantities = setOf("zero", "one", "two", "few", "many", "other")

    /** What is wrong with the file [text] of language [code]; empty when it is good. */
    private fun problems(code: String, text: String): List<String> {
        val out = ArrayList<String>()
        if (!Lang.validCode(code)) out += "$code: not a language code"
        if (text.length > 200_000) out += "$code: larger than the 200 KB Plume accepts"
        val pack = Lang.parse(text)
        if (pack.isEmpty()) out += "$code: no text"
        for ((k, v) in pack) {
            val plural = ':' in k
            if (k !in keys && !(plural && k.substringBefore(':') in pluralNames && k.substringAfter(':') in quantities)) out += "$code: unknown key $k"
            if (v.isBlank()) out += "$code: empty text for $k"
            val en = keys[k]
            if (!plural && en != null && placeholders(en) != placeholders(v)) out += "$code: markers of $k are ${placeholders(v)}, English has ${placeholders(en)}"
        }
        return out
    }

    @Test fun everyFileIsInTheIndexAndEveryLineHasAFile() {
        assertEquals("add a line to lang/index.txt for each new file, and a file for each line",
            packs.map { it.name.removeSuffix(".txt") }.sorted(), index.keys.sorted())
    }

    @Test fun everyLineOfTheIndexIsRead() {
        val lines = indexText.lines().filter { it.isNotEmpty() && !it.startsWith("#") }
        assertEquals("a line of lang/index.txt is not \"code<TAB>name\" or repeats a code", lines.size, index.size)
    }

    @Test fun everyPackIsGood() {
        for (pack in packs) assertEquals(emptyList<String>(), problems(pack.name.removeSuffix(".txt"), pack.readText()))
    }

    @Test fun aNewLanguageIsCheckedWithNoChangeHere() {
        assertEquals(emptyList<String>(), problems("pt-BR", "language\tIdioma\nphoto_count:one\t%d foto\nphoto_count:other\t%d fotos\n"))
        assertTrue(problems("xx", "no_such_key\tx\n").isNotEmpty())
        assertTrue(problems("fr", "delete_photo\tSupprimer ?\n").isNotEmpty()) // the %1\$s marker is lost
        assertTrue(problems("fr", "language\t \n").isNotEmpty())
        assertTrue(problems("../x", "language\tx\n").isNotEmpty())
        assertTrue(problems("fr", "").isNotEmpty())
        val four = Lang.parseIndex(indexText + "\npt-BR\tPortuguês (Brasil)\n")
        assertEquals(index.size + 1, four.size)
        assertEquals("Português (Brasil)", four["pt-BR"])
    }

    @Test fun unsafeLinesOfTheIndexAreDropped() {
        val m = Lang.parseIndex("# comment\n\n../evil\tx\nFR\tx\nfr;ls\tx\nde\t \nfr\tFrançais\nfr\tAgain\nno tab here\n")
        assertEquals(listOf("fr"), m.keys.toList())
        assertEquals("Français", m["fr"])
        assertFalse(Lang.validCode("a/b"))
        assertTrue(Lang.validCode("zh-Hans-CN"))
    }

    @Test fun escapesAreRead() {
        val m = Lang.parse("a\tone\\ntwo\\\\three\n# comment\n\nb\tx\ty")
        assertEquals("one\ntwo\\three", m["a"])
        assertEquals("x\ty", m["b"])
        assertEquals(2, m.size)
    }
}
