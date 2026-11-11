package com.example.mechboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.ContextCompat
import androidx.core.graphics.withTranslation
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper

/**
 * Draws a [KeyboardDefinition] and turns touches on it into key events.
 *
 * Behaviour:
 *  - A key is *pressed* when a finger lands on it and *typed* when the finger
 *    lifts; sliding to another key before lifting types that key instead.
 *  - A second finger landing while the first is still down types the first
 *    key immediately, so fast two-thumb typing does not drop characters.
 *  - Holding a [Key.repeatable] key types it repeatedly.
 *  - Holding a key with a [Key.longPressCode] switches it to that character
 *    (shown as a small hint in the key's corner); lifting then types it.
 *
 * Colours come from the theme of the [Context] the view is inflated with
 * ([R.attr.keyLabelColor], [R.attr.keyPressedColor] and the attributes used by
 * `key_background.xml`).
 * Each key is exposed to accessibility services as its own virtual view.
 */
class MechboardKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** Receives the key events produced by touches on the view. */
    interface Listener {
        /** A finger landed on the key with [code]. Called once per touch, before any [onKey]. */
        fun onPress(code: Int)

        /**
         * The key with [code] was typed. [isRepeat] is `true` for every event after
         * the first one produced by holding a repeatable key down.
         */
        fun onKey(code: Int, isRepeat: Boolean)
    }

    var listener: Listener? = null

    /** The keys to show. Changing it abandons any touch in progress. */
    var definition: KeyboardDefinition = KeyboardDefinition(emptyList())
        set(value) {
            if (field == value) return
            field = value
            releaseKey(commit = false)
            geometry = buildGeometry(width)
            accessHelper.invalidateRoot()
            requestLayout()
            invalidate()
        }

    /** Controls whether letters are drawn in upper case and how the shift key looks. */
    var shiftState: ShiftState = ShiftState.OFF
        set(value) {
            if (field == value) return
            field = value
            accessHelper.invalidateRoot()
            invalidate()
        }

    /**
     * The typeface to use for key labels and hints.
     * Set to `null` to use the system default typeface.
     */
    var labelTypeface: Typeface? = null
        set(value) {
            field = value
            val regular = value ?: Typeface.DEFAULT
            labelPaint.typeface = regular
            hintPaint.typeface = regular
            wordPaint.typeface = Typeface.create(regular, Typeface.BOLD)
            invalidate()
        }

    private val rowHeight = resources.getDimensionPixelSize(R.dimen.key_height)
    private val hintPadding = resources.getDimension(R.dimen.key_hint_padding)
    private val keyBackground: Drawable? =
        ContextCompat.getDrawable(context, R.drawable.key_background)?.mutate()

    private val labelColor = resolveColor(context, R.attr.keyLabelColor, Color.WHITE)
    private val accentColor = resolveColor(context, R.attr.keyPressedColor, labelColor)
    private val lockBarHeight = resources.getDimension(R.dimen.key_lock_bar_height)
    private val lockBarInset = resources.getDimension(R.dimen.key_lock_bar_inset)

    /** Paint for single-character labels. */
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = resources.getDimension(R.dimen.key_text_size)
        color = labelColor
    }

    /** Paint for labels that are words ("Space", "ABC"); smaller and bold. */
    private val wordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = resources.getDimension(R.dimen.key_word_text_size)
        typeface = Typeface.DEFAULT_BOLD
        color = labelColor
    }

    /** Paint for the long-press hint in a key's top-right corner. */
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        textSize = resources.getDimension(R.dimen.key_hint_text_size)
        color = Color.argb(
            HINT_ALPHA, Color.red(labelColor), Color.green(labelColor), Color.blue(labelColor)
        )
    }

    private var geometry = buildGeometry(0)

    // ── touch state ──────────────────────────────────────────────────────────

    private val handler = Handler(Looper.getMainLooper())
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()

    /** Id of the pointer holding [pressedIndex] down. */
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    /** Index in [KeyboardGeometry.keys] of the key held down, or [KeyboardGeometry.NO_KEY]. */
    private var pressedIndex = KeyboardGeometry.NO_KEY
    /** Whether the held key has switched to its [Key.longPressCode]. */
    private var longPressActive = false
    /** Whether the held key was already typed when it was pressed (repeatable keys). */
    private var typedOnPress = false

    private val repeatRunnable = object : Runnable {
        override fun run() {
            val key = pressedKey() ?: return
            listener?.onKey(key.code, true)
            handler.postDelayed(this, REPEAT_INTERVAL_MS)
        }
    }

    private val longPressRunnable = Runnable {
        if (pressedKey()?.longPressCode != null) {
            longPressActive = true
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            invalidate()
        }
    }

    private val accessHelper = KeyAccessHelper()

    init {
        ViewCompat.setAccessibilityDelegate(this, accessHelper)
    }

    // ── layout and drawing ───────────────────────────────────────────────────

    private fun buildGeometry(viewWidth: Int) = KeyboardGeometry(
        definition, (viewWidth - paddingLeft - paddingRight).coerceAtLeast(0), rowHeight
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val height = definition.rows.size * rowHeight + paddingTop + paddingBottom
        setMeasuredDimension(width, resolveSize(height, heightMeasureSpec))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        releaseKey(commit = false)
        geometry = buildGeometry(w)
        accessHelper.invalidateRoot()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.withTranslation(paddingLeft.toFloat(), paddingTop.toFloat()) {
            drawKeys(this)
        }
    }

    private fun drawKeys(canvas: Canvas) {
        geometry.keys.forEachIndexed { index, bounds ->
            val key = bounds.key
            val held = index == pressedIndex
            keyBackground?.let { background ->
                background.state = if (held) STATE_PRESSED else STATE_NORMAL
                background.setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
                background.draw(canvas)
            }

            val showLongPress = held && longPressActive
            val label = if (showLongPress) codeToString(key.longPressCode!!) else labelFor(key)
            val paint = if (label.codePointCount(0, label.length) > 1) wordPaint else labelPaint
            val baseline = bounds.centerY - (paint.ascent() + paint.descent()) / 2f
            // An active shift key is shown in the accent colour, unless the key is
            // held, when its whole face already is that colour.
            val activeShift = key.code == KeyCodes.SHIFT && shiftState.isUppercase && !held
            paint.color = if (activeShift) accentColor else labelColor
            canvas.drawText(label, bounds.centerX, baseline, paint)
            if (activeShift && shiftState == ShiftState.LOCKED) {
                // Caps lock: a bar under the arrow, so it differs from one-letter shift
                // even in fonts that have no caps-lock glyph.
                val halfWidth = (bounds.right - bounds.left) * LOCK_BAR_WIDTH_FRACTION / 2f
                val barTop = bounds.bottom - lockBarInset - lockBarHeight
                canvas.drawRect(
                    bounds.centerX - halfWidth, barTop,
                    bounds.centerX + halfWidth, barTop + lockBarHeight,
                    paint
                )
            }
            paint.color = labelColor

            val hint = key.longPressCode
            if (hint != null && !showLongPress) {
                canvas.drawText(
                    codeToString(hint),
                    bounds.right - hintPadding,
                    bounds.top + hintPadding - hintPaint.fontMetrics.top,
                    hintPaint
                )
            }
        }
    }

    /** The text drawn on [key] in the current [shiftState]. */
    private fun labelFor(key: Key): String = when {
        key.code == KeyCodes.SHIFT && shiftState == ShiftState.LOCKED -> LABEL_CAPS_LOCK
        key.code > 0 && shiftState.isUppercase -> codeToString(Character.toUpperCase(key.code))
            .takeIf { key.label == codeToString(key.code) } ?: key.label
        else -> key.label
    }

    // ── touch handling ───────────────────────────────────────────────────────

    // Keys are not clicked through performClick(): a touch can type several keys,
    // and accessibility services click individual keys through KeyAccessHelper.
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // A new finger types whatever the previous one is still holding.
                releaseKey(commit = true)
                val pointer = event.actionIndex
                activePointerId = event.getPointerId(pointer)
                pressKey(indexOfKeyAt(event.getX(pointer), event.getY(pointer)))
            }
            MotionEvent.ACTION_MOVE -> {
                val pointer = event.findPointerIndex(activePointerId)
                if (pointer >= 0 && pressedIndex != KeyboardGeometry.NO_KEY) {
                    slideTo(indexOfKeyAt(event.getX(pointer), event.getY(pointer)))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (event.getPointerId(event.actionIndex) == activePointerId) {
                    releaseKey(commit = true)
                }
            }
            MotionEvent.ACTION_CANCEL -> releaseKey(commit = false)
        }
        return true
    }

    private fun indexOfKeyAt(x: Float, y: Float): Int =
        geometry.indexOfKeyAt(x - paddingLeft, y - paddingTop)

    private fun pressedKey(): Key? = geometry.keys.getOrNull(pressedIndex)?.key

    private fun pressKey(index: Int) {
        val key = geometry.keys.getOrNull(index)?.key ?: return
        pressedIndex = index
        listener?.onPress(key.code)
        if (key.repeatable) {
            typedOnPress = true
            listener?.onKey(key.code, false)
            handler.postDelayed(repeatRunnable, REPEAT_START_DELAY_MS)
        } else {
            scheduleLongPress(key)
        }
        invalidate()
    }

    /** Moves the press to the key at [index] without typing the key it leaves. */
    private fun slideTo(index: Int) {
        if (index == pressedIndex || index == KeyboardGeometry.NO_KEY) return
        cancelTimers()
        pressedIndex = index
        longPressActive = false
        typedOnPress = false
        pressedKey()?.let(::scheduleLongPress)
        invalidate()
    }

    /** Ends the current press, typing the held key if [commit] is set. */
    private fun releaseKey(commit: Boolean) {
        cancelTimers()
        val key = pressedKey()
        val longPress = longPressActive
        val alreadyTyped = typedOnPress
        pressedIndex = KeyboardGeometry.NO_KEY
        activePointerId = MotionEvent.INVALID_POINTER_ID
        longPressActive = false
        typedOnPress = false
        if (key == null) return
        invalidate()
        if (!commit) return
        when {
            longPress && key.longPressCode != null -> listener?.onKey(key.longPressCode, false)
            !alreadyTyped -> listener?.onKey(key.code, false)
        }
    }

    private fun scheduleLongPress(key: Key) {
        if (key.longPressCode != null) handler.postDelayed(longPressRunnable, longPressTimeout)
    }

    private fun cancelTimers() {
        handler.removeCallbacks(repeatRunnable)
        handler.removeCallbacks(longPressRunnable)
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        // Do not keep repeating a held key once the keyboard is hidden.
        if (visibility != VISIBLE) releaseKey(commit = false)
    }

    override fun onDetachedFromWindow() {
        releaseKey(commit = false)
        super.onDetachedFromWindow()
    }

    // ── accessibility ────────────────────────────────────────────────────────

    override fun dispatchHoverEvent(event: MotionEvent): Boolean =
        accessHelper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)

    /** What a screen reader says for [key]. */
    private fun descriptionFor(key: Key): CharSequence = when (key.code) {
        KeyCodes.SHIFT -> context.getString(
            when (shiftState) {
                ShiftState.OFF    -> R.string.key_description_shift
                ShiftState.ON     -> R.string.key_description_shift_on
                ShiftState.LOCKED -> R.string.key_description_caps_lock
            }
        )
        KeyCodes.BACKSPACE -> context.getString(R.string.key_description_backspace)
        KeyCodes.ENTER     -> context.getString(R.string.key_description_enter)
        KeyCodes.SPACE     -> context.getString(R.string.key_description_space)
        KeyCodes.SETTINGS  -> context.getString(R.string.key_description_settings)
        KeyCodes.SYMBOLS   -> context.getString(
            if (definition == KeyboardDefinitions.SYMBOLS) R.string.key_description_letters
            else R.string.key_description_symbols
        )
        else -> labelFor(key)
    }

    /** Exposes every key as a virtual view that can be explored by touch and clicked. */
    private inner class KeyAccessHelper : ExploreByTouchHelper(this) {

        override fun getVirtualViewAt(x: Float, y: Float): Int =
            indexOfKeyAt(x, y).takeIf { it != KeyboardGeometry.NO_KEY } ?: INVALID_ID

        override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
            virtualViewIds.addAll(geometry.keys.indices)
        }

        override fun onPopulateNodeForVirtualView(
            virtualViewId: Int,
            node: AccessibilityNodeInfoCompat
        ) {
            val bounds = geometry.keys.getOrNull(virtualViewId)
            if (bounds == null) {
                // The key vanished between the query and now; report an empty node.
                node.contentDescription = ""
                node.setBoundsInParent(Rect(0, 0, 1, 1))
                return
            }
            node.contentDescription = descriptionFor(bounds.key)
            node.className = BUTTON_CLASS_NAME
            node.setBoundsInParent(
                Rect(
                    bounds.left + paddingLeft, bounds.top + paddingTop,
                    bounds.right + paddingLeft, bounds.bottom + paddingTop
                )
            )
            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
        }

        override fun onPerformActionForVirtualView(
            virtualViewId: Int,
            action: Int,
            arguments: Bundle?
        ): Boolean {
            if (action != AccessibilityNodeInfoCompat.ACTION_CLICK) return false
            val key = geometry.keys.getOrNull(virtualViewId)?.key ?: return false
            listener?.onPress(key.code)
            listener?.onKey(key.code, false)
            return true
        }
    }

    companion object {
        /** Delay before a held repeatable key starts repeating. */
        const val REPEAT_START_DELAY_MS = 400L
        /** Time between repeats of a held repeatable key. */
        const val REPEAT_INTERVAL_MS = 50L

        /** ~55 % opacity (140 / 255 ≈ 0.549). */
        private const val HINT_ALPHA = 140
        private const val LABEL_CAPS_LOCK = "⇪"
        /** Width of the caps-lock bar as a fraction of the shift key's width. */
        private const val LOCK_BAR_WIDTH_FRACTION = 0.3f
        private const val BUTTON_CLASS_NAME = "android.widget.Button"

        private val STATE_PRESSED = intArrayOf(android.R.attr.state_pressed)
        private val STATE_NORMAL = intArrayOf()

        private fun codeToString(code: Int): String = String(Character.toChars(code))

        /** Resolves colour attribute [attr] from the context's theme, or [fallback] if absent. */
        private fun resolveColor(context: Context, attr: Int, fallback: Int): Int {
            val typedValue = TypedValue()
            return if (context.theme.resolveAttribute(attr, typedValue, true)) {
                typedValue.data
            } else {
                fallback
            }
        }
    }
}
