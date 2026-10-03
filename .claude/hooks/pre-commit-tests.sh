#!/usr/bin/env bash
# PreToolUse hook for Bash. Blocks `git commit` if unit tests fail.

set -uo pipefail

payload=$(cat)
cmd=$(printf '%s' "$payload" | python3 -c 'import json,sys; d=json.load(sys.stdin); print((d.get("tool_input") or {}).get("command",""))' 2>/dev/null)
[ -z "$cmd" ] && exit 0

subcmd=$(printf '%s' "$cmd" | bash "$CLAUDE_PROJECT_DIR/.claude/hooks/guard-git-subcommand.sh")
[ "$subcmd" != "commit" ] && exit 0

# Skip --dry-run
printf '%s' "$cmd" | grep -Eq '(^| )--dry-run( |$)' && exit 0

[ -n "${JAVA_HOME:-}" ] && export PATH="$JAVA_HOME/bin:$PATH"
cwd=$(printf '%s' "$payload" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("cwd",""))' 2>/dev/null)
repo=$(git -C "${cwd:-$CLAUDE_PROJECT_DIR}" rev-parse --show-toplevel 2>/dev/null)
cd "$repo" || {
  echo '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"Pre-commit hook: could not find the git repository of the session cwd"}}'
  exit 0
}

changed=$( { git diff --name-only HEAD; git ls-files --others --exclude-standard; } )
if [ -n "$changed" ] && ! printf '%s\n' "$changed" | grep -Evq '^(docs/|\.claude/)|\.md$'; then
  echo '{"systemMessage":"Pre-commit tests skipped: only *.md, docs/ or .claude/ files changed."}'
  exit 0
fi

log=$(mktemp) && chmod 600 "$log"
trap 'rm -f "$log"' EXIT

(
  echo "== pre-commit: backend ./gradlew test =="
  ./gradlew test --console=plain
  backend=$?
  echo "== pre-commit: frontend unit tests =="
  if [ -d frontend/node_modules ]; then
    ( cd frontend && npm test )
  else
    echo "frontend unit tests skipped: run npm ci in frontend/ first"
  fi
  frontend=$?
  echo "== backend=$backend frontend=$frontend =="
  exit $(( backend || frontend ))
) >"$log" 2>&1
status=$?

if [ "$status" -ne 0 ]; then
  python3 - "$log" <<'PY'
import json, sys
log = open(sys.argv[1], encoding='utf-8', errors='replace').read()
tail = log[-4000:]
print(json.dumps({
  "hookSpecificOutput": {
    "hookEventName": "PreToolUse",
    "permissionDecision": "deny",
    "permissionDecisionReason": "Pre-commit tests failed.\nTail of output:\n" + tail
  },
  "systemMessage": "Pre-commit tests failed — commit blocked."
}))
PY
  exit 0
fi

echo '{"systemMessage":"Pre-commit tests passed."}'
