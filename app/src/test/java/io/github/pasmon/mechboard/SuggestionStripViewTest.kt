package io.github.pasmon.mechboard

import android.view.ContextThemeWrapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SuggestionStripViewTest {

    private lateinit var strip: SuggestionStripView
    private val events = mutableListOf<String>()
    /** What the listener answers to a long press. */
    private var useLongPress = true

    private fun suggestion(word: String) = StripItem(word, StripItem.Kind.SUGGESTION)

    @Before
    fun setUp() {
        val context = ContextThemeWrapper(
            RuntimeEnvironment.getApplication(), R.style.Theme_Mechboard_Keyboard_Dark
        )
        strip = SuggestionStripView(context)
        strip.listener = object : SuggestionStripView.Listener {
            override fun onSuggestionPress() {
                events += "press"
            }

            override fun onSuggestionPicked(item: StripItem) {
                events += "pick ${item.kind} ${item.word}"
            }

            override fun onSuggestionLongPressed(item: StripItem): Boolean {
                events += "hold ${item.word}"
                return useLongPress
            }
        }
    }

    @Test
    fun `slots show their items left to right`() {
        strip.items = listOf(suggestion("they"), suggestion("the"), suggestion("there"))
        assertEquals(listOf("they", "the", "there"), strip.slotTexts)
    }

    @Test
    fun `a word as typed is quoted and an offer to save it has a plus`() {
        strip.items = listOf(StripItem("Pasi", StripItem.Kind.TYPED_WORD), suggestion("Past"), null)
        assertEquals(listOf("“Pasi”", "Past", ""), strip.slotTexts)
        strip.items = WordSuggestions.saveOfferFor("Pasi")
        assertEquals(listOf("", "+ Pasi", ""), strip.slotTexts)
    }

    @Test
    fun `tapping a slot reports its item`() {
        strip.items = listOf(StripItem("Pasi", StripItem.Kind.TYPED_WORD), suggestion("the"), null)
        strip.clickSlot(0)
        strip.clickSlot(1)
        assertEquals(listOf("pick TYPED_WORD Pasi", "pick SUGGESTION the"), events)
    }

    @Test
    fun `empty slots show nothing and cannot be tapped or held`() {
        strip.items = listOf(null, suggestion("the"), null)
        assertEquals(listOf("", "the", ""), strip.slotTexts)
        strip.clickSlot(0)
        assertFalse(strip.longClickSlot(2))
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun `holding a slot reports its item`() {
        strip.items = listOf(null, suggestion("the"), null)
        assertTrue(strip.longClickSlot(1))
        assertEquals(listOf("hold the"), events)
    }

    @Test
    fun `a long press the listener does not use is not consumed`() {
        useLongPress = false
        strip.items = listOf(null, suggestion("the"), null)
        assertFalse(strip.longClickSlot(1))
    }

    @Test
    fun `clearing the items empties every slot`() {
        strip.items = listOf(suggestion("they"), suggestion("the"), null)
        strip.items = SuggestionStripView.EMPTY
        assertEquals(listOf("", "", ""), strip.slotTexts)
    }

    @Test
    fun `the correction a space will make is underlined`() {
        strip.items = listOf(StripItem("teh", StripItem.Kind.TYPED_WORD), StripItem("the", StripItem.Kind.AUTOCORRECTION), null)
        assertEquals(listOf("“teh”", "the", ""), strip.slotTexts)
        assertEquals(listOf(false, true, false), strip.slotUnderlines)
        strip.items = listOf(null, suggestion("the"), null)
        assertEquals(listOf(false, false, false), strip.slotUnderlines)
    }
}
