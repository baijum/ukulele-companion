#!/usr/bin/env python3
"""PreToolUse guard: enforce the project's hard constraints (AGENTS.md).

Blocks edits/writes that would introduce network access, analytics/tracking,
or committed secrets. The app must remain fully offline with no telemetry.

Reads the Claude Code hook payload from stdin. Exits 2 (blocking) with an
explanation on stderr when a forbidden pattern appears in *added* content;
exits 0 otherwise.
"""
import json
import re
import sys

# (label, compiled regex). Patterns target dependency coordinates and import
# statements specifically, to keep false positives low (a doc/comment that
# merely mentions a word will not match a coordinate or `import` line).
NETWORK = [
    (r"\bimport\s+okhttp3", "OkHttp"),
    (r"\bimport\s+retrofit2", "Retrofit"),
    (r"\bimport\s+io\.ktor\.client", "Ktor client"),
    (r"\bimport\s+java\.net\.(HttpURLConnection|HttpsURLConnection)", "java.net HTTP"),
    (r"com\.squareup\.okhttp3?[:\"]", "OkHttp dependency"),
    (r"com\.squareup\.retrofit2[:\"]", "Retrofit dependency"),
    (r"io\.ktor:ktor-client", "Ktor client dependency"),
    (r"\bAlamofire\b", "Alamofire (iOS networking)"),
    (r"\bURLSession\b", "URLSession (iOS networking)"),
]
ANALYTICS = [
    (r"com\.google\.firebase[:.]", "Firebase"),
    (r"\bFirebaseAnalytics\b", "Firebase Analytics"),
    (r"\bCrashlytics\b", "Crashlytics"),
    (r"com\.google\.android\.gms[:.]analytics", "GMS Analytics"),
    (r"com\.mixpanel", "Mixpanel"),
    (r"com\.amplitude", "Amplitude"),
    (r"com\.appsflyer", "AppsFlyer"),
    (r"com\.segment", "Segment"),
    (r"com\.adjust\.sdk", "Adjust"),
]
SECRETS = [
    (r"-----BEGIN [A-Z ]*PRIVATE KEY-----", "private key material"),
    (r"^\s*storePassword\s*=", "keystore password"),
    (r"^\s*keyPassword\s*=", "signing key password"),
]
# Files that must never be written through an agent edit at all.
FORBIDDEN_PATHS = (
    "keystore.properties",
    "play-service-account.json",
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


def scan(text: str, groups):
    hits = []
    for label, patterns in groups:
        for pat, name in patterns:
            if re.search(pat, text, re.MULTILINE | re.IGNORECASE):
                hits.append(f"{label}: {name}")
    return hits


def main() -> int:
    try:
        payload = json.load(sys.stdin)
    except (json.JSONDecodeError, ValueError):
        return 0  # not our concern; let the tool proceed
    tool_input = payload.get("tool_input", {}) or {}
    file_path = tool_input.get("file_path", "") or ""

    for forbidden in FORBIDDEN_PATHS:
        if file_path.endswith(forbidden):
            sys.stderr.write(
                f"BLOCKED: refusing to write '{forbidden}'. This file holds secrets "
                f"and must be managed by hand, never committed (see AGENTS.md).\n"
            )
            return 2
    if file_path.endswith(".jks"):
        sys.stderr.write("BLOCKED: refusing to write a keystore (*.jks) through an edit.\n")
        return 2

    text = added_text(tool_input)
    if not text.strip():
        return 0

    hits = scan(
        text,
        [("network", NETWORK), ("analytics/tracking", ANALYTICS), ("secret", SECRETS)],
    )
    if hits:
        sys.stderr.write(
            "BLOCKED by project hard constraints (AGENTS.md): this change appears to "
            "introduce " + "; ".join(hits) + ".\n"
            "Ukulele Companion is fully offline with no analytics, tracking, or "
            "committed secrets. If this is a false positive, ask the user to confirm "
            "and adjust scripts/hooks/guard-constraints.py.\n"
        )
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
