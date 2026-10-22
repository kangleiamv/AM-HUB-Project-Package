# 📱 Alight Motion Hub — 100% Native Android App

<div align="center">

![Alight Motion Hub Android](https://img.shields.io/badge/Android%20App-100%25%20Pure%20Native-00FFA8?style=for-the-badge&logo=android&logoColor=black)
[![WebView Free](https://img.shields.io/badge/WebView-None%20(100%25%20Native%20Views)-00FFA8?style=for-the-badge)](https://github.com)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-21%20(Android%205.0)-00FFA8?style=for-the-badge)](https://github.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-34%20(Android%2014)-00FFA8?style=for-the-badge)](https://github.com)

**A high-performance, 100% native Android application for inspecting, previewing, streaming, and downloading Alight Motion shared project packages (`alightcreative.com` and `alight.link`).**

*Powered by **CMAISNAM***

[📖 Main Repository README](../README.md) • [📦 Download APK](#-download--install) • [🏗️ Build from Source](#-build-instructions) • [📐 Architecture](#-architecture--components)

</div>

---

## 🌟 Why Pure Native?

Unlike hybrid apps or WebView wrappers, Alight Motion Hub for Android is built with **zero WebViews**. Every screen and control uses Android's native widget toolkit for maximum fluidity, instantaneous response, and rock-solid stability:

- **Ultra-fast 60/120fps performance**: Hardware-accelerated UI rendering.
- **Microscopic binary size**: ~74 KB signed release APK.
- **Instant cold startup**: No browser engine to initialize.
- **Native hardware integration**: Direct integration with hardware video decoders, audio output, and the Android system DownloadManager.

---

## 🚀 Key Features

- **100% Native View Hierarchy**:
  - `ScrollView`, `CardView`, `TextView`, `ImageView`, `ProgressBar`
  - Native `VideoView` with custom playback bar, seekbar, and time indicators
  - Native `MediaPlayer` for direct audio streaming
  - Custom full-screen image lightbox dialog (`ImagePreviewDialog`)
- **Direct Java Cloud Networking**:
  - Direct HTTP/HTTPS communication via `java.net.HttpsURLConnection`
  - Real-time token resolution for Firebase Storage assets
  - Native JSON parsing (`org.json.JSONObject`)
- **In-Memory Streaming ZIP Extractor**:
  - Native `java.util.zip.ZipInputStream` decompresses `.ampreset` / `.zip` packages on the fly
  - Extracts and catalogs media items (`.mp4`, `.wav`, `.m4a`, `.mp3`, `.png`, `.jpg`, `.xml`) without polluting physical device storage
- **System Deep-Linking & Intent Interception**:
  - Tapping an Alight Motion link (`https://alightcreative.com/am/share/...` or `https://alight.link/...`) opens the app directly
  - App appears in Android's system **"Share via..."** menu for quick preset transfers from WhatsApp, Telegram, Discord, and browsers
- **Android `DownloadManager`**:
  - Direct system-level downloads with notification bar progress, background queuing, and resume capabilities
- **Preset History Persistence**:
  - Recent inspections are cached locally using `SharedPreferences` for instant offline recall
- **Design & Theme**:
  - Dark cyberpunk aesthetic with neon green accents (`#00FFA8`), matching the Alight Motion Hub web ecosystem

---

## 📦 Download & Install

### Method 1: Direct Download
- From the web server: `http://localhost:3000/AlightMotionHub.apk`
- Local file path: [`android-app/AlightMotionHub.apk`](file:///data/data/com.termux/files/home/am/android-app/AlightMotionHub.apk)

### Method 2: Install via ADB
```bash
adb install -r android-app/AlightMotionHub.apk
```

---

## 🏗️ Build Instructions

### Option 1: Standalone CLI Build (Termux / Linux / macOS)
The project includes a self-contained compilation script that does not require Gradle, Gradle Daemons, or heavy IDEs:

```bash
cd android-app
bash build.sh
```

**Build Pipeline Details:**
1. `aapt2 compile`: Compiles Android XML layouts, drawables, colors, and styles into binary flat files.
2. `aapt2 link`: Links against `android.jar` and generates `R.java` and uncompressed resource package.
3. `javac`: Compiles all Java classes (`MainActivity`, `AlightMotionApi`, `DownloadHelper`, etc.).
4. `d8`: Desugars Java bytecode into optimized Dalvik bytecode (`classes.dex`).
5. `zip` & `zipalign`: Packages and 4-byte page-aligns the APK.
6. `apksigner`: Cryptographically signs the APK with v1, v2, and v3 schemes using `debug.keystore`.

---

### Option 2: Android Studio
1. Launch Android Studio.
2. Select **Open an Existing Project** and choose the `android-app/` directory.
3. Let Gradle sync project dependencies.
4. Select `Build > Build Bundle(s) / APK(s) > Build APK(s)` or click the green **Run** button.

---

## 📐 Architecture & Components

```
android-app/
├── AndroidManifest.xml          # Permissions, deep links (VIEW), share sheet (SEND)
├── build.sh                     # Standalone CLI toolchain builder
├── settings.gradle              # Android Studio Gradle configuration
├── build.gradle                 # Project Gradle settings
├── app/
│   └── build.gradle             # App-level Gradle script
├── res/                         # Native layouts, drawables, values
│   ├── layout/
│   │   ├── activity_main.xml    # Main UI screen with custom tab switcher
│   │   ├── item_media_asset.xml # List item for media files
│   │   ├── item_scene.xml       # Scene and element inspection card
│   │   └── dialog_image_preview.xml # Modal image preview
│   ├── drawable/                # High-contrast cyberpunk shape drawables
│   └── values/
│       ├── colors.xml           # Dark background (#080B14) & Neon Accent (#00FFA8)
│       ├── strings.xml
│       └── styles.xml
├── src/com/cmaisnam/amhub/
│   ├── MainActivity.java        # Core UI controller, VideoView/MediaPlayer handlers
│   ├── AlightMotionApi.java     # HTTPS client, token parser & in-memory ZIP unpacker
│   ├── DownloadHelper.java      # Android DownloadManager coordinator
│   ├── ProjectData.java         # Data models for presets, scenes, and media files
│   └── ImagePreviewDialog.java  # Native zoom dialog for image assets
└── AlightMotionHub.apk          # Pre-compiled, signed, install-ready APK
```

---

## 🔒 Permissions Used

| Permission | Purpose |
|---|---|
| `android.permission.INTERNET` | Required to communicate with Alight Motion Cloud Storage and fetch presets |
| `android.permission.ACCESS_NETWORK_STATE` | Checks for active network connectivity before dispatching requests |
| `android.permission.WRITE_EXTERNAL_STORAGE` | Used on Android 9 and lower for saving downloads via `DownloadManager` |
| `android.permission.READ_EXTERNAL_STORAGE` | Used on Android 12 and lower for reading saved downloads |

---

## 👤 Author

* **Created by**: **CMAISNAM**
* **Project**: Alight Motion Hub
