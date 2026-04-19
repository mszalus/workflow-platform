#!/usr/bin/env bash
# Extracts the git subcommand from a bash command string.
# Echoes the subcommand (e.g. "commit", "push", "log") or nothing if not a git invocation.
# Handles: git X, git -c k=v X, git --git-dir=... X, git -C path X
# Ignores subsequent commands in pipes/chains — checks the FIRST top-level command only.
# Reads the command from stdin.

cmd=$(cat)
# Trim leading whitespace
cmd="${cmd#"${cmd%%[![:space:]]*}"}"

# Strip any leading env-var assignments (VAR=val git ...)
while [[ "$cmd" =~ ^[A-Za-z_][A-Za-z0-9_]*= ]]; do
  cmd="${cmd#* }"
done

# If the first word isn't "git", not a git command.
read -r first rest <<<"$cmd"
if [ "$first" != "git" ]; then
  exit 0
fi

# Walk past git's own options (-c, -C, --git-dir, etc.) to find the subcommand.
set -- $rest
while [ $# -gt 0 ]; do
  case "$1" in
    -c)               shift 2 ;;
    -C)               shift 2 ;;
    --git-dir|--work-tree|--namespace|--exec-path|--config-env)
                      shift 2 ;;
    -c=*|-C=*|--git-dir=*|--work-tree=*|--namespace=*|--exec-path=*|--config-env=*|--literal-pathspecs|--no-optional-locks|--no-replace-objects|--bare|--paginate|--no-pager|--help|--version|-h|-v|--glob-pathspecs|--noglob-pathspecs|--icase-pathspecs)
                      shift ;;
    -*)               shift ;;  # unknown flag — skip
    *)                echo "$1"; exit 0 ;;
  esac
done
