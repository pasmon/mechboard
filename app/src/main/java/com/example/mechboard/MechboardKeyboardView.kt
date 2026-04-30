@file:Suppress("DEPRECATION")

package com.example.mechboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet
import android.util.TypedValue

/**
 * A [KeyboardView] subclass that paints a small digit hint in the top-right
 * corner of every key that has a [Keyboard.Key.popupCharacters] value set
 * (i.e. the top-row keys q–p that produce digits 1–0 on long-press).
 *
 * The hint colour is derived from the active theme's [R.attr.keyLabelColor] at
 * ~55 % opacity so it reads as a secondary label without competing with the
 * primary key label.
 */
class MechboardKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : KeyboardView(context, attrs, defStyleAttr) {

    private val hintPaddingPx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, HINT_PADDING_DP, context.resources.displayMetrics
    )

    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, HINT_TEXT_SIZE_SP, context.resources.displayMetrics
        )
        color = resolveHintColor(context)
    }

    /**
     * Baseline offset that places the glyph top exactly [hintPaddingPx] below the key top.
     * `fontMetrics.top` is negative (distance from baseline to top of tallest glyph),
     * so subtracting it converts a key-top position into the required baseline position.
     */
    private val hintBaselineOffsetPx = hintPaddingPx - hintPaint.fontMetrics.top

    /**
     * Per-key cached char arrays keyed by the hint character.  Populated lazily the first
     * time [onDraw] encounters a given character, then reused on every subsequent frame to
     * avoid per-draw allocations.
     */
    private val hintCharCache = HashMap<Char, CharArray>()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val kb = keyboard ?: return
        val keys = kb.keys
        for (i in keys.indices) {
            val key = keys[i]
            val hint = key.popupCharacters
            if (!hint.isNullOrEmpty()) {
                val ch = hint[0]
                val chars = hintCharCache.getOrPut(ch) { charArrayOf(ch) }
                val hintX = key.x + key.width - hintPaddingPx
                val hintY = key.y + hintBaselineOffsetPx
                canvas.drawText(chars, 0, 1, hintX, hintY, hintPaint)
            }
        }
    }

    companion object {
        private const val HINT_TEXT_SIZE_SP = 9f
        private const val HINT_PADDING_DP = 4f

        /**
         * Resolves [R.attr.keyLabelColor] from the context's theme and returns it
         * at [HINT_ALPHA] opacity (~55 %).  Falls back to white if the attribute is absent.
         */
        private fun resolveHintColor(context: Context): Int {
            val typedValue = TypedValue()
            val resolved = context.theme.resolveAttribute(R.attr.keyLabelColor, typedValue, true)
            val baseColor = if (resolved) typedValue.data else Color.WHITE
            return Color.argb(
                HINT_ALPHA,
                Color.red(baseColor),
                Color.green(baseColor),
                Color.blue(baseColor)
            )
        }

        /** ~55 % opacity (140 / 255 ≈ 0.549). */
        private const val HINT_ALPHA = 140
    }
}
