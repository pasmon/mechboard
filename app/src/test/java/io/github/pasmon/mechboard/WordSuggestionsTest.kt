package io.github.pasmon.mechboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
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
        assertEquals(listOf<String?>(null, null, null), WordSuggestions.arrangeInSlots(emptyList<String>()))
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

    // ── personal dictionary and the typed word ───────────────────────────────

    private val personal = WordDictionary(listOf("Mechboard", "helsinkiläinen", "Thessa"))

    @Test
    fun `personal words come before bundled ones but after an exact match`() {
        assertEquals(listOf("Thessa", "the", "they"), WordSuggestions.suggestionsFor("th", dictionary, personal))
        assertEquals(listOf("The", "Thessa", "They"), WordSuggestions.suggestionsFor("The", dictionary, personal))
    }

    @Test
    fun `personal words are suggested while the bundled dictionary is not loaded`() {
        assertEquals(listOf("Mechboard"), WordSuggestions.suggestionsFor("mec", null, personal))
    }

    @Test
    fun `a known word gets the usual arrangement`() {
        assertEquals(
            listOf("they", "the", "there").map { StripItem(it, StripItem.Kind.SUGGESTION) },
            WordSuggestions.stripFor("the", dictionary)
        )
    }

    @Test
    fun `an unknown word is offered as typed on the left`() {
        assertEquals(
            listOf(StripItem("Hels", StripItem.Kind.TYPED_WORD), StripItem("Helsinkiläinen", StripItem.Kind.SUGGESTION), null),
            WordSuggestions.stripFor("Hels", dictionary, personal)
        )
    }

    @Test
    fun `a saved word is not offered as typed`() {
        assertEquals(
            listOf(null, StripItem("Mechboard", StripItem.Kind.SUGGESTION), null),
            // Typed in lower case, the saved word keeps its capital.
            WordSuggestions.stripFor("mechboard", dictionary, personal)
        )
    }

    @Test
    fun `nothing is offered as typed until the bundled dictionary is loaded`() {
        assertEquals(listOf(null, null, null), WordSuggestions.stripFor("zzz", null, personal))
    }

    @Test
    fun `no word means an empty strip`() {
        assertEquals(listOf(null, null, null), WordSuggestions.stripFor("", dictionary, personal))
    }

    @Test
    fun `the save offer sits in the middle`() {
        assertEquals(
            listOf(null, StripItem("Pasi", StripItem.Kind.SAVE_WORD), null),
            WordSuggestions.saveOfferFor("Pasi")
        )
    }

    @Test
    fun `incognito fields do not allow learning words`() {
        assertTrue(WordSuggestions.fieldAllowsLearning(EditorInfo.IME_ACTION_SEND))
        assertFalse(WordSuggestions.fieldAllowsLearning(
            EditorInfo.IME_ACTION_GO or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        ))
    }
}
