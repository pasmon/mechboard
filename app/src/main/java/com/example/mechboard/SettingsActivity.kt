package com.example.mechboard

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat

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
        matchSystemBarIconsToBackground()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }

    /**
     * The window draws behind the status and navigation bars, so their icons sit
     * on the theme's background colour. Makes the icons dark on a light background
     * (Solarized Light) and light on a dark one.
     */
    private fun matchSystemBarIconsToBackground() {
        val background = TypedValue()
        if (!theme.resolveAttribute(android.R.attr.colorBackground, background, true)) return
        val isLight = ColorUtils.calculateLuminance(background.data) > LIGHT_BACKGROUND_LUMINANCE
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = isLight
            isAppearanceLightNavigationBars = isLight
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

    private companion object {
        /** Backgrounds brighter than this get dark system bar icons. */
        const val LIGHT_BACKGROUND_LUMINANCE = 0.5
    }
}
