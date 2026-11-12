package io.github.pasmon.mechboard

/**
 * SharedPreferences key constants used by [SoundManager], [SettingsFragment],
 * and [MechboardService].
 *
 * Keeping them in a separate file means they have no Android-SDK dependency
 * and can be referenced from plain JVM unit tests.
 */
object PrefsKeys {
    const val SOUND_PROFILE   = "sound_profile"
    const val SOUND_ENABLED   = "sound_enabled"
    const val VOLUME          = "volume"
    const val KEYBOARD_LAYOUT = "keyboard_layout"
    const val KEYBOARD_THEME  = "keyboard_theme"
    const val KEYBOARD_FONT   = "keyboard_font"
    /** Whether sentences start with a capital letter automatically. Boolean, default true. */
    const val AUTO_CAPITALIZE = "auto_capitalize"
    /** Whether word suggestions are shown above the keys. Boolean, default true. */
    const val WORD_SUGGESTIONS = "word_suggestions"
}
