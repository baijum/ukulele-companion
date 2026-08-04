#!/usr/bin/env python3
"""PostToolUse sensor: run ktlint on the edited Kotlin file.

Provides in-loop formatting feedback so violations are caught immediately,
not deferred to preflight or CI. Only fires for .kt/.kts files. Uses the
project's standalone ktlint runner (scripts/ktlint.sh) on a single file.

Exit 2 surfaces findings to the agent so it can correct them; exit 0 when
clean or not a Kotlin file.
"""
import json
import os
import subprocess
import sys

EXTS = (".kt", ".kts")
MAX_LINES = 20
TIMEOUT_SECONDS = 30


def main() -> int:
    try:
        payload = json.load(sys.stdin)
    except (json.JSONDecodeError, ValueError):
        return 0
    file_path = (payload.get("tool_input", {}) or {}).get("file_path", "") or ""
    if not file_path.endswith(EXTS) or not os.path.isfile(file_path):
        return 0

    project_dir = os.environ.get("CLAUDE_PROJECT_DIR", "")
    ktlint_sh = os.path.join(project_dir, "scripts", "ktlint.sh") if project_dir else ""
    if not ktlint_sh or not os.path.isfile(ktlint_sh):
        ktlint_sh = os.path.join(
            os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "ktlint.sh"
        )
    if not os.path.isfile(ktlint_sh):
        return 0

    baseline = os.path.join(os.path.dirname(ktlint_sh), "..", "ktlint-baseline.xml")
    cmd = [ktlint_sh]
    if os.path.isfile(baseline):
        cmd.append(f"--baseline={baseline}")
    cmd.append(file_path)

    try:
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=TIMEOUT_SECONDS,
        )
    except (subprocess.TimeoutExpired, FileNotFoundError, OSError):
        return 0

    if result.returncode == 0:
        return 0

    output = (result.stdout or "") + (result.stderr or "")
    lines = [l for l in output.strip().splitlines() if l.strip()]
    if not lines:
        return 0

    shown = lines[:MAX_LINES]
    extra = len(lines) - len(shown)
    name = os.path.basename(file_path)
    sys.stderr.write(
        f"ktlint violations in {name} — please fix before moving on:\n"
        + "\n".join(f"  {l}" for l in shown)
        + (f"\n  ... and {extra} more" if extra else "")
        + "\n"
    )
    return 2


if __name__ == "__main__":
    sys.exit(main())
