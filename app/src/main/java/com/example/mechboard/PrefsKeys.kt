package com.example.mechboard

/**
 * SharedPreferences key constants used by [SoundManager] and [SettingsFragment].
 *
 * Keeping them in a separate file means they have no Android-SDK dependency
 * and can be referenced from plain JVM unit tests.
 */
object PrefsKeys {
    const val SOUND_PROFILE = "sound_profile"
    const val SOUND_ENABLED = "sound_enabled"
    const val VOLUME        = "volume"
}
