package com.example.mechboard

import kotlin.math.abs
import kotlin.math.roundToInt

/** A [Key] and the pixel rectangle it occupies; [right] and [bottom] are exclusive. */
data class KeyBounds(
    val key: Key,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/**
 * Places the keys of a [KeyboardDefinition] on a surface [width] pixels wide,
 * with rows [rowHeight] pixels tall, and answers which key a touch belongs to.
 *
 * Coordinates are relative to the top-left corner of the first row. Pure
 * arithmetic with no Android dependency, so it is unit-testable on the JVM.
 */
class KeyboardGeometry(definition: KeyboardDefinition, val width: Int, val rowHeight: Int) {

    private val rows: List<List<KeyBounds>> = definition.rows.mapIndexed { rowIndex, row ->
        val top = rowIndex * rowHeight
        var x = 0f
        row.map { key ->
            x += key.gapPercent * width / 100f
            val left = x
            x += key.widthPercent * width / 100f
            // Round both edges from the running total so neighbours share an edge exactly.
            KeyBounds(key, left.roundToInt(), top, x.roundToInt(), top + rowHeight)
        }
    }

    /** Every key, row by row. The index of a key in this list identifies it. */
    val keys: List<KeyBounds> = rows.flatten()

    /** Index in [keys] of the first key of each row. */
    private val rowStartIndices: List<Int> = rows.runningFold(0) { start, row -> start + row.size }

    val height: Int = rows.size * rowHeight

    /**
     * Returns the index in [keys] of the key a touch at ([x], [y]) belongs to,
     * or [NO_KEY] if there are no keys.
     *
     * A touch always resolves to a key: one that lands in the empty space beside
     * a short row, or just outside the keyboard, goes to the nearest key of the
     * nearest row. A thumb that misses by a few pixels should still type.
     */
    fun indexOfKeyAt(x: Float, y: Float): Int {
        if (rows.isEmpty() || rowHeight <= 0) return NO_KEY
        val rowIndex = (y / rowHeight).toInt().coerceIn(0, rows.size - 1)
        val row = rows[rowIndex]
        if (row.isEmpty()) return NO_KEY
        val inRow = row.indexOfFirst { x >= it.left && x < it.right }.takeIf { it >= 0 }
            ?: row.indices.minByOrNull { minOf(abs(x - row[it].left), abs(x - row[it].right)) }!!
        return rowStartIndices[rowIndex] + inRow
    }

    companion object {
        const val NO_KEY = -1
    }
}
