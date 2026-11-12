package io.github.pasmon.mechboard

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * Which word followed which in what the user typed, per language, for
 * predicting the next word. Kept in app-private storage on the phone only:
 * [file] is excluded from backups (see `res/xml/backup_rules.xml`).
 *
 * Holds at most [MAX_PAIRS] pairs; beyond that the least used are forgotten,
 * the longest unused first. Changes are kept in memory and written by
 * [snapshotIfChanged] and [writeSnapshot], so the keyboard can write them off
 * the main thread.
 *
 * Use [LearnedWordPairs.of] for the app's shared instance; the constructor is
 * for tests. Not thread-safe: use it from the main thread, except
 * [writeSnapshot].
 */
class LearnedWordPairs(private val file: File) {

    /**
     * Use counts by pair, least recently used first. A key is the language id,
     * the previous word in lookup form ([WordDictionary.foldForLookup]) and the
     * next word as spelled, separated by tabs.
     */
    private val counts = LinkedHashMap<String, Int>(16, 0.75f, true)

    /** For each language and previous word (`"<language>\t<previous>"`), the next words seen and their counts. */
    private val index = HashMap<String, LinkedHashMap<String, Int>>()

    @Volatile
    private var changed = false

    /** Increased by [clear], so that a [Snapshot] taken before it is not written after it. */
    @Volatile
    private var generation = 0

    /** The pairs as file text, taken by [snapshotIfChanged]. */
    class Snapshot internal constructor(internal val text: String, internal val generation: Int)

    init {
        load()
    }

    /** Number of pairs learned, in all languages. */
    val size: Int get() = counts.size

    /** Records that [next] was typed right after [previous] in [language] (a [KeyboardLayout.id]). */
    fun learn(language: String, previous: String, next: String) {
        if (!isStorable(language) || !isStorable(previous) || !isStorable(next)) return
        val folded = WordDictionary.foldForLookup(previous)
        val key = "$language\t$folded\t$next"
        val count = (counts[key] ?: 0) + 1
        counts[key] = count // Also marks the pair as the most recently used.
        index.getOrPut("$language\t$folded") { LinkedHashMap() }[next] = count
        changed = true
        if (counts.size > MAX_PAIRS) forgetLeastUsed()
    }

    /** The words seen after [previous] in [language], most often seen first, at most [limit]. */
    fun after(language: String, previous: String, limit: Int): List<String> {
        val next = index["$language\t${WordDictionary.foldForLookup(previous)}"] ?: return emptyList()
        return next.entries.sortedByDescending { it.value }.take(limit).map { it.key }
    }

    /** Forgets every pair and deletes the file. */
    fun clear() {
        synchronized(this) {
            generation++
            counts.clear()
            index.clear()
            changed = false
            file.delete()
        }
    }

    /**
     * Returns the pairs to write to the file if they changed since the last
     * snapshot, otherwise `null`. Pass it to [writeSnapshot], from any thread.
     */
    fun snapshotIfChanged(): Snapshot? {
        if (!changed) return null
        changed = false
        val text = buildString {
            for ((key, count) in counts) append(key).append('\t').append(count).append('\n')
        }
        return Snapshot(text, generation)
    }

    /**
     * Writes [snapshot] to the file through a temporary file, so a crash cannot
     * leave half a file. Does nothing if [clear] was called after the snapshot.
     */
    fun writeSnapshot(snapshot: Snapshot) {
        synchronized(this) {
            if (snapshot.generation != generation) return
            try {
                file.parentFile?.mkdirs()
                val temporary = File(file.path + ".tmp")
                temporary.writeText(snapshot.text, Charsets.UTF_8)
                if (!temporary.renameTo(file)) {
                    file.writeText(snapshot.text, Charsets.UTF_8)
                    temporary.delete()
                }
            } catch (e: IOException) {
                // Keep the pairs in memory; the next snapshot tries again.
                changed = true
            }
        }
    }

    private fun load() {
        if (!file.isFile) return
        try {
            file.forEachLine(Charsets.UTF_8) { line ->
                val parts = line.split('\t')
                val count = parts.getOrNull(3)?.toIntOrNull()
                if (parts.size == 4 && count != null && count > 0 && parts.take(3).all { it.isNotEmpty() }) {
                    counts["${parts[0]}\t${parts[1]}\t${parts[2]}"] = count
                    index.getOrPut("${parts[0]}\t${parts[1]}") { LinkedHashMap() }[parts[2]] = count
                }
            }
        } catch (e: IOException) {
            counts.clear()
            index.clear()
        }
        if (counts.size > MAX_PAIRS) forgetLeastUsed()
    }

    /**
     * Brings the pairs down to 90 % of [MAX_PAIRS] by forgetting the pairs used
     * least often, the longest unused first.
     */
    private fun forgetLeastUsed() {
        val target = MAX_PAIRS * 9 / 10
        var threshold = 1
        while (counts.size > target) {
            val iterator = counts.entries.iterator()
            while (iterator.hasNext() && counts.size > target) {
                val (key, count) = iterator.next()
                if (count <= threshold) {
                    iterator.remove()
                    val (language, previous, next) = key.split('\t')
                    val group = "$language\t$previous"
                    index[group]?.let { nexts ->
                        nexts.remove(next)
                        if (nexts.isEmpty()) index.remove(group)
                    }
                }
            }
            threshold++
        }
        changed = true
    }

    private fun isStorable(text: String): Boolean =
        text.isNotEmpty() && text.none { it == '\t' || it == '\n' || it == '\r' }

    companion object {
        /** Most pairs kept, in all languages together. */
        const val MAX_PAIRS = 20_000

        /** Name of the file in the app's files directory; excluded from backups. */
        const val FILE_NAME = "learned_word_pairs.txt"

        private var shared: LearnedWordPairs? = null

        /** The app's learned pairs, shared by the keyboard and the settings screen. */
        fun of(context: Context): LearnedWordPairs {
            val file = File(context.applicationContext.filesDir, FILE_NAME)
            // The file only differs between tests, which each get a new app.
            return shared?.takeIf { it.file == file } ?: LearnedWordPairs(file).also { shared = it }
        }
    }
}
