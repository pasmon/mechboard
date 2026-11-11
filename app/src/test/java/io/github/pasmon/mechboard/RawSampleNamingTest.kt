package io.github.pasmon.mechboard

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Guards the file-name convention that ties audio files in `res/raw` to
 * [SoundProfile] and [KeyKind].
 *
 * Samples are looked up by name at runtime, so a misspelt file is silently
 * ignored and the key just falls back to the generic sound. This test makes
 * such a file fail the build instead.
 */
class RawSampleNamingTest {

    /** `res/raw`, relative to the module directory Gradle runs unit tests in. */
    private val rawDir = File("src/main/res/raw")

    private val expectedNames: Set<String> = SoundProfile.values().flatMap { profile ->
        listOfNotNull(profile.rawFileName) + KeyKind.values().mapNotNull { profile.sampleNameFor(it) }
    }.toSet()

    @Test
    fun `every file in res raw is a sample some profile can play`() {
        assumeTrue("res/raw not found from ${File("").absolutePath}", rawDir.isDirectory)
        val unknown = rawDir.listFiles().orEmpty()
            .map { it.nameWithoutExtension }
            .filterNot { it in expectedNames }
        assertTrue("Files in res/raw that no SoundProfile/KeyKind refers to: $unknown", unknown.isEmpty())
    }

    @Test
    fun `every profile with audio has its generic sample in res raw`() {
        assumeTrue("res/raw not found from ${File("").absolutePath}", rawDir.isDirectory)
        val present = rawDir.listFiles().orEmpty().map { it.nameWithoutExtension }.toSet()
        val missing = SoundProfile.values().mapNotNull { it.rawFileName }.filterNot { it in present }
        assertTrue("Profiles whose generic sample is missing from res/raw: $missing", missing.isEmpty())
    }
}
