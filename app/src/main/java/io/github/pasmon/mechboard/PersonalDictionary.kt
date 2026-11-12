package io.github.pasmon.mechboard

import android.content.Context
import java.io.File
import java.io.IOException
import java.util.Locale

/**
 * The words the user saved: names, slang, technical terms and anything else
 * the bundled word lists lack. One list for all languages.
 *
 * Stored in [file], one word per line. Changes are saved immediately and
 * reported to listeners, so the keyboard picks up words added or deleted on
 * the settings screen while it is open.
 *
 * Use [PersonalDictionary.of] for the app's shared instance; the constructor is
 * for tests. Not thread-safe: use it from the main thread.
 */
class PersonalDictionary(private val file: File) {

    /** Called after the words changed. */
    fun interface Listener {
        fun onPersonalDictionaryChanged()
    }

    private val listeners = mutableListOf<Listener>()

    /** The saved words, sorted alphabetically ignoring case. */
    var words: List<String> = load()
        private set

    private var dictionary: WordDictionary? = null

    /** The saved words as a [WordDictionary], for suggestions. */
    fun asWordDictionary(): WordDictionary =
        dictionary ?: WordDictionary(words).also { dictionary = it }

    /** Whether [word] is saved, ignoring case. */
    fun contains(word: String): Boolean {
        val lower = word.lowercase(Locale.ROOT)
        return words.any { it.lowercase(Locale.ROOT) == lower }
    }

    /**
     * Saves [word] (trimmed). Returns `false`, changing nothing, if it is not a
     * valid word (see [normalize]) or is already saved, ignoring case.
     */
    fun add(word: String): Boolean {
        val normalized = normalize(word) ?: return false
        if (contains(normalized)) return false
        update(words + normalized)
        return true
    }

    /** Deletes [word], matching case exactly. Returns `false` if it was not saved. */
    fun remove(word: String): Boolean {
        if (word !in words) return false
        update(words - word)
        return true
    }

    fun addListener(listener: Listener) {
        listeners += listener
    }

    fun removeListener(listener: Listener) {
        listeners -= listener
    }

    private fun update(newWords: List<String>) {
        words = sorted(newWords)
        dictionary = null
        save()
        listeners.toList().forEach { it.onPersonalDictionaryChanged() }
    }

    private fun load(): List<String> = try {
        if (file.isFile) {
            sorted(file.readLines(Charsets.UTF_8).mapNotNull { normalize(it) }.distinctBy { it.lowercase(Locale.ROOT) })
        } else {
            emptyList()
        }
    } catch (e: IOException) {
        emptyList()
    }

    /** Writes [words] to a temporary file and moves it over [file], so a crash cannot leave half a list. */
    private fun save() {
        try {
            file.parentFile?.mkdirs()
            val temporary = File(file.path + ".tmp")
            temporary.writeText(words.joinToString(separator = "") { it + "\n" }, Charsets.UTF_8)
            if (!temporary.renameTo(file)) {
                file.writeText(temporary.readText(Charsets.UTF_8), Charsets.UTF_8)
                temporary.delete()
            }
        } catch (e: IOException) {
            // Keep the words in memory; they are saved with the next change.
        }
    }

    companion object {
        /** Longest word that can be saved, in `char`s. */
        const val MAX_WORD_LENGTH = 48

        private const val FILE_NAME = "personal_dictionary.txt"

        private var shared: PersonalDictionary? = null

        /** The app's personal dictionary, shared by the keyboard and the settings screen. */
        fun of(context: Context): PersonalDictionary {
            val file = File(context.applicationContext.filesDir, FILE_NAME)
            // The file only differs between tests, which each get a new app.
            return shared?.takeIf { it.file == file } ?: PersonalDictionary(file).also { shared = it }
        }

        /**
         * Returns [word] trimmed, or `null` if it cannot be saved: empty, longer than
         * [MAX_WORD_LENGTH], containing whitespace, or not starting with a letter.
         */
        fun normalize(word: String): String? {
            val trimmed = word.trim()
            return trimmed.takeIf {
                it.isNotEmpty() &&
                    it.length <= MAX_WORD_LENGTH &&
                    it.none { c -> c.isWhitespace() } &&
                    it.first().isLetter()
            }
        }

        private fun sorted(words: List<String>): List<String> =
            words.sortedWith(compareBy<String> { it.lowercase(Locale.ROOT) }.thenBy { it })
    }
}
