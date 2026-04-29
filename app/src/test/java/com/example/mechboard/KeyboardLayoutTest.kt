package com.example.mechboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class KeyboardLayoutTest {

    @Test
    fun `all layouts have unique ids`() {
        val ids = KeyboardLayout.values().map { it.id }
        assertEquals("Duplicate KeyboardLayout ids detected", ids.size, ids.distinct().size)
    }

    @Test
    fun `all layouts have non-blank display names`() {
        KeyboardLayout.values().forEach { layout ->
            assert(layout.displayName.isNotBlank()) {
                "${layout.name} has a blank displayName"
            }
        }
    }

    @Test
    fun `all layouts have non-blank xmlResNames`() {
        KeyboardLayout.values().forEach { layout ->
            assert(layout.xmlResName.isNotBlank()) {
                "${layout.name} has a blank xmlResName"
            }
        }
    }

    @Test
    fun `fromId returns correct layout for each id`() {
        KeyboardLayout.values().forEach { layout ->
            assertSame(layout, KeyboardLayout.fromId(layout.id))
        }
    }

    @Test
    fun `fromId returns ENGLISH for unknown id`() {
        assertSame(KeyboardLayout.ENGLISH, KeyboardLayout.fromId("unknown_xyz"))
    }

    @Test
    fun `fromId returns ENGLISH for empty string`() {
        assertSame(KeyboardLayout.ENGLISH, KeyboardLayout.fromId(""))
    }

    @Test
    fun `layouts cover all expected languages`() {
        val expectedIds = setOf("english", "finnish", "german", "french", "spanish")
        val actualIds = KeyboardLayout.values().map { it.id }.toSet()
        assertEquals(expectedIds, actualIds)
    }

    @Test
    fun `all layouts have unique xmlResNames`() {
        val names = KeyboardLayout.values().map { it.xmlResName }
        assertEquals("Duplicate KeyboardLayout xmlResNames detected", names.size, names.distinct().size)
    }
}
