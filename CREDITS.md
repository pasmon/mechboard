# Audio Credits

This file documents the origin and license of every audio asset and word list bundled with Mechboard.

---

## How the samples are made

Every profile has an ordinary key sound and, where its source recorded them,
its own space bar, enter and backspace sounds. All samples of a profile come
from one recording session, so that all keys sound like the same keyboard.
Each sample is mixed down to mono, trimmed of leading silence and faded out
over its last 5 ms. All samples of a profile get one gain, so the keys keep
the loudness they have relative to each other in the recording; for a
profile that existed before, the gain keeps the ordinary key as loud as the
profile's previous sample (Blue Alps: as loud as clipping allows), and new
profiles are levelled to a similar existing one.

- `scripts/sounds/cut_mechvibes_pack.py` cuts the samples from a Mechvibes
  single-file pack: the *press* half of each key's entry in the pack's
  `config.json`. The ordinary key is the letter key of median loudness.
- `scripts/sounds/cut_key_files.py` does the same for sound sets with one file
  per key.

---

## Profiles from Mechvibes single-file packs

From the [Mechvibes](https://github.com/hainguyents13/mechvibes) repository,
tag `v3.5.6`, commit `56d7135` (these packs are unchanged at `v3.5.7`).

| Files | Profile | Upstream sound pack | Upstream keys | License |
|---|---|---|---|---|
| `cherry_blue.ogg`, `cherry_blue_space.ogg`, `cherry_blue_enter.ogg`, `cherry_blue_backspace.ogg` | Cherry MX Blue (Clicky) | `soundpacks/keyboard/cherrymx-blue-abs/sound.ogg` – "CherryMX Blue - ABS keycaps" | `KeyT`, `Space`, `Enter`, `Backspace` | MIT – Copyright © 2026 Hải Nguyễn |
| `cherry_red.ogg`, `cherry_red_space.ogg`, `cherry_red_enter.ogg`, `cherry_red_backspace.ogg` | Cherry MX Red (Linear) | `soundpacks/keyboard/cherrymx-red-abs/sound.ogg` – "CherryMX Red - ABS keycaps" | `KeyI`, `Space`, `Enter`, `Backspace` | MIT – Copyright © 2026 Hải Nguyễn |
| `cherry_brown.ogg`, `cherry_brown_space.ogg`, `cherry_brown_enter.ogg`, `cherry_brown_backspace.ogg` | Cherry MX Brown (Tactile) | `soundpacks/keyboard/cherrymx-brown-abs/sound.ogg` – "CherryMX Brown - ABS keycaps" | `KeyQ`, `Space`, `Enter`, `Backspace` | MIT – Copyright © 2026 Hải Nguyễn |
| `cherry_black.ogg`, `cherry_black_space.ogg`, `cherry_black_enter.ogg`, `cherry_black_backspace.ogg` | Cherry MX Black (Heavy Linear) | `soundpacks/keyboard/cherrymx-black-abs/sound.ogg` – "CherryMX Black - ABS keycaps" | `KeyC`, `Space`, `Enter`, `Backspace` | MIT – Copyright © 2026 Hải Nguyễn |
| `topre.ogg`, `topre_space.ogg`, `topre_enter.ogg`, `topre_backspace.ogg` | Topre (Thocky) | `soundpacks/keyboard/topre-purple-hybrid-pbt/sound.ogg` – "Topre Purple Hybrid - PBT keycaps" | `KeyM`, `Space`, `Enter`, `Backspace` | MIT – Copyright © 2026 Hải Nguyễn |

The Cherry MX Blue, Red, Brown and Topre samples replace earlier ones whose
ordinary key came from a different recording (Mechvibes' former
`src/audio/*-travel` packs, no longer in the repository; Cherry MX Red used
the Cherry MX Black recording), so the space, enter and backspace sounds did
not match the other keys.

---

## Profiles from per-key recordings

| Files | Profile | Source project | Upstream files | License |
|---|---|---|---|---|
| `nk_cream.ogg`, `nk_cream_space.ogg`, `nk_cream_enter.ogg`, `nk_cream_backspace.ogg` | NK Cream (Smooth Linear) | [Mechvibes](https://github.com/hainguyents13/mechvibes), commit `8be3f33` – "NK Cream (original by Ryan)" (pack removed from the repository in the next commit) | `soundpacks/keyboard/nk-cream/a.wav`, `space.wav`, `enter.wav`, `backspace.wav` | MIT – Copyright © 2026 Hải Nguyễn; recording original by Ryan |
| `holy_panda.ogg`, `holy_panda_space.ogg`, `holy_panda_enter.ogg`, `holy_panda_backspace.ogg` | Holy Panda (Tactile Thock) | [kbsim](https://github.com/tplai/kbsim), commit `ba103f3` – "Holy Pandas" | `src/assets/audio/holypanda/press/GENERIC_R0.mp3`, `SPACE.mp3`, `ENTER.mp3`, `BACKSPACE.mp3` | MIT – Copyright © Thomas Lai |
| `alps.ogg`, `alps_space.ogg`, `alps_enter.ogg`, `alps_backspace.ogg` | Blue Alps (Vintage Clicky) | [kbsim](https://github.com/tplai/kbsim), commit `ba103f3` – "SKCM Blue Alps" | `src/assets/audio/bluealps/press/GENERIC_R0.mp3`, `SPACE.mp3`, `ENTER.mp3`, `BACKSPACE.mp3` | MIT – Copyright © Thomas Lai |
| `gateron_black_ink.ogg`, `gateron_black_ink_space.ogg`, `gateron_black_ink_enter.ogg`, `gateron_black_ink_backspace.ogg` | Gateron Black Ink (Deep Linear) | [kbsim](https://github.com/tplai/kbsim), commit `ba103f3` – "Gateron Black Inks" | `src/assets/audio/blackink/press/GENERIC_R0.mp3`, `SPACE.mp3`, `ENTER.mp3`, `BACKSPACE.mp3` | MIT – Copyright © Thomas Lai |
| `box_navy.ogg`, `box_navy_space.ogg`, `box_navy_enter.ogg`, `box_navy_backspace.ogg` | Kailh Box Navy (Heavy Clicky) | [kbsim](https://github.com/tplai/kbsim), commit `ba103f3` – "Kailh Box Navies" | `src/assets/audio/boxnavy/press/GENERIC_R0.mp3`, `SPACE.mp3`, `ENTER.mp3`, `BACKSPACE.mp3` | MIT – Copyright © Thomas Lai |
| `buckling_spring.ogg`, `buckling_spring_space.ogg`, `buckling_spring_enter.ogg`, `buckling_spring_backspace.ogg` | Buckling Spring (IBM Model M) | [kbsim](https://github.com/tplai/kbsim), commit `ba103f3` – "Buckling Spring" | `src/assets/audio/buckling/press/GENERIC_R0.mp3`, `SPACE.mp3`, `ENTER.mp3`, `BACKSPACE.mp3` | MIT – Copyright © Thomas Lai |
| `typewriter.ogg`, `typewriter_enter.ogg` | Typewriter (Classic) | [daktilo](https://github.com/orhun/daktilo), commit `87c6032` – the `basic` preset, which pairs these two sounds | `sounds/keystroke.mp3`, `sounds/newline.mp3` (carriage return) | MIT – Copyright © 2023–2024 Orhun Parmaksız |

The ordinary-key samples of NK Cream, Holy Panda and Typewriter are the same
recordings as before (`nk_cream.wav`, `holy_panda.mp3`, `typewriter.mp3`),
now trimmed and encoded like the others, with the matching special keys from
the same source added. The typewriter has no dedicated space or backspace
sound upstream, so those keys use its ordinary keystroke.

The Blue Alps samples replace `alps.wav`, whose source was never recorded.

---

## Mechvibes MIT License

```
MIT License

Copyright (c) 2021 Hai Nguyen

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## kbsim (and Holy Pandas) MIT License

```
Copyright (c) Thomas Lai

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## daktilo MIT License

```
The MIT License (MIT)

Copyright (c) 2023-2024 Orhun Parmaksız

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

# Word List Credits

The word lists in `app/src/main/assets/dictionaries/` are the 60,000 most
frequent words of each language from the AOSP (Android Open Source Project)
keyboard dictionaries, with offensive and "never suggest" entries removed by
`scripts/wordlists/convert_wordlist.py`.

| File | Language | Upstream word list | Source chain |
|---|---|---|---|
| `english.txt` | English (US) | `main_en_US.combined.gz` – "English (US)", v54, 2014-10-31 | AOSP LatinIME → [OpenBoard v1.4.5](https://github.com/openboard-team/openboard/blob/v1.4.5/dictionaries/en_wordlist.combined.gz) → HeliBoard's `aosp-dictionaries` repository (`wordlists/`, commit `8b479ff`) |
| `finnish.txt` | Finnish | `main_fi.combined.gz` – "Suomi", v44, 2014-02-24 | as above (`fi_wordlist.combined.gz`) |
| `german.txt` | German | `main_de.combined.gz` – "Deutsch", v54, 2014-10-31 | as above (`de_wordlist.combined.gz`) |
| `french.txt` | French | `main_fr.combined.gz` – "Français", v54, 2014-10-31 | as above (`fr_wordlist.combined.gz`) |
| `english_next.txt` | English (US) | the word pairs ("bigram" lines) of `main_en_US.combined.gz` above | as for `english.txt` |
| `spanish.txt` | Spanish | `main_es.combined.gz` – "Español", v54, 2014-10-31 | as above (`es_wordlist.combined.gz`) |

**License status: to be confirmed.** The lists' version numbers and dates match
the dictionaries Google shipped with AOSP LatinIME, which AOSP distributes under
the Apache License 2.0. OpenBoard and HeliBoard, which republish them, are
GPL-3.0 projects and do not state a separate license for the word lists. Confirm
the license before a release that bundles them.
