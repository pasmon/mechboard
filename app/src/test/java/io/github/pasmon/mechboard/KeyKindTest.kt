package io.github.pasmon.mechboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class KeyKindTest {

    @Test
    fun `space bar maps to SPACE`() {
        assertEquals(KeyKind.SPACE, KeyKind.fromKeyCode(32))
    }

    @Test
    fun `enter key maps to ENTER`() {
        assertEquals(KeyKind.ENTER, KeyKind.fromKeyCode(-4))
    }

    @Test
    fun `delete key maps to BACKSPACE`() {
        assertEquals(KeyKind.BACKSPACE, KeyKind.fromKeyCode(-5))
    }

    @Test
    fun `letters digits and punctuation map to CHARACTER`() {
        (('a'..'z') + ('0'..'9') + listOf(',', '.', '@', '?', 'ä', 'ñ')).forEach { ch ->
            assertEquals("'$ch'", KeyKind.CHARACTER, KeyKind.fromKeyCode(ch.code))
        }
    }

    @Test
    fun `shift settings and symbols keys map to CHARACTER`() {
        listOf(-1, -101, -102).forEach { code ->
            assertEquals("code $code", KeyKind.CHARACTER, KeyKind.fromKeyCode(code))
        }
    }

    @Test
    fun `only CHARACTER lacks a sample suffix`() {
        assertNull(KeyKind.CHARACTER.sampleSuffix)
        KeyKind.values().filter { it != KeyKind.CHARACTER }.forEach { kind ->
            assertNotNull("${kind.name} should have a sample suffix", kind.sampleSuffix)
        }
    }

    @Test
    fun `sample suffixes are unique and valid resource name parts`() {
        val suffixes = KeyKind.values().mapNotNull { it.sampleSuffix }
        assertEquals(suffixes.size, suffixes.distinct().size)
        suffixes.forEach { suffix ->
            assert(Regex("[a-z][a-z0-9_]*").matches(suffix)) { "'$suffix' is not a valid resource name part" }
        }
    }
}
