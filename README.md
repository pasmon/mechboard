# Mechboard

A QWERTY soft keyboard for Android with **authentic mechanical keyboard sound effects**. Pick from six sound profiles, toggle sounds on/off, and fine-tune the volume — all from a built-in settings screen.

---

## Features

- 🎹 **Six sound profiles**
  | Profile | Switch type |
  |---|---|
  | Cherry MX Blue | Clicky |
  | Cherry MX Red | Linear |
  | Cherry MX Brown | Tactile |
  | Topre | Thocky |
  | Alps | Vintage |
  | Silent | No sound |
- 🔊 **Master on/off toggle** — silence all sounds instantly
- 🔉 **Volume slider** (0 – 100 %) independent of system volume
- ⚙️ **In-keyboard settings key** — opens the settings screen without leaving the current app
- 🌙 **Dark theme** out of the box
- Supports **all standard IME actions** (Search, Send, Next, Done) and multiline fields

---

## Requirements

| | |
|---|---|
| **Min Android version** | Android 5.0 (API 21) |
| **Target SDK** | 34 |
| **Language** | Kotlin |

---

## Installation

### Build from source

1. Clone the repository:
   ```bash
   git clone https://github.com/pasmon/mechboard.git
   ```
2. Open the project in **Android Studio** (Electric Eel or later recommended).
3. Connect a device or start an emulator running Android 5.0+.
4. Click **Run ▶** (or `./gradlew installDebug` from the command line).

### Enable the keyboard

After installing the app:

1. Go to **Settings → System → Language & input → On-screen keyboard** (path may vary by device).
2. Toggle **Mechboard** on and grant the permission when prompted.
3. Switch to Mechboard in any text field using the keyboard switcher button.

---

## Settings

Open the settings screen by tapping the **⚙ key** on the keyboard, or navigating to the Mechboard entry in *Language & input*.

| Setting | Description | Default |
|---|---|---|
| Key sounds | Master on/off toggle for all sound effects | On |
| Sound profile | Choose one of the six profiles listed above | Cherry MX Blue |
| Volume | Key-click volume independent of system volume (0 – 100) | 80 |

---

## Architecture

| Class | Role |
|---|---|
| `MechboardService` | Main `InputMethodService`; inflates the keyboard view, routes key events, manages caps-lock state, and triggers sound playback |
| `SoundManager` | Wraps `SoundPool`; loads WAV assets per profile, reads/writes preferences, exposes `playKeySound()` |
| `SoundProfile` | Enum of the six profiles; each carries an `id`, `displayName`, and optional `rawFileName` |
| `SettingsActivity` | `AppCompatActivity` that hosts `SettingsFragment` |
| `SettingsFragment` | `PreferenceFragmentCompat`; renders profile picker, master toggle, and volume slider |
| `PrefsKeys` | Plain object of `SharedPreferences` key constants (no Android dependency — keeps unit tests simple) |

All user preferences are stored in a `SharedPreferences` file named `mechboard_prefs`.

---

## Development

### Running tests

```bash
./gradlew test
```

Unit tests live in `app/src/test/` and use **JUnit 4 + Mockito**. They have no dependency on the Android SDK.

### Adding a new sound profile

1. Place a WAV file in `app/src/main/res/raw/` (e.g. `my_switch.wav`).
2. Add a new entry to the `SoundProfile` enum:
   ```kotlin
   MY_SWITCH(
       id = "my_switch",
       displayName = "My Switch (Description)",
       rawFileName = "my_switch"
   )
   ```
3. That's it — `SoundManager` iterates all profiles dynamically.

---

## License

This project does not currently include a license file. All rights reserved by the author unless stated otherwise.