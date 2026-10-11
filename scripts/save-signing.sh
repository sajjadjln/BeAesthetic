#!/bin/bash
# One-time setup: saves the release keystore's location and password in keystore.properties
# (git-ignored, readable only by you), so release builds don't ask again.
# Usage: scripts/save-signing.sh [path/to/keystore.jks] [alias]
set -euo pipefail
cd "$(dirname "$0")/.."

STORE="${1:-$HOME/peykar-release.jks}"
ALIAS="${2:-peykar}"
[ -f "$STORE" ] || { echo "Keystore not found: $STORE"; exit 1; }
STORE="$(cd "$(dirname "$STORE")" && pwd)/$(basename "$STORE")"

read -r -s -p "Keystore password: " PASS; echo
export PASS
# Check the password before saving it.
JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
if ! "$JAVA_HOME/bin/keytool" -list -keystore "$STORE" -alias "$ALIAS" -storepass:env PASS >/dev/null 2>&1; then
  echo "Wrong password or alias ($ALIAS). Nothing saved."; exit 1
fi

umask 077
{
  echo "storeFile=$STORE"
  echo "keyAlias=$ALIAS"
  printf 'storePassword=%s\n' "$PASS"
  printf 'keyPassword=%s\n' "$PASS"
} > keystore.properties
chmod 600 keystore.properties
git check-ignore -q keystore.properties && echo "Saved to keystore.properties (git-ignored, only you can read it)."
