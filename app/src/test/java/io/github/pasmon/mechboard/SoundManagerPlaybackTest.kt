package io.github.pasmon.mechboard

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.random.Random

/**
 * Unit tests for which sample [SoundManager] plays, and how.
 *
 * Playback goes to a [FakePlayer] and the resource lookup is mocked: every raw
 * resource in [rawResources] "exists" with the given resource id, and loading
 * resource id `n` yields sample id `n + SAMPLE_ID_OFFSET`.
 */
class SoundManagerPlaybackTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var context: Context
    private lateinit var player: FakePlayer

    private data class Played(val sampleId: Int, val volume: Float, val rate: Float)

    private class FakePlayer : SamplePlayer {
        /** Resource ids whose load should fail. */
        val failingResIds = mutableSetOf<Int>()
        val played = mutableListOf<Played>()
        var released = false

        override fun load(resId: Int): Int =
            if (resId in failingResIds) 0 else resId + SAMPLE_ID_OFFSET

        override fun play(sampleId: Int, volume: Float, rate: Float) {
            played += Played(sampleId, volume, rate)
        }

        override fun release() {
            released = true
        }
    }

    /** Raw-resource name → resource id for the resources that exist in a test. */
    private val rawResources = mutableMapOf<String, Int>()

    /** Variation that leaves rate and volume untouched, so exact values can be verified. */
    private val noVariation = SoundVariation(Random(0), pitchSpread = 0f, volumeSpread = 0f)

    @Before
    fun setUp() {
        prefs = mock()
        whenever(prefs.getBoolean(eq(PrefsKeys.SOUND_ENABLED), any())).thenReturn(true)
        whenever(prefs.getInt(eq(PrefsKeys.VOLUME), any())).thenReturn(80)
        selectProfile(SoundProfile.CHERRY_MX_BLUE)

        val resources: Resources = mock()
        whenever(resources.getIdentifier(anyOrNull(), eq("raw"), anyOrNull())).thenAnswer { call ->
            rawResources[call.getArgument<String>(0)] ?: 0
        }
        context = mock()
        whenever(context.resources).thenReturn(resources)
        whenever(context.packageName).thenReturn("io.github.pasmon.mechboard")

        player = FakePlayer()

        rawResources.clear()
        rawResources["cherry_blue"] = RES_BLUE
        rawResources["cherry_blue_space"] = RES_BLUE_SPACE
        rawResources["alps"] = RES_ALPS
    }

    private fun selectProfile(profile: SoundProfile) {
        whenever(prefs.getString(eq(PrefsKeys.SOUND_PROFILE), anyOrNull())).thenReturn(profile.id)
    }

    private fun newManager(variation: SoundVariation = noVariation) =
        SoundManager(context, prefs, player, variation)

    private fun verifyPlayed(resId: Int, volume: Float = 0.8f, rate: Float = 1f) {
        assertEquals(listOf(Played(resId + SAMPLE_ID_OFFSET, volume, rate)), player.played)
    }

    private fun verifyNothingPlayed() {
        assertEquals(emptyList<Played>(), player.played)
    }

    // ── sample selection ─────────────────────────────────────────────────────

    @Test
    fun `character key plays the generic sample`() {
        newManager().playKeySound(KeyKind.CHARACTER)
        verifyPlayed(RES_BLUE)
    }

    @Test
    fun `kind defaults to CHARACTER`() {
        newManager().playKeySound()
        verifyPlayed(RES_BLUE)
    }

    @Test
    fun `kind with a dedicated sample plays that sample`() {
        newManager().playKeySound(KeyKind.SPACE)
        verifyPlayed(RES_BLUE_SPACE)
    }

    @Test
    fun `kind without a dedicated sample falls back to the generic sample`() {
        newManager().playKeySound(KeyKind.ENTER)
        verifyPlayed(RES_BLUE)
    }

    @Test
    fun `profile without any dedicated samples uses its generic sample for every kind`() {
        selectProfile(SoundProfile.ALPS)
        val manager = newManager()
        KeyKind.values().forEach { manager.playKeySound(it) }
        assertEquals(
            List(KeyKind.values().size) { Played(RES_ALPS + SAMPLE_ID_OFFSET, 0.8f, 1f) },
            player.played
        )
    }

    @Test
    fun `dedicated sample that failed to load falls back to the generic sample`() {
        player.failingResIds += RES_BLUE_SPACE
        newManager().playKeySound(KeyKind.SPACE)
        verifyPlayed(RES_BLUE)
    }

    @Test
    fun `dedicated samples of other profiles are not used`() {
        selectProfile(SoundProfile.ALPS)
        newManager().playKeySound(KeyKind.SPACE)
        verifyPlayed(RES_ALPS)
    }

    // ── no-op cases ──────────────────────────────────────────────────────────

    @Test
    fun `nothing plays when sound is disabled`() {
        whenever(prefs.getBoolean(eq(PrefsKeys.SOUND_ENABLED), any())).thenReturn(false)
        newManager().playKeySound(KeyKind.SPACE)
        verifyNothingPlayed()
    }

    @Test
    fun `nothing plays for the SILENT profile`() {
        selectProfile(SoundProfile.SILENT)
        val manager = newManager()
        KeyKind.values().forEach { manager.playKeySound(it) }
        verifyNothingPlayed()
    }

    @Test
    fun `nothing plays when the profile has no loadable sample`() {
        selectProfile(SoundProfile.TOPRE) // not present in rawResources
        newManager().playKeySound(KeyKind.SPACE)
        verifyNothingPlayed()
    }

    @Test
    fun `nothing plays after release`() {
        val manager = newManager()
        manager.release()
        manager.playKeySound()
        verifyNothingPlayed()
        assertTrue(player.released)
    }

    // ── variation ────────────────────────────────────────────────────────────

    @Test
    fun `rate and volume come from the injected variation`() {
        val fixed = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextFloat(): Float = 0f
        }
        newManager(SoundVariation(fixed, pitchSpread = 0.04f, volumeSpread = 0.10f)).playKeySound()
        // nextFloat() == 0 → lowest rate, unattenuated volume.
        verifyPlayed(RES_BLUE, volume = 0.8f, rate = 0.96f)
    }

    private companion object {
        const val RES_BLUE = 1
        const val RES_BLUE_SPACE = 2
        const val RES_ALPS = 3
        const val SAMPLE_ID_OFFSET = 100
    }
}
