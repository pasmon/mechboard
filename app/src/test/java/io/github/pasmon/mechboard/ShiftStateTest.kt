package io.github.pasmon.mechboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftStateTest {

    @Test
    fun `tapping shift cycles off, next letter, caps lock, off`() {
        assertEquals(ShiftState.ON, ShiftState.OFF.afterShiftTap())
        assertEquals(ShiftState.LOCKED, ShiftState.ON.afterShiftTap())
        assertEquals(ShiftState.OFF, ShiftState.LOCKED.afterShiftTap())
    }

    @Test
    fun `typing a character uses up a one-letter shift`() {
        assertEquals(ShiftState.OFF, ShiftState.ON.afterCharacter())
    }

    @Test
    fun `typing a character keeps caps lock and lower case`() {
        assertEquals(ShiftState.LOCKED, ShiftState.LOCKED.afterCharacter())
        assertEquals(ShiftState.OFF, ShiftState.OFF.afterCharacter())
    }

    @Test
    fun `only OFF is lower case`() {
        assertFalse(ShiftState.OFF.isUppercase)
        assertTrue(ShiftState.ON.isUppercase)
        assertTrue(ShiftState.LOCKED.isUppercase)
    }
}
