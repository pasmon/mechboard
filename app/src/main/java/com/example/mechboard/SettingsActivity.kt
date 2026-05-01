package com.example.mechboard

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Hosts [SettingsFragment] and acts as the entry point for the keyboard
 * settings screen that users reach via the dedicated settings key or the
 * system's "Language & input" settings page.
 *
 * The Activity theme is set dynamically before [super.onCreate] to mirror
 * the keyboard colour theme selected by the user.
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences(MechboardService.PREFS_NAME, Context.MODE_PRIVATE)
        val themeId = prefs.getString(PrefsKeys.KEYBOARD_THEME, null) ?: KeyboardTheme.DARK.id
        setTheme(settingsThemeResId(KeyboardTheme.fromId(themeId)))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    /**
     * Maps a [KeyboardTheme] to the corresponding settings Activity style resource.
     * Kept here so that [KeyboardTheme] has no [R] dependency and stays unit-testable.
     */
    private fun settingsThemeResId(theme: KeyboardTheme): Int = when (theme) {
        KeyboardTheme.DARK           -> R.style.Theme_Mechboard_Settings_Dark
        KeyboardTheme.SOLARIZED_DARK -> R.style.Theme_Mechboard_Settings_SolarizedDark
        KeyboardTheme.SOLARIZED_LIGHT -> R.style.Theme_Mechboard_Settings_SolarizedLight
        KeyboardTheme.DRACULA        -> R.style.Theme_Mechboard_Settings_Dracula
        KeyboardTheme.NORD           -> R.style.Theme_Mechboard_Settings_Nord
        KeyboardTheme.MONOKAI        -> R.style.Theme_Mechboard_Settings_Monokai
    }
}
