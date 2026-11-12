package io.github.pasmon.mechboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LearnedWordPairsTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val file: File get() = File(folder.root, LearnedWordPairs.FILE_NAME)

    private fun LearnedWordPairs.save() {
        snapshotIfChanged()?.let { writeSnapshot(it) }
    }

    @Test
    fun `the most often seen next words come first`() {
        val pairs = LearnedWordPairs(file)
        pairs.learn("english", "going", "home")
        pairs.learn("english", "going", "to")
        pairs.learn("english", "going", "to")
        assertEquals(listOf("to", "home"), pairs.after("english", "going", 3))
        assertEquals(listOf("to"), pairs.after("english", "going", 1))
    }

    @Test
    fun `the previous word is matched ignoring case and accents`() {
        val pairs = LearnedWordPairs(file)
        pairs.learn("finnish", "Hyvää", "päivää")
        assertEquals(listOf("päivää"), pairs.after("finnish", "hyvaa", 3))
    }

    @Test
    fun `each language learns separately`() {
        val pairs = LearnedWordPairs(file)
        pairs.learn("finnish", "on", "hyvä")
        pairs.learn("english", "on", "time")
        assertEquals(listOf("hyvä"), pairs.after("finnish", "on", 3))
        assertEquals(listOf("time"), pairs.after("english", "on", 3))
        assertEquals(emptyList<String>(), pairs.after("german", "on", 3))
    }

    @Test
    fun `pairs are saved and loaded again`() {
        LearnedWordPairs(file).apply {
            learn("english", "thank", "you")
            learn("english", "thank", "you")
            learn("english", "thank", "goodness")
            save()
        }
        val loaded = LearnedWordPairs(file)
        assertEquals(listOf("you", "goodness"), loaded.after("english", "thank", 3))
        assertEquals(2, loaded.size)
    }

    @Test
    fun `nothing is written when nothing changed`() {
        val pairs = LearnedWordPairs(file)
        assertNull(pairs.snapshotIfChanged())
        pairs.learn("english", "a", "b")
        pairs.save()
        assertNull(pairs.snapshotIfChanged())
    }

    @Test
    fun `clearing forgets every pair and deletes the file`() {
        val pairs = LearnedWordPairs(file)
        pairs.learn("english", "thank", "you")
        pairs.save()
        pairs.clear()
        assertEquals(emptyList<String>(), pairs.after("english", "thank", 3))
        assertFalse(file.exists())
        assertEquals(0, LearnedWordPairs(file).size)
    }

    @Test
    fun `a snapshot taken before clearing is not written after it`() {
        val pairs = LearnedWordPairs(file)
        pairs.learn("english", "thank", "you")
        val snapshot = pairs.snapshotIfChanged()!!
        pairs.clear()
        pairs.writeSnapshot(snapshot)
        assertFalse(file.exists())
    }

    @Test
    fun `beyond the limit the least used pairs are forgotten first`() {
        val pairs = LearnedWordPairs(file)
        pairs.learn("english", "keep", "me")
        pairs.learn("english", "keep", "me")
        repeat(LearnedWordPairs.MAX_PAIRS) { pairs.learn("english", "word$it", "next") }
        assertTrue(pairs.size <= LearnedWordPairs.MAX_PAIRS)
        // Used twice, it outlives the pairs used once.
        assertEquals(listOf("me"), pairs.after("english", "keep", 3))
        // Of the pairs used once, the oldest went first.
        assertEquals(emptyList<String>(), pairs.after("english", "word0", 3))
        assertEquals(listOf("next"), pairs.after("english", "word${LearnedWordPairs.MAX_PAIRS - 1}", 3))
    }

    @Test
    fun `words that cannot be stored are ignored`() {
        val pairs = LearnedWordPairs(file)
        pairs.learn("english", "", "you")
        pairs.learn("english", "a\tb", "you")
        assertEquals(0, pairs.size)
    }

    @Test
    fun `malformed lines in the file are skipped`() {
        file.writeText("english\tthank\tyou\t3\nbroken line\nenglish\tthank\tgod\tnotanumber\nenglish\tgo\thome\t0\n")
        val pairs = LearnedWordPairs(file)
        assertEquals(1, pairs.size)
        assertEquals(listOf("you"), pairs.after("english", "thank", 3))
    }

    @Test
    fun `the learned pairs file is kept out of backups`() {
        listOf("src/main/res/xml/backup_rules.xml", "src/main/res/xml/data_extraction_rules.xml").forEach { path ->
            assertTrue(path, File(path).readText().contains("path=\"${LearnedWordPairs.FILE_NAME}\""))
        }
    }
}
