#!/usr/bin/env python3
"""PostToolUse sensor: lightweight accessibility structural checks.

Scans newly written/edited Compose (.kt) and SwiftUI (.swift) UI files for
common accessibility omissions. This is a fast, regex-based heuristic — not a
substitute for the full accessibility-reviewer subagent, but catches the most
frequent mistakes in-loop before they reach review.

Exit 2 surfaces findings; exit 0 when clean or file is outside UI paths.
"""
import json
import os
import re
import sys

MAX_REPORTED = 10

UI_PATHS_ANDROID = ("/ui/", "/views/")
UI_PATHS_IOS = ("/Views/", "/views/")

# --- Android Compose patterns ---

# Icon() without contentDescription argument
ICON_NO_DESC = re.compile(
    r"\bIcon\s*\("
    r"(?:(?!contentDescription)[^))])*"
    r"\)",
    re.DOTALL,
)

# Canvas without clearAndSetSemantics nearby (within 3 lines before)
CANVAS_BARE = re.compile(r"^\s*Canvas\s*\(", re.MULTILINE)
CLEAR_SEMANTICS = re.compile(r"clearAndSetSemantics")

# clickable{} without contentDescription or role in surrounding semantics
CLICKABLE_NO_SEMANTICS = re.compile(
    r"\.clickable\s*[\({]"
)

# --- iOS SwiftUI patterns ---

# Image() without .accessibilityLabel
IMAGE_NO_LABEL = re.compile(
    r"\bImage\s*\(\s*systemName\s*:",
)
A11Y_LABEL = re.compile(r"\.accessibilityLabel\(")

# Button without label or accessibilityLabel
DECORATIVE_IMAGE = re.compile(r"\.accessibilityHidden\s*\(\s*true\s*\)")


def is_android_ui(path: str) -> bool:
    return path.endswith(".kt") and any(p in path for p in UI_PATHS_ANDROID)


def is_ios_ui(path: str) -> bool:
    return path.endswith(".swift") and any(p in path for p in UI_PATHS_IOS)


def check_android(text: str, lines: list[str]) -> list[str]:
    findings = []

    for m in ICON_NO_DESC.finditer(text):
        line_no = text[:m.start()].count("\n") + 1
        snippet = text[m.start():m.end()].split("\n")[0][:60]
        if "contentDescription" not in text[m.start():m.end()]:
            findings.append(
                f"L{line_no}: Icon() missing contentDescription — "
                "add a descriptive label or null for decorative icons "
                "(compose-accessibility.mdc Rule 1)"
            )

    for m in CANVAS_BARE.finditer(text):
        line_no = text[:m.start()].count("\n") + 1
        context_start = max(0, m.start() - 300)
        context = text[context_start:m.end() + 200]
        if not CLEAR_SEMANTICS.search(context):
            findings.append(
                f"L{line_no}: Canvas without clearAndSetSemantics — "
                "add a text alternative for screen readers "
                "(compose-accessibility.mdc Rule 3)"
            )

    return findings


def check_ios(text: str, lines: list[str]) -> list[str]:
    findings = []

    for m in IMAGE_NO_LABEL.finditer(text):
        line_no = text[:m.start()].count("\n") + 1
        after = text[m.end():m.end() + 500]
        next_view_boundary = re.search(r"\n\s*(?:var |func |struct )", after)
        scope = after[:next_view_boundary.start()] if next_view_boundary else after
        if not A11Y_LABEL.search(scope) and not DECORATIVE_IMAGE.search(scope):
            findings.append(
                f"L{line_no}: Image(systemName:) without .accessibilityLabel — "
                "add a label or .accessibilityHidden(true) for decorative images "
                "(swiftui-accessibility.mdc)"
            )

    return findings


def main() -> int:
    try:
        payload = json.load(sys.stdin)
    except (json.JSONDecodeError, ValueError):
        return 0
    file_path = (payload.get("tool_input", {}) or {}).get("file_path", "") or ""
    if not os.path.isfile(file_path):
        return 0
    if not is_android_ui(file_path) and not is_ios_ui(file_path):
        return 0

    try:
        with open(file_path, "r", encoding="utf-8") as fh:
            raw = fh.read()
    except (OSError, UnicodeDecodeError):
        return 0

    lines = raw.splitlines()
    findings = []
    if is_android_ui(file_path):
        findings = check_android(raw, lines)
    elif is_ios_ui(file_path):
        findings = check_ios(raw, lines)

    if not findings:
        return 0

    shown = findings[:MAX_REPORTED]
    extra = len(findings) - len(shown)
    name = os.path.basename(file_path)
    sys.stderr.write(
        f"Accessibility check for {name}:\n"
        + "\n".join(f"  {f}" for f in shown)
        + (f"\n  ... and {extra} more" if extra else "")
        + "\n"
    )
    return 2


if __name__ == "__main__":
    sys.exit(main())
