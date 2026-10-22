# ⚡ AM-HUB-Project-Package (Alight Motion Hub)

<div align="center">

![Alight Motion Hub Banner](https://img.shields.io/badge/AM--HUB--Project--Package-v1.0.0-00FFA8?style=for-the-badge&logo=android&logoColor=black)
[![Platform](https://img.shields.io/badge/Platform-Web%20%7C%20Android%20Native-00FFA8?style=for-the-badge)](https://github.com)
[![Zero Dependencies](https://img.shields.io/badge/Dependencies-Zero%20NPM%20%7C%20Zero%20WebView-00FFA8?style=for-the-badge)](https://github.com)
[![License](https://img.shields.io/badge/License-MIT-00FFA8?style=for-the-badge)](LICENSE)

**An ultra-fast, zero-dependency Web Suite and 100% Pure Native Android App for inspecting, streaming, previewing, and extracting Alight Motion presets, project XMLs, and media assets.**

*Powered by **CMAISNAM***

[🌐 Web App Features](#-web-application) • [📱 Native Android App](#-native-android-application-no-webview) • [⚡ Zero-Download Streaming](#-zero-download-streaming-engine) • [🔌 REST API](#-rest-api-documentation) • [🛠️ Build & Install](#-getting-started)

</div>

---

## 📖 Overview

**Alight Motion Hub** is an all-in-one developer and creator toolkit engineered to resolve, inspect, and extract shared project packages from **Alight Motion** (`alightcreative.com` and `alight.link`). 

Unlike conventional tools that require downloading entire multi-gigabyte archives or loading heavy WebViews, Alight Motion Hub features:
- **Zero-Download Instant Media Streaming**: Play audio and video assets instantly using custom HTTP 206 Partial Content range requests and MP4 `faststart` (`moov` atom relocation).
- **100% Pure Native Android App**: Built entirely in Java using native Android SDK views (`VideoView`, `MediaPlayer`, `SeekBar`, `RecyclerView`) without any WebView overhead.
- **Zero-Dependency Node.js Server**: Custom-built using native Node.js standard libraries (`http`, `zlib`, `crypto`) with zero third-party NPM packages required.
- **Cyberpunk Dark UI**: High-contrast, ergonomic interface with neon green `#00FFA8` accents, thumb-friendly navigation, and mobile safe-area insets.

---

## ✨ Features at a Glance

| Feature | 🌐 Web Application | 📱 Native Android App |
|---|:---:|:---:|
| **Share Link Inspection** (`alightcreative.com` & `alight.link`) | ✅ | ✅ |
| **Pure Native Views (Zero WebView)** | N/A (Responsive HTML5) | ✅ (100% Native Views) |
| **Instant Video Streaming (No Full Download)** | ✅ (FastStart + Range) | ✅ (`VideoView` + Streaming) |
| **Instant Audio Streaming & Scrubbing** | ✅ (Custom Web Audio) | ✅ (`MediaPlayer` + `SeekBar`) |
| **XML Project Hierarchy & Element Inspector** | ✅ | ✅ |
| **Direct Project XML Export & Download** | ✅ | ✅ |
| **Media Asset Extraction** (MP4, WAV, M4A, PNG, JPG) | ✅ | ✅ |
| **Full ZIP Package Archiving** | ✅ | ✅ |
| **System Deep Linking & Intent Sharing** | ❌ | ✅ (`VIEW` & `SEND` Intents) |
| **Background Downloads** | Browser Downloads | Android `DownloadManager` |
| **Offline History Cache** | `localStorage` | Android `SharedPreferences` |
| **Dependencies** | 0 NPM packages | 0 Third-Party Libraries |

---

## 🌐 Web Application

The web frontend is designed for both desktop workstations and mobile browsers (Chrome, Safari, Firefox, Edge, Samsung Internet).

### Highlights:
- **Mobile-First Responsive Layout**: Dynamic viewport adjustments, iOS notch and Android gesture pill support via `env(safe-area-inset-*)`.
- **Thumb-Friendly Navigation**: Bottom-docked sticky navigation bar with illuminated neon tabs (`Preset`, `Media`, `XML`, `Help`).
- **Ergonomic Action Grid**: Full-width primary download button paired with a balanced 2x2 grid for secondary shortcuts (Open in AM, Copy ZIP URL, Copy XML, Share).
- **Custom Cyberpunk Media Player**:
  - Touch-scrubbable 8px progress slider.
  - Time indicators (`current / total`).
  - Native fullscreen toggle and playback speed options.
  - Automatically hides volume sliders on mobile devices to optimize horizontal space.
- **Swipeable Filter Chips**: Effortlessly switch between `All`, `Videos`, `Audio`, and `Images`.

---

## 📱 Native Android Application (No WebView)

Located in [`android-app/`](file:///data/data/com.termux/files/home/am/android-app/), this is an authentic, performance-focused Android client written in pure Java.

### Key Architecture:
- **No WebView Used**: Built strictly using Android's native View hierarchy (`ScrollView`, `CardView`, `TextView`, `ImageView`, `VideoView`, `MediaPlayer`, `SeekBar`, `ProgressBar`).
- **Direct Java HTTP Engine**: Utilizes `java.net.HttpsURLConnection` for direct, high-speed interaction with Google Cloud Storage and Firebase endpoints.
- **In-Memory ZIP Decompression**: Custom `ZipInputStream` pipeline extracts project XML and assets on the fly without writing temporary files to flash storage.
- **Android System Deep-Linking**:
  - Automatically captures taps on `https://alightcreative.com/am/share/*` and `https://alight.link/*`.
  - Registered in Android's system **"Share via..."** dialog (`android.intent.action.SEND`).
- **Android `DownloadManager` Integration**: Downloads are delegated directly to the native Android Download Manager for pause/resume support and system notification status.
- **Lightweight Binary Footprint**: Clean APK size (~74 KB) with zero bloat and near-instant cold startup.

---

## ⚡ Zero-Download Streaming Engine

Traditional cloud storage services force users to download entire multi-megabyte MP4/WAV packages before playing any media. Alight Motion Hub solves this with an intelligent byte-range streaming engine:

```
[ Client Browser / Android App ]
             │
             │ HTTP GET with "Range: bytes=0-1048575"
             ▼
[ Alight Motion Hub Server ]
             │
             ├─► Reads target asset chunk directly from cached package stream
             ├─► [MP4 Files] Dynamically moves 'moov' atom before 'mdat' (FastStart)
             │
             ▼ Answers HTTP 206 Partial Content
[ Instant Audio/Video Playback Begins in < 200ms ]
```

- **MP4 FastStart Optimization**: Standard MP4 recordings place video index metadata (`moov` atom) at the end of the file. Our engine parses the MP4 box hierarchy and serves the index first, enabling instant scrubbing without waiting for file download.
- **HTTP 206 Partial Content**: Full support for standard `Range` headers, allowing seamless seeking forward and backward in videos and audio files.

---

## 🔌 REST API Documentation

The server exposes a clean, self-documented JSON and binary API:

### 1. Inspect Share Link
```http
POST /api/inspect
Content-Type: application/json

{
  "url": "https://alightcreative.com/am/share/u/nX1X03aUecPC0EmYQecSiX6XITN2/p/lvOtWXtWob-bd75448ed87206bf"
}
```
**Response (200 OK):**
```json
{
  "success": true,
  "id": "nX1X03aUecPC0EmYQecSiX6XITN2:lvOtWXtWob-bd75448ed87206bf",
  "title": "Preset Title",
  "description": "10 projects, total 18.4 MB",
  "thumbnailUrl": "https://firebasestorage.googleapis.com/...",
  "packageSize": 19293792,
  "mediaCount": 8,
  "projectCount": 10,
  "media": [
    {
      "name": "audio_track.mp3",
      "type": "audio",
      "size": 3491820,
      "streamUrl": "/api/stream-media?id=...&file=audio_track.mp3",
      "downloadUrl": "/api/download-media?id=...&file=audio_track.mp3"
    }
  ]
}
```

### 2. Stream Media Asset
```http
GET /api/stream-media?id={uid}:{pid}&file={fileName}
Range: bytes=0-65536
```
Returns `206 Partial Content` (or `200 OK`) with media `Content-Type` (`video/mp4`, `audio/mpeg`, etc.).

### 3. Download Media Asset
```http
GET /api/download-media?id={uid}:{pid}&file={fileName}
```
Returns file with `Content-Disposition: attachment; filename="{fileName}"`.

### 4. Fetch Project XML
```http
GET /api/get-xml?id={uid}:{pid}&download=true
```
Returns the decrypted/extracted Alight Motion XML document.

### 5. Download Clean Package ZIP
```http
GET /api/download-package?id={uid}:{pid}
```
Downloads the reconstructed `.ampreset` or `.zip` archive containing the XML and all media assets.

---

## 🛠️ Getting Started

### Prerequisites
- **For Web Server**: Node.js 16+ (Zero external npm packages needed)
- **For Android App**: Android 5.0+ (Lollipop / API 21) or higher

---

### Running the Web Server

1. **Clone the repository:**
   ```bash
   git clone https://github.com/cmaisnam/alight-motion-hub.git
   cd alight-motion-hub
   ```

2. **Start the application:**
   ```bash
   npm start
   # or directly:
   node server.js
   ```

3. **Open in browser:**
   ```
   http://localhost:3000
   ```

---

### Installing the Native Android App

#### Option A: Direct APK Download
You can download the compiled and signed APK directly:
- From the web UI by clicking the **"Get APK"** button.
- Or navigating to: `http://localhost:3000/AlightMotionHub.apk`

#### Option B: ADB Install
```bash
adb install -r android-app/AlightMotionHub.apk
```

#### Option C: Build from Source via Command Line (Termux / Linux)
The app includes a standalone build script that compiles without needing Gradle or heavy IDEs:
```bash
cd android-app
bash build.sh
```
The script uses `aapt2`, `javac`, `d8`, `zipalign`, and `apksigner` to generate a production-ready APK in seconds.

#### Option D: Build in Android Studio
1. Open Android Studio.
2. Select **Open an Existing Project** and choose the [`android-app/`](file:///data/data/com.termux/files/home/am/android-app/) directory.
3. Sync Gradle and click **Run** (or `Build > Build Bundle(s) / APK(s) > Build APK(s)`).

---

## 📂 Project Structure

```
.
├── server.js                     # High-performance zero-dependency Node.js server
├── zip-helper.js                 # In-memory streaming ZIP archive reader & parser
├── package.json                  # Project manifest (0 dependencies)
├── public/                       # Web application frontend assets
│   ├── index.html                # Responsive cyberpunk HTML5 single-page application
│   ├── style.css                 # Cyberpunk Neon Dark CSS (Safe area insets, 2x2 grid)
│   ├── app.js                    # Frontend controller & streaming player orchestrator
│   └── AlightMotionHub.apk       # Production signed Native Android APK (74 KB)
│
└── android-app/                  # 100% Native Android Application
    ├── AndroidManifest.xml       # Manifest (Permissions, Deep Links, Share Sheet)
    ├── build.sh                  # Standalone CLI APK build pipeline
    ├── build.gradle              # Android Studio project configuration
    ├── settings.gradle
    ├── app/
    │   └── build.gradle
    ├── res/                      # Native layouts, drawables, styles, and colors
    │   ├── layout/
    │   │   ├── activity_main.xml # Primary native UI layout
    │   │   ├── item_media_asset.xml
    │   │   └── dialog_image_preview.xml
    │   └── values/
    └── src/com/cmaisnam/amhub/   # Pure Java source code
        ├── MainActivity.java     # View controller, VideoView, MediaPlayer & tabs
        ├── AlightMotionApi.java  # Native HTTPS client, parser & ZIP extractor
        ├── DownloadHelper.java   # Native DownloadManager & file management
        ├── ProjectData.java      # Data models
        └── ImagePreviewDialog.java
```

---

## 🔒 Security & Privacy

- **Safe Link Parsing**: Validates and sanitizes all input share URLs and IDs before processing.
- **Path Traversal Protection**: Media file streaming prevents directory traversal attacks by enforcing strict path resolution.
- **No Tracking or Telemetry**: Zero analytics, zero cookies, zero third-party tracking scripts.
- **No WebView Sandboxing Vulnerabilities**: The Android app does not execute arbitrary remote web scripts, guaranteeing full native sandbox security.

---

## 👤 Author & Credits

* **Developed & Designed by**: **CMAISNAM**
* **Project**: Alight Motion Hub (Web & Native Android)
* **Tagline**: *Empowering Alight Motion creators worldwide.*

---

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).
