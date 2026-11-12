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
}
