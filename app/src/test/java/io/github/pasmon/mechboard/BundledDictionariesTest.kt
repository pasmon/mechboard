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

    private companion object {
        const val MIN_WORDS = 10_000
    }
}
