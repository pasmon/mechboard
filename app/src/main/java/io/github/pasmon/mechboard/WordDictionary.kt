package io.github.pasmon.mechboard

import java.text.Normalizer
import java.util.Locale

/**
 * The words of one language, for looking up completions of a partly typed word.
 *
 * Built from a list of words ordered from most to least frequent; a word's
 * position in that list is its rank. Lookups ignore case and accents, so
 * "cafe" finds "café" and "hau" finds "Haus".
 *
 * This class has no Android dependency so it can be unit-tested on the plain JVM.
 */
class WordDictionary(wordsByFrequency: List<String>) {

    /** Lookup keys ([foldForLookup] of each word), sorted. */
    private val keys: Array<String>
    /** The word belonging to each entry of [keys]. */
    private val words: Array<String>
    /** The rank of each entry of [keys]: 0 for the most frequent word. */
    private val ranks: IntArray

    init {
        val unique = wordsByFrequency.filter { it.isNotEmpty() }.distinct()
        val folded = unique.map { foldForLookup(it) }
        val order = unique.indices.sortedWith(compareBy({ folded[it] }, { it }))
        // Reuse the word's own String as its key where they are equal (most
        // lower-case words), which saves a lot of memory for large lists.
        keys = Array(order.size) { i ->
            val word = unique[order[i]]
            val key = folded[order[i]]
            if (key == word) word else key
        }
        words = Array(order.size) { i -> unique[order[i]] }
        ranks = IntArray(order.size) { i -> order[i] }
    }

    val size: Int get() = words.size

    /** Whether [word] is in the dictionary, ignoring case but not accents. */
    fun contains(word: String): Boolean {
        val lower = word.lowercase(Locale.ROOT)
        return entriesStartingWith(foldForLookup(word)).any { words[it].lowercase(Locale.ROOT) == lower }
    }

    /**
     * Returns at most [limit] dictionary words that start with [prefix], best first.
     *
     * A word spelled exactly like [prefix] (ignoring case) comes first, then words
     * whose accents match what was typed, then the rest; within each group the more
     * frequent word wins. Words are returned as spelled in the dictionary.
     */
    fun completions(prefix: String, limit: Int): List<String> {
        if (prefix.isEmpty() || limit <= 0) return emptyList()
        val typedLower = prefix.lowercase(Locale.ROOT)
        // Keep the [limit] entries with the lowest score, in order. The ranges for
        // one-letter prefixes run to thousands of entries, so avoid sorting them all.
        val best = ArrayList<Long>(limit + 1)
        for (index in entriesStartingWith(foldForLookup(prefix))) {
            val score = score(index, typedLower)
            if (best.size == limit && score >= best.last()) continue
            val at = best.binarySearch(score).let { if (it < 0) -it - 1 else it }
            best.add(at, score)
            if (best.size > limit) best.removeAt(best.lastIndex)
        }
        return best.map { words[(it and INDEX_MASK).toInt()] }
    }

    /**
     * Orders the entry at [index] for [completions]: lower is better. Packs the
     * match group, then the rank, then the index (to keep scores unique).
     */
    private fun score(index: Int, typedLower: String): Long {
        val wordLower = words[index].lowercase(Locale.ROOT)
        val group = when {
            wordLower == typedLower -> 0L
            wordLower.startsWith(typedLower) -> 1L
            else -> 2L
        }
        return (group shl GROUP_SHIFT) or (ranks[index].toLong() shl RANK_SHIFT) or index.toLong()
    }

    /** Indices into [keys] of all entries whose key starts with [foldedPrefix]. */
    private fun entriesStartingWith(foldedPrefix: String): IntRange {
        if (foldedPrefix.isEmpty()) return IntRange.EMPTY
        val first = lowerBound(foldedPrefix)
        var end = first
        while (end < keys.size && keys[end].startsWith(foldedPrefix)) end++
        return first until end
    }

    /** Index of the first key that is not less than [key]. */
    private fun lowerBound(key: String): Int {
        var low = 0
        var high = keys.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (keys[mid] < key) low = mid + 1 else high = mid
        }
        return low
    }

    companion object {
        private val COMBINING_MARKS = Regex("\\p{Mn}+")

        // Score layout for [score]: 2 bits of group, 30 bits of rank, 30 bits of index.
        private const val RANK_SHIFT = 30
        private const val GROUP_SHIFT = 60
        private const val INDEX_MASK = (1L shl RANK_SHIFT) - 1

        /**
         * The form of [text] used for lookups: lower case, without accents, and with
         * typographic apostrophes replaced by the plain one.
         */
        fun foldForLookup(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFD)
                .replace(COMBINING_MARKS, "")
                .replace('’', '\'')
                .lowercase(Locale.ROOT)

        /** Builds a dictionary from lines of one word each, most frequent first; blank lines are skipped. */
        fun fromLines(lines: Sequence<String>): WordDictionary =
            WordDictionary(lines.map { it.trim() }.filter { it.isNotEmpty() }.toList())
    }
}
