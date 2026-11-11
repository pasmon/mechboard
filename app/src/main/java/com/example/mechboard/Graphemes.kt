package com.example.mechboard

import java.text.BreakIterator

/**
 * Returns how many `char`s the last user-perceived character of [text] takes up,
 * or 0 if [text] is empty.
 *
 * One perceived character can be several `char`s: an emoji outside the Basic
 * Multilingual Plane is a surrogate pair, and a letter may be followed by
 * combining marks. Deleting fewer `char`s than this leaves broken text behind.
 */
fun lastGraphemeLength(text: CharSequence): Int {
    if (text.isEmpty()) return 0
    val boundaries = BreakIterator.getCharacterInstance()
    boundaries.setText(text.toString())
    val end = boundaries.last()
    val start = boundaries.previous()
    return if (start == BreakIterator.DONE) text.length else end - start
}
