package io.github.pasmon.mechboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import java.util.Locale

/**
 * One slot's content in the suggestion strip.
 */
data class StripItem(val word: String, val kind: Kind) {
    enum class Kind {
        /** A completion from the bundled or the personal dictionary. */
        SUGGESTION,
        /** The word as typed, offered because no dictionary knows it. */
        TYPED_WORD,
        /** The correction a space (or punctuation) will make; otherwise like [SUGGESTION]. */
        AUTOCORRECTION,
        /** An offer to save [word] to the personal dictionary. */
        SAVE_WORD
    }
}

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

    /**
     * Returns the word before the one being typed (or before the cursor, when no
     * word is being typed), given the text before the cursor; an empty string if
     * there is none. The two must be separated by spaces only: after a full stop,
     * a comma or a line break there is no previous word to predict from.
     *
     * "I went to t" and "I went to " both give "to".
     */
    fun previousWord(textBefore: CharSequence): String {
        val wordStart = textBefore.length - wordBeforeCursor(textBefore).length
        var end = wordStart
        while (end > 0 && textBefore[end - 1] == ' ') end--
        if (end == wordStart) return ""
        return wordBeforeCursor(textBefore.subSequence(0, end))
    }

    /**
     * Returns the previous word and the word that ends at the cursor, given the
     * text before the cursor, or `null` unless both exist. Called when a word has
     * just been finished, to learn which word followed which.
     */
    fun finishedWordPair(textBefore: CharSequence): Pair<String, String>? {
        val word = wordBeforeCursor(textBefore)
        if (word.isEmpty()) return null
        val previous = previousWord(textBefore)
        return if (previous.isEmpty()) null else previous to word
    }

    /**
     * Returns up to [SLOT_COUNT] predictions of the next word: words [learned]
     * from the user's typing first, then [bundled] ones, each list best first.
     * They take the case [shift] asks for: capitalised when shift is on, all
     * capitals with caps lock.
     */
    fun predictionsFor(learned: List<String>, bundled: List<String>, shift: ShiftState): List<String> =
        (learned + bundled)
            .map { applyShift(it, shift) }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .take(SLOT_COUNT)

    private fun applyShift(word: String, shift: ShiftState): String = when (shift) {
        ShiftState.OFF    -> word
        ShiftState.ON     -> word.replaceFirstChar { it.titlecase(Locale.ROOT) }
        ShiftState.LOCKED -> word.uppercase(Locale.ROOT)
    }

    /** Whether the text after the cursor starts with part of a word, i.e. the cursor is inside one. */
    fun continuesWord(textAfter: CharSequence): Boolean =
        textAfter.isNotEmpty() && isLetterOrMark(textAfter[0])

    /**
     * Returns up to [SLOT_COUNT] suggestions for the [typed] word, best first, with
     * the capitalisation of what was typed: "Hel" gives "Hello", "HEL" gives
     * "HELLO". Dictionary capitals are kept ("haus" gives "Haus").
     *
     * A word spelled exactly as typed comes first, then words that often follow
     * the previous word ([likelyNext], best first) and start with what was typed,
     * then completions from the [personal] dictionary, then suggestions from the bundled [dictionary], which
     * include corrections of typos (see [WordDictionary.suggestions]); [neighbours]
     * are the current layout's, for telling likely typos. Either dictionary may be
     * `null` (not loaded, or not wanted).
     */
    fun suggestionsFor(
        typed: String,
        dictionary: WordDictionary?,
        personal: WordDictionary? = null,
        neighbours: KeyNeighbours? = null,
        likelyNext: List<String> = emptyList()
    ): List<String> {
        if (typed.isEmpty()) return emptyList()
        val own = personal?.completions(typed, SLOT_COUNT * 2).orEmpty()
        val bundled = dictionary?.suggestions(typed, SLOT_COUNT * 2, neighbours).orEmpty()
        val typedLower = typed.lowercase(Locale.ROOT)
        val exact = (own + bundled).filter { it.lowercase(Locale.ROOT) == typedLower }
        val foldedTyped = WordDictionary.foldForLookup(typed)
        val following = likelyNext.filter { WordDictionary.foldForLookup(it).startsWith(foldedTyped) }
        return (exact + following + own + bundled)
            .map { matchCase(typed, it) }
            .distinct()
            .take(SLOT_COUNT)
    }

    /**
     * Returns the strip's [SLOT_COUNT] slots, left to right, for the [typed] word;
     * empty slots are `null`. The best suggestion goes in the middle, where the
     * thumb finds it.
     *
     * When neither dictionary knows the typed word, it takes the left slot as
     * typed, with the best suggestion (often a correction: "teh" → "the") in the
     * middle ([StripItem.Kind.TYPED_WORD]), so it can be inserted unchanged and
     * then saved. Whether a word is known can only be told once [dictionary] has
     * loaded; until then the typed word is not offered. [likelyNext] are words
     * that often follow the previous word; see [suggestionsFor].
     *
     * With an [autocorrection] (see [autocorrectionFor]), the middle slot shows
     * it as [StripItem.Kind.AUTOCORRECTION], so the user sees what a space will do.
     */
    fun stripFor(
        typed: String,
        dictionary: WordDictionary?,
        personal: WordDictionary? = null,
        neighbours: KeyNeighbours? = null,
        likelyNext: List<String> = emptyList(),
        autocorrection: String? = null
    ): List<StripItem?> {
        val suggestions = suggestionsFor(typed, dictionary, personal, neighbours, likelyNext)
            .map { StripItem(it, StripItem.Kind.SUGGESTION) }
        if (autocorrection != null) {
            // Typed word on the left, as always for unknown words; the correction a
            // space will make in the middle, marked; the best other suggestion right.
            val other = suggestions.firstOrNull { !it.word.equals(autocorrection, ignoreCase = true) }
            return listOf(
                StripItem(typed, StripItem.Kind.TYPED_WORD),
                StripItem(autocorrection, StripItem.Kind.AUTOCORRECTION),
                other
            )
        }
        val unknown = typed.isNotEmpty() && dictionary != null &&
            !dictionary.contains(typed) && personal?.contains(typed) != true
        return if (unknown) {
            listOf(StripItem(typed, StripItem.Kind.TYPED_WORD), suggestions.getOrNull(0), suggestions.getOrNull(1))
        } else {
            arrangeInSlots(suggestions)
        }
    }

    /** The strip offering to save [word] to the personal dictionary, in the middle slot. */
    fun saveOfferFor(word: String): List<StripItem?> =
        listOf(null, StripItem(word, StripItem.Kind.SAVE_WORD), null)

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
    fun <T> arrangeInSlots(suggestions: List<T>): List<T?> = listOf(
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

    /**
     * Returns what a space after the [typed] word should correct it to, with the
     * capitalisation of what was typed, or `null` to leave it alone; see
     * [WordDictionary.autocorrection]. Words in the [personal] dictionary, words
     * with capitals after the first letter ("iPhone", "NASA") and anything typed
     * before [dictionary] has loaded are left alone.
     */
    fun autocorrectionFor(
        typed: String,
        dictionary: WordDictionary?,
        personal: WordDictionary? = null,
        neighbours: KeyNeighbours? = null
    ): String? {
        if (dictionary == null || typed.isEmpty()) return null
        if (typed.drop(1).any { it.isUpperCase() }) return null
        if (personal?.contains(typed) == true) return null
        return dictionary.autocorrection(typed, neighbours)?.let { matchCase(typed, it) }
    }

    /**
     * Whether a field of [inputType] may be autocorrected: one that gets
     * suggestions and either asks for autocorrection or is multi-line, like a
     * message or a note. Single-line fields that do not ask, such as search boxes
     * and name fields, are left alone, as are web text fields that do not ask.
     */
    fun fieldAllowsAutocorrect(inputType: Int): Boolean {
        if (!fieldAllowsSuggestions(inputType)) return false
        val asks = inputType and InputType.TYPE_TEXT_FLAG_AUTO_CORRECT != 0
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        if (variation == InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT) return asks
        return asks || inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0
    }

    /**
     * Whether words typed in a field with [imeOptions] may be saved to the
     * personal dictionary: not in fields that ask the keyboard not to learn from
     * what is typed (incognito tabs, for example).
     */
    fun fieldAllowsLearning(imeOptions: Int): Boolean =
        imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING == 0

    private fun isWordChar(c: Char): Boolean = isLetterOrMark(c) || isApostrophe(c)

    private fun isLetterOrMark(c: Char): Boolean =
        Character.isLetter(c) ||
            Character.getType(c) == Character.NON_SPACING_MARK.toInt() ||
            Character.getType(c) == Character.COMBINING_SPACING_MARK.toInt()

    private fun isApostrophe(c: Char): Boolean = c == '\'' || c == '’'
}
