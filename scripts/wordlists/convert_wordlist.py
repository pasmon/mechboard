#!/usr/bin/env python3
"""
Converts an AOSP keyboard word list (".combined" format, optionally gzipped)
into the plain word list Mechboard bundles in app/src/main/assets/dictionaries/.

The output has one word per line, most frequent first. Mechboard ranks
suggestions by line number, so the frequencies themselves are not kept.

Left out:
  * words with frequency 0 (AOSP's "never suggest" entries),
  * words flagged offensive, possibly offensive or "nonword",
  * shortcut and bigram lines, and anything containing a space or a digit.

Usage:
  convert_wordlist.py main_fi.combined.gz finnish.txt [--max-words 80000]
"""

import argparse
import gzip
import re
import sys

EXCLUDED_FLAGS = {"offensive", "nonword"}


def read_entries(path):
    opener = gzip.open if path.endswith(".gz") else open
    with opener(path, "rt", encoding="utf-8") as lines:
        for line in lines:
            # Word lines start with exactly one space; shortcut and bigram lines
            # with two, and the header line with none.
            if not line.startswith(" word="):
                continue
            fields = dict(
                field.split("=", 1) for field in line.strip().split(",") if "=" in field
            )
            yield fields


def keep(fields):
    word = fields.get("word", "")
    if not word or re.search(r"[\s\d]", word):
        return False
    if int(fields.get("f", "0")) <= 0:
        return False
    if fields.get("flags", "") in EXCLUDED_FLAGS:
        return False
    if fields.get("possibly_offensive") == "true":
        return False
    return True


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("source", help="AOSP .combined or .combined.gz word list")
    parser.add_argument("output", help="plain word list to write")
    parser.add_argument("--max-words", type=int, default=80000)
    args = parser.parse_args()

    entries = [fields for fields in read_entries(args.source) if keep(fields)]
    # Stable sort: words of equal frequency keep the source's order.
    entries.sort(key=lambda fields: -int(fields["f"]))

    seen = set()
    words = []
    for fields in entries:
        word = fields["word"]
        if word in seen:
            continue
        seen.add(word)
        words.append(word)
        if len(words) == args.max_words:
            break

    with open(args.output, "w", encoding="utf-8", newline="\n") as out:
        for word in words:
            out.write(word + "\n")
    print(f"{args.output}: {len(words)} words", file=sys.stderr)


if __name__ == "__main__":
    main()
