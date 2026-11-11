package com.example.mechboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SoundVariationTest {

    /** A [Random] whose [nextFloat] always returns [value]. */
    private class FixedRandom(private val value: Float) : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextFloat(): Float = value
    }

    /** Largest value [Random.nextFloat] can return (its range excludes 1). */
    private val almostOne = 0.999999f

    @Test
    fun `rate is lowest at the bottom of the random range`() {
        val variation = SoundVariation(FixedRandom(0f), pitchSpread = 0.04f)
        assertEquals(0.96f, variation.nextRate(), 0.0001f)
    }

    @Test
    fun `rate is unchanged at the middle of the random range`() {
        val variation = SoundVariation(FixedRandom(0.5f), pitchSpread = 0.04f)
        assertEquals(1.0f, variation.nextRate(), 0.0001f)
    }

    @Test
    fun `rate is highest at the top of the random range`() {
        val variation = SoundVariation(FixedRandom(almostOne), pitchSpread = 0.04f)
        assertEquals(1.04f, variation.nextRate(), 0.0001f)
    }

    @Test
    fun `volume is the base volume at the bottom of the random range`() {
        val variation = SoundVariation(FixedRandom(0f), volumeSpread = 0.10f)
        assertEquals(0.8f, variation.nextVolume(0.8f), 0.0001f)
    }

    @Test
    fun `volume is reduced by the spread at the top of the random range`() {
        val variation = SoundVariation(FixedRandom(almostOne), volumeSpread = 0.10f)
        assertEquals(0.72f, variation.nextVolume(0.8f), 0.0001f)
    }

    @Test
    fun `default spreads stay within their documented bounds`() {
        val variation = SoundVariation(Random(42))
        repeat(10_000) {
            val rate = variation.nextRate()
            assertTrue("rate $rate", rate in 0.96f..1.04f)
            val volume = variation.nextVolume(1f)
            assertTrue("volume $volume", volume in 0.9f..1f)
        }
    }

    @Test
    fun `default spreads actually vary the output`() {
        val variation = SoundVariation(Random(42))
        assertTrue(List(20) { variation.nextRate() }.distinct().size > 1)
        assertTrue(List(20) { variation.nextVolume(1f) }.distinct().size > 1)
    }

    @Test
    fun `zero volume stays silent`() {
        val variation = SoundVariation(Random(42))
        repeat(100) { assertEquals(0f, variation.nextVolume(0f), 0f) }
    }

    @Test
    fun `out-of-range base volume is clamped`() {
        val variation = SoundVariation(FixedRandom(0f))
        assertEquals(1f, variation.nextVolume(1.5f), 0f)
        assertEquals(0f, variation.nextVolume(-0.5f), 0f)
    }

    @Test
    fun `zero spreads disable variation`() {
        val variation = SoundVariation(Random(42), pitchSpread = 0f, volumeSpread = 0f)
        repeat(100) {
            assertEquals(1f, variation.nextRate(), 0f)
            assertEquals(0.8f, variation.nextVolume(0.8f), 0f)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative pitch spread is rejected`() {
        SoundVariation(pitchSpread = -0.1f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `excessive volume spread is rejected`() {
        SoundVariation(volumeSpread = 0.9f)
    }
}
