package io.github.pasmon.mechboard

/**
 * Bundled next-word predictions: for a word, the words that most often follow
 * it, best first. Read from `assets/dictionaries/<layout id>_next.txt`, which
 * only exists for languages whose source word list has word pairs (English).
 *
 * Words are looked up ignoring case and accents. Plain Kotlin, testable on the JVM.
 */
class NextWords private constructor(private val following: Map<String, List<String>>) {

    /** The words likely to follow [word], best first; empty if none are known. */
    fun after(word: String): List<String> = following[WordDictionary.foldForLookup(word)].orEmpty()

    val size: Int get() = following.size

    companion object {
        /**
         * Parses lines of tab-separated words: a word, then the words that follow
         * it, best first. When several lines are the same word ignoring case and
         * accents ("The", "the"), the first line's next words come first.
         */
        fun fromLines(lines: Sequence<String>): NextWords {
            val map = LinkedHashMap<String, MutableList<String>>()
            for (line in lines) {
                val words = line.split('\t').map { it.trim() }.filter { it.isNotEmpty() }
                if (words.size < 2) continue
                val list = map.getOrPut(WordDictionary.foldForLookup(words[0])) { mutableListOf() }
                words.drop(1).forEach { if (it !in list) list += it }
            }
            return NextWords(map)
        }
    }
}
