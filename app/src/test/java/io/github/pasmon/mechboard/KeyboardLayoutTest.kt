package io.github.pasmon.mechboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            assertFalse("${layout.name} has a blank displayName", layout.displayName.isBlank())
        }
    }

    @Test
    fun `all layouts have keys`() {
        KeyboardLayout.values().forEach { layout ->
            assertFalse("${layout.name} has no keys", layout.definition.keys.isEmpty())
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
    fun `all layouts have distinct definitions`() {
        val definitions = KeyboardLayout.values().map { it.definition }
        assertEquals("Two layouts share a definition", definitions.size, definitions.distinct().size)
    }
}
