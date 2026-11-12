package io.github.pasmon.mechboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordDictionaryTest {

    // Most frequent first.
    private val dictionary = WordDictionary(
        listOf("the", "they", "there", "then", "them", "theory", "I", "Haus", "hausen", "café", "cafe",
            "don't", "hän", "han", "hanki")
    )

    @Test
    fun `completions are the most frequent words starting with the prefix`() {
        assertEquals(listOf("the", "they", "there"), dictionary.completions("th", 3))
    }

    @Test
    fun `completions keep the limit`() {
        assertEquals(5, dictionary.completions("th", 5).size)
        assertEquals(emptyList<String>(), dictionary.completions("th", 0))
    }

    @Test
    fun `an exactly typed word comes first even if it is less frequent`() {
        assertEquals("then", dictionary.completions("then", 3).first())
        assertEquals(listOf("hanki"), dictionary.completions("hank", 3))
    }

    @Test
    fun `an empty or unknown prefix has no completions`() {
        assertEquals(emptyList<String>(), dictionary.completions("", 3))
        assertEquals(emptyList<String>(), dictionary.completions("xyz", 3))
    }

    @Test
    fun `lookups ignore case and return the dictionary spelling`() {
        assertEquals(listOf("Haus", "hausen"), dictionary.completions("hau", 3))
        assertEquals(listOf("the", "they"), dictionary.completions("THE", 2))
        assertEquals(listOf("I"), dictionary.completions("i", 3))
    }

    @Test
    fun `lookups ignore accents but prefer words whose accents match`() {
        assertEquals(listOf("café", "cafe"), dictionary.completions("caf", 3))
        assertEquals("café", dictionary.completions("café", 3).first())
        assertEquals(listOf("han", "hanki", "hän"), dictionary.completions("han", 3))
        assertEquals("hän", dictionary.completions("hä", 3).first())
    }

    @Test
    fun `typographic apostrophes match plain ones`() {
        assertEquals(listOf("don't"), dictionary.completions("don’", 3))
    }

    @Test
    fun `contains ignores case but not accents`() {
        assertTrue(dictionary.contains("THE"))
        assertTrue(dictionary.contains("haus"))
        assertTrue(dictionary.contains("hän"))
        assertFalse(dictionary.contains("haen"))
        assertFalse(dictionary.contains("th"))
    }

    @Test
    fun `duplicate and blank lines are ignored`() {
        val fromLines = WordDictionary.fromLines(sequenceOf("a", "", "  ", "b", "a"))
        assertEquals(2, fromLines.size)
        assertEquals(listOf("a"), fromLines.completions("a", 3))
    }

    @Test
    fun `a single letter prefix with many matches still returns the most frequent`() {
        val words = (0 until 5000).map { "a$it" }
        val large = WordDictionary(words)
        assertEquals(words.take(3), large.completions("a", 3))
    }

    // ── suggestions with typos ───────────────────────────────────────────────

    // Most frequent first.
    private val words = WordDictionary(
        listOf("the", "of", "and", "to", "hello", "help", "ten", "then", "received", "receive", "keyboard", "päivä", "päivänä")
    )
    private val qwerty = KeyNeighbours.fromDefinition(KeyboardDefinitions.ENGLISH)

    @Test
    fun `swapped letters are corrected`() {
        assertEquals("the", words.suggestions("teh", 3, qwerty).first())
        assertEquals("keyboard", words.suggestions("keybaord", 3, qwerty).first())
    }

    @Test
    fun `a missing, extra or wrong letter is corrected`() {
        assertEquals("keyboard", words.suggestions("keyboad", 3, qwerty).first())
        assertEquals("keyboard", words.suggestions("keyboaard", 3, qwerty).first())
        assertEquals("hello", words.suggestions("hwllo", 3, qwerty).first())
    }

    @Test
    fun `a correction spelled like the fixed text beats a longer, more frequent completion of it`() {
        assertEquals(listOf("receive", "received"), words.suggestions("recieve", 3, qwerty).take(2))
    }

    @Test
    fun `a word spelled as typed stays first`() {
        assertEquals("ten", words.suggestions("ten", 3, qwerty).first())
        assertEquals("then", words.suggestions("then", 3, qwerty).first())
    }

    @Test
    fun `a neighbouring key makes a typo likelier`() {
        // "tgen": g is next to h on QWERTY, so "then" is the likelier fix than "ten".
        assertEquals("then", words.suggestions("tgen", 3, qwerty).first())
        assertEquals("ten", words.suggestions("tgen", 3, null).first())
    }

    @Test
    fun `missing accents are added`() {
        assertEquals(listOf("päivä", "päivänä"), words.suggestions("paiva", 3, qwerty).take(2))
    }

    @Test
    fun `short text gets completions but no corrections`() {
        assertEquals(listOf("the", "then"), words.suggestions("th", 3, qwerty))
        // "xh" is one typo from "th", but too short to look for typos.
        assertEquals(emptyList<String>(), words.suggestions("xh", 3, qwerty))
    }

    @Test
    fun `nothing close gives no suggestions`() {
        assertEquals(emptyList<String>(), words.suggestions("zzzzz", 3, qwerty))
        assertEquals(emptyList<String>(), words.suggestions("", 3, qwerty))
    }

    @Test
    fun `the dictionary spelling is found ignoring case but not accents`() {
        assertEquals("Haus", dictionary.spellingOf("HAUS"))
        assertEquals("the", dictionary.spellingOf("The"))
        assertEquals("hän", dictionary.spellingOf("Hän"))
        assertEquals(null, dictionary.spellingOf("haen"))
    }

    @Test
    fun `an exact spelling is preferred among case variants`() {
        val variants = WordDictionary(listOf("bank", "Bank"))
        assertEquals("Bank", variants.spellingOf("Bank"))
        assertEquals("bank", variants.spellingOf("bank"))
        assertEquals("bank", variants.spellingOf("BANK"))
    }
}
