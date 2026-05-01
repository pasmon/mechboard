package com.example.mechboard

import androidx.test.espresso.Espresso.onView
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
 * volume slider, keyboard-layout picker, and keyboard-theme picker) is
 * visible when the settings screen is launched.
 *
 * Run with: ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class SettingsActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(SettingsActivity::class.java)

    @Test
    fun soundTogglePreferenceIsDisplayed() {
        onView(withText(R.string.pref_sound_enabled_title))
            .check(matches(isDisplayed()))
    }

    @Test
    fun soundProfilePreferenceIsDisplayed() {
        onView(withText(R.string.pref_sound_profile_title))
            .check(matches(isDisplayed()))
    }

    @Test
    fun volumePreferenceIsDisplayed() {
        onView(withText(R.string.pref_volume_title))
            .check(matches(isDisplayed()))
    }

    @Test
    fun keyboardLayoutPreferenceIsDisplayed() {
        onView(withText(R.string.pref_keyboard_layout_title))
            .check(matches(isDisplayed()))
    }

    @Test
    fun keyboardThemePreferenceIsDisplayed() {
        onView(withText(R.string.pref_keyboard_theme_title))
            .check(matches(isDisplayed()))
    }
}
