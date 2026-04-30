package com.example.mechboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class SoundProfileTest {

    @Test
    fun `all profiles have unique ids`() {
        val ids = SoundProfile.values().map { it.id }
        assertEquals("Duplicate SoundProfile ids detected", ids.size, ids.distinct().size)
    }

    @Test
    fun `all profiles have non-blank display names`() {
        SoundProfile.values().forEach { profile ->
            assert(profile.displayName.isNotBlank()) {
                "${profile.name} has a blank displayName"
            }
        }
    }

    @Test
    fun `only SILENT has null rawFileName`() {
        SoundProfile.values()
            .filter { it != SoundProfile.SILENT }
            .forEach { profile ->
                assertNotNull(
                    "${profile.name} should have a non-null rawFileName",
                    profile.rawFileName
                )
            }
        assertNull(
            "SILENT should have a null rawFileName",
            SoundProfile.SILENT.rawFileName
        )
    }

    @Test
    fun `non-SILENT rawFileNames are non-blank`() {
        SoundProfile.values()
            .filter { it != SoundProfile.SILENT }
            .forEach { profile ->
                assert(!profile.rawFileName.isNullOrBlank()) {
                    "${profile.name} rawFileName is blank"
                }
            }
    }

    @Test
    fun `fromId returns correct profile for each id`() {
        SoundProfile.values().forEach { profile ->
            assertSame(profile, SoundProfile.fromId(profile.id))
        }
    }

    @Test
    fun `fromId returns CHERRY_MX_BLUE for unknown id`() {
        assertSame(SoundProfile.CHERRY_MX_BLUE, SoundProfile.fromId("unknown_xyz"))
    }

    @Test
    fun `fromId returns CHERRY_MX_BLUE for empty string`() {
        assertSame(SoundProfile.CHERRY_MX_BLUE, SoundProfile.fromId(""))
    }

    @Test
    fun `profiles cover all expected switch types`() {
        val expectedIds = setOf(
            "cherry_blue",
            "cherry_red",
            "cherry_brown",
            "topre",
            "alps",
            "nk_cream",
            "holy_panda",
            "typewriter",
            "silent"
        )
        val actualIds = SoundProfile.values().map { it.id }.toSet()
        assertEquals(expectedIds, actualIds)
    }

    @Test
    fun `rawFileName matches profile id for non-SILENT profiles`() {
        SoundProfile.values()
            .filter { it != SoundProfile.SILENT }
            .forEach { profile ->
                assertEquals(
                    "rawFileName should equal id for ${profile.name}",
                    profile.id,
                    profile.rawFileName
                )
            }
    }

    @Test
    fun `all profiles are non-null`() {
        SoundProfile.values().forEach { assertNotNull(it) }
    }
}
