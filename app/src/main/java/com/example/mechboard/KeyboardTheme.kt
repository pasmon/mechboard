package com.example.mechboard

/**
 * Represents a keyboard colour theme.
 *
 * Each entry has a stable [id] used for SharedPreferences storage and a
 * human-readable [displayName] shown in the settings UI.
 *
 * The corresponding Android style resource is resolved at runtime in
 * [MechboardService] via a `when` expression so that this class has no direct
 * dependency on the generated [R] class and remains testable with plain JVM
 * unit tests.
 */
enum class KeyboardTheme(
    val id: String,
    val displayName: String
) {
    DARK(id = "dark",            displayName = "Dark"),
    SOLARIZED_DARK(
        id = "solarized_dark",   displayName = "Solarized Dark"
    ),
    SOLARIZED_LIGHT(
        id = "solarized_light",  displayName = "Solarized Light"
    ),
    DRACULA(id = "dracula",      displayName = "Dracula"),
    NORD(id = "nord",            displayName = "Nord"),
    MONOKAI(id = "monokai",      displayName = "Monokai");

    companion object {
        /** Returns the theme whose [id] matches [id], or [DARK] as the default. */
        fun fromId(id: String): KeyboardTheme =
            values().firstOrNull { it.id == id } ?: DARK
    }
}
