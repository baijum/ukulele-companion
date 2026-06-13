#!/usr/bin/env python3
"""PreToolUse guard: keep the KMP shared commonMain source pure.

Code in shared/src/commonMain must not import platform APIs. Platform needs go
through expect/actual in the platform/ package (see .cursor/rules/shared-module.mdc).

Blocks (exit 2) when an edit/write to commonMain adds a forbidden import; exits 0
otherwise, including for files outside commonMain.
"""
import json
import re
import sys

COMMON_MAIN = "shared/src/commonMain/"

# Imports that pull in a specific platform and are therefore illegal in commonMain.
FORBIDDEN_IMPORT = re.compile(
    r"^\s*import\s+(java|javax|android|androidx|platform|kotlinx\.cinterop|co\.touchlab)\.",
    re.MULTILINE,
)


def added_text(tool_input: dict) -> str:
    parts = []
    if isinstance(tool_input.get("content"), str):
        parts.append(tool_input["content"])
    if isinstance(tool_input.get("new_string"), str):
        parts.append(tool_input["new_string"])
    for edit in tool_input.get("edits", []) or []:
        if isinstance(edit, dict) and isinstance(edit.get("new_string"), str):
            parts.append(edit["new_string"])
    return "\n".join(parts)


def main() -> int:
    try:
        payload = json.load(sys.stdin)
    except (json.JSONDecodeError, ValueError):
        return 0
    tool_input = payload.get("tool_input", {}) or {}
    file_path = tool_input.get("file_path", "") or ""

    if COMMON_MAIN not in file_path or not file_path.endswith(".kt"):
        return 0

    offending = sorted(set(FORBIDDEN_IMPORT.findall(added_text(tool_input))))
    if offending:
        pkgs = ", ".join(f"{p}.*" for p in offending)
        sys.stderr.write(
            f"BLOCKED: platform import(s) in commonMain ({pkgs}).\n"
            "shared/src/commonMain must stay pure Kotlin. Move platform-specific code "
            "behind an expect/actual declaration in the platform/ package, or implement "
            "it in androidMain / iosMain. See .cursor/rules/shared-module.mdc.\n"
        )
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
