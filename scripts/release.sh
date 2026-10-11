#!/bin/bash
# Builds the signed release APK (ARCHITECTURE §13) and installs it on a phone connected over USB.
# Usage: scripts/release.sh [path/to/keystore.jks] [alias]
# The password is asked here, never stored, and only passed to Gradle for this one build.
set -euo pipefail
cd "$(dirname "$0")/.."

export PEYKAR_KEYSTORE="${1:-$HOME/peykar-release.jks}"
export PEYKAR_KEY_ALIAS="${2:-peykar}"
[ -f "$PEYKAR_KEYSTORE" ] || { echo "Keystore not found: $PEYKAR_KEYSTORE"; exit 1; }

read -r -s -p "Keystore password: " PEYKAR_STORE_PASSWORD; echo
export PEYKAR_STORE_PASSWORD
export PEYKAR_KEY_PASSWORD="$PEYKAR_STORE_PASSWORD"   # keytool's default PKCS12 store uses one password

export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
# No configuration cache here: it would store the build's inputs, passwords included, on disk.
./gradlew :app:assembleRelease --no-configuration-cache --console=plain -q

APK=app/build/outputs/apk/release/app-release.apk
[ -f "$APK" ] || { echo "No signed APK: is the password right?"; exit 1; }
"$JAVA_HOME/bin/keytool" -printcert -jarfile "$APK" | grep -m1 "SHA256" || true
echo "Signed: $APK"

ADB="$HOME/Library/Android/sdk/platform-tools/adb"
PHONE=$("$ADB" devices | awk 'NR>1 && $2=="device" && $1 !~ /^emulator-/ {print $1; exit}')
if [ -n "$PHONE" ]; then
  echo "Installing on $PHONE…"
  "$ADB" -s "$PHONE" install -r "$APK"
  "$ADB" -s "$PHONE" shell am start -n app.beaesthetic/.MainActivity >/dev/null
  echo "Installed and opened «پیکر»."
else
  echo "No phone over USB. Connect it with USB debugging on and run: adb install -r $APK"
fi
