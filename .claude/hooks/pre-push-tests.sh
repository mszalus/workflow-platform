#!/usr/bin/env bash
# PreToolUse hook for Bash. Blocks `git push` if the build or frontend unit tests fail. E2E runs in CI.

set -uo pipefail

payload=$(cat)
cmd=$(printf '%s' "$payload" | python3 -c 'import json,sys; d=json.load(sys.stdin); print((d.get("tool_input") or {}).get("command",""))' 2>/dev/null)
[ -z "$cmd" ] && exit 0

subcmd=$(printf '%s' "$cmd" | bash "$CLAUDE_PROJECT_DIR/.claude/hooks/guard-git-subcommand.sh")
[ "$subcmd" != "push" ] && exit 0

# Skip --dry-run
printf '%s' "$cmd" | grep -Eq '(^| )--dry-run( |$)' && exit 0

[ -n "${JAVA_HOME:-}" ] && export PATH="$JAVA_HOME/bin:$PATH"
cwd=$(printf '%s' "$payload" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("cwd",""))' 2>/dev/null)
repo=$(git -C "${cwd:-$CLAUDE_PROJECT_DIR}" rev-parse --show-toplevel 2>/dev/null)
cd "$repo" || {
  echo '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"Pre-push hook: could not find the git repository of the session cwd"}}'
  exit 0
}

changed=$(git diff --name-only origin/main...HEAD 2>/dev/null)
if [ -n "$changed" ] && ! printf '%s\n' "$changed" | grep -Evq '^(docs/|\.claude/)|\.md$'; then
  echo '{"systemMessage":"Pre-push suite skipped: only *.md, docs/ or .claude/ files differ from origin/main."}'
  exit 0
fi

log=$(mktemp) && chmod 600 "$log"
trap 'rm -f "$log"' EXIT

(
  echo "== pre-push: backend ./gradlew build =="
  ./gradlew build --console=plain
  backend=$?
  echo "== pre-push: frontend unit tests =="
  ( cd frontend && npm run test --workspaces --if-present )
  frontend=$?
  echo "== backend=$backend frontend=$frontend =="
  exit $(( backend || frontend ))
) >"$log" 2>&1
status=$?

if [ "$status" -ne 0 ]; then
  python3 - "$log" <<'PY'
import json, sys
log = open(sys.argv[1], encoding='utf-8', errors='replace').read()
tail = log[-6000:]
print(json.dumps({
  "hookSpecificOutput": {
    "hookEventName": "PreToolUse",
    "permissionDecision": "deny",
    "permissionDecisionReason": "Pre-push suite failed.\nTail of output:\n" + tail
  },
  "systemMessage": "Pre-push suite failed — push blocked."
}))
PY
  exit 0
fi

echo '{"systemMessage":"Pre-push suite (backend build + frontend unit tests) passed."}'
