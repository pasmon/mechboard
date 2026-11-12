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

With --next-words, also writes the list's word pairs (AOSP "bigram" lines,
present only in some lists, e.g. English): one line per word, the word and
then its likely next words, best first, separated by tabs. Only words in the
written word list are used, on both sides, and only the --next-words-for most
frequent words get a line.

Usage:
  convert_wordlist.py main_fi.combined.gz finnish.txt [--max-words 80000]
  convert_wordlist.py main_en_US.combined.gz english.txt \
      --next-words english_next.txt [--next-words-for 30000]
"""

import argparse
import gzip
import re
import sys

EXCLUDED_FLAGS = {"offensive", "nonword"}


def parse_fields(line):
    return dict(field.split("=", 1) for field in line.strip().split(",") if "=" in field)


def read_entries(path):
    """Yields the fields of each word line, with its bigrams under "bigrams"."""
    opener = gzip.open if path.endswith(".gz") else open
    current = None
    with opener(path, "rt", encoding="utf-8") as lines:
        for line in lines:
            # Word lines start with exactly one space; shortcut and bigram lines
            # with two, and the header line with none.
            if line.startswith(" word="):
                if current is not None:
                    yield current
                current = parse_fields(line)
                current["bigrams"] = []
            elif line.startswith("  bigram=") and current is not None:
                bigram = parse_fields(line)
                current["bigrams"].append((bigram["bigram"], int(bigram.get("f", "0"))))
    if current is not None:
        yield current


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
    parser.add_argument("--next-words", help="also write the word pairs to this file")
    parser.add_argument("--next-words-for", type=int, default=30000,
                        help="how many of the most frequent words get next words")
    args = parser.parse_args()

    entries = [fields for fields in read_entries(args.source) if keep(fields)]
    # Stable sort: words of equal frequency keep the source's order.
    entries.sort(key=lambda fields: -int(fields["f"]))

    seen = set()
    words = []
    kept_entries = []
    for fields in entries:
        word = fields["word"]
        if word in seen:
            continue
        seen.add(word)
        words.append(word)
        kept_entries.append(fields)
        if len(words) == args.max_words:
            break

    if args.next_words:
        write_next_words(args.next_words, kept_entries[: args.next_words_for], seen)

    with open(args.output, "w", encoding="utf-8", newline="\n") as out:
        for word in words:
            out.write(word + "\n")
    print(f"{args.output}: {len(words)} words", file=sys.stderr)


def write_next_words(path, entries, known):
    lines = 0
    with open(path, "w", encoding="utf-8", newline="\n") as out:
        for fields in entries:
            # A higher bigram f means a likelier next word.
            following = [
                word for word, _ in sorted(fields["bigrams"], key=lambda b: -b[1])
                if word in known and word != fields["word"]
            ]
            if following:
                out.write("\t".join([fields["word"]] + following) + "\n")
                lines += 1
    print(f"{path}: next words for {lines} words", file=sys.stderr)


if __name__ == "__main__":
    main()
