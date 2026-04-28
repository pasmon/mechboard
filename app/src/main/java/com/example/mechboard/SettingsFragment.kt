package com.example.mechboard

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat

/**
 * Preference fragment that lets the user choose between:
 *  - Sound profile (Cherry MX Blue / Red / Brown, Topre, Alps, Silent)
 *  - Master sound toggle
 *  - Volume level (0–100)
 *
 * Entries and values for the [ListPreference] are derived from [SoundProfile]
 * at runtime so that the XML and the enum stay in sync automatically.
 */
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = MechboardService.PREFS_NAME
        setPreferencesFromResource(R.xml.preferences, rootKey)
        bindSoundProfileEntries()
    }

    private fun bindSoundProfileEntries() {
        val pref = findPreference<ListPreference>(PrefsKeys.SOUND_PROFILE)
            ?: return
        val profiles = SoundProfile.values()
        pref.entries     = profiles.map { it.displayName }.toTypedArray()
        pref.entryValues = profiles.map { it.id }.toTypedArray()
    }
}
