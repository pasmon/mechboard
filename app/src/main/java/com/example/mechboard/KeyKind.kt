package com.example.mechboard

/**
 * Groups key codes by how the physical key they imitate sounds.
 *
 * Large stabilised keys (space bar, enter, backspace) sound noticeably different
 * from ordinary keys on a mechanical keyboard, so each gets its own kind and may
 * have a dedicated sample per [SoundProfile]. Everything else is [CHARACTER].
 *
 * This file has no Android dependency so it can be unit-tested on the plain JVM.
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
        /** Returns the kind of the key that emits [keyCode]. */
        fun fromKeyCode(keyCode: Int): KeyKind = when (keyCode) {
            KeyCodes.SPACE     -> SPACE
            KeyCodes.ENTER     -> ENTER
            KeyCodes.BACKSPACE -> BACKSPACE
            else               -> CHARACTER
        }
    }
}
