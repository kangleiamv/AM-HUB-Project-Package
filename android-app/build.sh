#!/bin/bash
set -e

APP_DIR="/data/data/com.termux/files/home/am/android-app"
BUILD_DIR="$APP_DIR/build"
ANDROID_JAR="/usr/lib/android-sdk/platforms/android-33/android.jar"
D8_JAR="/usr/lib/android-sdk/build-tools/35.0.0/lib/d8.jar"
KEYSTORE="$APP_DIR/debug.keystore"

echo "=== 1. Cleaning build directory ==="
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/gen" "$BUILD_DIR/compiled_res" "$BUILD_DIR/obj" "$BUILD_DIR/apk"

echo "=== 2. Compiling Android Resources with aapt2 ==="
/usr/bin/aapt2 compile --dir "$APP_DIR/res" -o "$BUILD_DIR/compiled_res.zip"

echo "=== 3. Linking Resources and Generating R.java ==="
/usr/bin/aapt2 link \
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
java -cp "$D8_JAR" com.android.tools.r8.D8 \
    --release \
    --min-api 21 \
    --output "$BUILD_DIR/apk" \
    --lib "$ANDROID_JAR" \
    @"$BUILD_DIR/classes.txt"

echo "=== 6. Packaging APK ==="
cd "$BUILD_DIR/apk"
# Add classes.dex into res.apk
zip -uj res.apk classes.dex
cd "$APP_DIR"

echo "=== 7. Zipalign APK ==="
zipalign -f -p 4 "$BUILD_DIR/apk/res.apk" "$BUILD_DIR/AlightMotionHub-unaligned.apk"

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
apksigner sign \
    --ks "$KEYSTORE" \
    --ks-key-alias androiddebugkey \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$APP_DIR/AlightMotionHub.apk" \
    "$BUILD_DIR/AlightMotionHub-unaligned.apk"

echo "=== 10. Verifying Signed APK ==="
apksigner verify --verbose "$APP_DIR/AlightMotionHub.apk"

echo "================================================="
echo "✅ BUILD SUCCESSFUL!"
echo "Native Android APK generated at:"
echo "$APP_DIR/AlightMotionHub.apk"
ls -lh "$APP_DIR/AlightMotionHub.apk"
echo "================================================="
