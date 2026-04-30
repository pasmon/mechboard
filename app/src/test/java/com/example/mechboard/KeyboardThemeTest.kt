package com.example.mechboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class KeyboardThemeTest {

    @Test
    fun `all themes have unique ids`() {
        val ids = KeyboardTheme.values().map { it.id }
        assertEquals("Duplicate KeyboardTheme ids detected", ids.size, ids.distinct().size)
    }

    @Test
    fun `all themes have non-blank display names`() {
        KeyboardTheme.values().forEach { theme ->
            assertFalse("${theme.name} has a blank displayName", theme.displayName.isBlank())
        }
    }

    @Test
    fun `all themes have non-blank ids`() {
        KeyboardTheme.values().forEach { theme ->
            assertFalse("${theme.name} has a blank id", theme.id.isBlank())
        }
    }

    @Test
    fun `fromId returns correct theme for each id`() {
        KeyboardTheme.values().forEach { theme ->
            assertSame(theme, KeyboardTheme.fromId(theme.id))
        }
    }

    @Test
    fun `fromId returns DARK for unknown id`() {
        assertSame(KeyboardTheme.DARK, KeyboardTheme.fromId("unknown_xyz"))
    }

    @Test
    fun `fromId returns DARK for empty string`() {
        assertSame(KeyboardTheme.DARK, KeyboardTheme.fromId(""))
    }

    @Test
    fun `themes cover all expected entries`() {
        val expectedIds = setOf(
            "dark",
            "solarized_dark",
            "solarized_light",
            "dracula",
            "nord",
            "monokai"
        )
        val actualIds = KeyboardTheme.values().map { it.id }.toSet()
        assertEquals(expectedIds, actualIds)
    }
}
