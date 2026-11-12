package io.github.pasmon.mechboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
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
 * Shows [WordSuggestions.SLOT_COUNT] slots of [StripItem]s, left to right, as
 * laid out by [WordSuggestions.stripFor]. A word as typed is shown in quotes and
 * an offer to save a word with a "+". Taps and long presses are reported to
 * [listener]. Colours come from the active keyboard theme, like
 * [MechboardKeyboardView]. The correction a space will make is underlined.
 */
class SuggestionStripView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    /** Receives taps on suggestions. */
    interface Listener {
        /** A finger landed on a suggestion; used to play the key sound on touch-down. */
        fun onSuggestionPress()

        /** The slot showing [item] was tapped. */
        fun onSuggestionPicked(item: StripItem)

        /**
         * The slot showing [item] was held down. Returns whether the long press
         * was used; if not, it is treated as a tap once the finger lifts.
         */
        fun onSuggestionLongPressed(item: StripItem): Boolean
    }

    var listener: Listener? = null

    /** What each slot shows, left to right; `null` for an empty slot. */
    var items: List<StripItem?> = EMPTY
        set(value) {
            require(value.size == WordSuggestions.SLOT_COUNT) { "need ${WordSuggestions.SLOT_COUNT} slots" }
            if (field == value) return
            field = value
            value.forEachIndexed { i, item ->
                slots[i].text = item?.let { label(it) } ?: ""
                slots[i].isEnabled = item != null
                slots[i].contentDescription = item?.let { description(it) }
                // Underline the correction a space will make.
                slots[i].paintFlags = if (item?.kind == StripItem.Kind.AUTOCORRECTION) {
                    slots[i].paintFlags or Paint.UNDERLINE_TEXT_FLAG
                } else {
                    slots[i].paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
                }
            }
        }

    private fun label(item: StripItem): String = when (item.kind) {
        StripItem.Kind.SUGGESTION, StripItem.Kind.AUTOCORRECTION -> item.word
        StripItem.Kind.TYPED_WORD -> context.getString(R.string.suggestion_typed_word, item.word)
        StripItem.Kind.SAVE_WORD  -> context.getString(R.string.suggestion_save_word, item.word)
    }

    private fun description(item: StripItem): String = when (item.kind) {
        StripItem.Kind.SUGGESTION -> context.getString(R.string.suggestion_description, item.word)
        StripItem.Kind.AUTOCORRECTION -> context.getString(R.string.suggestion_autocorrection_description, item.word)
        StripItem.Kind.TYPED_WORD -> context.getString(R.string.suggestion_typed_word_description, item.word)
        StripItem.Kind.SAVE_WORD  -> context.getString(R.string.suggestion_save_word_description, item.word)
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
            createSlot(i).also { addView(it, LayoutParams(0, height, 1f)) }
        }
        labelTypeface = null
    }

    // The touch listener only plays the sound and returns false, so the TextView
    // still handles the touch and performs the click.
    @SuppressLint("ClickableViewAccessibility")
    private fun createSlot(index: Int) = TextView(context).apply {
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
            if (isEnabled) items[index]?.let { listener?.onSuggestionPicked(it) }
        }
        setOnLongClickListener {
            isEnabled && items[index]?.let { listener?.onSuggestionLongPressed(it) } == true
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

    /** Test access to whether each slot is underlined, left to right. */
    internal val slotUnderlines: List<Boolean>
        get() = slots.map { it.paintFlags and Paint.UNDERLINE_TEXT_FLAG != 0 }

    /** Test access: taps the slot at [index], left to right. */
    internal fun clickSlot(index: Int) {
        slots[index].performClick()
    }

    /** Test access: holds down the slot at [index], left to right. */
    internal fun longClickSlot(index: Int): Boolean = slots[index].performLongClick()

    companion object {
        /** A strip with every slot empty. */
        val EMPTY: List<StripItem?> = List(WordSuggestions.SLOT_COUNT) { null }


        /** Index of the slot holding the best suggestion; see [WordSuggestions.arrangeInSlots]. */
        private const val BEST_SLOT = 1
        private const val DIVIDER_ALPHA = 0x40
    }
}
