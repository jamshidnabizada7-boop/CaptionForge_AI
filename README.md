# CaptionForge — AI Video Subtitle Editor

CaptionForge is a production-quality, cross-platform AI video subtitle and editing application comprising:
1. **Flutter Mobile Application (`captionforge_mobile`)**: High-performance Android and iOS client featuring interactive 9:16 video preview canvas, multi-track timeline scrubber, custom style and typography galleries, bilingual subtitles, and native file import/export.
2. **FastAPI AI Video Backend (`captionforge`)**: Python asynchronous microservice integrating Faster-Whisper ASR, PyAV 19 audio alignment, OpenCV face tracking, FFmpeg filtergraphs (color grading, Clear Voice afftdn audio denoising, sound effect mixing), and ASS subtitle burning.

---

## Architecture Overview

```
CaptionForge_AI_Video_Subtitle_App/
├── captionforge_mobile/              # Flutter 3.47.9 / Dart 3.13.7 Mobile App
│   ├── android/                      # Android Native Platform (Gradle 9.1, SDK 36, JDK 17)
│   ├── ios/                          # iOS Native Platform (Xcode Runner, Swift)
│   ├── lib/
│   │   ├── core/                     # Networking, theme, constants, and utilities
│   │   ├── features/
│   │   │   ├── home/                 # Project list, video picker, trimming modal
│   │   │   └── editor/               # 9:16 preview canvas, timeline view, contextual actions
│   │   └── models/                   # Project, CaptionCue, WordTiming, SubtitleStyle models
│   └── test/                         # Unit, widget, and contract test suites
│
├── captionforge/                     # FastAPI Backend Microservice
│   ├── app.py                        # REST API, Faster-Whisper ASR, FFmpeg rendering pipeline
│   ├── requirements.txt              # Core dependencies
│   ├── requirements-ai.txt           # Faster-Whisper, PyAV, CTranslate2, OpenCV
│   └── tests/                        # Pytest suite with end-to-end ASR, SFX, and ASS export
│
├── .agents/skills/                   # 8 Custom Antigravity Engineering Skills
│   ├── flutter-mobile-development
│   ├── ffmpeg-video-engineering
│   ├── ai-speech-transcription
│   ├── multilingual-subtitles
│   ├── subtitle-animation-engine
│   ├── video-ai-effects
│   ├── fastapi-backend
│   └── mobile-app-testing
│
├── references/                       # 21 UI/UX reference design screenshots
├── IMPLEMENTATION_TRACKER.md         # Parity matrix, test logs, and toolchain audit
├── FEATURE_AUDIT.md                  # Comprehensive feature audit
└── NATIVE_APP_ROADMAP.md             # Native architecture roadmap
```

---

## Prerequisites & Toolchain Setup

### 1. Flutter & Android Toolchain
- **Flutter SDK**: 3.47.9+ (Dart 3.13.7+)
- **Java Development Kit**: Microsoft OpenJDK 17 (`C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot`)
- **Android SDK**: `platforms;android-36`, `build-tools;36.0.0`, `platform-tools`
- **Environment Variables**:
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot"
  $env:ANDROID_HOME = "D:\Android\Sdk"
  $env:Path = "$env:JAVA_HOME\bin;D:\flutter\bin;$env:ANDROID_HOME\platform-tools;" + $env:Path
  ```

### 2. Python & Media Dependencies
- **Python**: 3.11+ (Python 3.13.15 tested)
- **FFmpeg**: Gyan.FFmpeg 9.0.1+ with `libx264`, `libass`, `fribidi`, `harfbuzz`, `afftdn` on system PATH.

---

## Quickstart Guide

### Step 1 — Start the FastAPI Backend

```powershell
cd captionforge
.\.venv\Scripts\activate
uvicorn app:app --host 127.0.0.1 --port 8000 --reload
```

The API documentation and health endpoint will be accessible at:
- **Health Check**: `http://127.0.0.1:8000/api/health`
- **Swagger Docs**: `http://127.0.0.1:8000/docs`

