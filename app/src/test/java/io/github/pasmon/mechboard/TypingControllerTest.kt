package io.github.pasmon.mechboard

import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TypingControllerTest {

    private lateinit var ic: InputConnection
    private lateinit var editorInfo: EditorInfo
    private var autoCapitalize = true
    private val shiftChanges = mutableListOf<ShiftState>()
    private lateinit var typing: TypingController

    @Before
    fun setUp() {
        ic = mock()
        // By default the cursor is mid-sentence in a field with nothing selected.
        whenever(ic.getCursorCapsMode(any())).doReturn(0)
        whenever(ic.getSelectedText(any())).doReturn(null)
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("abc")
        editorInfo = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_NONE
        }
        typing = TypingController(
            connection = { ic },
            editorInfo = { editorInfo },
            autoCapitalize = { autoCapitalize },
            onShiftStateChanged = { shiftChanges += it }
        )
    }

    private fun atSentenceStart() {
        whenever(ic.getCursorCapsMode(any())).doReturn(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
    }

    // ── characters and shift ─────────────────────────────────────────────────

    @Test
    fun `character key types its character`() {
        typing.onKey('a'.code)
        verify(ic).commitText("a", 1)
    }

    @Test
    fun `shift makes only the next letter a capital`() {
        typing.onKey(KeyCodes.SHIFT)
        typing.onKey('a'.code)
        typing.onKey('b'.code)
        verify(ic).commitText("A", 1)
        verify(ic).commitText("b", 1)
        assertEquals(ShiftState.OFF, typing.shiftState)
    }

    @Test
    fun `tapping shift twice locks capitals until it is tapped again`() {
        typing.onKey(KeyCodes.SHIFT)
        typing.onKey(KeyCodes.SHIFT)
        assertEquals(ShiftState.LOCKED, typing.shiftState)
        typing.onKey('a'.code)
        typing.onKey(KeyCodes.SPACE)
        typing.onKey('b'.code)
        verify(ic).commitText("A", 1)
        verify(ic).commitText("B", 1)
        assertEquals(ShiftState.LOCKED, typing.shiftState)

        typing.onKey(KeyCodes.SHIFT)
        typing.onKey('c'.code)
        verify(ic).commitText("c", 1)
    }

    @Test
    fun `shift state changes are reported once each`() {
        typing.onKey(KeyCodes.SHIFT)
        typing.onKey(KeyCodes.SHIFT)
        typing.onKey(KeyCodes.SHIFT)
        assertEquals(listOf(ShiftState.ON, ShiftState.LOCKED, ShiftState.OFF), shiftChanges)
    }

    @Test
    fun `shift works without a text field`() {
        val detached = TypingController({ null }, { null }, { true }, { shiftChanges += it })
        detached.onKey(KeyCodes.SHIFT)
        detached.onKey('a'.code)
        assertEquals(ShiftState.ON, detached.shiftState)
    }

    @Test
    fun `accented letters are capitalised and sharp s is left alone`() {
        typing.onKey(KeyCodes.SHIFT)
        typing.onKey(KeyCodes.SHIFT)
        typing.onKey('ä'.code)
        typing.onKey('ñ'.code)
        typing.onKey('ß'.code)
        verify(ic).commitText("Ä", 1)
        verify(ic).commitText("Ñ", 1)
        verify(ic).commitText("ß", 1)
    }

    @Test
    fun `space types a space and uses up shift`() {
        typing.onKey(KeyCodes.SHIFT)
        typing.onKey(KeyCodes.SPACE)
        verify(ic).commitText(" ", 1)
        assertEquals(ShiftState.OFF, typing.shiftState)
    }

    @Test
    fun `unknown special codes do nothing`() {
        typing.onKey(KeyCodes.SETTINGS)
        typing.onKey(KeyCodes.SYMBOLS)
        verify(ic, never()).commitText(any(), any())
    }

    // ── auto-capitalisation ──────────────────────────────────────────────────

    @Test
    fun `shift turns on where the field wants a capital letter`() {
        atSentenceStart()
        typing.updateShiftForCursor()
        assertEquals(ShiftState.ON, typing.shiftState)
    }

    @Test
    fun `shift turns back off once the cursor is mid-sentence`() {
        atSentenceStart()
        typing.updateShiftForCursor()
        whenever(ic.getCursorCapsMode(any())).doReturn(0)
        typing.onKey('h'.code)
        verify(ic).commitText("H", 1)
        assertEquals(ShiftState.OFF, typing.shiftState)
    }

    @Test
    fun `shift turns on after typing ends a sentence`() {
        typing.onKey('.'.code)
        atSentenceStart()
        typing.onKey(KeyCodes.SPACE)
        assertEquals(ShiftState.ON, typing.shiftState)
    }

    @Test
    fun `the field's own capitalisation flags are what is asked about`() {
        typing.updateShiftForCursor()
        verify(ic).getCursorCapsMode(editorInfo.inputType)
    }

    @Test
    fun `auto-capitalisation can be switched off`() {
        autoCapitalize = false
        atSentenceStart()
        typing.updateShiftForCursor()
        assertEquals(ShiftState.OFF, typing.shiftState)
    }

    @Test
    fun `auto-capitalisation does not apply to number fields`() {
        // In number fields the capitalisation bits mean something else (here: "signed").
        editorInfo.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
        atSentenceStart()
        typing.updateShiftForCursor()
        assertEquals(ShiftState.OFF, typing.shiftState)
    }

    @Test
    fun `auto-capitalisation leaves caps lock alone`() {
        typing.onKey(KeyCodes.SHIFT)
        typing.onKey(KeyCodes.SHIFT)
        typing.updateShiftForCursor()
        assertEquals(ShiftState.LOCKED, typing.shiftState)
    }

    @Test
    fun `resetShift clears caps lock`() {
        typing.onKey(KeyCodes.SHIFT)
        typing.onKey(KeyCodes.SHIFT)
        typing.resetShift()
        assertEquals(ShiftState.OFF, typing.shiftState)
    }

    // ── backspace ────────────────────────────────────────────────────────────

    @Test
    fun `backspace deletes one plain character`() {
        typing.onKey(KeyCodes.BACKSPACE)
        verify(ic).deleteSurroundingText(1, 0)
    }

    @Test
    fun `backspace deletes a whole emoji`() {
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("ok 😀")
        typing.onKey(KeyCodes.BACKSPACE)
        verify(ic).deleteSurroundingText(2, 0)
    }

    @Test
    fun `backspace deletes the selection instead of a character`() {
        whenever(ic.getSelectedText(any())).doReturn("selected")
        typing.onKey(KeyCodes.BACKSPACE)
        verify(ic).commitText("", 1)
        verify(ic, never()).deleteSurroundingText(any(), any())
    }

    @Test
    fun `backspace with no visible text sends the delete key instead`() {
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("")
        typing.onKey(KeyCodes.BACKSPACE)

        val events = argumentCaptor<KeyEvent>()
        verify(ic, times(2)).sendKeyEvent(events.capture())
        assertEquals(
            listOf(KeyEvent.ACTION_DOWN to KeyEvent.KEYCODE_DEL, KeyEvent.ACTION_UP to KeyEvent.KEYCODE_DEL),
            events.allValues.map { it.action to it.keyCode }
        )
        verify(ic, never()).deleteSurroundingText(any(), any())
    }

    @Test
    fun `backspace keeps a pending shift`() {
        typing.onKey(KeyCodes.SHIFT)
        typing.onKey(KeyCodes.SHIFT)
        typing.onKey(KeyCodes.BACKSPACE)
        assertEquals(ShiftState.LOCKED, typing.shiftState)
    }

    // ── enter ────────────────────────────────────────────────────────────────

    @Test
    fun `enter performs the field's action`() {
        editorInfo.imeOptions = EditorInfo.IME_ACTION_SEARCH
        typing.onKey(KeyCodes.ENTER)
        verify(ic).performEditorAction(EditorInfo.IME_ACTION_SEARCH)
        verify(ic, never()).commitText(any(), any())
    }

    @Test
    fun `enter ignores option flags when reading the action`() {
        editorInfo.imeOptions = EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_EXTRACT_UI
        typing.onKey(KeyCodes.ENTER)
        verify(ic).performEditorAction(EditorInfo.IME_ACTION_SEND)
    }

    @Test
    fun `enter inserts a line break when the field has no action`() {
        editorInfo.imeOptions = EditorInfo.IME_ACTION_NONE
        typing.onKey(KeyCodes.ENTER)
        verify(ic).commitText("\n", 1)
        verify(ic, never()).performEditorAction(any())
    }

    @Test
    fun `enter inserts a line break when the action is unspecified`() {
        editorInfo.imeOptions = EditorInfo.IME_ACTION_UNSPECIFIED
        typing.onKey(KeyCodes.ENTER)
        verify(ic).commitText("\n", 1)
        verify(ic, never()).performEditorAction(any())
    }

    @Test
    fun `enter inserts a line break when the field reserves enter for line breaks`() {
        editorInfo.imeOptions = EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_ENTER_ACTION
        typing.onKey(KeyCodes.ENTER)
        verify(ic).commitText("\n", 1)
        verify(ic, never()).performEditorAction(any())
    }

    // ── word suggestions ─────────────────────────────────────────────────────

    @Test
    fun `the word at the cursor is the word that ends there`() {
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("Hello wor")
        assertEquals("wor", typing.wordAtCursor())
    }

    @Test
    fun `there is no word at the cursor inside a word or with a selection`() {
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("Hello wor")
        whenever(ic.getTextAfterCursor(any(), any())).doReturn("ld")
        assertEquals("", typing.wordAtCursor())

        whenever(ic.getTextAfterCursor(any(), any())).doReturn(" ")
        whenever(ic.getSelectedText(any())).doReturn("wor")
        assertEquals("", typing.wordAtCursor())
    }

    @Test
    fun `a word filling the whole look-behind is not completed`() {
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("a".repeat(48))
        assertEquals("", typing.wordAtCursor())
    }

    @Test
    fun `picking a suggestion replaces the word and adds a space`() {
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("Hello wor", "Hello world ")
        typing.pickSuggestion("world")
        verify(ic).beginBatchEdit()
        verify(ic).deleteSurroundingText(3, 0)
        verify(ic).commitText("world ", 1)
        verify(ic).endBatchEdit()
    }

    @Test
    fun `picking a suggestion with no word at the cursor only inserts it`() {
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("Hello ", "Hello world ")
        typing.pickSuggestion("world")
        verify(ic, never()).deleteSurroundingText(any(), any())
        verify(ic).commitText("world ", 1)
    }

    @Test
    fun `punctuation after a picked suggestion takes the place of its space`() {
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("Hello wor", "Hello world ")
        typing.pickSuggestion("world")
        typing.onKey('.'.code)
        verify(ic).deleteSurroundingText(1, 0)
        verify(ic).commitText(". ", 1)
    }

    @Test
    fun `punctuation is typed normally if the text changed since the pick`() {
        whenever(ic.getTextBeforeCursor(any(), any()))
            .doReturn("Hello wor", "Hello world ", "Elsewhere ")
        typing.pickSuggestion("world")
        typing.onKey(','.code)
        verify(ic).commitText(",", 1)
        verify(ic, never()).deleteSurroundingText(1, 0)
    }

    @Test
    fun `punctuation is typed normally after another key`() {
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("Hello wor", "Hello world ")
        typing.pickSuggestion("world")
        typing.onKey(KeyCodes.SPACE)
        typing.onKey('.'.code)
        verify(ic).commitText(".", 1)
        verify(ic, never()).deleteSurroundingText(1, 0)
    }

    @Test
    fun `french keeps the space before an exclamation mark`() {
        val french = TypingController(
            connection = { ic },
            editorInfo = { editorInfo },
            autoCapitalize = { false },
            onShiftStateChanged = {},
            spaceSwappingPunctuation = { TypingController.FRENCH_SPACE_SWAPPING_PUNCTUATION }
        )
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("Bonjour mon", "Bonjour monde ")
        french.pickSuggestion("monde")
        french.onKey('!'.code)
        verify(ic).commitText("!", 1)
        verify(ic, never()).deleteSurroundingText(1, 0)
    }

    @Test
    fun `the picked suggestion is known until the text changes`() {
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("Hello Pas", "Hello Pasi ", "Hello Pasi ")
        typing.pickSuggestion("Pasi")
        assertEquals("Pasi", typing.pickedSuggestionIfUnchanged())

        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("Hello Pasi x")
        assertEquals(null, typing.pickedSuggestionIfUnchanged())
    }

    @Test
    fun `the picked suggestion is forgotten after the next key`() {
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("Hello Pas", "Hello Pasi ")
        typing.pickSuggestion("Pasi")
        typing.onKey(KeyCodes.SHIFT)
        assertEquals("Pasi", typing.pickedSuggestionIfUnchanged())
        typing.onKey('x'.code)
        assertEquals(null, typing.pickedSuggestionIfUnchanged())
    }

    @Test
    fun `the previous word is read before the cursor`() {
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("I went to ")
        assertEquals("to", typing.previousWord())
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("I went to t")
        assertEquals("to", typing.previousWord())
    }

    @Test
    fun `there is no previous word with a selection or inside a word`() {
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("I went to t")
        whenever(ic.getTextAfterCursor(any(), any())).doReturn("he")
        assertEquals("", typing.previousWord())
        whenever(ic.getTextAfterCursor(any(), any())).doReturn(" ")
        whenever(ic.getSelectedText(any())).doReturn("t")
        assertEquals("", typing.previousWord())
    }

    @Test
    fun `the finished word pair can skip the space a picked suggestion added`() {
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("going home")
        assertEquals("going" to "home", typing.finishedWordPair())
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("going home ")
        assertEquals(null, typing.finishedWordPair())
        assertEquals("going" to "home", typing.finishedWordPair(ignoreTrailingSpace = true))
    }

    // ── autocorrect ──────────────────────────────────────────────────────────

    @Test
    fun `autocorrecting replaces the word and types the separator`() {
        typing.autocorrectAndType("teh", "the", KeyCodes.SPACE)
        verify(ic).deleteSurroundingText(3, 0)
        verify(ic).commitText("the", 1)
        verify(ic).commitText(" ", 1)
    }

    @Test
    fun `backspace right after undoes the correction and keeps the separator`() {
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("I said the ")
        typing.autocorrectAndType("teh", "the", KeyCodes.SPACE)
        typing.onKey(KeyCodes.BACKSPACE)
        verify(ic).deleteSurroundingText(4, 0)
        verify(ic).commitText("teh ", 1)
        assertTrue(typing.isAutocorrectionRejected("TEH"))
    }

    @Test
    fun `backspace deletes normally once the text changed`() {
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("I said the ", "I said the x")
        typing.autocorrectAndType("teh", "the", KeyCodes.SPACE)
        typing.onKey(KeyCodes.BACKSPACE)
        verify(ic, never()).commitText("teh ", 1)
        verify(ic).deleteSurroundingText(1, 0)
    }

    @Test
    fun `backspace deletes normally after another key`() {
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("I said the ")
        typing.autocorrectAndType("teh", "the", KeyCodes.SPACE)
        typing.onKey('x'.code)
        typing.onKey(KeyCodes.BACKSPACE)
        verify(ic, never()).commitText("teh ", 1)
    }

    @Test
    fun `a new field forgets rejected corrections`() {
        whenever(ic.getTextBeforeCursor(any(), any())).doReturn("I said the ")
        typing.autocorrectAndType("teh", "the", KeyCodes.SPACE)
        typing.onKey(KeyCodes.BACKSPACE)
        typing.startField()
        assertFalse(typing.isAutocorrectionRejected("teh"))
    }
}
