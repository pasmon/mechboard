package io.github.pasmon.mechboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The row of word suggestions above the keys.
 *
 * Shows up to [WordSuggestions.SLOT_COUNT] words, arranged by
 * [WordSuggestions.arrangeInSlots]; tapping one reports it to [listener].
 * Colours come from the active keyboard theme, like [MechboardKeyboardView].
 */
class SuggestionStripView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    /** Receives taps on suggestions. */
    interface Listener {
        /** A finger landed on a suggestion; used to play the key sound on touch-down. */
        fun onSuggestionPress()

        /** The suggestion [word] was tapped. */
        fun onSuggestionPicked(word: String)
    }

    var listener: Listener? = null

    /** The suggestions shown, best first. */
    var suggestions: List<String> = emptyList()
        set(value) {
            if (field == value) return
            field = value
            WordSuggestions.arrangeInSlots(value).forEachIndexed { i, word ->
                slots[i].text = word ?: ""
                slots[i].isEnabled = word != null
                slots[i].contentDescription = word?.let {
                    context.getString(R.string.suggestion_description, it)
                }
            }
        }

    /**
     * The typeface to use for suggestions, matching the keys' labels.
     * Set to `null` to use the system default typeface.
     */
    var labelTypeface: Typeface? = null
        set(value) {
            field = value
            slots.forEachIndexed { i, slot ->
                // The middle slot holds the best suggestion; make it stand out.
                slot.setTypeface(value ?: Typeface.DEFAULT, if (i == BEST_SLOT) Typeface.BOLD else Typeface.NORMAL)
            }
        }

    private val labelColor = resolveColor(R.attr.keyLabelColor, Color.WHITE)
    private val pressedColor = resolveColor(R.attr.keyPressedColor, Color.GRAY)

    private val slots: List<TextView>

    init {
        orientation = HORIZONTAL
        val height = resources.getDimensionPixelSize(R.dimen.suggestion_strip_height)
        minimumHeight = height
        slots = List(WordSuggestions.SLOT_COUNT) { i ->
            if (i > 0) addView(divider(), LayoutParams(dividerWidth(), height / 2).apply {
                gravity = Gravity.CENTER_VERTICAL
            })
            createSlot().also { addView(it, LayoutParams(0, height, 1f)) }
        }
        labelTypeface = null
    }

    // The touch listener only plays the sound and returns false, so the TextView
    // still handles the touch and performs the click.
    @SuppressLint("ClickableViewAccessibility")
    private fun createSlot() = TextView(context).apply {
        gravity = Gravity.CENTER
        setTextColor(labelColor)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.suggestion_text_size))
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        val padding = resources.getDimensionPixelSize(R.dimen.suggestion_padding)
        setPadding(padding, 0, padding, 0)
        background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(pressedColor))
            addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
        }
        isEnabled = false
        setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN && isEnabled) {
                listener?.onSuggestionPress()
            }
            false
        }
        // performClick() (used by accessibility services) runs this even on a
        // disabled view, so check for an empty slot here too.
        setOnClickListener {
            if (isEnabled && text.isNotEmpty()) listener?.onSuggestionPicked(text.toString())
        }
    }

    private fun divider() = View(context).apply {
        setBackgroundColor(Color.argb(
            DIVIDER_ALPHA, Color.red(labelColor), Color.green(labelColor), Color.blue(labelColor)
        ))
    }

    private fun dividerWidth(): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics)
            .toInt().coerceAtLeast(1)

    private fun resolveColor(attr: Int, fallback: Int): Int {
        val typedValue = TypedValue()
        return if (context.theme.resolveAttribute(attr, typedValue, true)) typedValue.data else fallback
    }

    /** Test access to the text shown in each slot, left to right. */
    internal val slotTexts: List<String> get() = slots.map { it.text.toString() }

    /** Test access: taps the slot at [index], left to right. */
    internal fun clickSlot(index: Int) {
        slots[index].performClick()
    }

    private companion object {
        /** Index of the slot holding the best suggestion; see [WordSuggestions.arrangeInSlots]. */
        const val BEST_SLOT = 1
        const val DIVIDER_ALPHA = 0x40
    }
}
