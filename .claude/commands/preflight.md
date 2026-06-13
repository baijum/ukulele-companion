---
description: Run pre-submission checks — automated gates + manual checklist
allowed-tools: Bash(scripts/preflight.sh*), Bash(PREFLIGHT_SKIP_LINT=1 scripts/preflight.sh*)
---

Run `scripts/preflight.sh` and report the outcome.

- Summarize the automated gate results (ktlint ratchet, shared KMP tests, Android
  unit tests, Android lint). If any gate **failed**, show the relevant error output
  and propose the fix — do not declare the change ready.
- Then walk through the **manual checklist** the script prints. For each item, state
  whether it's relevant to the current change (based on what files were touched) and,
  where you can, verify it from the code; flag anything that needs the user to check
  on a device/simulator (TalkBack/VoiceOver, themes, High-G/Low-G, left-handed mode).
- Remember iOS build + VoiceOver are **not** covered by this script — call that out
  explicitly if iOS code changed.
