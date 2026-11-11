# Copilot Instructions for Mechboard

## Project Overview

**Mechboard** is an Android Input Method Editor (IME / soft keyboard) written in Kotlin. It provides a QWERTY keyboard in five languages with authentic mechanical keyboard sound effects. Users can pick from nine sound profiles, toggle sounds, adjust volume, and choose a language, colour theme and key font via a built-in settings screen.

## Architecture

| Class | Role |
|---|---|
| `MechboardService` | Main `InputMethodService`; inflates the keyboard view, picks the keyboard to show, and routes key events to sound playback and `TypingController` |
| `MechboardKeyboardView` | Custom `View` that draws a keyboard and turns touches into key events (multi-touch, key repeat, long-press characters, accessibility nodes) |
| `KeyboardDefinitions` | Key arrangements of every language layout and the symbols keyboard as plain Kotlin data (`KeyboardDefinition`, `Key`, `KeyCodes`) |
| `KeyboardGeometry` | Places a definition's keys in pixels and finds the key under a touch |
| `KeyboardLayout`, `KeyboardTheme`, `KeyboardFont` | Enums behind the language, theme and font settings |
| `TypingController` | Turns key codes into edits of the text field: characters, backspace, enter, shift / caps lock (`ShiftState`), auto-capitalisation |
| `SoundManager` | Loads sound samples per profile, picks the sample for each key (`KeyKind`), applies `SoundVariation`, reads/writes preferences, exposes `playKeySound(kind)` |
| `SamplePlayer` | Thin interface over `SoundPool` so `SoundManager` can be unit-tested with a fake |
| `SoundProfile` | Enum of nine profiles; each carries an `id`, `displayName`, and optional `rawFileName` |
| `SettingsActivity` | `AppCompatActivity` that hosts `SettingsFragment` |
| `SettingsFragment` | `PreferenceFragmentCompat`; renders the settings |
| `PrefsKeys` | Plain object of SharedPreferences key constants (no Android dependency — keeps unit tests simple) |

## Key Conventions

- **Package**: `com.example.mechboard`
- **Language**: Kotlin (JVM target 11)
- **Min SDK**: 21 · **Target/Compile SDK**: 36
- **Version**: `versionName` comes from `-PversionName` (set from the git tag by the release workflow); `versionCode` is derived from it in `app/build.gradle`.
- **Preferences**: All user settings live in `SharedPreferences` keyed by constants in `PrefsKeys`. The preference name is `MechboardService.PREFS_NAME = "mechboard_prefs"`.
- **Key codes**: Character keys use their Unicode code point; special keys use the negative constants in `KeyCodes`.
- **Keyboard layouts**: Defined in Kotlin (`KeyboardDefinitions.kt`), not XML. Widths are percentages of the keyboard width.
- **Sound loading**: `SoundManager` loads every non-silent profile at construction time. `playKeySound()` is a no-op when sound is disabled or the sample is not yet loaded. Sound plays on key press (`onPress`), not release.
- **Edge-to-edge**: Windows draw behind the system bars (target SDK 35+). The keyboard pads itself by the navigation-bar inset; the settings screen relies on AppCompat's action bar handling.
- **No deprecated keyboard API**: `android.inputmethodservice.KeyboardView` / `Keyboard` are not used.

## Resource Layout

```
res/
  layout/
    input.xml           # Keyboard host layout (contains MechboardKeyboardView)
    activity_settings.xml
  xml/
    method.xml          # IME metadata
    preferences.xml     # Preference screen definition
  raw/                  # Sound samples: <profile>.{mp3,wav} and <profile>_{space,enter,backspace}.ogg
  font/                 # Bundled key fonts
  values/
    attrs.xml           # Theme attributes for keyboard colours
    colors.xml          # Theme palettes
    dimens.xml          # Key height and text sizes
    strings.xml
    themes.xml          # Keyboard and settings themes
  drawable/
    key_background.xml  # State-list drawable (normal / pressed)
```

## Testing

- **Framework**: JUnit 4 + Mockito 5 + mockito-kotlin 5; Robolectric for code that needs Android classes
- **Location**: `app/src/test/java/com/example/mechboard/`
- **Run tests**: `./gradlew test`
- Prefer plain JVM tests of pure Kotlin classes. Use `@RunWith(RobolectricTestRunner::class)` only when the code under test needs Android classes (views, `EditorInfo`, window insets).
- `SoundPool` is a native component; do not mock it. Test `SoundManager` through a fake `SamplePlayer` (see `SoundManagerPlaybackTest`).
- New business logic should be extracted into plain Kotlin classes/objects so it can be tested without an emulator.

## Adding a New Sound Profile

1. Add a WAV, MP3 or OGG file to `app/src/main/res/raw/`.
2. Add a new entry to the `SoundProfile` enum with a unique `id`, a `displayName`, and the matching `rawFileName`.
3. No changes to `SoundManager` are needed — it iterates all profiles dynamically.

## Code Style

- Follow standard Kotlin idioms (data classes, `when` expressions, extension functions where they aid readability).
- Keep Android-framework calls confined to `MechboardService`, `MechboardKeyboardView`, `TypingController`, `SettingsActivity`, `SettingsFragment`, and `SoundManager`/`SoundPoolSamplePlayer`; business/domain logic should be pure Kotlin.
- Prefer constructor injection of `SharedPreferences` over calling `context.getSharedPreferences()` deep inside helper classes.
