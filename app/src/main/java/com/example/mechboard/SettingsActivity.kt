package com.example.mechboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Hosts [SettingsFragment] and acts as the entry point for the keyboard
 * settings screen that users reach via the dedicated settings key or the
 * system's "Language & input" settings page.
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }
}
