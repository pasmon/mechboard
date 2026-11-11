package io.github.pasmon.mechboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardDefinitionsTest {

    private val letterKeyboards = KeyboardLayout.values().associate { it.name to it.definition }
    private val allKeyboards = letterKeyboards + ("SYMBOLS" to KeyboardDefinitions.SYMBOLS)

    private fun KeyboardDefinition.codes() = keys.map { it.code }

    @Test
    fun `every keyboard has four rows`() {
        allKeyboards.forEach { (name, keyboard) -> assertEquals(name, 4, keyboard.rows.size) }
    }

    @Test
    fun `no row is wider than the keyboard`() {
        allKeyboards.forEach { (name, keyboard) ->
            keyboard.rows.forEachIndexed { index, row ->
                val width = row.sumOf { (it.widthPercent + it.gapPercent).toDouble() }
                assertTrue("$name row $index is $width % wide", width <= 100.0001)
            }
        }
    }

    @Test
    fun `every row fills the keyboard except the indented English home row`() {
        allKeyboards.forEach { (name, keyboard) ->
            keyboard.rows.forEachIndexed { index, row ->
                val width = row.sumOf { (it.widthPercent + it.gapPercent).toDouble() }
                val expected = if (name == "ENGLISH" && index == 1) 95.0 else 100.0
                assertEquals("$name row $index", expected, width, 0.0001)
            }
        }
    }

    @Test
    fun `no keyboard has two keys with the same code`() {
        allKeyboards.forEach { (name, keyboard) ->
            val codes = keyboard.codes()
            assertEquals("$name has duplicate key codes", codes.size, codes.distinct().size)
        }
    }

    @Test
    fun `every key has a label and a positive width`() {
        allKeyboards.forEach { (name, keyboard) ->
            keyboard.keys.forEach { key ->
                assertTrue("$name key ${key.code} has no label", key.label.isNotEmpty())
                assertTrue("$name key ${key.code} has no width", key.widthPercent > 0f)
            }
        }
    }

    @Test
    fun `character keys are labelled with the character they type`() {
        allKeyboards.forEach { (name, keyboard) ->
            keyboard.keys.filter { it.code > 0 && it.code != KeyCodes.SPACE }.forEach { key ->
                assertEquals("$name key ${key.code}", String(Character.toChars(key.code)), key.label)
            }
        }
    }

    @Test
    fun `every letters keyboard has the keys needed to type`() {
        val required = listOf(
            KeyCodes.SHIFT, KeyCodes.BACKSPACE, KeyCodes.SYMBOLS, KeyCodes.SPACE, KeyCodes.ENTER,
            ','.code, '.'.code
        )
        letterKeyboards.forEach { (name, keyboard) ->
            assertTrue("$name lacks one of $required", keyboard.codes().containsAll(required))
        }
    }

    @Test
    fun `every letters keyboard has the 26 basic letters`() {
        letterKeyboards.forEach { (name, keyboard) ->
            val missing = ('a'..'z').filterNot { it.code in keyboard.codes() }
            assertTrue("$name lacks $missing", missing.isEmpty())
        }
    }

    @Test
    fun `every letters keyboard offers the digits by long-press on its top row`() {
        letterKeyboards.forEach { (name, keyboard) ->
            val longPress = keyboard.rows.first().mapNotNull { it.longPressCode?.toChar() }
            assertEquals(name, "1234567890".toList(), longPress)
        }
    }

    @Test
    fun `layouts have their language specific characters`() {
        val expected = mapOf(
            KeyboardLayout.FINNISH to "åöä",
            KeyboardLayout.GERMAN to "öäüß",
            KeyboardLayout.FRENCH to "éçàèù",
            KeyboardLayout.SPANISH to "ñ¿¡"
        )
        expected.forEach { (layout, chars) ->
            val missing = chars.filterNot { it.code in layout.definition.codes() }
            assertTrue("${layout.name} lacks $missing", missing.isEmpty())
        }
    }

    @Test
    fun `symbols keyboard has digits settings and the way back to letters`() {
        val codes = KeyboardDefinitions.SYMBOLS.codes()
        assertTrue(codes.containsAll(('0'..'9').map { it.code }))
        assertTrue(
            codes.containsAll(
                listOf(KeyCodes.SETTINGS, KeyCodes.SYMBOLS, KeyCodes.BACKSPACE, KeyCodes.SPACE, KeyCodes.ENTER)
            )
        )
    }

    @Test
    fun `only backspace repeats`() {
        allKeyboards.forEach { (name, keyboard) ->
            assertEquals(name, listOf(KeyCodes.BACKSPACE), keyboard.keys.filter { it.repeatable }.map { it.code })
        }
    }
}