### Step 2 — Run or Build the Flutter Mobile App

#### Run Automated Tests
```powershell
cd captionforge_mobile
flutter test
```

#### Analyze Code Quality
```powershell
cd captionforge_mobile
flutter analyze
```

#### Build Android Debug APK
```powershell
cd captionforge_mobile
flutter build apk --debug
```
The compiled package will be located at:
`captionforge_mobile/build/app/outputs/flutter-apk/app-debug.apk`

#### Run on Physical Android Device
1. Connect Android handset via USB with **USB Debugging** enabled in Developer Options.
2. Verify connection:
   ```powershell
   & "D:\Android\Sdk\platform-tools\adb.exe" devices
   ```
3. Forward FastAPI backend socket port to the Android handset:
   ```powershell
   & "D:\Android\Sdk\platform-tools\adb.exe" reverse tcp:8000 tcp:8000
   ```
4. Install debug APK:
   ```powershell
   & "D:\Android\Sdk\platform-tools\adb.exe" install -r d:\CaptionForge_AI_Video_Subtitle_App\captionforge_mobile\build\app\outputs\flutter-apk\app-debug.apk
   ```
5. Launch app on device:
   ```powershell
   & "D:\Android\Sdk\platform-tools\adb.exe" shell am start -n com.captionforge.mobile.captionforge_mobile/com.captionforge.mobile.captionforge_mobile.MainActivity
   ```

#### Android Emulator & BIOS Hardware Virtualization Setup
To run an Android virtual device (AVD), Intel VT-x hardware virtualization must be enabled:
1. Restart host PC and press `F10` (or `F2`/`Del`) to enter BIOS/UEFI setup.
2. Navigate to **Advanced** > **CPU Configuration**.
3. Set **Intel (VMX) Virtualization Technology** (VT-x) and **Intel VT-d** to **Enabled**.
4. Press `F10` to save and exit.
5. In Windows PowerShell, verify virtualization is active:
   ```powershell
   (Get-CimInstance Win32_Processor).VirtualizationFirmwareEnabled
   # Must return 'True'
   ```

---

## Automated Verification Suite

Run all verification tests across both backend and mobile tiers:

```powershell
# 1. Run Flutter Mobile Tests (14 unit, widget, and contract tests)
cd captionforge_mobile
& "D:\flutter\bin\flutter.bat" test

# 2. Run Python Backend Tests (8 end-to-end tests including Whisper ASR, trimming boundary exclusion, and novel translations)
cd ..\captionforge
& ".\.venv\Scripts\pytest.exe" -v

# 3. Run Live Socket HTTP Verification (0 mock responses)
cd ..
& ".\captionforge\.venv\Scripts\python.exe" test_live_backend_http.py

# 4. Run Complete 14-Step Real Workflow + Multi-Aspect Ratio Exports
& ".\captionforge\.venv\Scripts\python.exe" test_full_workflow.py
```

---

## Key Features & Parity Status

- **Video Import & Segment Trimming**: Native file picker for `.mp4`, `.mov`, `.mkv` with dual-handle trim slider, duration calculation, and zero-leakage cut synchronization.
- **AI Speech-to-Text & Word Alignment**: Offline Whisper ASR producing word timestamps synchronized to absolute video time.
- **Interactive Timeline**: Multi-track horizontal scrubbing with cue selection, split, merge, and floating contextual toolbars.
- **Multilingual & RTL Support**: Native shaping for Persian/Dari, Pashto, Urdu, and Arabic with Vazirmatn and Noto Sans Arabic fonts; novel sentence offline translation engine.
- **Video Effects & Clear Voice**: Real-time FFmpeg audio denoising (`afftdn`), color balance / saturation grading, and caption-triggered SFX mixing.
- **Authoritative Video Export**: MP4 rendering with libx264, burn-in ASS subtitle tracks (9:16 portrait, 16:9 landscape, 1:1 square), and companion `.srt` export.
