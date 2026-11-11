package com.example.mechboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardGeometryTest {

    private val english = KeyboardGeometry(KeyboardDefinitions.ENGLISH, width = 1000, rowHeight = 100)

    private fun KeyboardGeometry.codeAt(x: Float, y: Float): Int = keys[indexOfKeyAt(x, y)].key.code

    @Test
    fun `height is the number of rows times the row height`() {
        assertEquals(400, english.height)
    }

    @Test
    fun `keys are placed left to right by their percentage widths`() {
        val q = english.keys[0]
        val w = english.keys[1]
        assertEquals(KeyBounds(q.key, left = 0, top = 0, right = 100, bottom = 100), q)
        assertEquals(KeyBounds(w.key, left = 100, top = 0, right = 200, bottom = 100), w)
    }

    @Test
    fun `a gap shifts the key right`() {
        val a = english.keys.first { it.key.code == 'a'.code }
        assertEquals(50, a.left)
        assertEquals(150, a.right)
        assertEquals(100, a.top)
    }

    @Test
    fun `wide keys get their share of the width`() {
        val space = english.keys.first { it.key.code == KeyCodes.SPACE }
        assertEquals(200, space.left)
        assertEquals(800, space.right)
        assertEquals(300, space.top)
        assertEquals(400, space.bottom)
    }

    @Test
    fun `neighbouring keys share an edge at every width`() {
        val definitions = KeyboardLayout.values().map { it.definition } + KeyboardDefinitions.SYMBOLS
        for (definition in definitions) {
            for (width in listOf(320, 479, 1080, 1439)) {
                val geometry = KeyboardGeometry(definition, width, rowHeight = 56)
                geometry.keys.zipWithNext().filter { (a, b) -> a.top == b.top }.forEach { (a, b) ->
                    assertEquals("width $width, after key ${a.key.code}", a.right, b.left - gapPx(b, width))
                }
            }
        }
    }

    private fun gapPx(bounds: KeyBounds, width: Int) = Math.round(bounds.key.gapPercent * width / 100f)

    @Test
    fun `full rows end exactly at the right edge`() {
        for (width in listOf(320, 479, 1080, 1439)) {
            val geometry = KeyboardGeometry(KeyboardDefinitions.FINNISH, width, rowHeight = 56)
            geometry.keys.groupBy { it.top }.values.forEach { row ->
                assertEquals("width $width", width, row.last().right)
            }
        }
    }

    @Test
    fun `a touch inside a key resolves to that key`() {
        assertEquals('q'.code, english.codeAt(50f, 50f))
        assertEquals('p'.code, english.codeAt(999f, 0f))
        assertEquals('s'.code, english.codeAt(150f, 199f))
        assertEquals(KeyCodes.SPACE, english.codeAt(500f, 350f))
        assertEquals(KeyCodes.ENTER, english.codeAt(950f, 399f))
    }

    @Test
    fun `a touch on a shared edge belongs to the key on the right`() {
        assertEquals('w'.code, english.codeAt(100f, 50f))
    }

    @Test
    fun `a touch beside a short row resolves to the nearest key in it`() {
        assertEquals('a'.code, english.codeAt(10f, 150f))
        assertEquals('l'.code, english.codeAt(990f, 150f))
    }

    @Test
    fun `a touch outside the keyboard resolves to the nearest key`() {
        assertEquals('q'.code, english.codeAt(-20f, -30f))
        assertEquals(KeyCodes.ENTER, english.codeAt(1200f, 900f))
        assertEquals('t'.code, english.codeAt(450f, -5f))
    }

    @Test
    fun `every key is found at its own centre`() {
        english.keys.forEachIndexed { index, bounds ->
            assertEquals(index, english.indexOfKeyAt(bounds.centerX, bounds.centerY))
        }
    }

    @Test
    fun `an empty keyboard has no key anywhere`() {
        val empty = KeyboardGeometry(KeyboardDefinition(emptyList()), width = 1000, rowHeight = 100)
        assertEquals(KeyboardGeometry.NO_KEY, empty.indexOfKeyAt(10f, 10f))
        assertEquals(0, empty.height)
        assertTrue(empty.keys.isEmpty())
    }

    @Test
    fun `a keyboard that has not been laid out yet has no key anywhere`() {
        val unsized = KeyboardGeometry(KeyboardDefinitions.ENGLISH, width = 0, rowHeight = 0)
        assertEquals(KeyboardGeometry.NO_KEY, unsized.indexOfKeyAt(10f, 10f))
    }
}
