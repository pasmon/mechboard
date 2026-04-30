@file:Suppress("DEPRECATION")

package com.example.mechboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet
import android.util.TypedValue

/**
 * A [KeyboardView] subclass that paints a small digit hint in the top-right
 * corner of every key that has a [android.inputmethodservice.Keyboard.Key.popupCharacters]
 * value set (i.e. the top-row keys q–p that produce digits 1–0 on long-press).
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val kb = keyboard ?: return
        for (key in kb.keys) {
            val hint = key.popupCharacters
            if (!hint.isNullOrEmpty()) {
                val hintX = key.x + key.width - hintPaddingPx
                val hintY = key.y + hintPaint.textSize + hintPaddingPx
                canvas.drawText(hint[0].toString(), hintX, hintY, hintPaint)
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
