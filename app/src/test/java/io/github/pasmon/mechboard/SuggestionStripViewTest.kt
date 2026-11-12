package io.github.pasmon.mechboard

import android.view.ContextThemeWrapper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SuggestionStripViewTest {

    private lateinit var strip: SuggestionStripView
    private val events = mutableListOf<String>()

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

            override fun onSuggestionPicked(word: String) {
                events += "pick $word"
            }
        }
    }

    @Test
    fun `suggestions are shown with the best in the middle`() {
        strip.suggestions = listOf("the", "they", "there")
        assertEquals(listOf("they", "the", "there"), strip.slotTexts)
    }

    @Test
    fun `tapping a suggestion reports its word`() {
        strip.suggestions = listOf("the", "they", "there")
        strip.clickSlot(1)
        strip.clickSlot(2)
        assertEquals(listOf("pick the", "pick there"), events)
    }

    @Test
    fun `empty slots show nothing and cannot be tapped`() {
        strip.suggestions = listOf("the")
        assertEquals(listOf("", "the", ""), strip.slotTexts)
        strip.clickSlot(0)
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun `clearing the suggestions empties every slot`() {
        strip.suggestions = listOf("the", "they")
        strip.suggestions = emptyList()
        assertEquals(listOf("", "", ""), strip.slotTexts)
    }
}
