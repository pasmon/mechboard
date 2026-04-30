package com.example.mechboard

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat

/**
 * Preference fragment that lets the user choose between:
 *  - Keyboard language (English, Finnish, German, French, Spanish)
 *  - Sound profile
 *  - Master sound toggle
 *  - Volume level (0–100)
 *  - Keyboard theme (Dark, Solarized Dark, Solarized Light, Dracula, Nord, Monokai)
 *
 * Entries for all [ListPreference] widgets are derived at runtime from their
 * respective enums ([KeyboardLayout], [SoundProfile], and [KeyboardTheme]) so
 * that the XML and the enums stay in sync automatically.
 */
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = MechboardService.PREFS_NAME
        setPreferencesFromResource(R.xml.preferences, rootKey)
        bindKeyboardLayoutEntries()
        bindSoundProfileEntries()
        bindKeyboardThemeEntries()
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
}
