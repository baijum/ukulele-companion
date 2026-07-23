#!/bin/bash
# postToolUse sensor: run all post-edit feedback sensors.
# Mirrors the Claude Code PostToolUse hooks for Cursor.
# Runs style-check, ktlint-check, accessibility-check, and swift-style-check.

input=$(cat)

# Run each sensor; collect any feedback
feedback=""

for sensor in style-check.py ktlint-check.py accessibility-check.py swift-style-check.py; do
    script="scripts/hooks/$sensor"
    if [ -f "$script" ]; then
        result=$(echo "$input" | python3 -c "
import json, sys, os, subprocess

data = json.load(sys.stdin)
ti = data.get('toolInput', data.get('tool_input', {})) or {}

payload = {'tool_input': {
    'file_path': ti.get('file_path', ti.get('path', '')),
}}
for key in ('content', 'contents', 'new_string'):
    if ti.get(key):
        payload['tool_input'][key] = ti[key]

result = subprocess.run(
    ['python3', os.path.join(os.getcwd(), '$script')],
    input=json.dumps(payload),
    capture_output=True,
    text=True,
    timeout=30,
)
if result.returncode == 2 and result.stderr.strip():
    print(result.stderr.strip())
" 2>/dev/null)
        if [ -n "$result" ]; then
            feedback="$feedback$result
"
        fi
    fi
done

if [ -n "$feedback" ]; then
    # Escape for JSON
    escaped=$(echo "$feedback" | python3 -c "import json,sys; print(json.dumps(sys.stdin.read().strip()))" 2>/dev/null)
    echo "{\"additional_context\": $escaped}"
else
    echo '{}'
fi
exit 0
