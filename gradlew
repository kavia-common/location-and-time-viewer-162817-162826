#!/usr/bin/env bash
# Proxy gradle wrapper to the android_frontend container's wrapper to satisfy CI environments
# that invoke ./gradlew from the workspace root.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="${SCRIPT_DIR}/android_frontend"

if [[ ! -x "${FRONTEND_DIR}/gradlew" ]]; then
  echo "Error: gradle wrapper not found at ${FRONTEND_DIR}/gradlew" >&2
  exit 127
fi

exec "${FRONTEND_DIR}/gradlew" "$@"
