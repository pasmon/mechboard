package io.github.pasmon.mechboard

import org.junit.Assert.assertEquals
import org.junit.Test

class GraphemesTest {

    @Test
    fun `empty text has no last character`() {
        assertEquals(0, lastGraphemeLength(""))
    }

    @Test
    fun `plain letter is one char`() {
        assertEquals(1, lastGraphemeLength("hello"))
        assertEquals(1, lastGraphemeLength("ä"))
    }

    @Test
    fun `emoji outside the basic plane is two chars`() {
        assertEquals(2, lastGraphemeLength("hi 😀")) // 😀
        assertEquals(2, lastGraphemeLength("😀"))
    }

    @Test
    fun `letter with a combining mark is deleted as a whole`() {
        assertEquals(2, lastGraphemeLength("café")) // e + combining acute accent
    }

    @Test
    fun `only the last character counts`() {
        assertEquals(1, lastGraphemeLength("😀a"))
    }

    @Test
    fun `line break is one char`() {
        assertEquals(1, lastGraphemeLength("a\n"))
    }
}
