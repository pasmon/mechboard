package com.example.mechboard

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * Manages key-click audio playback.
 *
 * Sounds are loaded into a [SoundPool] at construction time. The raw-resource
 * IDs are resolved dynamically via [android.content.res.Resources.getIdentifier]
 * so that [SoundProfile] stays free of direct [R]-class references and is
 * testable with plain JVM unit tests.
 *
 * Call [release] when the owning component is destroyed to free native resources.
 *
 * The active [SoundProfile] and volume are persisted via [SharedPreferences]
 * so that user preferences survive across IME sessions.
 */
class SoundManager(context: Context, prefs: SharedPreferences) {

    private var soundPool: SoundPool? = buildSoundPool()

    /**
     * Maps each profile that has audio to its loaded SoundPool stream id.
     * Entries are inserted as the SoundPool finishes loading each sample.
     */
    private val soundIds: MutableMap<SoundProfile, Int> = mutableMapOf()

    private val sharedPrefs: SharedPreferences = prefs

    var currentProfile: SoundProfile = SoundProfile.fromId(
        prefs.getString(PREF_SOUND_PROFILE, DEFAULT_PROFILE_ID) ?: DEFAULT_PROFILE_ID
    )
        private set

    init {
        val sp = soundPool ?: return
        SoundProfile.values()
            .filter { it.rawFileName != null }
            .forEach { profile ->
                val resId = context.resources.getIdentifier(
                    profile.rawFileName, "raw", context.packageName
                )
                if (resId != 0) {
                    soundIds[profile] = sp.load(context, resId, 1)
                }
            }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Plays the currently selected key-click sound (no-op when silent or muted). */
    fun playKeySound() {
        if (!isSoundEnabled) return
        val profile = currentProfile
        if (profile == SoundProfile.SILENT) return
        val soundId = soundIds[profile] ?: return
        val vol = volume
        soundPool?.play(soundId, vol, vol, 1, 0, 1.0f)
    }

    /** Changes the active profile and persists the choice. */
    fun setProfile(profile: SoundProfile) {
        currentProfile = profile
        sharedPrefs.edit().putString(PREF_SOUND_PROFILE, profile.id).apply()
    }

    var isSoundEnabled: Boolean
        get() = sharedPrefs.getBoolean(PREF_SOUND_ENABLED, true)
        set(value) { sharedPrefs.edit().putBoolean(PREF_SOUND_ENABLED, value).apply() }

    /** Volume as a [0f, 1f] float derived from the 0–100 integer pref. */
    var volume: Float
        get() = sharedPrefs.getInt(PREF_VOLUME, DEFAULT_VOLUME_PERCENT) / 100f
        set(value) {
            val pct = (value.coerceIn(0f, 1f) * 100).toInt()
            sharedPrefs.edit().putInt(PREF_VOLUME, pct).apply()
        }

    /** Releases the underlying [SoundPool]. Must be called in onDestroy. */
    fun release() {
        soundPool?.release()
        soundPool = null
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildSoundPool(): SoundPool {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        return SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(attrs)
            .build()
    }

    companion object {
        const val PREF_SOUND_PROFILE = PrefsKeys.SOUND_PROFILE
        const val PREF_SOUND_ENABLED = PrefsKeys.SOUND_ENABLED
        const val PREF_VOLUME        = PrefsKeys.VOLUME

        private const val DEFAULT_PROFILE_ID    = "cherry_blue"   // == SoundProfile.CHERRY_MX_BLUE.id
        private const val MAX_STREAMS           = 5
        private const val DEFAULT_VOLUME_PERCENT = 80
    }
}
