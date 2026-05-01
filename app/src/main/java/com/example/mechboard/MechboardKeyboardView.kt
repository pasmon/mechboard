@file:Suppress("DEPRECATION")

package com.example.mechboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
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
 *
 * A custom [labelTypeface] can be set to apply a bundled font to both the key
 * labels (drawn by the parent [KeyboardView]) and the digit hints.  The
 * typeface is injected into [KeyboardView]'s internal `mPaint` field via
 * reflection immediately before each [onDraw] call, then restored afterwards,
 * so that no other logic in the parent class is affected.  If reflection fails
 * (e.g. due to a future platform change) the view falls back silently to the
 * system default typeface.
 */
class MechboardKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : KeyboardView(context, attrs, defStyleAttr) {

    /**
     * The typeface to use for key labels and digit hints.
     * Set to `null` to use the system default typeface.
     * Changing this property triggers a full key redraw.
     */
    var labelTypeface: Typeface? = null
        set(value) {
            field = value
            hintPaint.typeface = value ?: Typeface.DEFAULT
            invalidateAllKeys()
        }

    /**
     * Lazily resolved reference to [KeyboardView]'s private `mPaint` field.
     * `null` if the field could not be found or made accessible via reflection.
     */
    private val labelPaintField: java.lang.reflect.Field? by lazy {
        runCatching {
            KeyboardView::class.java.getDeclaredField("mPaint").also { it.isAccessible = true }
        }.getOrNull()
    }

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
        // Inject the custom typeface into KeyboardView's internal label Paint so that
        // the parent's key-label drawing respects the user's font preference.
        val desiredTypeface = labelTypeface ?: Typeface.DEFAULT
        val labelPaint = labelPaintField?.runCatching { get(this@MechboardKeyboardView as KeyboardView) as? Paint }
            ?.getOrNull()
        val previousTypeface = labelPaint?.typeface
        labelPaint?.typeface = desiredTypeface

        super.onDraw(canvas)

        // Restore the original typeface so that the parent view's state is unchanged
        // between draws (relevant if the view is recycled without a new font being set).
        labelPaint?.typeface = previousTypeface

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
