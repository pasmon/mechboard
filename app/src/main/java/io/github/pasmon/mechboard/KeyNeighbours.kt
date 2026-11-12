package io.github.pasmon.mechboard

/**
 * Which letters sit next to each other on a keyboard, for telling a likely
 * typo (a neighbouring key hit by mistake) from an unlikely one.
 *
 * Letters are compared in [WordDictionary.foldForLookup] form, lower case and
 * without accents, like dictionary lookups. Plain Kotlin, testable on the JVM.
 */
class KeyNeighbours private constructor(private val neighbours: Map<Char, Set<Char>>) {

    /** Whether the keys typing [a] and [b] touch or nearly touch. A key is not its own neighbour. */
    fun areNeighbours(a: Char, b: Char): Boolean = neighbours[a]?.contains(b) == true

    /** The letters next to [letter], for tests and debugging. */
    fun of(letter: Char): Set<Char> = neighbours[letter].orEmpty()

    companion object {
        /** Nominal key size used to lay out the keys; only the proportions matter. */
        private const val WIDTH = 1000
        private const val ROW_HEIGHT = 150
        /**
         * Keys whose centres are within this many key sizes are neighbours:
         * the keys either side in the same row and the two or three closest keys
         * in the rows above and below.
         */
        private const val MAX_DISTANCE = 1.5f

        /** Works out the neighbours of every letter key of [definition]. */
        fun fromDefinition(definition: KeyboardDefinition): KeyNeighbours {
            val geometry = KeyboardGeometry(definition, WIDTH, ROW_HEIGHT)
            val letterKeys = geometry.keys.mapNotNull { bounds ->
                val code = bounds.key.code
                if (code <= 0 || !Character.isLetter(code)) return@mapNotNull null
                val folded = WordDictionary.foldForLookup(String(Character.toChars(code)))
                folded.singleOrNull()?.let { it to bounds }
            }
            val map = mutableMapOf<Char, MutableSet<Char>>()
            for ((a, boundsA) in letterKeys) {
                for ((b, boundsB) in letterKeys) {
                    if (a == b) continue
                    val keyWidth = minOf(boundsA.right - boundsA.left, boundsB.right - boundsB.left).toFloat()
                    val dx = (boundsA.centerX - boundsB.centerX) / keyWidth
                    val dy = (boundsA.centerY - boundsB.centerY) / ROW_HEIGHT
                    if (dx * dx + dy * dy <= MAX_DISTANCE * MAX_DISTANCE) {
                        map.getOrPut(a) { mutableSetOf() } += b
                    }
                }
            }
            return KeyNeighbours(map)
        }
    }
}
