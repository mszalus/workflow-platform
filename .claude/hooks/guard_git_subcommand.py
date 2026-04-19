#!/usr/bin/env python3
"""Extract a git subcommand from an arbitrary bash command string.

Reads the command from stdin. Echoes the first git subcommand found in any
segment of the chain (e.g. "commit", "push"), or nothing if no git invocation
is present.

Handles:
  - plain:            git commit -m msg
  - env-prefix:       FOO=bar BAZ="a b" git commit
  - absolute path:    /usr/bin/git commit
  - chained/piped:    cd foo && git commit / x; git push / a | git log
  - git options:      git -c k=v commit / git -C path push / git --git-dir=x log
  - shell delegation: bash -c "git commit"  (inspects the quoted argument)
"""

import re
import shlex
import sys

GIT_OPTS_WITH_ARG = {"-c", "-C", "--git-dir", "--work-tree", "--namespace",
                     "--exec-path", "--config-env"}
GIT_OPTS_FLAG = {"--literal-pathspecs", "--no-optional-locks",
                 "--no-replace-objects", "--bare", "--paginate", "--no-pager",
                 "--help", "--version", "-h", "-v", "--glob-pathspecs",
                 "--noglob-pathspecs", "--icase-pathspecs"}

ENV_ASSIGN = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*=")


def _is_git(token):
    base = token.rsplit("/", 1)[-1].rsplit("\\", 1)[-1].lower()
    return base in {"git", "git.exe"}


def _extract_subcommand(tokens):
    i = 0
    while i < len(tokens) and ENV_ASSIGN.match(tokens[i]):
        i += 1
    if i >= len(tokens) or not _is_git(tokens[i]):
        return None
    i += 1
    while i < len(tokens):
        tok = tokens[i]
        if tok in GIT_OPTS_WITH_ARG:
            i += 2
            continue
        if tok in GIT_OPTS_FLAG:
            i += 1
            continue
        if "=" in tok and tok.split("=", 1)[0] in GIT_OPTS_WITH_ARG | GIT_OPTS_FLAG:
            i += 1
            continue
        if tok.startswith("-"):
            i += 1
            continue
        return tok
    return None


def _split_on_operators(tokens):
    segments, current = [], []
    for t in tokens:
        if t in {"&&", "||", ";", "|", "&", "(", ")"}:
            if current:
                segments.append(current)
                current = []
        else:
            current.append(t)
    if current:
        segments.append(current)
    return segments


def find_subcommand(cmd):
    try:
        lexer = shlex.shlex(cmd, posix=True, punctuation_chars=True)
        lexer.whitespace_split = True
        tokens = list(lexer)
    except ValueError:
        tokens = cmd.split()
    for segment in _split_on_operators(tokens):
        if (len(segment) >= 3 and segment[0] in {"bash", "sh", "zsh"}
                and segment[1] == "-c"):
            inner = find_subcommand(segment[2])
            if inner:
                return inner
            continue
        sub = _extract_subcommand(segment)
        if sub:
            return sub
    return None


if __name__ == "__main__":
    sub = find_subcommand(sys.stdin.read())
    if sub:
        print(sub)
