package com.example.mechboard

/**
 * Represents a supported keyboard language layout.
 *
 * Each layout has a stable [id] used for SharedPreferences storage, a
 * human-readable [displayName] shown in the settings UI, and the XML
 * resource filename (without extension) used to load the key definitions.
 *
 * The corresponding XML resource is resolved at runtime via
 * [android.content.res.Resources.getIdentifier] so that this class has no
 * direct dependency on the generated [R] class and remains testable with
 * plain JVM unit tests.
 */
enum class KeyboardLayout(
    val id: String,
    val displayName: String,
    /** XML resource filename (no extension) for the keyboard definition. */
    val xmlResName: String
) {
    ENGLISH(id = "english",  displayName = "English",  xmlResName = "keyboard"),
    FINNISH(id = "finnish",  displayName = "Finnish",  xmlResName = "keyboard_finnish"),
    GERMAN( id = "german",   displayName = "German",   xmlResName = "keyboard_german"),
    FRENCH( id = "french",   displayName = "French",   xmlResName = "keyboard_french"),
    SPANISH(id = "spanish",  displayName = "Spanish",  xmlResName = "keyboard_spanish");

    companion object {
        /** Returns the layout whose [id] matches [id], or [ENGLISH] as the default. */
        fun fromId(id: String): KeyboardLayout =
            values().firstOrNull { it.id == id } ?: ENGLISH
    }
}
