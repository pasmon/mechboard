package io.github.pasmon.mechboard

import android.text.InputType
import java.util.Locale

/**
 * The rules behind the suggestion strip: which word is being typed, which
 * suggestions to offer for it, where they go in the strip, and which text
 * fields get suggestions at all.
 *
 * Plain Kotlin; the [InputType] constants it uses are compile-time constants,
 * so it is testable on the plain JVM.
 */
object WordSuggestions {

    /** Number of slots in the suggestion strip. */
    const val SLOT_COUNT = 3

    /**
     * Returns the word that ends at the cursor, given the text before the cursor,
     * or an empty string if the character before the cursor is not part of a word.
     *
     * A word is a run of letters and combining marks, which may contain
     * apostrophes ("don't", "aujourd'hui") but does not start with one.
     */
    fun wordBeforeCursor(textBefore: CharSequence): String {
        var start = textBefore.length
        while (start > 0 && isWordChar(textBefore[start - 1])) start--
        while (start < textBefore.length && isApostrophe(textBefore[start])) start++
        return textBefore.subSequence(start, textBefore.length).toString()
    }

    /** Whether the text after the cursor starts with part of a word, i.e. the cursor is inside one. */
    fun continuesWord(textAfter: CharSequence): Boolean =
        textAfter.isNotEmpty() && isLetterOrMark(textAfter[0])

    /**
     * Returns up to [SLOT_COUNT] suggestions for the [typed] word from [dictionary],
     * best first, with the capitalisation of what was typed: "Hel" gives "Hello",
     * "HEL" gives "HELLO". Dictionary capitals are kept ("haus" gives "Haus").
     */
    fun suggestionsFor(typed: String, dictionary: WordDictionary): List<String> {
        if (typed.isEmpty()) return emptyList()
        return dictionary.completions(typed, SLOT_COUNT * 2)
            .map { matchCase(typed, it) }
            .distinct()
            .take(SLOT_COUNT)
    }

    /** Applies the capitalisation of [typed] to [word]. */
    fun matchCase(typed: String, word: String): String {
        val letters = typed.filter { it.isLetter() }
        return when {
            letters.length > 1 && letters.all { it.isUpperCase() } -> word.uppercase(Locale.ROOT)
            typed.first().isUpperCase() ->
                word.replaceFirstChar { it.titlecase(Locale.ROOT) }
            else -> word
        }
    }

    /**
     * Places [suggestions] (best first) in the strip's [SLOT_COUNT] slots, left to
     * right: the best in the middle, where the thumb finds it, the second on the
     * left and the third on the right. Empty slots are `null`.
     */
    fun arrangeInSlots(suggestions: List<String>): List<String?> = listOf(
        suggestions.getOrNull(1),
        suggestions.getOrNull(0),
        suggestions.getOrNull(2)
    )

    /**
     * Whether a field of [inputType] should get word suggestions: text fields that
     * are not for passwords, addresses or other non-words, and that do not ask
     * for no suggestions.
     */
    fun fieldAllowsSuggestions(inputType: Int): Boolean {
        if (inputType and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return false
        if (inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0) return false
        return when (inputType and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_URI -> false
            else -> true
        }
    }

    private fun isWordChar(c: Char): Boolean = isLetterOrMark(c) || isApostrophe(c)

    private fun isLetterOrMark(c: Char): Boolean =
        Character.isLetter(c) ||
            Character.getType(c) == Character.NON_SPACING_MARK.toInt() ||
            Character.getType(c) == Character.COMBINING_SPACING_MARK.toInt()

    private fun isApostrophe(c: Char): Boolean = c == '\'' || c == '’'
}
