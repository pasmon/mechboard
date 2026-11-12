package io.github.pasmon.mechboard

import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import java.util.Locale

/**
 * Turns key codes into edits of the text field the keyboard is attached to,
 * and keeps track of the [ShiftState].
 *
 * Everything it needs from the input method service is passed in as functions,
 * so it can be tested without one.
 */
class TypingController(
    /** The connection to the current text field, or `null` when there is none. */
    private val connection: () -> InputConnection?,
    /** Describes the current text field, or `null` when there is none. */
    private val editorInfo: () -> EditorInfo?,
    /** Whether sentences should start with a capital letter where the field asks for it. */
    private val autoCapitalize: () -> Boolean,
    private val onShiftStateChanged: (ShiftState) -> Unit,
    /**
     * Code points of the punctuation that takes the place of the space a picked
     * suggestion added ("word ." becomes "word. "). Depends on the language:
     * French keeps a space before "!", "?", ":" and ";".
     */
    private val spaceSwappingPunctuation: () -> Set<Int> = { SPACE_SWAPPING_PUNCTUATION }
) {

    /**
     * The text before the cursor right after a suggestion was picked, until the
     * next key; `null` otherwise. See [typePunctuationAfterSuggestion].
     */
    private var textAfterPickedSuggestion: String? = null

    /** The suggestion picked last, valid while [textAfterPickedSuggestion] is set. */
    private var pickedSuggestion: String? = null

    /** What [autocorrectAndType] changed, until the next key; see [undoAutocorrection]. */
    private var lastAutocorrection: Autocorrection? = null

    private class Autocorrection(
        val typed: String,
        val correction: String,
        val separator: String,
        /** The text before the cursor right after the correction and separator. */
        val textAfter: String?
    )

    /** Words, in lower case, whose autocorrection was undone in this field; not corrected again. */
    private val rejectedCorrections = mutableSetOf<String>()

    var shiftState: ShiftState = ShiftState.OFF
        private set(value) {
            if (field == value) return
            field = value
            onShiftStateChanged(value)
        }

    /**
     * Handles a typed key: [KeyCodes.SHIFT], [KeyCodes.BACKSPACE], [KeyCodes.ENTER]
     * or a code point to insert. Codes below zero other than those are ignored.
     */
    fun onKey(code: Int) {
        if (code == KeyCodes.SHIFT) {
            shiftState = shiftState.afterShiftTap()
            return
        }
        val ic = connection() ?: return
        val textAfterSuggestion = textAfterPickedSuggestion
        textAfterPickedSuggestion = null
        val autocorrection = lastAutocorrection
        lastAutocorrection = null
        if (code == KeyCodes.BACKSPACE && autocorrection != null && undoAutocorrection(ic, autocorrection)) {
            updateShiftForCursor()
            return
        }
        when {
            code == KeyCodes.BACKSPACE -> deleteBackwards(ic)
            code == KeyCodes.ENTER     -> enter(ic)
            code > 0                   -> {
                if (textAfterSuggestion != null && code in spaceSwappingPunctuation()) {
                    typePunctuationAfterSuggestion(ic, code, textAfterSuggestion)
                } else {
                    typeCodePoint(ic, code)
                }
            }
            else                       -> return
        }
        updateShiftForCursor()
    }

    /**
     * Returns the word that ends at the cursor, the one suggestions are offered
     * for, or an empty string when there is none: nothing before the cursor is
     * part of a word, the cursor is inside a word, or text is selected.
     */
    fun wordAtCursor(): String {
        val ic = connection() ?: return ""
        if (!ic.getSelectedText(0).isNullOrEmpty()) return ""
        val after = ic.getTextAfterCursor(1, 0)
        if (after != null && WordSuggestions.continuesWord(after)) return ""
        val before = ic.getTextBeforeCursor(WORD_LOOKBEHIND, 0) ?: return ""
        val word = WordSuggestions.wordBeforeCursor(before)
        // A word filling the whole look-behind may have started before it.
        return if (word.length >= WORD_LOOKBEHIND) "" else word
    }

    /**
     * Returns the word before the one at the cursor (see [wordAtCursor]), or
     * before the cursor when no word is being typed; see
     * [WordSuggestions.previousWord]. Empty when there is none, the cursor is
     * inside a word, or text is selected.
     */
    fun previousWord(): String {
        val ic = connection() ?: return ""
        if (!ic.getSelectedText(0).isNullOrEmpty()) return ""
        val after = ic.getTextAfterCursor(1, 0)
        if (after != null && WordSuggestions.continuesWord(after)) return ""
        val before = ic.getTextBeforeCursor(PAIR_LOOKBEHIND, 0) ?: return ""
        return WordSuggestions.previousWord(before)
    }

    /**
     * Returns the previous word and the word that ends at the cursor, or `null`
     * unless both exist; see [WordSuggestions.finishedWordPair]. With
     * [ignoreTrailingSpace], one space right before the cursor is skipped, for
     * reading a word that a picked suggestion just inserted.
     */
    fun finishedWordPair(ignoreTrailingSpace: Boolean = false): Pair<String, String>? {
        val before = connection()?.getTextBeforeCursor(PAIR_LOOKBEHIND, 0) ?: return null
        val text = if (ignoreTrailingSpace && before.endsWith(" ")) before.dropLast(1) else before
        return WordSuggestions.finishedWordPair(text)
    }

    /**
     * Replaces the word at the cursor (see [wordAtCursor]) with [suggestion]
     * followed by a space, as when a suggestion is picked from the strip.
     */
    fun pickSuggestion(suggestion: String) {
        val ic = connection() ?: return
        val typed = wordAtCursor()
        ic.beginBatchEdit()
        if (typed.isNotEmpty()) ic.deleteSurroundingText(typed.length, 0)
        ic.commitText("$suggestion ", 1)
        ic.endBatchEdit()
        textAfterPickedSuggestion = ic.getTextBeforeCursor(WORD_LOOKBEHIND, 0)?.toString()
        pickedSuggestion = suggestion
        updateShiftForCursor()
    }

    /**
     * Returns the suggestion picked last if nothing happened since: no key was
     * typed and the text before the cursor is still what the pick left behind.
     * Otherwise `null`.
     */
    fun pickedSuggestionIfUnchanged(): String? {
        val expected = textAfterPickedSuggestion ?: return null
        val before = connection()?.getTextBeforeCursor(WORD_LOOKBEHIND, 0)?.toString()
        return if (before == expected) pickedSuggestion else null
    }

    /**
     * Replaces the word at the cursor, [typed], with [correction], then types
     * [code] (a space or punctuation) as [onKey] would. A backspace right after
     * undoes the correction; see [undoAutocorrection].
     */
    fun autocorrectAndType(typed: String, correction: String, code: Int) {
        val ic = connection() ?: return
        ic.beginBatchEdit()
        ic.deleteSurroundingText(typed.length, 0)
        ic.commitText(correction, 1)
        ic.endBatchEdit()
        onKey(code)
        lastAutocorrection = Autocorrection(
            typed, correction, String(Character.toChars(code)),
            ic.getTextBeforeCursor(WORD_LOOKBEHIND, 0)?.toString()
        )
    }

    /** Whether the autocorrection of [word] was undone in this field, so it should be left alone. */
    fun isAutocorrectionRejected(word: String): Boolean = word.lowercase(Locale.ROOT) in rejectedCorrections

    /**
     * Puts back the word [autocorrection] replaced, keeping the separator
     * ("the " becomes "teh "), if the text before the cursor is still what the
     * correction left behind. Returns whether it did.
     */
    private fun undoAutocorrection(ic: InputConnection, autocorrection: Autocorrection): Boolean {
        val before = ic.getTextBeforeCursor(WORD_LOOKBEHIND, 0)?.toString()
        if (before == null || before != autocorrection.textAfter) return false
        ic.beginBatchEdit()
        ic.deleteSurroundingText(autocorrection.correction.length + autocorrection.separator.length, 0)
        ic.commitText(autocorrection.typed + autocorrection.separator, 1)
        ic.endBatchEdit()
        rejectedCorrections += autocorrection.typed.lowercase(Locale.ROOT)
        return true
    }

    /**
     * Forgets everything about the previous field: shift, caps lock, picked
     * suggestions and autocorrections. Call when a new field gains focus.
     */
    fun startField() {
        resetShift()
        textAfterPickedSuggestion = null
        lastAutocorrection = null
        rejectedCorrections.clear()
    }

    /** Forgets a pending one-letter shift or caps lock, e.g. when a new field gains focus. */
    fun resetShift() {
        shiftState = ShiftState.OFF
    }

    /**
     * Applies auto-capitalisation: turns shift on when the cursor is where the text
     * field wants a capital letter (typically the start of a sentence) and off
     * where it does not. Caps lock is left alone.
     *
     * Call after the text or the cursor position changed.
     */
    fun updateShiftForCursor() {
        if (shiftState == ShiftState.LOCKED) return
        shiftState = if (wantsCapitalLetter()) ShiftState.ON else ShiftState.OFF
    }

    private fun wantsCapitalLetter(): Boolean {
        if (!autoCapitalize()) return false
        val inputType = editorInfo()?.inputType ?: return false
        // The capitalisation flags only mean something for text fields; in number
        // fields the same bits have other meanings.
        if (inputType and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return false
        val ic = connection() ?: return false
        return ic.getCursorCapsMode(inputType) != 0
    }

    /**
     * Types punctuation such as "." or "," right after a picked suggestion, before
     * the space the suggestion added: "word ." becomes "word. ".
     *
     * Only does so if the text before the cursor is still what the pick left
     * behind ([textAfterSuggestion]); if the cursor has moved or the text was
     * changed since, the punctuation is typed normally.
     */
    private fun typePunctuationAfterSuggestion(
        ic: InputConnection, code: Int, textAfterSuggestion: String
    ) {
        val before = ic.getTextBeforeCursor(WORD_LOOKBEHIND, 0)?.toString()
        if (before != textAfterSuggestion || !before.endsWith(" ")) {
            typeCodePoint(ic, code)
            return
        }
        ic.beginBatchEdit()
        ic.deleteSurroundingText(1, 0)
        ic.commitText(String(Character.toChars(code)) + " ", 1)
        ic.endBatchEdit()
        shiftState = shiftState.afterCharacter()
    }

    private fun typeCodePoint(ic: InputConnection, code: Int) {
        val typed = if (shiftState.isUppercase) Character.toUpperCase(code) else code
        ic.commitText(String(Character.toChars(typed)), 1)
        shiftState = shiftState.afterCharacter()
    }

    /** Deletes the selection if there is one, otherwise the character before the cursor. */
    private fun deleteBackwards(ic: InputConnection) {
        if (!ic.getSelectedText(0).isNullOrEmpty()) {
            ic.commitText("", 1)
            return
        }
        val before = ic.getTextBeforeCursor(GRAPHEME_LOOKBEHIND, 0)
        if (before.isNullOrEmpty()) {
            // Nothing visible to us before the cursor. The field may still have
            // something to delete (some do not report their text), so let it
            // decide by sending the key it would get from a hardware keyboard.
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
        } else {
            ic.deleteSurroundingText(lastGraphemeLength(before), 0)
        }
    }

    /**
     * Performs the text field's action (Search, Send, Next, Done…), or inserts a
     * line break if the field has no action or reserves enter for line breaks.
     */
    private fun enter(ic: InputConnection) {
        val options = editorInfo()?.imeOptions ?: EditorInfo.IME_ACTION_UNSPECIFIED
        val action = options and EditorInfo.IME_MASK_ACTION
        val wantsLineBreak = options and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0 ||
            action == EditorInfo.IME_ACTION_NONE ||
            action == EditorInfo.IME_ACTION_UNSPECIFIED
        if (wantsLineBreak) {
            ic.commitText("\n", 1)
        } else {
            ic.performEditorAction(action)
        }
        shiftState = shiftState.afterCharacter()
    }

    companion object {
        /** Punctuation that takes the place of the space a picked suggestion added, in most languages. */
        val SPACE_SWAPPING_PUNCTUATION: Set<Int> = ".,!?:;".map { it.code }.toSet()

        /** [SPACE_SWAPPING_PUNCTUATION] for French, which keeps a space before "!", "?", ":" and ";". */
        val FRENCH_SPACE_SWAPPING_PUNCTUATION: Set<Int> = ".,".map { it.code }.toSet()

        /**
         * How many `char`s before the cursor to look at for the word being typed.
         * Words longer than this are not completed.
         */
        private const val WORD_LOOKBEHIND = 48

        /** Room for two words and the spaces between them; see [previousWord]. */
        private const val PAIR_LOOKBEHIND = 2 * WORD_LOOKBEHIND + 8

        /**
         * How many `char`s before the cursor to look at when deleting. Long enough
         * for the longest emoji sequences (a family emoji is 11 `char`s).
         */
        private const val GRAPHEME_LOOKBEHIND = 32
    }
}
