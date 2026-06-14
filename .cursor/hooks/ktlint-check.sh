#!/bin/bash
# Post-edit hook: runs ktlint on the edited file if it's a .kt file.
# Returns additional_context so the agent sees lint errors immediately.
set -euo pipefail

input=$(cat)
file_path=$(echo "$input" | python3 -c "import sys,json; print(json.load(sys.stdin).get('path',''))" 2>/dev/null || true)

# Only check Kotlin files
if [[ "$file_path" != *.kt ]]; then
    exit 0
fi

# Skip if ktlint isn't available
if [[ ! -f "scripts/ktlint.sh" ]]; then
    exit 0
fi

# Run ktlint on just this file (with baseline)
output=$(scripts/ktlint.sh --baseline=ktlint-baseline.xml "$file_path" 2>&1 || true)

if [[ -n "$output" && "$output" == *".kt:"* ]]; then
    # Feed lint errors back as context for the agent
    cat <<EOF
{
  "additional_context": "ktlint violations in $file_path:\n$output\nPlease fix these formatting issues."
}
EOF
fi

exit 0
