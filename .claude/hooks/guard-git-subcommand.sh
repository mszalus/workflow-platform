#!/usr/bin/env bash
# Thin wrapper: delegate parsing to the Python sibling so shell-quoting doesn't
# trip us up on cases like `FOO="a b" git commit`, `bash -c "git commit"`, or
# chained commands. Reads the bash command from stdin.

here="$(dirname "${BASH_SOURCE[0]}")"
exec python3 "$here/guard_git_subcommand.py"
