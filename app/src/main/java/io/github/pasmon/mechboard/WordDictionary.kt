package io.github.pasmon.mechboard

import java.text.Normalizer
import java.util.Locale
import kotlin.math.ln

/**
 * The words of one language, for looking up completions of a partly typed word.
 *
 * Built from a list of words ordered from most to least frequent; a word's
 * position in that list is its rank. Lookups ignore case and accents, so
 * "cafe" finds "café" and "hau" finds "Haus". [suggestions] also finds words
 * the typed text is one typo away from ("teh" finds "the").
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

    /**
     * For each node of a segment tree over the entries, the index of the entry
     * with the lowest rank (most frequent word) below it; see [mostFrequentIn].
     * Leaves are at `[size, 2 * size)`.
     */
    private val minRankTree: IntArray = IntArray(2 * words.size).also { tree ->
        val n = words.size
        for (i in 0 until n) tree[n + i] = i
        for (node in n - 1 downTo 1) {
            val left = tree[2 * node]
            val right = tree[2 * node + 1]
            tree[node] = if (ranks[left] <= ranks[right]) left else right
        }
    }

    /** The characters lookup keys are made of; the letters a typo can insert or substitute. */
    private val alphabet: CharArray =
        keys.flatMapTo(sortedSetOf()) { key -> key.asIterable() }.toCharArray()

    val size: Int get() = words.size

    /** Whether [word] is in the dictionary, ignoring case but not accents. */
    fun contains(word: String): Boolean = spellingOf(word) != null

    /**
     * Returns how the dictionary spells [word], comparing without case but with
     * accents ("HAUS" gives "Haus"), or `null` if it is not in the dictionary.
     * An exact match is preferred when the dictionary has several spellings.
     */
    fun spellingOf(word: String): String? {
        val lower = word.lowercase(Locale.ROOT)
        var found: String? = null
        for (i in entriesStartingWith(foldForLookup(word))) {
            if (words[i] == word) return word
            if (found == null && words[i].lowercase(Locale.ROOT) == lower) found = words[i]
        }
        return found
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
     * Returns at most [limit] suggestions for [typed], best first: words that
     * start with it, and words that start with something one typo away from it.
     *
     * A typo is one letter left out, one too many, two letters swapped, or a wrong
     * letter; a wrong letter is likelier when its key [neighbours] the right one,
     * and a left-out apostrophe is likelier than a left-out letter.
     * Every candidate is scored by how rare the word is (log₂ of its rank) plus
     * the cost of the typo, so a correction only beats a completion when it is a
     * much more common word. A correction that is also a completion ("tge" →
     * "together") costs a little more than one spelled like the corrected text. A word spelled exactly as typed (ignoring case) always
     * comes first. Words are returned as spelled in the dictionary.
     *
     * Typos are only considered once [typed] has [MIN_TYPO_LENGTH] letters; a
     * shorter word is one typo away from too much.
     */
    fun suggestions(typed: String, limit: Int, neighbours: KeyNeighbours? = null): List<String> =
        scoredSuggestions(typed, limit, neighbours).map { it.first }

    /**
     * Returns the word to replace [typed] with when autocorrecting, as spelled in
     * the dictionary, or `null` to leave it alone. Corrects only when:
     *  - [typed] is not a dictionary word and has at least [MIN_TYPO_LENGTH] letters;
     *  - the correction is one typo or only accents away from the whole of
     *    [typed], and neither starts with the other: no completions ("nii" is
     *    not made "niin") and no dropped last letter ("thats" is not made "that");
     *  - it starts with the same letter, unless the first two were swapped ("hte"),
     *    so names such as "Gboard" are not made "board", and it is not an
     *    acronym ("wtf" is not made "WTC");
     *  - for a word of at most [SHORT_WORD_LENGTH] letters, the typo is a likely
     *    one: two letters swapped or a neighbouring key ("teh", "thw", but not
     *    "thx" or "hehe");
     *  - it is common enough (score at most [MAX_AUTOCORRECT_SCORE]); and
     *  - it is clearly better than the next such correction, by
     *    [MIN_AUTOCORRECT_MARGIN] ("wierd" could be "wired" or "weird").
     */
    fun autocorrection(typed: String, neighbours: KeyNeighbours? = null): String? {
        val folded = foldForLookup(typed)
        if (folded.length < MIN_TYPO_LENGTH || contains(typed)) return null
        val eligible = scoredSuggestions(typed, AUTOCORRECT_CANDIDATES, neighbours)
            .filter { (word, _) -> !isAcronym(word) && isAutocorrection(folded, foldForLookup(word), neighbours) }
        val (best, bestScore) = eligible.firstOrNull() ?: return null
        // The same word in another case ("Bank", "bank") is not a rival.
        val runnerUp = eligible.drop(1).firstOrNull { !it.first.equals(best, ignoreCase = true) }?.second
        return best.takeIf {
            bestScore <= MAX_AUTOCORRECT_SCORE &&
                (runnerUp == null || runnerUp - bestScore >= MIN_AUTOCORRECT_MARGIN)
        }
    }

    /** Whether [word] has a capital after its first letter, like "WTC" or "iPhone". */
    private fun isAcronym(word: String): Boolean = word.drop(1).any { it.isUpperCase() }

    /** Whether [word] may replace [typed] (both in lookup form); see [autocorrection]. */
    private fun isAutocorrection(typed: String, word: String, neighbours: KeyNeighbours?): Boolean {
        val cost = typoCost(typed, word, neighbours) ?: return false
        // "nii" → "niin" would be a completion; "thats" → "that" changes the meaning.
        if (word != typed && (word.startsWith(typed) || typed.startsWith(word))) return false
        val keepsFirstLetter = word[0] == typed[0] ||
            (word.length >= 2 && typed.length >= 2 && word[0] == typed[1] && word[1] == typed[0])
        if (!keepsFirstLetter) return false
        return typed.length > SHORT_WORD_LENGTH || cost <= TRANSPOSITION_COST
    }

    /**
     * The cost of the typo that turns [typed] into [word] (both in lookup form),
     * as in [suggestions]: 0 if they are equal, `null` if they are not one typo apart.
     */
    private fun typoCost(typed: String, word: String, neighbours: KeyNeighbours?): Double? {
        if (typed == word) return 0.0
        if (!isOneTypoApart(typed, word)) return null
        if (word.length == typed.length + 1 && word.filter { it != '\'' } == typed) return APOSTROPHE_COST
        if (typed.length != word.length) return TYPO_COST
        val i = typed.commonPrefixWith(word).length
        return when {
            typed.substring(i + 1) == word.substring(i + 1) ->
                if (neighbours?.areNeighbours(typed[i], word[i]) == true) NEIGHBOUR_TYPO_COST else TYPO_COST
            else -> TRANSPOSITION_COST
        }
    }

    /** [suggestions] with their scores: lower is better. */
    private fun scoredSuggestions(
        typed: String,
        limit: Int,
        neighbours: KeyNeighbours?
    ): List<Pair<String, Double>> {
        if (typed.isEmpty() || limit <= 0) return emptyList()
        val folded = foldForLookup(typed)
        val typedLower = typed.lowercase(Locale.ROOT)
        val scores = HashMap<Int, Double>()
        fun offer(index: Int, score: Double) {
            val previous = scores[index]
            if (previous == null || score < previous) scores[index] = score
        }

        // Completions: every word starting with what was typed.
        for (index in entriesStartingWith(folded)) {
            val wordLower = words[index].lowercase(Locale.ROOT)
            val score = when {
                wordLower == typedLower -> Double.NEGATIVE_INFINITY
                wordLower.startsWith(typedLower) -> rarity(index)
                // Accents differ: a correction, and also a completion if longer.
                keys[index] == folded -> rarity(index) + ACCENT_MISMATCH_COST
                else -> rarity(index) + ACCENT_MISMATCH_COST + CORRECTED_COMPLETION_COST
            }
            offer(index, score)
        }

        // Corrections: for each one-typo variant, the word spelled like it and the
        // most frequent word starting with it. A longer word is both a correction
        // and a completion, so it costs a little more.
        if (folded.length >= MIN_TYPO_LENGTH) {
            forEachTypoVariant(folded, neighbours) { variant, cost ->
                val range = entriesStartingWith(variant)
                if (!range.isEmpty()) {
                    var i = range.first
                    while (i <= range.last && keys[i] == variant) {
                        offer(i, rarity(i) + cost)
                        i++
                    }
                    val index = mostFrequentIn(range)
                    if (keys[index] != variant) offer(index, rarity(index) + cost + CORRECTED_COMPLETION_COST)
                }
            }
        }

        return scores.entries
            .sortedWith(compareBy({ it.value }, { ranks[it.key] }))
            .take(limit)
            .map { words[it.key] to it.value }
    }

    /** How rare the word at [index] is: log₂ of its rank, 0 for the most frequent word. */
    private fun rarity(index: Int): Double = ln(ranks[index] + 1.0) / LN_2

    /**
     * Calls [action] with every string one typo away from [folded] and the cost of
     * that typo. Adding a letter at the very end is left out: completions cover it.
     */
    private inline fun forEachTypoVariant(
        folded: String,
        neighbours: KeyNeighbours?,
        action: (variant: String, cost: Double) -> Unit
    ) {
        val n = folded.length
        val builder = StringBuilder(n + 1)
        for (i in 0 until n) {
            // Letter i typed by mistake.
            builder.setLength(0)
            builder.append(folded, 0, i).append(folded, i + 1, n)
            action(builder.toString(), TYPO_COST)
            // Letters i and i + 1 swapped.
            if (i + 1 < n && folded[i] != folded[i + 1]) {
                builder.setLength(0)
                builder.append(folded, 0, i).append(folded[i + 1]).append(folded[i]).append(folded, i + 2, n)
                action(builder.toString(), TRANSPOSITION_COST)
            }
            for (c in alphabet) {
                // Letter i should have been c.
                if (c != folded[i]) {
                    builder.setLength(0)
                    builder.append(folded, 0, i).append(c).append(folded, i + 1, n)
                    val cost = if (neighbours?.areNeighbours(folded[i], c) == true) NEIGHBOUR_TYPO_COST else TYPO_COST
                    action(builder.toString(), cost)
                }
                // Letter c left out before letter i.
                builder.setLength(0)
                builder.append(folded, 0, i).append(c).append(folded, i, n)
                action(builder.toString(), if (c == '\'') APOSTROPHE_COST else TYPO_COST)
            }
        }
    }

    /** Index of the most frequent entry in [range], which must not be empty. */
    private fun mostFrequentIn(range: IntRange): Int {
        val n = words.size
        var low = range.first + n
        var high = range.last + n + 1
        var best = range.first
        while (low < high) {
            if (low and 1 == 1) {
                if (ranks[minRankTree[low]] < ranks[best]) best = minRankTree[low]
                low++
            }
            if (high and 1 == 1) {
                high--
                if (ranks[minRankTree[high]] < ranks[best]) best = minRankTree[high]
            }
            low = low shr 1
            high = high shr 1
        }
        return best
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

        /** Shortest typed text, in letters, for which [suggestions] looks for typos. */
        const val MIN_TYPO_LENGTH = 3

        /**
         * Highest score [autocorrection] accepts: a correction costing 5 must be
         * among the 2¹¹ = 2048 most frequent words, a swap among the 8192 most
         * frequent, a neighbouring key among about 11,500.
         */
        private const val MAX_AUTOCORRECT_SCORE = 16.0
        /** How many suggestions [autocorrection] considers. */
        private const val AUTOCORRECT_CANDIDATES = 6
        /** Words up to this long are only corrected for likely typos; see [autocorrection]. */
        private const val SHORT_WORD_LENGTH = 4
        /** How much better than the next suggestion a correction must score: twice as likely. */
        private const val MIN_AUTOCORRECT_MARGIN = 1.0

        /**
         * Whether [a] and [b] are equal or one typo apart: one letter left out,
         * added, changed, or two neighbouring letters swapped.
         */
        fun isOneTypoApart(a: String, b: String): Boolean {
            if (a == b) return true
            val lengthDifference = a.length - b.length
            if (lengthDifference !in -1..1) return false
            val prefix = a.commonPrefixWith(b).length
            val restA = a.substring(prefix)
            val restB = b.substring(prefix)
            return when (lengthDifference) {
                1 -> restA.substring(1) == restB
                -1 -> restB.substring(1) == restA
                else -> restA.substring(1) == restB.substring(1) ||
                    (restA.length >= 2 && restA[0] == restB[1] && restA[1] == restB[0] &&
                        restA.substring(2) == restB.substring(2))
            }
        }

        // Costs of typos for [suggestions], in the same units as rarity: a typo
        // costing 5 needs a word 2⁵ = 32 times more frequent to win.
        private const val TYPO_COST = 5.0
        private const val NEIGHBOUR_TYPO_COST = 2.5
        private const val TRANSPOSITION_COST = 3.0
        /** A missing apostrophe ("dont" for "don't") is a common omission. */
        private const val APOSTROPHE_COST = 2.0
        /** Extra cost of a correction that is also longer than the corrected text. */
        private const val CORRECTED_COMPLETION_COST = 2.0
        /** Typed without an accent the word has, or the other way round ("paiva" for "päivä"). */
        private const val ACCENT_MISMATCH_COST = 1.0
        private val LN_2 = ln(2.0)

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
