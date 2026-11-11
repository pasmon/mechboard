package com.example.mechboard

/** Whether, and for how long, the keyboard types capital letters. */
enum class ShiftState {
    /** Lower case. */
    OFF,
    /** Upper case for the next letter only. */
    ON,
    /** Upper case until switched off (caps lock). */
    LOCKED;

    val isUppercase: Boolean get() = this != OFF

    /** The state a tap on the shift key leads to: off → next letter → caps lock → off. */
    fun afterShiftTap(): ShiftState = when (this) {
        OFF    -> ON
        ON     -> LOCKED
        LOCKED -> OFF
    }

    /** The state after a character has been typed: a one-letter shift is used up. */
    fun afterCharacter(): ShiftState = if (this == ON) OFF else this
}
