@file:Suppress("DEPRECATION")

package com.example.mechboard

import android.content.Intent
import android.content.SharedPreferences
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
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
     * Keyboard definition reloaded whenever the layout changes.
     */
    private lateinit var keyboard: Keyboard
    private lateinit var soundManager: SoundManager
    private lateinit var prefs: SharedPreferences

    private var isCapsLock = false
    /** Resource id of the currently active keyboard XML definition. */
    private var currentLayoutResId = 0

    /**
     * Ordered list of all supported layouts built from [KeyboardLayout] entries.
     * Used to resolve the layout selected via [SettingsActivity].
     * Initialised in [onCreate] where [android.content.res.Resources] is available.
     */
    private lateinit var layoutCycle: List<Pair<KeyboardLayout, Int>>

    /**
     * Listens for changes to [PrefsKeys.KEYBOARD_LAYOUT] or [PrefsKeys.KEYBOARD_THEME] made via
     * [SettingsActivity] and applies the change immediately, even while the keyboard is visible.
     */
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            PrefsKeys.KEYBOARD_LAYOUT -> applyLayoutFromPrefs()
            PrefsKeys.KEYBOARD_THEME  -> if (::keyboardView.isInitialized) setInputView(onCreateInputView())
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val resolvedLayoutCycle = KeyboardLayout.values().mapNotNull { layout ->
            val resId = resources.getIdentifier(layout.xmlResName, "xml", packageName)
            if (resId == 0) null else layout to resId
        }
        layoutCycle = resolvedLayoutCycle.ifEmpty {
            throw IllegalStateException(
                "No keyboard layouts could be resolved from KeyboardLayout. " +
                    "Check xmlResName values and XML resources under res/xml."
            )
        }
        currentLayoutResId = layoutResIdFromId(
            prefs.getString(PrefsKeys.KEYBOARD_LAYOUT, KeyboardLayout.ENGLISH.id)
                ?: KeyboardLayout.ENGLISH.id
        )
        keyboard = Keyboard(this, currentLayoutResId)
        soundManager = SoundManager(this, prefs)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onCreateInputView(): View {
        // Inflate the wrapper layout (LinearLayout) and look up the KeyboardView
        // inside it. Casting the root to KeyboardView would throw ClassCastException.
        // A ContextThemeWrapper applies the active KeyboardTheme so that all ?attr/
        // colour references in input.xml and key_background.xml are resolved correctly.
        val themeStyleResId = styleResIdForTheme(
            KeyboardTheme.fromId(
                prefs.getString(PrefsKeys.KEYBOARD_THEME, KeyboardTheme.DARK.id)
                    ?: KeyboardTheme.DARK.id
            )
        )
        val themedContext = ContextThemeWrapper(this, themeStyleResId)
        val rootView = LayoutInflater.from(themedContext).inflate(R.layout.input, null)
        keyboardView = rootView.findViewById(R.id.keyboard_view)
        keyboardView.keyboard = keyboard
        keyboardView.setOnKeyboardActionListener(this)
        keyboardView.isPreviewEnabled = false
        return rootView
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
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

    /**
     * Applies the layout stored in [PrefsKeys.KEYBOARD_LAYOUT] immediately.
     * Called on startup and whenever the preference changes (e.g. from [SettingsActivity]).
     */
    private fun applyLayoutFromPrefs() {
        val layoutId = prefs.getString(PrefsKeys.KEYBOARD_LAYOUT, KeyboardLayout.ENGLISH.id)
            ?: KeyboardLayout.ENGLISH.id
        val newResId = layoutResIdFromId(layoutId)
        if (newResId == currentLayoutResId) return
        currentLayoutResId = newResId
        isCapsLock = false
        keyboard = Keyboard(this, currentLayoutResId)
        if (::keyboardView.isInitialized) {
            keyboardView.keyboard = keyboard
            keyboardView.invalidateAllKeys()
        }
    }

    /** Maps a stored layout id (e.g. [KeyboardLayout.ENGLISH.id]) to its XML resource id. */
    private fun layoutResIdFromId(layoutId: String): Int =
        layoutCycle.firstOrNull { it.first.id == layoutId }?.second ?: layoutCycle.first().second

    /**
     * Returns the Android style resource ID for the given [theme].
     * Kept in [MechboardService] so that [KeyboardTheme] has no [R] dependency.
     */
    private fun styleResIdForTheme(theme: KeyboardTheme): Int = when (theme) {
        KeyboardTheme.DARK           -> R.style.Theme_Mechboard_Keyboard_Dark
        KeyboardTheme.SOLARIZED_DARK -> R.style.Theme_Mechboard_Keyboard_SolarizedDark
        KeyboardTheme.SOLARIZED_LIGHT -> R.style.Theme_Mechboard_Keyboard_SolarizedLight
        KeyboardTheme.DRACULA        -> R.style.Theme_Mechboard_Keyboard_Dracula
        KeyboardTheme.NORD           -> R.style.Theme_Mechboard_Keyboard_Nord
        KeyboardTheme.MONOKAI        -> R.style.Theme_Mechboard_Keyboard_Monokai
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
