#!/bin/sh
set -eu

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="${ADB:-}"
if [ -z "$ADB" ]; then
  if command -v adb >/dev/null 2>&1; then
    ADB="$(command -v adb)"
  else
    ADB="$SDK/platform-tools/adb"
  fi
fi
APK="${1:-build/qr-galaxy-debug.apk}"

if [ ! -f "$APK" ]; then
  echo "Missing APK: $APK" >&2
  echo "Run ./build.sh first." >&2
  exit 1
fi

"$ADB" devices
"$ADB" install -r "$APK"
