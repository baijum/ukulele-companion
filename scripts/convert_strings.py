#!/usr/bin/env python3
"""
Converts Android strings.xml files to Xcode String Catalog (.xcstrings) format.

Usage:
    python3 scripts/convert_strings.py

Reads from app/src/main/res/values*/strings.xml and writes to
iosApp/UkuleleCompanion/Localizable.xcstrings.
"""

import json
import os
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

# Android locale dir name -> Apple locale code
LOCALE_MAP = {
    "values": "en",
    "values-fr": "fr",
    "values-de": "de",
    "values-es": "es",
    "values-it": "it",
    "values-pt": "pt",
    "values-nl": "nl",
    "values-ru": "ru",
    "values-tr": "tr",
    "values-pl": "pl",
    "values-ja": "ja",
    "values-ko": "ko",
    "values-hi": "hi",
    "values-in": "id",
    "values-ar": "ar",
    "values-b+zh+Hans": "zh-Hans",
}


def convert_android_format(text: str) -> str:
    """Convert Android string format specifiers to Apple format.

    Android uses %1$s, %2$d, %d, %s etc.
    Apple uses %1$@, %2$lld, %lld, %@ etc.
    """
    if text is None:
        return ""

    # Handle positional string args: %1$s -> %1$@
    text = re.sub(r"%(\d+)\$s", r"%\1$@", text)
    # Handle positional int args: %1$d -> %1$lld
    text = re.sub(r"%(\d+)\$d", r"%\1$lld", text)
    # Handle simple string args: %s -> %@
    text = re.sub(r"(?<!%)%s", "%@", text)
    # Handle simple int args: %d -> %lld
    text = re.sub(r"(?<!%)%d", "%lld", text)
    # Unescape Android apostrophe escaping
    text = text.replace("\\'", "'")

    return text


def parse_strings_xml(filepath: str) -> dict[str, str]:
    """Parse an Android strings.xml file and return a dict of key -> value."""
    strings = {}
    try:
        tree = ET.parse(filepath)
        root = tree.getroot()
        for elem in root.findall("string"):
            name = elem.get("name")
            if name is None:
                continue
            # Skip non-translatable strings for non-default locales
            # Get the text content (handles nested elements like <b>, <i>)
            text = elem.text or ""
            # If there are child elements, concatenate their text
            for child in elem:
                text += (child.text or "") + (child.tail or "")
            strings[name] = convert_android_format(text.strip())
    except ET.ParseError as e:
        print(f"Warning: Failed to parse {filepath}: {e}", file=sys.stderr)
    return strings


def build_xcstrings(project_root: str) -> dict:
    """Build the .xcstrings JSON structure from all Android string files."""
    res_dir = os.path.join(project_root, "app", "src", "main", "res")

    # Collect all strings from all locales
    all_keys: set[str] = set()
    locale_strings: dict[str, dict[str, str]] = {}

    for dir_name, apple_locale in LOCALE_MAP.items():
        filepath = os.path.join(res_dir, dir_name, "strings.xml")
        if os.path.exists(filepath):
            strings = parse_strings_xml(filepath)
            locale_strings[apple_locale] = strings
            all_keys.update(strings.keys())
        else:
            print(f"Warning: {filepath} not found, skipping {apple_locale}", file=sys.stderr)

    # Build xcstrings structure
    xcstrings = {
        "sourceLanguage": "en",
        "version": "1.0",
        "strings": {}
    }

    for key in sorted(all_keys):
        localizations = {}
        for apple_locale, strings in locale_strings.items():
            if key in strings:
                localizations[apple_locale] = {
                    "stringUnit": {
                        "state": "translated",
                        "value": strings[key]
                    }
                }

        xcstrings["strings"][key] = {
            "localizations": localizations
        }

    return xcstrings


def main():
    # Find project root (script is in scripts/ dir)
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)

    xcstrings = build_xcstrings(project_root)

    output_path = os.path.join(
        project_root, "iosApp", "UkuleleCompanion", "Localizable.xcstrings"
    )
    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(xcstrings, f, indent=2, ensure_ascii=False)

    total_keys = len(xcstrings["strings"])
    locales = set()
    for entry in xcstrings["strings"].values():
        locales.update(entry.get("localizations", {}).keys())

    print(f"Generated {output_path}")
    print(f"  {total_keys} string keys")
    print(f"  {len(locales)} locales: {', '.join(sorted(locales))}")


if __name__ == "__main__":
    main()
