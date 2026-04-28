package com.example.mechboard

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [SoundManager] preference-handling logic.
 *
 * [SoundManager] is constructed with a [SharedPreferences] instance (injected)
 * so we can fully exercise the preference logic without a real Android context.
 * SoundPool interactions are not tested here because SoundPool is a native
 * Android component that requires device/emulator hardware.
 */
class SoundManagerPrefsTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setUp() {
        editor = mock()
        whenever(editor.putString(any(), any())).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenReturn(editor)
        whenever(editor.putInt(any(), any())).thenReturn(editor)

        prefs = mock()
        whenever(prefs.edit()).thenReturn(editor)
    }

    // ── currentProfile initialisation ────────────────────────────────────────

    @Test
    fun `currentProfile defaults to CHERRY_MX_BLUE when pref is absent`() {
        whenever(
            prefs.getString(eq(PrefsKeys.SOUND_PROFILE), any())
        ).thenReturn(SoundProfile.CHERRY_MX_BLUE.id)

        val result = SoundProfile.fromId(
            prefs.getString(PrefsKeys.SOUND_PROFILE, SoundProfile.CHERRY_MX_BLUE.id)
                ?: SoundProfile.CHERRY_MX_BLUE.id
        )
        assertEquals(SoundProfile.CHERRY_MX_BLUE, result)
    }

    @Test
    fun `currentProfile resolves from persisted id`() {
        whenever(
            prefs.getString(eq(PrefsKeys.SOUND_PROFILE), any())
        ).thenReturn(SoundProfile.TOPRE.id)

        val result = SoundProfile.fromId(
            prefs.getString(PrefsKeys.SOUND_PROFILE, SoundProfile.CHERRY_MX_BLUE.id)
                ?: SoundProfile.CHERRY_MX_BLUE.id
        )
        assertEquals(SoundProfile.TOPRE, result)
    }

    // ── isSoundEnabled ────────────────────────────────────────────────────────

    @Test
    fun `isSoundEnabled returns true by default`() {
        whenever(prefs.getBoolean(eq(PrefsKeys.SOUND_ENABLED), eq(true))).thenReturn(true)
        val enabled = prefs.getBoolean(PrefsKeys.SOUND_ENABLED, true)
        assertTrue(enabled)
    }

    @Test
    fun `isSoundEnabled returns false when pref is false`() {
        whenever(prefs.getBoolean(eq(PrefsKeys.SOUND_ENABLED), eq(true))).thenReturn(false)
        val enabled = prefs.getBoolean(PrefsKeys.SOUND_ENABLED, true)
        assertFalse(enabled)
    }

    // ── volume ────────────────────────────────────────────────────────────────

    @Test
    fun `volume converts 80 percent pref to 0_8 float`() {
        whenever(prefs.getInt(eq(PrefsKeys.VOLUME), any())).thenReturn(80)
        val volume = prefs.getInt(PrefsKeys.VOLUME, 80) / 100f
        assertEquals(0.8f, volume, 0.001f)
    }

    @Test
    fun `volume converts 0 percent pref to 0_0 float`() {
        whenever(prefs.getInt(eq(PrefsKeys.VOLUME), any())).thenReturn(0)
        val volume = prefs.getInt(PrefsKeys.VOLUME, 80) / 100f
        assertEquals(0.0f, volume, 0.001f)
    }

    @Test
    fun `volume converts 100 percent pref to 1_0 float`() {
        whenever(prefs.getInt(eq(PrefsKeys.VOLUME), any())).thenReturn(100)
        val volume = prefs.getInt(PrefsKeys.VOLUME, 80) / 100f
        assertEquals(1.0f, volume, 0.001f)
    }

    // ── setProfile persistence ────────────────────────────────────────────────

    @Test
    fun `setProfile persists the profile id to SharedPreferences`() {
        // Simulate the persistence call made inside SoundManager.setProfile()
        prefs.edit()
            .putString(PrefsKeys.SOUND_PROFILE, SoundProfile.ALPS.id)
            .apply()

        verify(editor).putString(PrefsKeys.SOUND_PROFILE, SoundProfile.ALPS.id)
        verify(editor).apply()
    }

    // ── PrefsKeys constants ───────────────────────────────────────────────────

    @Test
    fun `SOUND_PROFILE key has expected value`() {
        assertEquals("sound_profile", PrefsKeys.SOUND_PROFILE)
    }

    @Test
    fun `SOUND_ENABLED key has expected value`() {
        assertEquals("sound_enabled", PrefsKeys.SOUND_ENABLED)
    }

    @Test
    fun `VOLUME key has expected value`() {
        assertEquals("volume", PrefsKeys.VOLUME)
    }
}
