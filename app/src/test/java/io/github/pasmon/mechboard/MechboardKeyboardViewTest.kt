package io.github.pasmon.mechboard

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.os.SystemClock
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class MechboardKeyboardViewTest {

    private lateinit var view: MechboardKeyboardView
    private lateinit var geometry: KeyboardGeometry

    /** Everything the view reported, e.g. `press q`, `key q`, `repeat ⌫`. */
    private val events = mutableListOf<String>()

    private val longPressMs = ViewConfiguration.getLongPressTimeout().toLong()

    @Before
    fun setUp() {
        val context = ContextThemeWrapper(
            RuntimeEnvironment.getApplication(), R.style.Theme_Mechboard_Keyboard_Dark
        )
        view = MechboardKeyboardView(context)
        view.definition = KeyboardDefinitions.ENGLISH
        view.listener = object : MechboardKeyboardView.Listener {
            override fun onPress(code: Int) {
                events += "press ${name(code)}"
            }

            override fun onKey(code: Int, isRepeat: Boolean) {
                events += "${if (isRepeat) "repeat" else "key"} ${name(code)}"
            }
        }
        layout(WIDTH)
    }

    private fun layout(width: Int) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        geometry = KeyboardGeometry(view.definition, width, view.measuredHeight / view.definition.rows.size)
    }

    private fun name(code: Int): String = when (code) {
        KeyCodes.BACKSPACE -> "⌫"
        KeyCodes.SHIFT -> "⇧"
        KeyCodes.SPACE -> "space"
        else -> String(Character.toChars(code))
    }

    private fun center(code: Int): Pair<Float, Float> =
        geometry.keys.first { it.key.code == code }.let { it.centerX to it.centerY }

    private fun center(char: Char) = center(char.code)

    private fun touch(action: Int, at: Pair<Float, Float>) {
        val now = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(now, now, action, at.first, at.second, 0)
        view.dispatchTouchEvent(event)
        event.recycle()
    }

    /** Sends an event with two pointers; [actionIndex] is the pointer going down or up. */
    private fun twoFingerTouch(
        actionMasked: Int, actionIndex: Int, first: Pair<Float, Float>, second: Pair<Float, Float>
    ) {
        val properties = Array(2) { MotionEvent.PointerProperties().apply { id = it } }
        val coords = arrayOf(first, second).map { (x, y) ->
            MotionEvent.PointerCoords().apply { this.x = x; this.y = y }
        }.toTypedArray()
        val now = SystemClock.uptimeMillis()
        val action = actionMasked or (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        val event = MotionEvent.obtain(now, now, action, 2, properties, coords, 0, 0, 1f, 1f, 0, 0, 0, 0)
        view.dispatchTouchEvent(event)
        event.recycle()
    }

    private fun down(char: Char) = touch(MotionEvent.ACTION_DOWN, center(char))
    private fun up(char: Char) = touch(MotionEvent.ACTION_UP, center(char))
    private fun advance(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(ms, TimeUnit.MILLISECONDS)

    // ── measuring ────────────────────────────────────────────────────────────

    @Test
    fun `height is one key height per row`() {
        val keyHeight = view.resources.getDimensionPixelSize(R.dimen.key_height)
        assertEquals(4 * keyHeight, view.measuredHeight)
    }

    @Test
    fun `padding adds to the height`() {
        view.setPadding(0, 10, 0, 20)
        val keyHeight = view.resources.getDimensionPixelSize(R.dimen.key_height)
        layout(WIDTH)
        assertEquals(4 * keyHeight + 30, view.measuredHeight)
    }

    // ── taps ─────────────────────────────────────────────────────────────────

    @Test
    fun `key is pressed when the finger lands and typed when it lifts`() {
        down('q')
        assertEquals(listOf("press q"), events)
        up('q')
        assertEquals(listOf("press q", "key q"), events)
    }

    @Test
    fun `sliding to another key types that key without a second press`() {
        down('q')
        touch(MotionEvent.ACTION_MOVE, center('w'))
        up('w')
        assertEquals(listOf("press q", "key w"), events)
    }

    @Test
    fun `cancelled touch types nothing`() {
        down('q')
        touch(MotionEvent.ACTION_CANCEL, center('q'))
        assertEquals(listOf("press q"), events)
    }

    @Test
    fun `changing the keyboard abandons the touch in progress`() {
        down('q')
        view.definition = KeyboardDefinitions.SYMBOLS
        layout(WIDTH)
        touch(MotionEvent.ACTION_UP, center('1'))
        assertEquals(listOf("press q"), events)
    }

    @Test
    fun `touch respects padding`() {
        view.setPadding(0, 40, 0, 0)
        layout(WIDTH)
        val (x, y) = center('a')
        touch(MotionEvent.ACTION_DOWN, x to y + 40)
        touch(MotionEvent.ACTION_UP, x to y + 40)
        assertEquals(listOf("press a", "key a"), events)
    }

    // ── two fingers ──────────────────────────────────────────────────────────

    @Test
    fun `second finger landing types the first key at once`() {
        down('t')
        twoFingerTouch(MotionEvent.ACTION_POINTER_DOWN, 1, center('t'), center('h'))
        assertEquals(listOf("press t", "key t", "press h"), events)

        // First finger lifts: it no longer holds a key.
        twoFingerTouch(MotionEvent.ACTION_POINTER_UP, 0, center('t'), center('h'))
        assertEquals(listOf("press t", "key t", "press h"), events)

        // Second finger (the only one left, still pointer id 1) lifts.
        val now = SystemClock.uptimeMillis()
        val properties = arrayOf(MotionEvent.PointerProperties().apply { id = 1 })
        val coords = arrayOf(MotionEvent.PointerCoords().apply { x = center('h').first; y = center('h').second })
        val lift = MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, 1, properties, coords, 0, 0, 1f, 1f, 0, 0, 0, 0)
        view.dispatchTouchEvent(lift)
        lift.recycle()
        assertEquals(listOf("press t", "key t", "press h", "key h"), events)
    }

    // ── repeat ───────────────────────────────────────────────────────────────

    @Test
    fun `repeatable key is typed on press and not again on release`() {
        touch(MotionEvent.ACTION_DOWN, center(KeyCodes.BACKSPACE))
        touch(MotionEvent.ACTION_UP, center(KeyCodes.BACKSPACE))
        assertEquals(listOf("press ⌫", "key ⌫"), events)
    }

    @Test
    fun `held repeatable key repeats after a delay until released`() {
        touch(MotionEvent.ACTION_DOWN, center(KeyCodes.BACKSPACE))
        advance(MechboardKeyboardView.REPEAT_START_DELAY_MS - 1)
        assertEquals(listOf("press ⌫", "key ⌫"), events)

        advance(1)
        assertEquals(listOf("press ⌫", "key ⌫", "repeat ⌫"), events)

        advance(2 * MechboardKeyboardView.REPEAT_INTERVAL_MS)
        assertEquals(listOf("press ⌫", "key ⌫", "repeat ⌫", "repeat ⌫", "repeat ⌫"), events)

        touch(MotionEvent.ACTION_UP, center(KeyCodes.BACKSPACE))
        advance(1000)
        assertEquals(5, events.size)
    }

    @Test
    fun `repeat stops when the keyboard is hidden`() {
        touch(MotionEvent.ACTION_DOWN, center(KeyCodes.BACKSPACE))
        view.dispatchWindowVisibilityChanged(View.GONE)
        advance(1000)
        assertEquals(listOf("press ⌫", "key ⌫"), events)
    }

    @Test
    fun `ordinary key does not repeat`() {
        down('a')
        advance(2000)
        assertEquals(listOf("press a"), events)
    }

    // ── long press ───────────────────────────────────────────────────────────

    @Test
    fun `holding a top-row key types its digit`() {
        down('q')
        advance(longPressMs)
        up('q')
        assertEquals(listOf("press q", "key 1"), events)
    }

    @Test
    fun `releasing before the long-press delay types the letter`() {
        down('q')
        advance(longPressMs - 1)
        up('q')
        assertEquals(listOf("press q", "key q"), events)
    }

    @Test
    fun `sliding to a new key restarts its long-press delay`() {
        down('q')
        advance(longPressMs - 1)
        touch(MotionEvent.ACTION_MOVE, center('w'))
        advance(longPressMs - 1)
        up('w')
        assertEquals(listOf("press q", "key w"), events)
    }

    @Test
    fun `sliding away after a long press types the new key's letter`() {
        down('q')
        advance(longPressMs)
        touch(MotionEvent.ACTION_MOVE, center('a'))
        up('a')
        assertEquals(listOf("press q", "key a"), events)
    }

    @Test
    fun `holding a key without a long-press character types it normally`() {
        down('a')
        advance(longPressMs * 3)
        up('a')
        assertEquals(listOf("press a", "key a"), events)
    }

    // ── drawing ──────────────────────────────────────────────────────────────

    @Test
    fun `draws every keyboard in every shift state`() {
        val definitions = KeyboardLayout.values().map { it.definition } + KeyboardDefinitions.SYMBOLS
        for (definition in definitions) {
            for (state in ShiftState.values()) {
                view.definition = definition
                view.shiftState = state
                layout(WIDTH)
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                view.draw(Canvas(bitmap))
            }
        }
    }

    // ── accessibility ────────────────────────────────────────────────────────

    private fun nodeFor(code: Int): AccessibilityNodeInfoCompat {
        val index = geometry.keys.indexOfFirst { it.key.code == code }
        val provider = ViewCompat.getAccessibilityDelegate(view)!!.getAccessibilityNodeProvider(view)
        return provider!!.createAccessibilityNodeInfo(index)!!
    }

    @Test
    fun `keys are announced by what they do`() {
        assertEquals("q", nodeFor('q'.code).contentDescription.toString())
        assertEquals("Space", nodeFor(KeyCodes.SPACE).contentDescription.toString())
        assertEquals("Delete", nodeFor(KeyCodes.BACKSPACE).contentDescription.toString())
        assertEquals("Enter", nodeFor(KeyCodes.ENTER).contentDescription.toString())
        assertEquals("Symbols", nodeFor(KeyCodes.SYMBOLS).contentDescription.toString())
        assertEquals("Shift", nodeFor(KeyCodes.SHIFT).contentDescription.toString())
    }

    @Test
    fun `announcements follow the shift state`() {
        view.shiftState = ShiftState.LOCKED
        assertEquals("Q", nodeFor('q'.code).contentDescription.toString())
        assertEquals("Caps lock on", nodeFor(KeyCodes.SHIFT).contentDescription.toString())
    }

    @Test
    fun `symbols keyboard announces the way back to letters`() {
        view.definition = KeyboardDefinitions.SYMBOLS
        layout(WIDTH)
        assertEquals("Letters", nodeFor(KeyCodes.SYMBOLS).contentDescription.toString())
        assertEquals("Keyboard settings", nodeFor(KeyCodes.SETTINGS).contentDescription.toString())
    }

    @Test
    fun `clicking a key through accessibility presses and types it`() {
        val index = geometry.keys.indexOfFirst { it.key.code == 'g'.code }
        val provider = ViewCompat.getAccessibilityDelegate(view)!!.getAccessibilityNodeProvider(view)!!
        assertTrue(provider.performAction(index, AccessibilityNodeInfoCompat.ACTION_CLICK, null))
        assertEquals(listOf("press g", "key g"), events)
    }

    private companion object {
        const val WIDTH = 1000
    }
}
