package io.github.pasmon.mechboard

import androidx.test.espresso.Espresso.onView
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for [SettingsActivity].
 *
 * Verifies that each preference row (sound toggle, sound profile chooser,
 * volume slider, keyboard-layout picker, keyboard-theme picker and personal
 * dictionary) is shown. The list does not fit on every screen, so each test
 * scrolls to its row first.
 *
 * Run with: ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class SettingsActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(SettingsActivity::class.java)

    /** Scrolls the settings list so that the preference with [key] is on screen. */
    private fun scrollTo(key: String) {
        activityRule.scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager
                .findFragmentById(R.id.settings_container) as SettingsFragment
            fragment.scrollToPreference(key)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    @Test
    fun soundTogglePreferenceIsDisplayed() {
        scrollTo(PrefsKeys.SOUND_ENABLED)
        onView(withText(R.string.pref_sound_enabled_title))
            .check(matches(isDisplayed()))
    }

    @Test
    fun soundProfilePreferenceIsDisplayed() {
        scrollTo(PrefsKeys.SOUND_PROFILE)
        onView(withText(R.string.pref_sound_profile_title))
            .check(matches(isDisplayed()))
    }

    @Test
    fun volumePreferenceIsDisplayed() {
        scrollTo(PrefsKeys.VOLUME)
        onView(withText(R.string.pref_volume_title))
            .check(matches(isDisplayed()))
    }

    @Test
    fun keyboardLayoutPreferenceIsDisplayed() {
        scrollTo(PrefsKeys.KEYBOARD_LAYOUT)
        onView(withText(R.string.pref_keyboard_layout_title))
            .check(matches(isDisplayed()))
    }

    @Test
    fun keyboardThemePreferenceIsDisplayed() {
        scrollTo(PrefsKeys.KEYBOARD_THEME)
        onView(withText(R.string.pref_keyboard_theme_title))
            .check(matches(isDisplayed()))
    }

    @Test
    fun personalDictionaryPreferenceIsDisplayed() {
        scrollTo(PrefsKeys.PERSONAL_DICTIONARY)
        onView(withText(R.string.pref_personal_dictionary_title))
            .check(matches(isDisplayed()))
    }
}
