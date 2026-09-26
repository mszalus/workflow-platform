#!/usr/bin/env bash
# PreToolUse hook for Bash. Blocks `git push` if the full suite fails.

set -uo pipefail

payload=$(cat)
cmd=$(printf '%s' "$payload" | python3 -c 'import json,sys; d=json.load(sys.stdin); print((d.get("tool_input") or {}).get("command",""))' 2>/dev/null)
[ -z "$cmd" ] && exit 0

subcmd=$(printf '%s' "$cmd" | bash "$CLAUDE_PROJECT_DIR/.claude/hooks/guard-git-subcommand.sh")
[ "$subcmd" != "push" ] && exit 0

# Skip --dry-run
printf '%s' "$cmd" | grep -Eq '(^| )--dry-run( |$)' && exit 0

[ -n "${JAVA_HOME:-}" ] && export PATH="$JAVA_HOME/bin:$PATH"
cd "$CLAUDE_PROJECT_DIR" || {
  echo '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"Pre-push hook: could not cd to $CLAUDE_PROJECT_DIR"}}'
  exit 0
}

log=$(mktemp) && chmod 600 "$log"
trap 'rm -f "$log"' EXIT

{
  echo "== pre-push: backend ./gradlew build =="
  ./gradlew build --console=plain
  backend=$?
  echo "== pre-push: frontend unit tests =="
  ( cd frontend && npm run test --workspaces --if-present )
  frontend=$?
  echo "== pre-push: Playwright E2E =="
  if docker compose -f docker/docker-compose.yml ps --status=running --format '{{.Name}}' 2>/dev/null | grep -q wfp-gateway; then
    ( cd e2e && npm test )
    e2e=$?
  else
    echo "Docker stack not running — starting it for E2E..."
    docker compose -f docker/docker-compose.yml up -d
    timeout=120
    gateway_ready=0
    while [ $timeout -gt 0 ]; do
      if curl -sf http://localhost:9080/actuator/health >/dev/null 2>&1; then
        gateway_ready=1
        break
      fi
      sleep 3; timeout=$((timeout-3))
    done
    if [ $gateway_ready -ne 1 ]; then
      echo "ERROR: gateway never became healthy within 120s — skipping E2E."
      e2e=1
    else
      ( cd e2e && npm test )
      e2e=$?
    fi
  fi
  echo "== backend=$backend frontend=$frontend e2e=$e2e =="
  exit $(( backend || frontend || e2e ))
} >"$log" 2>&1
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

echo '{"systemMessage":"Pre-push suite (backend + frontend + Playwright) passed."}'
