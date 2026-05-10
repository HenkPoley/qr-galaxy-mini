#!/bin/sh
set -eu

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
BUILD_TOOLS="$SDK/build-tools/25.0.0"
PLATFORM="$SDK/platforms/android-10/android.jar"
JDK7="/Library/Java/JavaVirtualMachines/jdk1.7.0_11.jdk/Contents/Home"
JAVAC="${JAVAC:-javac}"
KEYTOOL="${KEYTOOL:-keytool}"
JARSIGNER="${JARSIGNER:-jarsigner}"
if [ -x "$JDK7/bin/javac" ]; then
  JAVAC="$JDK7/bin/javac"
fi
if [ -x "$JDK7/bin/keytool" ]; then
  KEYTOOL="$JDK7/bin/keytool"
fi
if [ -x "$JDK7/bin/jarsigner" ]; then
  JARSIGNER="$JDK7/bin/jarsigner"
fi
OUT="build"
APK_UNSIGNED="$OUT/qr-galaxy-unsigned.apk"
APK_ALIGNED="$OUT/qr-galaxy-aligned.apk"
APK_FINAL="$OUT/qr-galaxy-debug.apk"
KEYSTORE="debug.keystore"
PROGUARD="$SDK/tools/proguard/bin/proguard.sh"

rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/dex" "$OUT/apk" "$OUT/res" "$OUT/proguard"

"$BUILD_TOOLS/aapt" package -f -m \
  -J "$OUT" \
  -M AndroidManifest.xml \
  -S res \
  -I "$PLATFORM"

"$JAVAC" -source 7 -target 7 \
  -bootclasspath "$PLATFORM" \
  -classpath "app/libs/zxing-core-3.3.3.jar" \
  -d "$OUT/classes" \
  $(find src "$OUT/com" -name '*.java')

if [ -x "$PROGUARD" ]; then
  "$PROGUARD" @proguard.pro
  "$BUILD_TOOLS/dx" --dex --output="$OUT/classes.dex" "$OUT/proguard/classes.jar"
else
  "$BUILD_TOOLS/dx" --dex --output="$OUT/classes.dex" \
    "$OUT/classes" "app/libs/zxing-core-3.3.3.jar"
fi

"$BUILD_TOOLS/aapt" package -f \
  -M AndroidManifest.xml \
  -S res \
  -I "$PLATFORM" \
  -F "$APK_UNSIGNED"

cp "$OUT/classes.dex" "$OUT/dex/classes.dex"
(cd "$OUT/dex" && "$BUILD_TOOLS/aapt" add "../qr-galaxy-unsigned.apk" classes.dex >/dev/null)

if [ ! -f "$KEYSTORE" ]; then
  "$KEYTOOL" -genkeypair -v \
    -keystore "$KEYSTORE" \
    -storepass android \
    -alias androiddebugkey \
    -keypass android \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US"
fi

"$BUILD_TOOLS/zipalign" -f 4 "$APK_UNSIGNED" "$APK_ALIGNED"
"$JARSIGNER" -keystore "$KEYSTORE" \
  -storepass android \
  -keypass android \
  -sigalg SHA1withRSA \
  -digestalg SHA1 \
  "$APK_ALIGNED" androiddebugkey

cp "$APK_ALIGNED" "$APK_FINAL"
echo "$APK_FINAL"
