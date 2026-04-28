# Copilot Instructions for Mechboard

## Project Overview

**Mechboard** is an Android Input Method Editor (IME / soft keyboard) written in Kotlin. It provides a QWERTY keyboard with authentic mechanical keyboard sound effects. Users can pick from six sound profiles, toggle sounds, and adjust volume via a built-in settings screen.

## Architecture

| Class | Role |
|---|---|
| `MechboardService` | Main `InputMethodService`; inflates the keyboard view, routes key events, manages caps-lock state, and triggers sound playback |
| `SoundManager` | Wraps `SoundPool`; loads WAV assets per profile, reads/writes preferences, exposes `playKeySound()` |
| `SoundProfile` | Enum of six profiles (Cherry MX Blue/Red/Brown, Topre, Alps, Silent); each carries an `id`, `displayName`, and optional `rawFileName` |
| `SettingsActivity` | `AppCompatActivity` that hosts `SettingsFragment` |
| `SettingsFragment` | `PreferenceFragmentCompat`; renders profile picker, master toggle, and volume slider |
| `PrefsKeys` | Plain object of SharedPreferences key constants (no Android dependency — keeps unit tests simple) |

## Key Conventions

- **Package**: `com.example.mechboard`
- **Language**: Kotlin (JVM target 1.8, `compileOptions` set to Java 8)
- **Min SDK**: 21 · **Target/Compile SDK**: 34
- **Preferences**: All user settings live in `SharedPreferences` keyed by constants in `PrefsKeys`. The preference name is `MechboardService.PREFS_NAME = "mechboard_prefs"`.
- **Custom key codes**: Settings key uses `KEYCODE_SETTINGS = -101`; all other keys use standard ASCII / `Keyboard.KEYCODE_*` values.
- **Sound loading**: `SoundManager` loads every non-silent profile at construction time. `playKeySound()` is a no-op when sound is disabled or the sample is not yet loaded.
- **Deprecated API awareness**: `KeyboardView` / `Keyboard` are deprecated in Android but deliberately used for now. Any future replacement should be a custom `View`.

## Resource Layout

```
res/
  layout/
    input.xml           # Keyboard host layout (contains KeyboardView)
    activity_settings.xml
  xml/
    keyboard.xml        # Key rows and codes
    method.xml          # IME metadata (subtype declarations)
    preferences.xml     # Preference screen definition
  raw/
    cherry_blue.wav
    cherry_red.wav
    cherry_brown.wav
    topre.wav
    alps.wav
  values/
    colors.xml          # Dark theme palette
    dimens.xml          # key_height = 56dp
    strings.xml
    themes.xml
  drawable/
    key_background.xml  # State-list drawable (normal / pressed)
```

## Testing

- **Framework**: JUnit 4 + Mockito 5 + mockito-kotlin 5
- **Location**: `app/src/test/java/com/example/mechboard/`
- **Run tests**: `./gradlew test`
- Tests must **not** depend on the Android SDK. Use plain JUnit + Mockito and inject `SharedPreferences` via constructor arguments (see `SoundManagerPrefsTest`).
- `SoundPool` is a native component; do not attempt to mock or unit-test it directly.
- New business logic should be extracted into plain Kotlin classes/objects so it can be tested without an emulator.

## Adding a New Sound Profile

1. Add a WAV file to `app/src/main/res/raw/`.
2. Add a new entry to the `SoundProfile` enum with a unique `id`, a `displayName`, and the matching `rawFileName`.
3. No changes to `SoundManager` are needed — it iterates all profiles dynamically.

## Code Style

- Follow standard Kotlin idioms (data classes, `when` expressions, extension functions where they aid readability).
- Keep Android-framework calls confined to `MechboardService`, `SettingsActivity`, `SettingsFragment`, and `SoundManager`; business/domain logic should be pure Kotlin.
- Prefer constructor injection of `SharedPreferences` over calling `context.getSharedPreferences()` deep inside helper classes.
