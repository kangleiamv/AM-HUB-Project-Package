#!/bin/bash
set -e

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$APP_DIR/build"
KEYSTORE="$APP_DIR/debug.keystore"

# Locate Android SDK
SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/lib/android-sdk}}"
if [ ! -d "$SDK_ROOT" ] && [ -d "/usr/local/lib/android/sdk" ]; then
    SDK_ROOT="/usr/local/lib/android/sdk"
fi

# Locate android.jar
ANDROID_JAR=""
for platform in android-34 android-33 android-32 android-31 android-30; do
    if [ -f "$SDK_ROOT/platforms/$platform/android.jar" ]; then
        ANDROID_JAR="$SDK_ROOT/platforms/$platform/android.jar"
        break
    elif [ -f "/usr/lib/android-sdk/platforms/$platform/android.jar" ]; then
        ANDROID_JAR="/usr/lib/android-sdk/platforms/$platform/android.jar"
        break
    fi
done

if [ -z "$ANDROID_JAR" ] || [ ! -f "$ANDROID_JAR" ]; then
    echo "❌ Error: Could not locate android.jar in $SDK_ROOT/platforms"
    exit 1
fi

# Locate build-tools
BUILD_TOOLS_DIR=""
if [ -d "$SDK_ROOT/build-tools" ]; then
    LATEST_BT=$(ls -1v "$SDK_ROOT/build-tools" | grep -E '^[0-9]' | tail -n 1)
    if [ -n "$LATEST_BT" ]; then
        BUILD_TOOLS_DIR="$SDK_ROOT/build-tools/$LATEST_BT"
    fi
fi
if [ -z "$BUILD_TOOLS_DIR" ] && [ -d "/usr/lib/android-sdk/build-tools" ]; then
    LATEST_BT=$(ls -1v "/usr/lib/android-sdk/build-tools" | grep -E '^[0-9]' | tail -n 1)
    BUILD_TOOLS_DIR="/usr/lib/android-sdk/build-tools/$LATEST_BT"
fi

# Locate D8, AAPT2, ZIPALIGN, APKSIGNER
D8_CMD=""
if [ -f "$BUILD_TOOLS_DIR/lib/d8.jar" ]; then
    D8_CMD="java -cp $BUILD_TOOLS_DIR/lib/d8.jar com.android.tools.r8.D8"
elif command -v d8 >/dev/null 2>&1; then
    D8_CMD="d8"
elif [ -f "$BUILD_TOOLS_DIR/d8" ]; then
    D8_CMD="$BUILD_TOOLS_DIR/d8"
else
    echo "❌ Error: Could not locate d8 tool"
    exit 1
fi

AAPT2_CMD="$(command -v aapt2 2>/dev/null || echo "$BUILD_TOOLS_DIR/aapt2")"
ZIPALIGN_CMD="$(command -v zipalign 2>/dev/null || echo "$BUILD_TOOLS_DIR/zipalign")"
APKSIGNER_CMD="$(command -v apksigner 2>/dev/null || echo "$BUILD_TOOLS_DIR/apksigner")"

echo "=== Android Build Configuration ==="
echo "APP_DIR:       $APP_DIR"
echo "SDK_ROOT:      $SDK_ROOT"
echo "ANDROID_JAR:   $ANDROID_JAR"
echo "BUILD_TOOLS:   $BUILD_TOOLS_DIR"
echo "==================================="

echo "=== 1. Cleaning build directory ==="
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/gen" "$BUILD_DIR/compiled_res" "$BUILD_DIR/obj" "$BUILD_DIR/apk"

echo "=== 2. Compiling Android Resources with aapt2 ==="
"$AAPT2_CMD" compile --dir "$APP_DIR/res" -o "$BUILD_DIR/compiled_res.zip"

echo "=== 3. Linking Resources and Generating R.java ==="
"$AAPT2_CMD" link \
    -I "$ANDROID_JAR" \
    --manifest "$APP_DIR/AndroidManifest.xml" \
    --java "$BUILD_DIR/gen" \
    -o "$BUILD_DIR/apk/res.apk" \
    --auto-add-overlay \
    "$BUILD_DIR/compiled_res.zip"

echo "=== 4. Compiling Java Source Files ==="
find "$APP_DIR/src" "$BUILD_DIR/gen" -name "*.java" > "$BUILD_DIR/sources.txt"
javac -encoding UTF-8 \
    -cp "$ANDROID_JAR" \
    -d "$BUILD_DIR/obj" \
    --release 8 \
    @"$BUILD_DIR/sources.txt"

echo "=== 5. Converting Bytecode to Dex with D8 ==="
find "$BUILD_DIR/obj" -name "*.class" > "$BUILD_DIR/classes.txt"
$D8_CMD \
    --release \
    --min-api 21 \
    --output "$BUILD_DIR/apk" \
    --lib "$ANDROID_JAR" \
    @"$BUILD_DIR/classes.txt"

echo "=== 6. Packaging APK ==="
cd "$BUILD_DIR/apk"
zip -uj res.apk classes.dex
cd "$APP_DIR"

echo "=== 7. Zipalign APK ==="
"$ZIPALIGN_CMD" -f -p 4 "$BUILD_DIR/apk/res.apk" "$BUILD_DIR/AlightMotionHub-unaligned.apk"

echo "=== 8. Creating/Checking Signing Key ==="
if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair \
        -keystore "$KEYSTORE" \
        -alias androiddebugkey \
        -keypass android \
        -storepass android \
        -dname "CN=CMAISNAM, OU=Mobile, O=AMHub, L=City, S=State, C=US" \
        -validity 10000 \
        -keyalg RSA \
        -keysize 2048
fi

echo "=== 9. Signing APK with apksigner ==="
"$APKSIGNER_CMD" sign \
    --ks "$KEYSTORE" \
    --ks-key-alias androiddebugkey \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$APP_DIR/AlightMotionHub.apk" \
    "$BUILD_DIR/AlightMotionHub-unaligned.apk"

echo "=== 10. Verifying Signed APK ==="
"$APKSIGNER_CMD" verify --verbose "$APP_DIR/AlightMotionHub.apk"

# Update web public folder copy if present
if [ -d "$APP_DIR/../public" ]; then
    cp "$APP_DIR/AlightMotionHub.apk" "$APP_DIR/../public/AlightMotionHub.apk"
fi

echo "================================================="
echo "✅ BUILD SUCCESSFUL!"
echo "Native Android APK generated at:"
echo "$APP_DIR/AlightMotionHub.apk"
ls -lh "$APP_DIR/AlightMotionHub.apk"
echo "================================================="
