package io.github.pasmon.mechboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Checks the word lists in `src/main/assets/dictionaries/`: every keyboard
 * language has one, and each is a clean list the dictionary can use.
 */
class BundledDictionariesTest {

    /** Unit tests run with the module directory (app/) as working directory. */
    private fun wordListFile(layout: KeyboardLayout) =
        File("src/main/assets/dictionaries/${layout.id}.txt")

    private fun wordsOf(layout: KeyboardLayout): List<String> =
        wordListFile(layout).readLines(Charsets.UTF_8)

    @Test
    fun `every keyboard language has a word list`() {
        KeyboardLayout.values().forEach { layout ->
            assertTrue("missing ${wordListFile(layout)}", wordListFile(layout).isFile)
        }
    }

    @Test
    fun `word lists hold one word per line without blanks, spaces, digits or duplicates`() {
        KeyboardLayout.values().forEach { layout ->
            val words = wordsOf(layout)
            assertTrue("${layout.id}: only ${words.size} words", words.size >= MIN_WORDS)
            assertEquals("${layout.id}: duplicate words", words.size, words.toSet().size)
            val bad = words.filter { it.isBlank() || it.any { c -> c.isWhitespace() || c.isDigit() } }
            assertEquals("${layout.id}: malformed lines", emptyList<String>(), bad)
        }
    }

    @Test
    fun `the most frequent words of each language come first`() {
        val expectedNearTop = mapOf(
            KeyboardLayout.ENGLISH to "the",
            KeyboardLayout.FINNISH to "ja",
            KeyboardLayout.GERMAN to "und",
            KeyboardLayout.FRENCH to "et",
            KeyboardLayout.SPANISH to "que"
        )
        expectedNearTop.forEach { (layout, word) ->
            assertTrue("${layout.id}: '$word' not near the top", word in wordsOf(layout).take(20))
        }
    }

    @Test
    fun `a bundled list gives sensible completions`() {
        val english = WordDictionary(wordsOf(KeyboardLayout.ENGLISH))
        assertEquals("the", english.completions("th", 3).first())
        assertTrue(english.completions("keyb", 3).contains("keyboard"))
    }

    @Test
    fun `common typos are corrected with the bundled lists`() {
        val english = WordDictionary(wordsOf(KeyboardLayout.ENGLISH))
        val qwerty = KeyboardLayout.ENGLISH.neighbours
        mapOf("teh" to "the", "becuase" to "because", "tomorow" to "tomorrow", "wrok" to "work", "jsut" to "just")
            .forEach { (typo, word) ->
                assertEquals(typo, word, english.suggestions(typo, 3, qwerty).first())
            }
        val finnish = WordDictionary(wordsOf(KeyboardLayout.FINNISH))
        assertEquals("päivä", finnish.suggestions("paiva", 3, KeyboardLayout.FINNISH.neighbours).first())
        assertEquals("kiitos", finnish.suggestions("kiitso", 3, KeyboardLayout.FINNISH.neighbours).first())
    }

    @Test
    fun `ordinary typing still gets completions first`() {
        val english = WordDictionary(wordsOf(KeyboardLayout.ENGLISH))
        val qwerty = KeyboardLayout.ENGLISH.neighbours
        assertEquals("ten", english.suggestions("ten", 3, qwerty).first())
        assertTrue(english.suggestions("keyb", 3, qwerty).contains("keyboard"))
    }

    @Test
    fun `english next words are well formed and use only listed words`() {
        val known = wordsOf(KeyboardLayout.ENGLISH).toSet()
        val lines = File("src/main/assets/dictionaries/english_next.txt").readLines(Charsets.UTF_8)
        assertTrue("only ${lines.size} lines", lines.size >= MIN_WORDS)
        lines.forEach { line ->
            val words = line.split('\t')
            assertTrue(line, words.size >= 2 && words.all { it in known })
        }
        val nextWords = NextWords.fromLines(lines.asSequence())
        assertTrue(nextWords.after("to").contains("be"))
        assertTrue(nextWords.after("thank").contains("you"))
    }

    @Test
    fun `next words files belong to keyboard languages`() {
        val ids = KeyboardLayout.values().map { it.id }.toSet()
        File("src/main/assets/dictionaries").listFiles()!!
            .filter { it.name.endsWith("_next.txt") }
            .forEach { assertTrue(it.name, it.name.removeSuffix("_next.txt") in ids) }
    }

    @Test
    fun `autocorrect fixes clear typos and leaves slang and names alone`() {
        val english = WordDictionary(wordsOf(KeyboardLayout.ENGLISH))
        val qwerty = KeyboardLayout.ENGLISH.neighbours
        mapOf(
            "teh" to "the", "hte" to "the", "becuase" to "because", "recieve" to "receive",
            "jsut" to "just", "freind" to "friend", "goverment" to "government",
            "dont" to "don't", "ive" to "I've", "thats" to "that's"
        ).forEach { (typo, word) -> assertEquals(typo, word, english.autocorrection(typo, qwerty)) }
        listOf("wierd", "lol", "btw", "omg", "idk", "thx", "haha", "hehe", "apps", "Pasi", "Kotlin", "Gboard", "wtf", "gonna")
            .forEach { assertEquals(it, null, english.autocorrection(it, qwerty)) }

        val finnish = WordDictionary(wordsOf(KeyboardLayout.FINNISH))
        val finnishKeys = KeyboardLayout.FINNISH.neighbours
        assertEquals("päivä", finnish.autocorrection("paiva", finnishKeys))
        assertEquals("hyvää", finnish.autocorrection("hyvaa", finnishKeys))
        listOf("nii", "sit", "mut", "joo", "moi", "kiitti", "mennää", "ollaa")
            .forEach { assertEquals(it, null, finnish.autocorrection(it, finnishKeys)) }
    }

    private companion object {
        const val MIN_WORDS = 10_000
    }
}
