package io.github.pasmon.mechboard

import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PersonalDictionaryFragmentTest {

    private lateinit var dictionary: PersonalDictionary
    private lateinit var fragment: PersonalDictionaryFragment

    private val root: View get() = fragment.requireView()
    private val newWord: EditText get() = root.findViewById(R.id.new_word)
    private val list: ListView get() = root.findViewById(R.id.word_list)

    @Before
    fun setUp() {
        dictionary = PersonalDictionary.of(RuntimeEnvironment.getApplication())
        dictionary.words.forEach { dictionary.remove(it) }
        dictionary.add("Kotlin")
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()
        fragment = PersonalDictionaryFragment()
        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.settings_container, fragment)
            .commitNow()
    }

    /** The words the list shows, laid out as rows. */
    private fun shownWords(): List<String> {
        shadowOf(Looper.getMainLooper()).idle()
        list.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(2000, View.MeasureSpec.EXACTLY)
        )
        list.layout(0, 0, 1000, 2000)
        return (0 until list.childCount).map { list.getChildAt(it).findViewById<TextView>(R.id.word).text.toString() }
    }

    private fun type(word: String) {
        newWord.setText(word)
        root.findViewById<View>(R.id.add_word).performClick()
    }

    @Test
    fun `saved words are listed`() {
        assertEquals(listOf("Kotlin"), shownWords())
    }

    @Test
    fun `a typed word is added and the field cleared`() {
        type("Ahvenanmaa")
        assertEquals(listOf("Ahvenanmaa", "Kotlin"), dictionary.words)
        assertEquals(listOf("Ahvenanmaa", "Kotlin"), shownWords())
        assertEquals("", newWord.text.toString())
    }

    @Test
    fun `an invalid or duplicate word is refused with an explanation`() {
        type("two words")
        assertNotNull(newWord.error)
        type("kotlin")
        assertNotNull(newWord.error)
        assertEquals(listOf("Kotlin"), dictionary.words)
    }

    @Test
    fun `the delete button removes its word`() {
        shownWords()
        list.getChildAt(0).findViewById<ImageButton>(R.id.delete_word).performClick()
        assertEquals(emptyList<String>(), dictionary.words)
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.empty_message).visibility)
    }

    @Test
    fun `the empty message shows only while no words are saved`() {
        assertEquals(View.GONE, root.findViewById<View>(R.id.empty_message).visibility)
        dictionary.remove("Kotlin")
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.empty_message).visibility)
        assertEquals(View.GONE, list.visibility)
        assertNull(newWord.error)
    }
}
