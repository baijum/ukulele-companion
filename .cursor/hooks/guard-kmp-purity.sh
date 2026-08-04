#!/bin/bash
# preToolUse guard: block platform imports in KMP commonMain.
# Mirrors scripts/hooks/guard-kmp-purity.py for Claude Code.
# Delegates to the same Python script for pattern matching.

input=$(cat)

echo "$input" | python3 -c "
import json, sys, os, subprocess

data = json.load(sys.stdin)
ti = data.get('toolInput', data.get('tool_input', {})) or {}

payload = {'tool_input': {
    'file_path': ti.get('file_path', ti.get('path', '')),
}}
for key in ('content', 'contents', 'new_string'):
    if ti.get(key):
        payload['tool_input'][key] = ti[key]
if ti.get('edits'):
    payload['tool_input']['edits'] = ti['edits']

guard_py = os.path.join(os.getcwd(), 'scripts', 'hooks', 'guard-kmp-purity.py')
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

if [ $? -ne 0 ]; then
    echo '{ "permission": "allow" }'
fi
exit 0
