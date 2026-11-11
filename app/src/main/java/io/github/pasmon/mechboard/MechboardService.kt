package io.github.pasmon.mechboard

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * The mechboard [InputMethodService].
 *
 * Shows a [MechboardKeyboardView], plays a mechanical key-click sound through
 * [SoundManager] on every key press, edits the text field through
 * [TypingController], and provides a dedicated key to open [SettingsActivity].
 */
class MechboardService : InputMethodService(), MechboardKeyboardView.Listener {

    private lateinit var keyboardView: MechboardKeyboardView
    private lateinit var soundManager: SoundManager
    private lateinit var prefs: SharedPreferences

    private val typing = TypingController(
        connection = { currentInputConnection },
        editorInfo = { currentInputEditorInfo },
        autoCapitalize = { prefs.getBoolean(PrefsKeys.AUTO_CAPITALIZE, true) },
        onShiftStateChanged = { state ->
            if (::keyboardView.isInitialized) keyboardView.shiftState = state
        }
    )

    /** The language layout selected in [SettingsActivity]. */
    private var currentLayout = KeyboardLayout.ENGLISH
    /** Whether the symbols/numbers keyboard is currently displayed. */
    private var isSymbolsMode = false

    /**
     * Listens for changes to [PrefsKeys.KEYBOARD_LAYOUT], [PrefsKeys.KEYBOARD_THEME], or
     * [PrefsKeys.KEYBOARD_FONT] made via [SettingsActivity] and applies the change
     * immediately, even while the keyboard is visible.
     */
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            PrefsKeys.KEYBOARD_LAYOUT -> applyLayoutFromPrefs()
            PrefsKeys.KEYBOARD_THEME  -> if (::keyboardView.isInitialized) setInputView(onCreateInputView())
            PrefsKeys.KEYBOARD_FONT   -> applyFontFromPrefs()
            PrefsKeys.AUTO_CAPITALIZE -> typing.updateShiftForCursor()
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        currentLayout = layoutFromPrefs()
        soundManager = SoundManager(this, prefs)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onCreateInputView(): View {
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
        keyboardView.listener = this
        keyboardView.shiftState = typing.shiftState
        showCurrentKeyboard()
        applyFontFromPrefs()
        keepClearOfSystemBars(rootView)
        return rootView
    }

    /**
     * Pads [rootView] by the size of the navigation bar and any display cutout.
     *
     * On Android 15 and later the keyboard window extends behind the navigation
     * bar; without the padding the bottom row of keys would sit under it. The
     * padded area shows [rootView]'s background, i.e. the keyboard colour. Where
     * the window does not extend behind the bars the insets are zero.
     */
    private fun keepClearOfSystemBars(rootView: View) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(bars.left, 0, bars.right, bars.bottom)
            insets
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (!restarting) {
            // A new field: start on the keyboard that suits what it asks for.
            isSymbolsMode = wantsSymbolsFirst(info)
            showCurrentKeyboard()
            typing.resetShift()
        }
        typing.updateShiftForCursor()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd
        )
        // The cursor moved, possibly to or from the start of a sentence.
        typing.updateShiftForCursor()
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        soundManager.release()
    }

    // -------------------------------------------------------------------------
    // MechboardKeyboardView.Listener
    // -------------------------------------------------------------------------

    /**
     * Plays the key sound on touch-down so it lines up with the finger, the way a
     * switch sounds when it is pressed rather than when it is let go.
     */
    override fun onPress(code: Int) {
        soundManager.playKeySound(KeyKind.fromKeyCode(code))
    }

    override fun onKey(code: Int, isRepeat: Boolean) {
        // The first event of a press already made its sound in onPress.
        if (isRepeat) soundManager.playKeySound(KeyKind.fromKeyCode(code))
        when (code) {
            KeyCodes.SETTINGS -> openSettings()
            KeyCodes.SYMBOLS  -> {
                isSymbolsMode = !isSymbolsMode
                showCurrentKeyboard()
            }
            else -> typing.onKey(code)
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Shows the symbols keyboard or the current language's letters, per [isSymbolsMode]. */
    private fun showCurrentKeyboard() {
        if (!::keyboardView.isInitialized) return
        keyboardView.definition =
            if (isSymbolsMode) KeyboardDefinitions.SYMBOLS else currentLayout.definition
    }

    /** Whether a field is for numbers, so that the digits should be shown first. */
    private fun wantsSymbolsFirst(info: EditorInfo?): Boolean =
        when ((info?.inputType ?: InputType.TYPE_NULL) and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_NUMBER,
            InputType.TYPE_CLASS_PHONE,
            InputType.TYPE_CLASS_DATETIME -> true
            else -> false
        }

    private fun layoutFromPrefs(): KeyboardLayout = KeyboardLayout.fromId(
        prefs.getString(PrefsKeys.KEYBOARD_LAYOUT, KeyboardLayout.ENGLISH.id)
            ?: KeyboardLayout.ENGLISH.id
    )

    /**
     * Applies the layout stored in [PrefsKeys.KEYBOARD_LAYOUT] immediately.
     * Called whenever the preference changes (e.g. from [SettingsActivity]).
     */
    private fun applyLayoutFromPrefs() {
        currentLayout = layoutFromPrefs()
        isSymbolsMode = false
        showCurrentKeyboard()
    }

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

    /**
     * Reads [PrefsKeys.KEYBOARD_FONT] and applies the resolved [Typeface] to the
     * [keyboardView].  No-op if the view has not been initialised yet.
     */
    private fun applyFontFromPrefs() {
        if (!::keyboardView.isInitialized) return
        val fontId = prefs.getString(PrefsKeys.KEYBOARD_FONT, KeyboardFont.DEFAULT.id)
            ?: KeyboardFont.DEFAULT.id
        keyboardView.labelTypeface = resolveTypeface(KeyboardFont.fromId(fontId))
    }

    /**
     * Resolves a [Typeface] for the given [font].
     * Returns `null` for [KeyboardFont.DEFAULT] (system typeface).
     * For bundled fonts the resource is looked up by name via [resources.getIdentifier]
     * so that [KeyboardFont] has no direct [R] dependency.
     */
    private fun resolveTypeface(font: KeyboardFont): Typeface? {
        val resName = font.fontResName ?: return null
        val resId = resources.getIdentifier(resName, "font", packageName)
        if (resId == 0) return null
        return runCatching { ResourcesCompat.getFont(this, resId) }.getOrNull()
    }

    companion object {
        const val PREFS_NAME = "mechboard_prefs"
    }
}
