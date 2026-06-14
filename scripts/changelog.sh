#!/usr/bin/env bash
# Generates a categorized changelog section from commits between two git refs.
#
# Usage:
#   scripts/changelog.sh <from-ref> <to-ref> [version-label]
#
# Examples:
#   scripts/changelog.sh v9.11.0 v9.12.0           # between two tags
#   scripts/changelog.sh v9.11.0 HEAD v9.12.0       # unreleased changes with explicit label
#
# Output: markdown section to stdout (## version, ### Added, ### Fixed, etc.)

set -euo pipefail

if [ $# -lt 2 ]; then
  echo "Usage: $0 <from-ref> <to-ref> [version-label]" >&2
  exit 1
fi

FROM_REF="$1"
TO_REF="$2"
VERSION_LABEL="${3:-$TO_REF}"

added=()
fixed=()
changed=()
maintenance=()

while IFS= read -r line; do
  # Skip merge commits
  [[ "$line" =~ ^Merge\ pull\ request ]] && continue
  [[ "$line" =~ ^Merge\ branch ]] && continue

  # Strip PR number suffix like " (#123)"
  msg="${line% \(\#[0-9]*\)}"

  # Strip leading hash + space from --oneline format
  msg="${msg#* }"

  # Categorize by commit prefix
  case "$msg" in
    Add:*|Add\ *)
      entry="${msg#Add: }"
      entry="${entry#Add }"
      added+=("$entry")
      ;;
    Fix:*|Fix\ *)
      entry="${msg#Fix: }"
      entry="${entry#Fix }"
      fixed+=("$entry")
      ;;
    Update:*|Update\ *|Refactor:*|Refactor\ *)
      entry="${msg#Update: }"
      entry="${entry#Update }"
      entry="${entry#Refactor: }"
      entry="${entry#Refactor }"
      changed+=("$entry")
      ;;
    Chore:*|Chore\ *|Docs:*|Docs\ *|Test:*|Test\ *)
      entry="${msg#Chore: }"
      entry="${entry#Chore }"
      entry="${entry#Docs: }"
      entry="${entry#Docs }"
      entry="${entry#Test: }"
      entry="${entry#Test }"
      maintenance+=("$entry")
      ;;
    *)
      # Uncategorized commits go to maintenance
      maintenance+=("$msg")
      ;;
  esac
done < <(git log "${FROM_REF}..${TO_REF}" --oneline --no-merges)

# Deduplicate entries (preserving order)
dedup() {
  local -A seen
  for entry in "$@"; do
    if [[ -z "${seen[$entry]+x}" ]]; then
      seen[$entry]=1
      echo "- $entry"
    fi
  done
}

# Output
echo "## ${VERSION_LABEL}"
echo ""

if [ ${#added[@]} -gt 0 ]; then
  echo "### Added"
  dedup "${added[@]}"
  echo ""
fi

if [ ${#fixed[@]} -gt 0 ]; then
  echo "### Fixed"
  dedup "${fixed[@]}"
  echo ""
fi

if [ ${#changed[@]} -gt 0 ]; then
  echo "### Changed"
  dedup "${changed[@]}"
  echo ""
fi

if [ ${#maintenance[@]} -gt 0 ]; then
  echo "### Maintenance"
  dedup "${maintenance[@]}"
  echo ""
fi
