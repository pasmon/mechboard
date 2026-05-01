package com.example.mechboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class KeyboardFontTest {

    @Test
    fun `all fonts have unique ids`() {
        val ids = KeyboardFont.values().map { it.id }
        assertEquals("Duplicate KeyboardFont ids detected", ids.size, ids.distinct().size)
    }

    @Test
    fun `all fonts have non-blank display names`() {
        KeyboardFont.values().forEach { font ->
            assertFalse("${font.name} has a blank displayName", font.displayName.isBlank())
        }
    }

    @Test
    fun `all fonts have non-blank ids`() {
        KeyboardFont.values().forEach { font ->
            assertFalse("${font.name} has a blank id", font.id.isBlank())
        }
    }

    @Test
    fun `fromId returns correct font for each id`() {
        KeyboardFont.values().forEach { font ->
            assertSame(font, KeyboardFont.fromId(font.id))
        }
    }

    @Test
    fun `fromId returns DEFAULT for unknown id`() {
        assertSame(KeyboardFont.DEFAULT, KeyboardFont.fromId("unknown_xyz"))
    }

    @Test
    fun `fromId returns DEFAULT for empty string`() {
        assertSame(KeyboardFont.DEFAULT, KeyboardFont.fromId(""))
    }

    @Test
    fun `DEFAULT has null fontResName`() {
        assertNull(KeyboardFont.DEFAULT.fontResName)
    }

    @Test
    fun `bundled fonts have non-null fontResName`() {
        KeyboardFont.values()
            .filter { it != KeyboardFont.DEFAULT }
            .forEach { font ->
                assertFalse(
                    "${font.name} should have a non-blank fontResName",
                    font.fontResName.isNullOrBlank()
                )
            }
    }

    @Test
    fun `fonts cover all expected entries`() {
        val expectedIds = setOf(
            "default",
            "jetbrains_mono",
            "share_tech_mono"
        )
        val actualIds = KeyboardFont.values().map { it.id }.toSet()
        assertEquals(expectedIds, actualIds)
    }
}
