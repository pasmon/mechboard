package io.github.pasmon.mechboard

import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

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
    private val onShiftStateChanged: (ShiftState) -> Unit
) {

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
        when {
            code == KeyCodes.BACKSPACE -> deleteBackwards(ic)
            code == KeyCodes.ENTER     -> enter(ic)
            code > 0                   -> typeCodePoint(ic, code)
            else                       -> return
        }
        updateShiftForCursor()
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

    private companion object {
        /**
         * How many `char`s before the cursor to look at when deleting. Long enough
         * for the longest emoji sequences (a family emoji is 11 `char`s).
         */
        const val GRAPHEME_LOOKBEHIND = 32
    }
}
