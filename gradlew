#!/bin/sh
set -eu

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

cat >&2 <<'MSG'
Gradle is not installed on this system.
Please install Gradle 8.5 or newer and re-run this command.
MSG
exit 1
