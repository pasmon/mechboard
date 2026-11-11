package com.example.mechboard

/**
 * The key arrangements of every keyboard Mechboard can show.
 *
 * Plain Kotlin data with no Android dependency, so the arrangements can be
 * checked by JVM unit tests.
 */
object KeyboardDefinitions {

    /** Builds one row of keys. */
    private class RowBuilder {
        val keys = mutableListOf<Key>()
        private var pendingGap = 0f

        /** Leaves [percent] of the keyboard width empty before the next key. */
        fun gap(percent: Float) {
            pendingGap += percent
        }

        fun key(code: Int, label: String, width: Float = Key.DEFAULT_WIDTH_PERCENT,
                longPress: Char? = null, repeatable: Boolean = false) {
            keys += Key(code, label, width, pendingGap, longPress?.code, repeatable)
            pendingGap = 0f
        }

        /** Adds one key per character of [chars], each typing that character. */
        fun chars(chars: String, width: Float = Key.DEFAULT_WIDTH_PERCENT, longPress: String? = null) {
            require(longPress == null || longPress.length == chars.length)
            chars.forEachIndexed { i, ch -> key(ch.code, ch.toString(), width, longPress?.get(i)) }
        }

        fun shift() = key(KeyCodes.SHIFT, "⇧", width = 15f)
        fun backspace() = key(KeyCodes.BACKSPACE, "⌫", width = 15f, repeatable = true)
        fun symbols(label: String) = key(KeyCodes.SYMBOLS, label)
        fun settings() = key(KeyCodes.SETTINGS, "⚙")
        fun space(width: Float) = key(KeyCodes.SPACE, "Space", width)
        fun enter() = key(KeyCodes.ENTER, "↵")
    }

    private class KeyboardBuilder {
        val rows = mutableListOf<List<Key>>()
        fun row(build: RowBuilder.() -> Unit) {
            rows += RowBuilder().apply(build).keys
        }
    }

    private fun keyboard(build: KeyboardBuilder.() -> Unit) =
        KeyboardDefinition(KeyboardBuilder().apply(build).rows)

    private const val LABEL_TO_SYMBOLS = "?123"
    private const val LABEL_TO_LETTERS = "ABC"
    private const val DIGITS = "1234567890"

    /** Rows shared by every letters keyboard: shift, seven letters, backspace. */
    private fun KeyboardBuilder.shiftRow(letters: String) = row {
        shift()
        chars(letters)
        backspace()
    }

    /** Bottom row: symbols toggle, comma, space, [extra] characters, period, enter. */
    private fun KeyboardBuilder.bottomRow(extra: String = "") = row {
        symbols(LABEL_TO_SYMBOLS)
        chars(",")
        space(width = 60f - 10f * extra.length)
        chars(extra)
        chars(".")
        enter()
    }

    val ENGLISH = keyboard {
        row { chars("qwertyuiop", longPress = DIGITS) }
        row { gap(5f); chars("asdfghjkl") }
        shiftRow("zxcvbnm")
        bottomRow()
    }

    val FINNISH = keyboard {
        row { chars("qwertyuiop", width = 9f, longPress = DIGITS); chars("å") }
        row { chars("asdfghjklö", width = 9f); chars("ä") }
        shiftRow("zxcvbnm")
        bottomRow()
    }

    val GERMAN = keyboard {
        row { chars("qwertzuiop", longPress = DIGITS) }
        row { chars("asdfghjklö", width = 9f); chars("ä") }
        shiftRow("yxcvbnm")
        bottomRow(extra = "üß")
    }

    val FRENCH = keyboard {
        row { chars("qwertyuiop", longPress = DIGITS) }
        row { chars("asdfghjklé", width = 9f); chars("ç") }
        shiftRow("zxcvbnm")
        bottomRow(extra = "àèù")
    }

    val SPANISH = keyboard {
        row { chars("qwertyuiop", longPress = DIGITS) }
        row { chars("asdfghjklñ") }
        shiftRow("zxcvbnm")
        bottomRow(extra = "¿¡")
    }

    /** Digits and punctuation; shared by every language. */
    val SYMBOLS = keyboard {
        row { chars(DIGITS) }
        row { chars("@#$%&-+()/") }
        row { chars("~", width = 15f); chars("*\"':;!?"); backspace() }
        row {
            symbols(LABEL_TO_LETTERS)
            settings()
            space(width = 50f)
            chars(",.")
            enter()
        }
    }
}
