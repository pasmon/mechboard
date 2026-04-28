@file:Suppress("DEPRECATION")

package com.example.mechboard

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.View
import android.view.inputmethod.EditorInfo

/**
 * The mechboard [InputMethodService].
 *
 * Inflates a [KeyboardView] backed by an XML [Keyboard] definition, plays a
 * mechanical key-click sound through [SoundManager] on every key event, and
 * provides a dedicated key to open [SettingsActivity].
 *
 * Note: [android.inputmethodservice.Keyboard] / [KeyboardView] are deprecated
 * in API 29 but remain available on all supported API levels (21+). A migration
 * to a custom View is planned as a follow-up.
 */
class MechboardService : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var keyboardView: KeyboardView
    /**
     * Keyboard definition loaded once in [onCreate] and reused across
     * [onCreateInputView] calls to avoid redundant parsing.
     */
    private lateinit var keyboard: Keyboard
    private lateinit var soundManager: SoundManager

    private var isCapsLock = false

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        keyboard = Keyboard(this, R.xml.keyboard)
        soundManager = SoundManager(
            this,
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        )
    }

    override fun onCreateInputView(): View {
        // Inflate the wrapper layout (LinearLayout) and look up the KeyboardView
        // inside it. Casting the root to KeyboardView would throw ClassCastException.
        val rootView = layoutInflater.inflate(R.layout.input, null)
        keyboardView = rootView.findViewById(R.id.keyboard_view)
        keyboardView.keyboard = keyboard
        keyboardView.setOnKeyboardActionListener(this)
        keyboardView.isPreviewEnabled = false
        return rootView
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }

    // -------------------------------------------------------------------------
    // KeyboardView.OnKeyboardActionListener
    // -------------------------------------------------------------------------

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        soundManager.playKeySound()
        val ic = currentInputConnection ?: return

        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> ic.deleteSurroundingText(1, 0)
            Keyboard.KEYCODE_SHIFT  -> handleShift()
            Keyboard.KEYCODE_DONE   -> {
                // Honour the field's requested action (Search, Send, Next, Done…).
                // Fall back to committing a newline for multiline fields (no action set).
                val imeAction = currentInputEditorInfo
                    ?.imeOptions
                    ?.and(EditorInfo.IME_MASK_ACTION)
                    ?: EditorInfo.IME_ACTION_NONE
                if (imeAction != EditorInfo.IME_ACTION_NONE) {
                    ic.performEditorAction(imeAction)
                } else {
                    ic.commitText("\n", 1)
                }
            }
            KEYCODE_SPACE           -> ic.commitText(" ", 1)
            KEYCODE_SETTINGS        -> openSettings()
            else -> {
                val ch = primaryCode.toChar()
                ic.commitText(
                    if (isCapsLock) ch.uppercaseChar().toString() else ch.toString(),
                    1
                )
            }
        }
    }

    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun onText(text: CharSequence?) {
        val safeText = text ?: return
        currentInputConnection?.commitText(safeText, 1)
    }
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun handleShift() {
        isCapsLock = !isCapsLock
        keyboard.isShifted = isCapsLock
        keyboardView.invalidateAllKeys()
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    companion object {
        const val PREFS_NAME = "mechboard_prefs"

        /** Key code for the space bar (ASCII space). */
        const val KEYCODE_SPACE = 32

        /** Key code for the dedicated settings key (negative = custom). */
        const val KEYCODE_SETTINGS = -101
    }
}
