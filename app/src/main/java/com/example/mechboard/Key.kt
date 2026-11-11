package com.example.mechboard

/**
 * Codes of the keys that do something other than type a character.
 *
 * Character keys use their Unicode code point as key code, so every special
 * code is negative, with the exception of [SPACE] which is the ASCII space.
 */
object KeyCodes {
    const val SHIFT = -1
    const val ENTER = -4
    const val BACKSPACE = -5
    const val SETTINGS = -101
    /** Toggles between the letters keyboard and the symbols keyboard. */
    const val SYMBOLS = -102
    const val SPACE = 32
}

/**
 * One key of a [KeyboardDefinition].
 *
 * Widths are percentages of the keyboard width so that a definition is
 * independent of the screen it is shown on.
 */
data class Key(
    /** A Unicode code point for character keys, or one of [KeyCodes]. */
    val code: Int,
    /** Text drawn on the key. */
    val label: String,
    val widthPercent: Float = DEFAULT_WIDTH_PERCENT,
    /** Empty space left of the key, as a percentage of the keyboard width. */
    val gapPercent: Float = 0f,
    /**
     * Code point typed when the key is held down instead of tapped, or `null`.
     * Shown as a small hint in the key's corner.
     */
    val longPressCode: Int? = null,
    /** Whether holding the key down keeps firing it (backspace). */
    val repeatable: Boolean = false
) {
    companion object {
        const val DEFAULT_WIDTH_PERCENT = 10f
    }
}

/** The rows of keys that make up one keyboard, top to bottom. */
data class KeyboardDefinition(val rows: List<List<Key>>) {
    val keys: List<Key> get() = rows.flatten()
}
