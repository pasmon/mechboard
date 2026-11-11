package com.example.mechboard

/**
 * Groups key codes by how the physical key they imitate sounds.
 *
 * Large stabilised keys (space bar, enter, backspace) sound noticeably different
 * from ordinary keys on a mechanical keyboard, so each gets its own kind and may
 * have a dedicated sample per [SoundProfile]. Everything else is [CHARACTER].
 *
 * This file has no Android dependency so it can be unit-tested on the plain JVM;
 * the key-code constants mirror `android.inputmethodservice.Keyboard.KEYCODE_*`.
 */
enum class KeyKind(
    /**
     * Suffix appended to [SoundProfile.rawFileName] (as `<name>_<suffix>`) to form the
     * raw-resource name of this kind's dedicated sample, or `null` when the kind uses
     * the profile's generic sample.
     */
    val sampleSuffix: String?
) {
    /** Letters, digits, punctuation, and modifier/function keys. */
    CHARACTER(null),
    SPACE("space"),
    ENTER("enter"),
    BACKSPACE("backspace");

    companion object {
        /** ASCII space. */
        private const val KEYCODE_SPACE = 32
        /** Same value as `Keyboard.KEYCODE_DONE`. */
        private const val KEYCODE_DONE = -4
        /** Same value as `Keyboard.KEYCODE_DELETE`. */
        private const val KEYCODE_DELETE = -5

        /** Returns the kind of the key that emits [keyCode]. */
        fun fromKeyCode(keyCode: Int): KeyKind = when (keyCode) {
            KEYCODE_SPACE  -> SPACE
            KEYCODE_DONE   -> ENTER
            KEYCODE_DELETE -> BACKSPACE
            else           -> CHARACTER
        }
    }
}
