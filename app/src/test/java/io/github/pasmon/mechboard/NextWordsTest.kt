package io.github.pasmon.mechboard

import org.junit.Assert.assertEquals
import org.junit.Test

class NextWordsTest {

    private val nextWords = NextWords.fromLines(
        sequenceOf("to\tbe\ta\tdo", "I\tthink\tdon't", "The\tUnited", "the\tsame\tfirst", "single", "", "café\tau")
    )

    @Test
    fun `next words are returned best first`() {
        assertEquals(listOf("be", "a", "do"), nextWords.after("to"))
    }

    @Test
    fun `lookups ignore case and accents`() {
        assertEquals(listOf("think", "don't"), nextWords.after("i"))
        assertEquals(listOf("au"), nextWords.after("CAFE"))
    }

    @Test
    fun `lines for the same word are merged, the first line first`() {
        assertEquals(listOf("United", "same", "first"), nextWords.after("the"))
    }

    @Test
    fun `unknown words and lines without next words give nothing`() {
        assertEquals(emptyList<String>(), nextWords.after("single"))
        assertEquals(emptyList<String>(), nextWords.after("zebra"))
        assertEquals(4, nextWords.size)
    }
}
