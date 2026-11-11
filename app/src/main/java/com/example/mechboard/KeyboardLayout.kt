package com.example.mechboard

/**
 * Represents a supported keyboard language layout.
 *
 * Each layout has a stable [id] used for SharedPreferences storage, a
 * human-readable [displayName] shown in the settings UI, and the
 * [definition] of its keys.
 *
 * This class has no Android dependency and is testable with plain JVM unit tests.
 */
enum class KeyboardLayout(
    val id: String,
    val displayName: String,
    /** The arrangement of keys shown for this language. */
    val definition: KeyboardDefinition
) {
    ENGLISH(id = "english",  displayName = "English",  definition = KeyboardDefinitions.ENGLISH),
    FINNISH(id = "finnish",  displayName = "Finnish",  definition = KeyboardDefinitions.FINNISH),
    GERMAN( id = "german",   displayName = "German",   definition = KeyboardDefinitions.GERMAN),
    FRENCH( id = "french",   displayName = "French",   definition = KeyboardDefinitions.FRENCH),
    SPANISH(id = "spanish",  displayName = "Spanish",  definition = KeyboardDefinitions.SPANISH);

    companion object {
        /** Returns the layout whose [id] matches [id], or [ENGLISH] as the default. */
        fun fromId(id: String): KeyboardLayout =
            values().firstOrNull { it.id == id } ?: ENGLISH
    }
}
