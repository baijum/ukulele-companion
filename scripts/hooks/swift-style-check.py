#!/usr/bin/env python3
"""PostToolUse sensor: Swift style checks for iOS source files.

Runs SwiftLint on the edited file if available; otherwise falls back to basic
heuristic checks (line length, trailing whitespace, force-unwrap usage).

Exit 2 surfaces findings; exit 0 when clean or not a Swift file.
"""
import json
import os
import re
import shutil
import subprocess
import sys

MAX_LEN = 120
MAX_REPORTED = 15
TIMEOUT_SECONDS = 30


def run_swiftlint(file_path: str) -> list[str]:
    """Try SwiftLint on the file. Returns findings or empty list."""
    swiftlint = shutil.which("swiftlint")
    if not swiftlint:
        return []
    try:
        result = subprocess.run(
            [swiftlint, "lint", "--path", file_path, "--quiet"],
            capture_output=True,
            text=True,
            timeout=TIMEOUT_SECONDS,
        )
    except (subprocess.TimeoutExpired, FileNotFoundError, OSError):
        return []
    if result.returncode == 0:
        return []
    output = (result.stdout or "") + (result.stderr or "")
    return [l.strip() for l in output.strip().splitlines() if l.strip()]


def basic_checks(file_path: str, raw: str) -> list[str]:
    """Heuristic checks when SwiftLint is not available."""
    findings = []
    lines = raw.split("\n")
    for i, line in enumerate(lines, start=1):
        if len(line) > MAX_LEN:
            findings.append(f"L{i}: line is {len(line)} chars (max {MAX_LEN})")
        elif line != line.rstrip():
            findings.append(f"L{i}: trailing whitespace")
    if raw and not raw.endswith("\n"):
        findings.append("missing final newline")

    force_unwraps = list(re.finditer(r"(?<!\w)(\w+)!\.", raw))
    for m in force_unwraps:
        line_no = raw[:m.start()].count("\n") + 1
        name = m.group(1)
        if name not in ("self", "super", "Bundle", "UIApplication", "NSBundle"):
            findings.append(
                f"L{line_no}: force-unwrap on '{name}' — prefer optional binding"
            )

    return findings


def main() -> int:
    try:
        payload = json.load(sys.stdin)
    except (json.JSONDecodeError, ValueError):
        return 0
    file_path = (payload.get("tool_input", {}) or {}).get("file_path", "") or ""
    if not file_path.endswith(".swift") or not os.path.isfile(file_path):
        return 0

    findings = run_swiftlint(file_path)

    if not findings:
        try:
            with open(file_path, "r", encoding="utf-8") as fh:
                raw = fh.read()
        except (OSError, UnicodeDecodeError):
            return 0
        findings = basic_checks(file_path, raw)

    if not findings:
        return 0

    shown = findings[:MAX_REPORTED]
    extra = len(findings) - len(shown)
    name = os.path.basename(file_path)
    sys.stderr.write(
        f"Swift style check for {name}:\n"
        + "\n".join(f"  {f}" for f in shown)
        + (f"\n  ... and {extra} more" if extra else "")
        + "\n"
    )
    return 2


if __name__ == "__main__":
    sys.exit(main())
