package io.github.pasmon.mechboard

import android.text.InputType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSuggestionsTest {

    // ── word at the cursor ───────────────────────────────────────────────────

    @Test
    fun `the word before the cursor is the trailing run of letters`() {
        assertEquals("wor", WordSuggestions.wordBeforeCursor("Hello wor"))
        assertEquals("Hello", WordSuggestions.wordBeforeCursor("Hello"))
        assertEquals("päivä", WordSuggestions.wordBeforeCursor("Hyvää päivä"))
    }

    @Test
    fun `there is no word after a space, punctuation or digits`() {
        assertEquals("", WordSuggestions.wordBeforeCursor("Hello "))
        assertEquals("", WordSuggestions.wordBeforeCursor("Hello."))
        assertEquals("", WordSuggestions.wordBeforeCursor("abc1"))
        assertEquals("", WordSuggestions.wordBeforeCursor(""))
    }

    @Test
    fun `apostrophes inside a word belong to it but a leading one does not`() {
        assertEquals("don't", WordSuggestions.wordBeforeCursor("I don't"))
        assertEquals("aujourd’hui", WordSuggestions.wordBeforeCursor("c'est aujourd’hui"))
        assertEquals("quote", WordSuggestions.wordBeforeCursor("say 'quote"))
    }

    @Test
    fun `combining accents belong to the word`() {
        assertEquals("café", WordSuggestions.wordBeforeCursor("un café"))
    }

    @Test
    fun `a letter after the cursor means the cursor is inside a word`() {
        assertTrue(WordSuggestions.continuesWord("llo"))
        assertFalse(WordSuggestions.continuesWord(" world"))
        assertFalse(WordSuggestions.continuesWord("."))
        assertFalse(WordSuggestions.continuesWord(""))
    }

    // ── suggestions ──────────────────────────────────────────────────────────

    private val dictionary = WordDictionary(listOf("the", "they", "there", "then", "I", "Haus", "hello", "help"))

    @Test
    fun `suggestions take the capitalisation of what was typed`() {
        assertEquals(listOf("The", "They", "There"), WordSuggestions.suggestionsFor("Th", dictionary))
        assertEquals(listOf("THE", "THEY", "THERE"), WordSuggestions.suggestionsFor("THE", dictionary))
        assertEquals(listOf("the", "they", "there"), WordSuggestions.suggestionsFor("th", dictionary))
    }

    @Test
    fun `dictionary capitals are kept`() {
        assertEquals(listOf("Haus"), WordSuggestions.suggestionsFor("hau", dictionary))
        assertEquals(listOf("I"), WordSuggestions.suggestionsFor("i", dictionary))
    }

    @Test
    fun `a single typed capital capitalises only the first letter`() {
        assertEquals(listOf("Haus", "Hello", "Help"), WordSuggestions.suggestionsFor("H", dictionary))
    }

    @Test
    fun `no word means no suggestions`() {
        assertEquals(emptyList<String>(), WordSuggestions.suggestionsFor("", dictionary))
    }

    @Test
    fun `suggestions that become equal after matching case appear once`() {
        val withCaseVariants = WordDictionary(listOf("bank", "Bank", "banker"))
        assertEquals(listOf("Bank", "Banker"), WordSuggestions.suggestionsFor("Ban", withCaseVariants))
    }

    // ── strip arrangement ────────────────────────────────────────────────────

    @Test
    fun `the best suggestion goes in the middle slot`() {
        assertEquals(listOf("b", "a", "c"), WordSuggestions.arrangeInSlots(listOf("a", "b", "c")))
        assertEquals(listOf("b", "a", null), WordSuggestions.arrangeInSlots(listOf("a", "b")))
        assertEquals(listOf(null, "a", null), WordSuggestions.arrangeInSlots(listOf("a")))
        assertEquals(listOf(null, null, null), WordSuggestions.arrangeInSlots(emptyList()))
    }

    // ── which fields ─────────────────────────────────────────────────────────

    @Test
    fun `plain text fields get suggestions`() {
        assertTrue(WordSuggestions.fieldAllowsSuggestions(InputType.TYPE_CLASS_TEXT))
        assertTrue(WordSuggestions.fieldAllowsSuggestions(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        ))
    }

    @Test
    fun `password, address and non-text fields get no suggestions`() {
        listOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_URI
        ).forEach { variation ->
            assertFalse(WordSuggestions.fieldAllowsSuggestions(InputType.TYPE_CLASS_TEXT or variation))
        }
        assertFalse(WordSuggestions.fieldAllowsSuggestions(InputType.TYPE_CLASS_NUMBER))
        assertFalse(WordSuggestions.fieldAllowsSuggestions(InputType.TYPE_CLASS_PHONE))
        assertFalse(WordSuggestions.fieldAllowsSuggestions(InputType.TYPE_NULL))
    }

    @Test
    fun `fields that ask for no suggestions get none`() {
        assertFalse(WordSuggestions.fieldAllowsSuggestions(
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        ))
    }
}
