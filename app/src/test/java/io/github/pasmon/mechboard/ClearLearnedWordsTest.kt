package io.github.pasmon.mechboard

import android.content.DialogInterface
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
class ClearLearnedWordsTest {

    private lateinit var pairs: LearnedWordPairs
    private lateinit var fragment: SettingsFragment

    @Before
    fun setUp() {
        pairs = LearnedWordPairs.of(RuntimeEnvironment.getApplication())
        pairs.clear()
        pairs.learn("english", "thank", "you")
        val activity = Robolectric.buildActivity(SettingsActivity::class.java).setup().get()
        fragment = activity.supportFragmentManager.findFragmentById(R.id.settings_container) as SettingsFragment
    }

    private fun tapClear(): AlertDialog {
        fragment.findPreference<Preference>(PrefsKeys.CLEAR_LEARNED_WORDS)!!.performClick()
        shadowOf(Looper.getMainLooper()).idle()
        return ShadowDialog.getLatestDialog() as AlertDialog
    }

    @Test
    fun `confirming clears the learned words`() {
        tapClear().getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, pairs.size)
    }

    @Test
    fun `cancelling keeps them`() {
        tapClear().getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf("you"), pairs.after("english", "thank", 3))
    }
}
