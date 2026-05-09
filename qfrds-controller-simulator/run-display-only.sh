#!/usr/bin/env bash
# Passenger display only — no engineering window, no RS232 (demo data on screen).
# Usage:
#   ./run-display-only.sh       → UTS layout
#   ./run-display-only.sh prs   → PRS layout
set -euo pipefail
cd "$(dirname "$0")"
if [[ "${1:-}" == "prs" ]]; then
  export QFRDS_PREVIEW=prs
fi
exec mvn -U -Ppassenger-display-only org.openjfx:javafx-maven-plugin:0.0.8:run
