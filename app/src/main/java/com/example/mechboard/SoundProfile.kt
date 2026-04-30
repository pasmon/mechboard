package com.example.mechboard

/**
 * Represents a mechanical keyboard sound profile.
 *
 * Each profile has a stable [id] used for SharedPreferences storage and as
 * the raw-resource filename (minus extension), plus a human-readable
 * [displayName] shown in the settings UI.
 *
 * Profiles with a non-null [rawFileName] resolve their audio resource at
 * runtime via [android.content.res.Resources.getIdentifier] so that this class
 * has no direct dependency on the generated [R] class and remains testable
 * with plain JVM unit tests.
 *
 * Profiles whose [rawFileName] is `null` intentionally produce no sound.
 */
enum class SoundProfile(
    val id: String,
    val displayName: String,
    /** Raw-resource filename (no extension) used to load the audio asset, or null for a silent profile. */
    val rawFileName: String?
) {
    CHERRY_MX_BLUE(
        id = "cherry_blue",
        displayName = "Cherry MX Blue (Clicky)",
        rawFileName = null
    ),
    CHERRY_MX_RED(
        id = "cherry_red",
        displayName = "Cherry MX Red (Linear)",
        rawFileName = null
    ),
    CHERRY_MX_BROWN(
        id = "cherry_brown",
        displayName = "Cherry MX Brown (Tactile)",
        rawFileName = null
    ),
    TOPRE(
        id = "topre",
        displayName = "Topre (Thocky)",
        rawFileName = null
    ),
    ALPS(
        id = "alps",
        displayName = "Alps (Vintage)",
        rawFileName = "alps"
    ),
    NK_CREAM(
        id = "nk_cream",
        displayName = "NK Cream (Smooth Linear)",
        rawFileName = "nk_cream"
    ),
    HOLY_PANDA(
        id = "holy_panda",
        displayName = "Holy Panda (Tactile Thock)",
        rawFileName = "holy_panda"
    ),
    TYPEWRITER(
        id = "typewriter",
        displayName = "Typewriter (Classic)",
        rawFileName = "typewriter"
    ),
    SILENT(
        id = "silent",
        displayName = "Silent",
        rawFileName = null
    );

    companion object {
        /** Returns the profile whose [SoundProfile.id] matches [id], or [CHERRY_MX_BLUE] as default. */
        fun fromId(id: String): SoundProfile =
            values().firstOrNull { it.id == id } ?: CHERRY_MX_BLUE
    }
}
