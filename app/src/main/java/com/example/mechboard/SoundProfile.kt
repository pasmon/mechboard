package com.example.mechboard

/**
 * Represents a mechanical keyboard sound profile.
 *
 * Each profile has a stable [id] used for SharedPreferences storage and as
 * the raw-resource filename (minus extension), plus a human-readable
 * [displayName] shown in the settings UI.
 *
 * The corresponding audio resource is resolved at runtime via
 * [android.content.res.Resources.getIdentifier] so that this class has no
 * direct dependency on the generated [R] class and remains testable with
 * plain JVM unit tests.
 *
 * [SILENT] is a special profile that produces no sound; its [rawFileName] is
 * `null`.
 */
enum class SoundProfile(
    val id: String,
    val displayName: String,
    /** Raw-resource filename (no extension) used to load the audio asset, or null for [SILENT]. */
    val rawFileName: String?
) {
    CHERRY_MX_BLUE(
        id = "cherry_blue",
        displayName = "Cherry MX Blue (Clicky)",
        rawFileName = "cherry_blue"
    ),
    CHERRY_MX_RED(
        id = "cherry_red",
        displayName = "Cherry MX Red (Linear)",
        rawFileName = "cherry_red"
    ),
    CHERRY_MX_BROWN(
        id = "cherry_brown",
        displayName = "Cherry MX Brown (Tactile)",
        rawFileName = "cherry_brown"
    ),
    TOPRE(
        id = "topre",
        displayName = "Topre (Thocky)",
        rawFileName = "topre"
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
