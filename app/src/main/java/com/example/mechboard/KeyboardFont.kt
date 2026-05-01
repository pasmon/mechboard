package com.example.mechboard

/**
 * Represents a font choice for keyboard key labels.
 *
 * Each entry has a stable [id] used for SharedPreferences storage, a
 * human-readable [displayName] shown in the settings UI, and an optional
 * [fontResName] that names the font file under `res/font/`.  A `null`
 * [fontResName] means "use the system default typeface".
 *
 * The actual [android.graphics.Typeface] is resolved at runtime in
 * [MechboardService] via [android.content.res.Resources.getIdentifier] so that
 * this class has no direct dependency on the generated [R] class and remains
 * testable with plain JVM unit tests.
 */
enum class KeyboardFont(
    val id: String,
    val displayName: String,
    val fontResName: String?
) {
    DEFAULT(
        id = "default",
        displayName = "Default",
        fontResName = null
    ),
    JETBRAINS_MONO(
        id = "jetbrains_mono",
        displayName = "JetBrains Mono",
        fontResName = "jetbrains_mono"
    ),
    SHARE_TECH_MONO(
        id = "share_tech_mono",
        displayName = "Share Tech Mono",
        fontResName = "share_tech_mono"
    );

    companion object {
        /** Returns the font whose [id] matches [id], or [DEFAULT] as the fallback. */
        fun fromId(id: String): KeyboardFont =
            values().firstOrNull { it.id == id } ?: DEFAULT
    }
}
