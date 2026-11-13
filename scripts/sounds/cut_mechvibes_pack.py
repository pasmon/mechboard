#!/usr/bin/env python3
"""
Cuts a profile's key sounds out of a Mechvibes single-file sound pack.

A Mechvibes pack is one recording (sound.ogg) and a config.json giving, for
every key, the time ranges of its press and release sounds. This script writes
four samples for one Mechboard profile, all from that one recording:

  <profile>.ogg            an ordinary key: of the letter keys, the one of
                           median loudness, so it is a typical keystroke
  <profile>_space.ogg      the space bar
  <profile>_enter.ogg      enter
  <profile>_backspace.ogg  backspace

Each is the press half of the key's entry, mixed down to mono, trimmed of
leading silence (otherwise the sound lags the key press) and faded out over
the last few milliseconds. All four get the same gain, so the keys keep the
loudness they have relative to each other in the recording; with --match-level
that gain makes the ordinary key as loud as a previous sample.

Needs ffmpeg (with libvorbis) and numpy.

Usage:
  cut_mechvibes_pack.py PACK_DIR PROFILE OUT_DIR [--match-level OLD_SAMPLE]

  e.g. cut_mechvibes_pack.py mechvibes/soundpacks/keyboard/cherrymx-blue-abs \\
           cherry_blue app/src/main/res/raw --match-level old/cherry_blue.mp3
"""

import argparse
import json
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np

SAMPLE_RATE = 44100
SPECIAL_KEYS = {"space": "Space", "enter": "Enter", "backspace": "Backspace"}
LETTER_KEYS = [f"Key{chr(c)}" for c in range(ord("A"), ord("Z") + 1)]
SILENCE_DB = -24.0      # below the segment's peak, counts as silence: trims faint noise before the click
LEAD_IN_MS = 2.0        # kept before the first sound
FADE_OUT_MS = 5.0
MAX_PEAK = 0.98


def decode(path, start_ms=None, end_ms=None):
    """Decodes [start_ms, end_ms) of path to mono float samples at SAMPLE_RATE."""
    cmd = ["ffmpeg", "-v", "error", "-i", path]
    if start_ms is not None:
        cmd += ["-ss", f"{start_ms / 1000:.4f}", "-to", f"{end_ms / 1000:.4f}"]
    cmd += ["-ac", "1", "-ar", str(SAMPLE_RATE), "-f", "f32le", "-"]
    raw = subprocess.run(cmd, check=True, capture_output=True).stdout
    return np.frombuffer(raw, dtype="<f4").astype(np.float64)


def trim(samples):
    """Drops leading silence and fades out the end."""
    peak = np.max(np.abs(samples))
    if peak == 0:
        return samples
    threshold = peak * 10 ** (SILENCE_DB / 20)
    first = int(np.argmax(np.abs(samples) > threshold))
    start = max(0, first - int(SAMPLE_RATE * LEAD_IN_MS / 1000))
    trimmed = samples[start:].copy()
    fade = min(len(trimmed), int(SAMPLE_RATE * FADE_OUT_MS / 1000))
    if fade > 0:
        trimmed[-fade:] *= np.linspace(1.0, 0.0, fade)
    return trimmed


def rms(samples):
    return float(np.sqrt(np.mean(samples ** 2))) if len(samples) else 0.0


def write_ogg(samples, path):
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wav_path = tmp.name
    try:
        pcm = (np.clip(samples, -1.0, 1.0) * 32767).astype("<i2")
        with wave.open(wav_path, "wb") as out:
            out.setnchannels(1)
            out.setsampwidth(2)
            out.setframerate(SAMPLE_RATE)
            out.writeframes(pcm.tobytes())
        subprocess.run(
            ["ffmpeg", "-v", "error", "-y", "-i", wav_path, "-c:a", "libvorbis", "-q:a", "5", path],
            check=True,
        )
    finally:
        os.unlink(wav_path)


def press(pack_dir, definitions, key):
    start, end = definitions[key]["timing"][0]
    return trim(decode(os.path.join(pack_dir, "sound.ogg"), start, end))


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("pack_dir", help="Mechvibes pack directory (config.json, sound.ogg)")
    parser.add_argument("profile", help="Mechboard SoundProfile.rawFileName, e.g. cherry_blue")
    parser.add_argument("out_dir", help="where to write the .ogg files, e.g. app/src/main/res/raw")
    parser.add_argument("--match-level", metavar="OLD_SAMPLE",
                        help="scale so the ordinary key is as loud (RMS) as this sample")
    args = parser.parse_args()

    with open(os.path.join(args.pack_dir, "config.json"), encoding="utf-8") as f:
        definitions = json.load(f)["definitions"]

    letters = {key: press(args.pack_dir, definitions, key) for key in LETTER_KEYS if key in definitions}
    by_loudness = sorted(letters, key=lambda key: rms(letters[key]))
    generic_key = by_loudness[len(by_loudness) // 2]
    samples = {"": letters[generic_key]}
    for suffix, key in SPECIAL_KEYS.items():
        samples["_" + suffix] = press(args.pack_dir, definitions, key)

    gain = 1.0
    if args.match_level:
        gain = rms(trim(decode(args.match_level))) / rms(samples[""])
    loudest = max(np.max(np.abs(s)) for s in samples.values())
    gain = min(gain, MAX_PEAK / loudest)

    for suffix, sample in samples.items():
        path = os.path.join(args.out_dir, f"{args.profile}{suffix}.ogg")
        write_ogg(sample * gain, path)
        print(f"{path}: {len(sample) / SAMPLE_RATE * 1000:.0f} ms", file=sys.stderr)
    print(f"{args.profile}: ordinary key {generic_key}, gain {gain:.2f}", file=sys.stderr)


if __name__ == "__main__":
    main()
