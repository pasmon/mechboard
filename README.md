# Mechboard

[![CI](https://github.com/pasmon/mechboard/actions/workflows/ci.yml/badge.svg)](https://github.com/pasmon/mechboard/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/pasmon/mechboard?include_prereleases)](https://github.com/pasmon/mechboard/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Min SDK](https://img.shields.io/badge/Android-API%2021%2B-brightgreen?logo=android)](https://developer.android.com/about/versions/lollipop)
[![Language: Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)

A QWERTY soft keyboard for Android with **authentic mechanical keyboard sound effects**. Pick from nine sound profiles, toggle sounds on/off, and fine-tune the volume — all from a built-in settings screen.

---

## Features

- 🎹 **Nine sound profiles**
  | Profile | Switch type |
  |---|---|
  | Cherry MX Blue | Clicky |
  | Cherry MX Red | Linear |
  | Cherry MX Brown | Tactile |
  | Topre | Thocky |
  | Alps | Vintage |
  | NK Cream | Smooth Linear |
  | Holy Panda | Tactile Thock |
  | Typewriter | Classic typewriter |
  | Silent | No sound |
- ⌨️ **Sounds like typing, not like a loop** — the click plays the moment your finger lands, every keystroke varies slightly in pitch and loudness, and the Cherry MX and Topre profiles have their own space bar, enter and backspace sounds
- 🔊 **Master on/off toggle** — silence all sounds instantly
- 🔉 **Volume slider** (0 – 100 %) independent of system volume
- ⚙️ **In-keyboard settings key** — opens the settings screen without leaving the current app
- 🔠 **Shift and caps lock** — tap ⇧ for one capital letter, tap it again for caps lock; sentences start with a capital automatically in fields that ask for it
- 🔢 **Digits without switching keyboards** — hold a top-row key for its digit; number fields open on the digits keyboard
- 🌍 **Five languages** — English, Finnish, German, French and Spanish layouts
- 🎨 **Six colour themes** (dark by default) and **three key fonts**
- ♿ **Screen-reader support** — every key is announced and can be activated with TalkBack
- Supports **all standard IME actions** (Search, Send, Next, Done) and multiline fields

---

## Requirements

| | |
|---|---|
| **Min Android version** | Android 5.0 (API 21) |
| **Target SDK** | 36 |
| **Language** | Kotlin |

---

## Installation

### Build from source

1. Clone the repository:
   ```bash
   git clone https://github.com/pasmon/mechboard.git
   ```
2. Open the project in **Android Studio** (a recent version; the project uses Android Gradle Plugin 8.13).
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
| Sound profile | Choose one of the nine profiles listed above | Cherry MX Blue |
| Volume | Key-click volume independent of system volume (0 – 100) | 80 |
| Keyboard language | English, Finnish, German, French or Spanish | English |
| Auto-capitalisation | Start sentences with a capital letter, in text fields that ask for it | On |
| Keyboard theme | Dark, Solarized Dark, Solarized Light, Dracula, Nord or Monokai | Dark |
| Key font | Default, JetBrains Mono or Share Tech Mono | Default |

---

## Architecture

| Class | Role |
|---|---|
| `MechboardService` | Main `InputMethodService`; inflates the keyboard view, picks the keyboard to show, and routes key events to sound playback and `TypingController` |
| `MechboardKeyboardView` | Custom `View` that draws a keyboard and turns touches into key events: multi-touch, key repeat, long-press characters, and per-key accessibility nodes |
| `KeyboardDefinitions` | The key arrangement of every language layout and of the symbols keyboard, as plain Kotlin data (`KeyboardDefinition`, `Key`, `KeyCodes`) |
| `KeyboardGeometry` | Places a definition's keys in pixels and finds the key under a touch |
| `TypingController` | Turns key codes into edits of the text field: characters, backspace, enter, shift / caps lock (`ShiftState`) and auto-capitalisation |
| `SoundManager` | Loads short sound effects (WAV/MP3/OGG) per profile, picks the sample for each key, reads/writes preferences, exposes `playKeySound(kind)` |
| `SamplePlayer` | Thin interface over `SoundPool` (`SoundPoolSamplePlayer`) so `SoundManager` can be unit-tested with a fake |
| `KeyKind` | Enum grouping key codes by how the key sounds: character, space, enter, backspace |
| `SoundVariation` | Small random pitch (±4 %) and volume (up to −10 %) change applied to every keystroke |
| `SoundProfile` | Enum of the nine profiles; each carries an `id`, `displayName`, and optional `rawFileName` |
| `SettingsActivity` | `AppCompatActivity` that hosts `SettingsFragment` |
| `SettingsFragment` | `PreferenceFragmentCompat`; renders the settings listed above |
| `PrefsKeys` | Plain object of `SharedPreferences` key constants (no Android dependency — keeps unit tests simple) |

All user preferences are stored in a `SharedPreferences` file named `mechboard_prefs`.

---

## Development

### Running tests

```bash
./gradlew test
```

Unit tests live in `app/src/test/` and use **JUnit 4 + Mockito**. Most are plain
JVM tests of classes with no Android dependency. Tests of code that needs Android
classes (the keyboard view, `TypingController`, window insets) run on the JVM too,
with **Robolectric** (`@RunWith(RobolectricTestRunner::class)`).

### Adding or changing a keyboard layout

Layouts are Kotlin data in `KeyboardDefinitions.kt`. To add a language, define
its rows there and add an entry to the `KeyboardLayout` enum; the settings
screen lists all entries automatically. `KeyboardDefinitionsTest` checks every
layout (row widths, required keys, duplicate codes).

### Releasing

Push a tag of the form `v1.2.3`. The release workflow builds a signed APK and AAB
with `versionName` `1.2.3` and `versionCode` `10203` (major × 10000 + minor × 100
+ patch), so minor and patch must stay below 100. A local build without
`-PversionName=…` is version `0.0.0-dev`.

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

### Adding a per-key sound to a profile

A profile can have its own sound for the space bar, enter and backspace. Place a
file named `<rawFileName>_space`, `<rawFileName>_enter` or
`<rawFileName>_backspace` in `app/src/main/res/raw/` (e.g. `my_switch_space.ogg`);
no code change is needed. Keys without a dedicated file use the profile's
generic sample. Trim leading silence from the file, otherwise the sound lags the
key press.

---

## License

This project is licensed under the [MIT License](LICENSE).

Audio assets bundled with the app come from open-source projects and are used
in accordance with their respective licenses. See [CREDITS.md](CREDITS.md) for
a full breakdown of every audio file's origin and license.
