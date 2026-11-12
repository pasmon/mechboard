package io.github.pasmon.mechboard

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

/**
 * Preference fragment that lets the user choose between:
 *  - Keyboard language (English, Finnish, German, French, Spanish)
 *  - Sound profile
 *  - Master sound toggle
 *  - Volume level (0–100)
 *  - Keyboard theme (Dark, Solarized Dark, Solarized Light, Dracula, Nord, Monokai)
 *  - Keyboard font (Default, JetBrains Mono, Share Tech Mono)
 *  - Word suggestions, and a link to the [PersonalDictionaryFragment]
 *  - Learning next words from typing, and clearing what was learned
 *
 * Entries for all [ListPreference] widgets are derived at runtime from their
 * respective enums ([KeyboardLayout], [SoundProfile], [KeyboardTheme], and
 * [KeyboardFont]) so that the XML and the enums stay in sync automatically.
 */
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = MechboardService.PREFS_NAME
        setPreferencesFromResource(R.xml.preferences, rootKey)
        bindKeyboardLayoutEntries()
        bindSoundProfileEntries()
        bindKeyboardThemeEntries()
        bindKeyboardFontEntries()
        bindClearLearnedWords()
    }

    /** Clears the [LearnedWordPairs] after the user confirms. */
    private fun bindClearLearnedWords() {
        val pref = findPreference<Preference>(PrefsKeys.CLEAR_LEARNED_WORDS) ?: return
        pref.setOnPreferenceClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.clear_learned_words_confirm)
                .setPositiveButton(R.string.clear_learned_words_action) { _, _ ->
                    LearnedWordPairs.of(requireContext()).clear()
                    Toast.makeText(requireContext(), R.string.clear_learned_words_done, Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        // Back from the personal dictionary, which shows its own title.
        requireActivity().setTitle(R.string.settings_title)
    }

    private fun bindKeyboardLayoutEntries() {
        val pref = findPreference<ListPreference>(PrefsKeys.KEYBOARD_LAYOUT) ?: return
        val layouts = KeyboardLayout.values()
        pref.entries     = layouts.map { it.displayName }.toTypedArray()
        pref.entryValues = layouts.map { it.id }.toTypedArray()
    }

    private fun bindSoundProfileEntries() {
        val pref = findPreference<ListPreference>(PrefsKeys.SOUND_PROFILE)
            ?: return
        val profiles = SoundProfile.values()
        pref.entries     = profiles.map { it.displayName }.toTypedArray()
        pref.entryValues = profiles.map { it.id }.toTypedArray()
    }

    private fun bindKeyboardThemeEntries() {
        val pref = findPreference<ListPreference>(PrefsKeys.KEYBOARD_THEME) ?: return
        val themes = KeyboardTheme.values()
        pref.entries     = themes.map { it.displayName }.toTypedArray()
        pref.entryValues = themes.map { it.id }.toTypedArray()
    }

    private fun bindKeyboardFontEntries() {
        val pref = findPreference<ListPreference>(PrefsKeys.KEYBOARD_FONT) ?: return
        val fonts = KeyboardFont.values()
        pref.entries     = fonts.map { it.displayName }.toTypedArray()
        pref.entryValues = fonts.map { it.id }.toTypedArray()
    }
}
