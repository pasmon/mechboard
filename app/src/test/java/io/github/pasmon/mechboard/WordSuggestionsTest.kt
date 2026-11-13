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
            // "Help" is a correction: one wrong letter away.
            listOf(
                StripItem("Hels", StripItem.Kind.TYPED_WORD),
                StripItem("Helsinkiläinen", StripItem.Kind.SUGGESTION),
                StripItem("Help", StripItem.Kind.SUGGESTION)
            ),
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
    fun `an unknown word gets corrections from the bundled dictionary`() {
        val qwerty = KeyNeighbours.fromDefinition(KeyboardDefinitions.ENGLISH)
        assertEquals(
            listOf(StripItem("Teh", StripItem.Kind.TYPED_WORD), StripItem("The", StripItem.Kind.SUGGESTION), null),
            WordSuggestions.stripFor("Teh", dictionary, personal, qwerty)
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

    // ── next-word prediction ─────────────────────────────────────────────────

    @Test
    fun `the previous word is the one before the spaces`() {
        assertEquals("to", WordSuggestions.previousWord("I went to "))
        assertEquals("to", WordSuggestions.previousWord("I went to  "))
        assertEquals("to", WordSuggestions.previousWord("I went to t"))
        assertEquals("don't", WordSuggestions.previousWord("I don't k"))
    }

    @Test
    fun `there is no previous word across punctuation, line breaks or none at all`() {
        assertEquals("", WordSuggestions.previousWord("Hello. "))
        assertEquals("", WordSuggestions.previousWord("Hello, w"))
        assertEquals("", WordSuggestions.previousWord("Hello\nw"))
        assertEquals("", WordSuggestions.previousWord("Hello"))
        assertEquals("", WordSuggestions.previousWord(" "))
        assertEquals("", WordSuggestions.previousWord(""))
    }

    @Test
    fun `a finished word pair needs both words`() {
        assertEquals("going" to "home", WordSuggestions.finishedWordPair("I am going home"))
        assertEquals(null, WordSuggestions.finishedWordPair("home"))
        assertEquals(null, WordSuggestions.finishedWordPair("going home "))
        assertEquals(null, WordSuggestions.finishedWordPair("Hello. home"))
    }

    @Test
    fun `predictions put learned words before bundled ones without repeats`() {
        assertEquals(
            listOf("home", "to", "out"),
            WordSuggestions.predictionsFor(listOf("home", "to"), listOf("To", "out", "on"), ShiftState.OFF)
        )
    }

    @Test
    fun `predictions follow the shift state`() {
        assertEquals(listOf("Be", "A"), WordSuggestions.predictionsFor(emptyList(), listOf("be", "a"), ShiftState.ON))
        assertEquals(listOf("BE", "A"), WordSuggestions.predictionsFor(emptyList(), listOf("be", "a"), ShiftState.LOCKED))
    }

    @Test
    fun `likely next words that fit what was typed come right after an exact match`() {
        // After "going", "to" is likely; typing "t" puts it before the more frequent "the".
        assertEquals(
            listOf("to", "the", "they"),
            WordSuggestions.suggestionsFor("t", WordDictionary(listOf("the", "they", "to")), likelyNext = listOf("to", "home"))
        )
        // Likely words that do not fit what was typed are not suggested.
        assertFalse(WordSuggestions.suggestionsFor("t", dictionary, likelyNext = listOf("home")).contains("home"))
    }

    // ── autocorrect ──────────────────────────────────────────────────────────

    private val qwertyLayout = KeyNeighbours.fromDefinition(KeyboardDefinitions.ENGLISH)
    private val withTypos = WordDictionary(listOf("the", "and", "because", "work"))

    @Test
    fun `autocorrections take the capitalisation of what was typed`() {
        assertEquals("The", WordSuggestions.autocorrectionFor("Teh", withTypos, null, qwertyLayout))
        assertEquals("Because", WordSuggestions.autocorrectionFor("Becuase", withTypos, null, qwertyLayout))
    }

    @Test
    fun `words typed in capitals are left alone, as they may be acronyms`() {
        assertEquals(null, WordSuggestions.autocorrectionFor("BECUASE", withTypos, null, qwertyLayout))
    }

    @Test
    fun `saved words, inner capitals and an unloaded dictionary are left alone`() {
        assertEquals(null, WordSuggestions.autocorrectionFor("teh", withTypos, WordDictionary(listOf("teh")), qwertyLayout))
        assertEquals(null, WordSuggestions.autocorrectionFor("tEh", withTypos, null, qwertyLayout))
        assertEquals(null, WordSuggestions.autocorrectionFor("teh", null, null, qwertyLayout))
    }

    @Test
    fun `only multi-line fields and fields that ask are autocorrected`() {
        val text = InputType.TYPE_CLASS_TEXT
        assertTrue(WordSuggestions.fieldAllowsAutocorrect(text or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT))
        assertTrue(WordSuggestions.fieldAllowsAutocorrect(text or InputType.TYPE_TEXT_FLAG_MULTI_LINE))
        assertFalse(WordSuggestions.fieldAllowsAutocorrect(text))
        assertFalse(WordSuggestions.fieldAllowsAutocorrect(
            text or InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        ))
        assertTrue(WordSuggestions.fieldAllowsAutocorrect(
            text or InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
        ))
        assertFalse(WordSuggestions.fieldAllowsAutocorrect(
            text or InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
        ))
    }

    @Test
    fun `the strip marks the correction a space will make`() {
        assertEquals(
            listOf(
                StripItem("teh", StripItem.Kind.TYPED_WORD),
                StripItem("the", StripItem.Kind.AUTOCORRECTION),
                null
            ),
            WordSuggestions.stripFor("teh", withTypos, null, qwertyLayout, autocorrection = "the")
        )
    }
}
