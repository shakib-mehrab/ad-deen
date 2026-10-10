#!/usr/bin/env bash
# Regenerates every Android icon from assets/logo/ad-deen-logo.svg. See docs/LOGO.md.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/icons"
[ -d node_modules ] || npm install --no-audit --no-fund
npm run icons
