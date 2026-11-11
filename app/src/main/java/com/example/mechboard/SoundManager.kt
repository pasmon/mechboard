package com.example.mechboard

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages key-click audio playback.
 *
 * Sounds are loaded into a [SamplePlayer] (a [android.media.SoundPool] in
 * production) at construction time: one generic sample per profile plus any
 * dedicated per-[KeyKind] samples the profile ships. The raw-resource IDs are
 * resolved dynamically via [android.content.res.Resources.getIdentifier] so that
 * [SoundProfile] stays free of direct [R]-class references and is testable with
 * plain JVM unit tests.
 *
 * Call [release] when the owning component is destroyed to free native resources.
 *
 * The active [SoundProfile] and volume are persisted via [SharedPreferences]
 * so that user preferences survive across IME sessions.
 */
class SoundManager(
    context: Context,
    prefs: SharedPreferences,
    player: SamplePlayer = SoundPoolSamplePlayer(context),
    private val variation: SoundVariation = SoundVariation()
) {

    private var player: SamplePlayer? = player

    /**
     * Maps each profile that has audio to its generic *sample ID* as returned by
     * [SamplePlayer.load]. A value of 0 indicates the load failed.
     */
    private val sampleIds: MutableMap<SoundProfile, Int> = mutableMapOf()

    /**
     * Sample IDs of the dedicated per-[KeyKind] samples a profile ships (e.g. its
     * space-bar sound). Kinds without an entry fall back to [sampleIds].
     */
    private val kindSampleIds: MutableMap<Pair<SoundProfile, KeyKind>, Int> = mutableMapOf()

    private val sharedPrefs: SharedPreferences = prefs

    @Volatile
    var currentProfile: SoundProfile = SoundProfile.fromId(
        prefs.getString(PREF_SOUND_PROFILE, DEFAULT_PROFILE_ID) ?: DEFAULT_PROFILE_ID
    )
        private set

    /**
     * Keeps [currentProfile] in sync whenever the user changes the profile via
     * [SettingsFragment], which writes directly to [SharedPreferences] without
     * going through [setProfile].
     */
    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PREF_SOUND_PROFILE) {
            currentProfile = SoundProfile.fromId(
                sharedPrefs.getString(PREF_SOUND_PROFILE, DEFAULT_PROFILE_ID) ?: DEFAULT_PROFILE_ID
            )
        }
    }

    init {
        sharedPrefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
        this.player?.let { sp ->
            SoundProfile.values()
                .filter { it.rawFileName != null }
                .forEach { profile ->
                    val resId = context.resources.getIdentifier(
                        profile.rawFileName, "raw", context.packageName
                    )
                    if (resId != 0) {
                        sampleIds[profile] = sp.load(resId)
                    }
                    KeyKind.values().forEach { kind ->
                        val kindResId = profile.sampleNameFor(kind)?.let { name ->
                            context.resources.getIdentifier(name, "raw", context.packageName)
                        } ?: 0
                        if (kindResId != 0) {
                            kindSampleIds[profile to kind] = sp.load(kindResId)
                        }
                    }
                }
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Plays the currently selected profile's sound for a key of the given [kind]
     * (no-op when muted, silent, or sample unavailable).
     *
     * Uses the profile's dedicated sample for [kind] when it ships one and its
     * generic sample otherwise. Pitch and volume vary slightly per call, see
     * [SoundVariation].
     */
    fun playKeySound(kind: KeyKind = KeyKind.CHARACTER) {
        if (!isSoundEnabled) return
        val profile = currentProfile
        if (profile.rawFileName == null) return
        // A sample ID of 0 means the load failed; fall back or skip it silently.
        val sampleId = kindSampleIds[profile to kind]?.takeIf { it > 0 }
            ?: sampleIds[profile]?.takeIf { it > 0 }
            ?: return
        player?.play(sampleId, variation.nextVolume(volume), variation.nextRate())
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

    /** Releases the underlying [SamplePlayer]. Must be called in onDestroy. */
    fun release() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(prefChangeListener)
        player?.release()
        player = null
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    companion object {
        const val PREF_SOUND_PROFILE = PrefsKeys.SOUND_PROFILE
        const val PREF_SOUND_ENABLED = PrefsKeys.SOUND_ENABLED
        const val PREF_VOLUME        = PrefsKeys.VOLUME

        private const val DEFAULT_PROFILE_ID    = "cherry_blue"   // == SoundProfile.CHERRY_MX_BLUE.id
        private const val DEFAULT_VOLUME_PERCENT = 80
    }
}
