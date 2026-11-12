package io.github.pasmon.mechboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyNeighboursTest {

    private val english = KeyNeighbours.fromDefinition(KeyboardDefinitions.ENGLISH)

    @Test
    fun `keys either side in the same row are neighbours`() {
        assertTrue(english.areNeighbours('e', 'w'))
        assertTrue(english.areNeighbours('e', 'r'))
        assertTrue(english.areNeighbours('b', 'n'))
    }

    @Test
    fun `the closest keys in the rows above and below are neighbours`() {
        // The middle row is offset by half a key: s sits below w and e.
        assertEquals(setOf('a', 'd', 'w', 'e', 'z', 'x'), english.of('s'))
        assertEquals(setOf('w', 'r', 's', 'd'), english.of('e'))
    }

    @Test
    fun `keys further apart are not neighbours`() {
        assertFalse(english.areNeighbours('e', 't'))
        assertFalse(english.areNeighbours('q', 'p'))
        assertFalse(english.areNeighbours('e', 'e'))
    }

    @Test
    fun `neighbours follow each language's layout`() {
        val german = KeyNeighbours.fromDefinition(KeyboardDefinitions.GERMAN)
        assertTrue(german.areNeighbours('t', 'z'))
        assertFalse(english.areNeighbours('t', 'z'))
    }

    @Test
    fun `accented keys count as their plain letter`() {
        val finnish = KeyNeighbours.fromDefinition(KeyboardDefinitions.FINNISH)
        // ö sits next to l; looked up without accents it is "o".
        assertTrue(finnish.areNeighbours('l', 'o'))
    }

    @Test
    fun `every layout gives every letter key some neighbours`() {
        KeyboardLayout.values().forEach { layout ->
            layout.definition.keys
                .filter { it.code > 0 && Character.isLetter(it.code) }
                .forEach { key ->
                    val letter = WordDictionary.foldForLookup(String(Character.toChars(key.code))).single()
                    assertTrue("${layout.id} '${key.label}'", layout.neighbours.of(letter).isNotEmpty())
                }
        }
    }
}
