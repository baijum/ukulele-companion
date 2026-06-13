#!/usr/bin/env python3
"""PostToolUse style nudge for Kotlin/Swift source.

Runs after an edit/write completes and reports basic .editorconfig violations
(max line length 120, trailing whitespace, missing final newline) so they get
fixed in-loop. This is a fast nudge only; ktlint (preflight + CI) is the
authoritative formatter. Silent (exit 0) when the file is clean or not source.

Exit 2 surfaces the findings to Claude after the edit so it can correct them;
the edit itself is not reverted.
"""
import json
import os
import sys

MAX_LEN = 120
EXTS = (".kt", ".kts", ".swift")
MAX_REPORTED = 15


def main() -> int:
    try:
        payload = json.load(sys.stdin)
    except (json.JSONDecodeError, ValueError):
        return 0
    file_path = (payload.get("tool_input", {}) or {}).get("file_path", "") or ""
    if not file_path.endswith(EXTS) or not os.path.isfile(file_path):
        return 0

    try:
        with open(file_path, "r", encoding="utf-8") as fh:
            raw = fh.read()
    except (OSError, UnicodeDecodeError):
        return 0

    findings = []
    lines = raw.split("\n")
    for i, line in enumerate(lines, start=1):
        if len(line) > MAX_LEN:
            findings.append(f"  L{i}: line is {len(line)} chars (max {MAX_LEN})")
        elif line != line.rstrip():
            findings.append(f"  L{i}: trailing whitespace")
    if raw and not raw.endswith("\n"):
        findings.append("  missing final newline")

    if not findings:
        return 0

    shown = findings[:MAX_REPORTED]
    extra = len(findings) - len(shown)
    name = os.path.basename(file_path)
    sys.stderr.write(
        f"Style nudge for {name} (.editorconfig): please fix before moving on.\n"
        + "\n".join(shown)
        + (f"\n  ... and {extra} more" if extra else "")
        + "\n"
    )
    return 2


if __name__ == "__main__":
    sys.exit(main())
