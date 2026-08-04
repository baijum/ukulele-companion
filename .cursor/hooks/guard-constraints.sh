#!/bin/bash
# preToolUse guard: block edits introducing network, analytics, or secrets.
# Mirrors scripts/hooks/guard-constraints.py for Claude Code.
# Delegates to the same Python script for pattern matching.

input=$(cat)

# Reformat Cursor hook JSON to Claude Code hook format and delegate
echo "$input" | python3 -c "
import json, sys, os

data = json.load(sys.stdin)
ti = data.get('toolInput', data.get('tool_input', {})) or {}

# Normalize to Claude Code hook format.
# For targeted edits (old_string/new_string present, e.g. StrReplace), scan only
# the diff being added -- Cursor's toolInput may also include a 'content'/'contents'
# key holding the *entire* resulting file, which would cause unrelated pre-existing
# text elsewhere in the file to trigger false positives. Only fall back to
# 'content'/'contents' for whole-file writes (no old_string).
payload = {'tool_input': {
    'file_path': ti.get('file_path', ti.get('path', '')),
}}
is_targeted_edit = bool(ti.get('old_string')) or bool(ti.get('edits'))
keys = ('new_string',) if is_targeted_edit else ('content', 'contents', 'new_string')
for key in keys:
    if ti.get(key):
        payload['tool_input'][key] = ti[key]
if ti.get('edits'):
    payload['tool_input']['edits'] = ti['edits']

# Write to the guard script's stdin
import subprocess
project_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath('$0'))))
# Find guard-constraints.py relative to this script
script_dir = os.path.dirname(os.path.realpath('__file__'))
guard_py = os.path.join(os.getcwd(), 'scripts', 'hooks', 'guard-constraints.py')
if not os.path.isfile(guard_py):
    sys.exit(0)

result = subprocess.run(
    ['python3', guard_py],
    input=json.dumps(payload),
    capture_output=True,
    text=True,
    timeout=5,
)
if result.returncode == 2:
    msg = result.stderr.strip()
    out = {
        'permission': 'deny',
        'user_message': msg,
        'agent_message': msg,
    }
    json.dump(out, sys.stdout)
    sys.exit(0)
elif result.returncode == 0:
    json.dump({'permission': 'allow'}, sys.stdout)
    sys.exit(0)
else:
    sys.exit(0)
" 2>/dev/null

# If Python failed, fail open
if [ $? -ne 0 ]; then
    echo '{ "permission": "allow" }'
fi
exit 0
