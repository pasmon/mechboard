#!/usr/bin/env python3
"""
Makes a profile's key sounds from separate per-key recordings.

Some sound sets ship one file per key instead of one recording with a timing
table (e.g. kbsim's press/GENERIC_R0.mp3, SPACE.mp3, ENTER.mp3, BACKSPACE.mp3).
This script treats them as cut_mechvibes_pack.py treats a pack's keys: each
file is mixed down to mono, trimmed of leading silence and faded out, and all
get one gain so they keep their loudness relative to each other. Only use
files from the same recording session, or the keys sound like different
keyboards.

Writes <profile>.ogg and, for each of --space/--enter/--backspace given,
<profile>_space.ogg, <profile>_enter.ogg, <profile>_backspace.ogg. Keys left
out use the profile's ordinary sound in the app.

Needs ffmpeg (with libvorbis) and numpy.

Usage:
  cut_key_files.py PROFILE OUT_DIR --key FILE [--space FILE] [--enter FILE]
                   [--backspace FILE] [--match-level OLD_SAMPLE]

  e.g. cut_key_files.py holy_panda app/src/main/res/raw \\
           --key kbsim/src/assets/audio/holypanda/press/GENERIC_R0.mp3 \\
           --space kbsim/src/assets/audio/holypanda/press/SPACE.mp3 \\
           --enter kbsim/src/assets/audio/holypanda/press/ENTER.mp3 \\
           --backspace kbsim/src/assets/audio/holypanda/press/BACKSPACE.mp3 \\
           --match-level old/holy_panda.mp3
"""

import argparse
import sys

from cut_mechvibes_pack import SPECIAL_KEYS, decode, trim, write_profile


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("profile", help="Mechboard SoundProfile.rawFileName, e.g. holy_panda")
    parser.add_argument("out_dir", help="where to write the .ogg files, e.g. app/src/main/res/raw")
    parser.add_argument("--key", required=True, help="an ordinary key's recording")
    for suffix in SPECIAL_KEYS:
        parser.add_argument(f"--{suffix}", help=f"the {suffix} key's recording")
    parser.add_argument("--match-level", metavar="OLD_SAMPLE",
                        help="scale so the ordinary key is as loud (RMS) as this sample")
    args = parser.parse_args()

    samples = {"": trim(decode(args.key))}
    for suffix in SPECIAL_KEYS:
        path = getattr(args, suffix)
        if path:
            samples["_" + suffix] = trim(decode(path))

    gain = write_profile(samples, args.profile, args.out_dir, args.match_level)
    print(f"{args.profile}: gain {gain:.2f}", file=sys.stderr)


if __name__ == "__main__":
    main()
