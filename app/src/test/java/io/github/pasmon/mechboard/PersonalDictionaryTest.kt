package io.github.pasmon.mechboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PersonalDictionaryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val file: File get() = File(folder.root, "personal_dictionary.txt")

    @Test
    fun `a missing file means no words`() {
        assertEquals(emptyList<String>(), PersonalDictionary(file).words)
    }

    @Test
    fun `added words are kept sorted ignoring case`() {
        val dictionary = PersonalDictionary(file)
        assertTrue(dictionary.add("mechboard"))
        assertTrue(dictionary.add("Ahvenanmaa"))
        assertTrue(dictionary.add("Kotlin"))
        assertEquals(listOf("Ahvenanmaa", "Kotlin", "mechboard"), dictionary.words)
    }

    @Test
    fun `words are saved and loaded again`() {
        PersonalDictionary(file).apply {
            add("Pasi")
            add("mechboard")
            remove("Pasi")
        }
        assertEquals(listOf("mechboard"), PersonalDictionary(file).words)
        assertFalse(File(file.path + ".tmp").exists())
    }

    @Test
    fun `a word already saved in another case is not added again`() {
        val dictionary = PersonalDictionary(file)
        dictionary.add("Kotlin")
        assertFalse(dictionary.add("kotlin"))
        assertEquals(listOf("Kotlin"), dictionary.words)
        assertTrue(dictionary.contains("KOTLIN"))
    }

    @Test
    fun `words are trimmed and invalid ones are refused`() {
        val dictionary = PersonalDictionary(file)
        assertTrue(dictionary.add("  Pasi "))
        assertEquals(listOf("Pasi"), dictionary.words)
        assertFalse(dictionary.add(""))
        assertFalse(dictionary.add("two words"))
        assertFalse(dictionary.add("1st"))
        assertFalse(dictionary.add("a".repeat(PersonalDictionary.MAX_WORD_LENGTH + 1)))
        assertEquals(listOf("Pasi"), dictionary.words)
    }

    @Test
    fun `normalize trims valid words and rejects the rest`() {
        assertEquals("don't", PersonalDictionary.normalize(" don't "))
        assertNull(PersonalDictionary.normalize("   "))
        assertNull(PersonalDictionary.normalize("'quote"))
    }

    @Test
    fun `removing needs the exact spelling`() {
        val dictionary = PersonalDictionary(file)
        dictionary.add("Kotlin")
        assertFalse(dictionary.remove("kotlin"))
        assertTrue(dictionary.remove("Kotlin"))
        assertEquals(emptyList<String>(), dictionary.words)
    }

    @Test
    fun `listeners hear about every change until removed`() {
        val dictionary = PersonalDictionary(file)
        var changes = 0
        val listener = PersonalDictionary.Listener { changes++ }
        dictionary.addListener(listener)
        dictionary.add("Pasi")
        dictionary.add("Pasi") // already saved: no change
        dictionary.remove("Pasi")
        dictionary.removeListener(listener)
        dictionary.add("Kotlin")
        assertEquals(2, changes)
    }

    @Test
    fun `invalid and duplicate lines in the file are skipped`() {
        file.writeText("Kotlin\n\nkotlin\ntwo words\nPasi\n")
        assertEquals(listOf("Kotlin", "Pasi"), PersonalDictionary(file).words)
    }

    @Test
    fun `the saved words are offered as completions`() {
        val dictionary = PersonalDictionary(file)
        dictionary.add("Mechboard")
        assertEquals(listOf("Mechboard"), dictionary.asWordDictionary().completions("mech", 3))
        dictionary.remove("Mechboard")
        assertEquals(emptyList<String>(), dictionary.asWordDictionary().completions("mech", 3))
    }
}
