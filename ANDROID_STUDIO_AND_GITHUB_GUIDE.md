# CaptionForge — Android Studio Export & GitHub Publishing Guide

## 1. Repository Structure (All 3 Stacks Preserved)

The workspace root contains the **Native Kotlin + Jetpack Compose Android project** while keeping your original **Python FastAPI backend** and **Flutter mobile project** 100% untouched:

```text
/
├── app/                                  # Native Kotlin + Jetpack Compose Android App
│   ├── build.gradle.kts                  # Android app Gradle configuration (Media3, Compose, Retrofit)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml       # Permissions & Cleartext network config for FastAPI
│       │   ├── assets/
│       │   │   ├── sample_video.mp4      # Bundled sample video (synced from real_speech_video.mp4)
│       │   │   └── sfx/*.wav             # 22 studio WAV sound effects across 11 categories
│       │   ├── java/com/captionforge/android/
│       │   │   ├── MainActivity.kt       # App entry point & navigation host
│       │   │   ├── audio/SfxPlayerManager.kt
│       │   │   ├── data/ProjectRepository.kt
│       │   │   ├── export/OnDeviceMp4Renderer.kt
│       │   │   ├── model/ProjectModels.kt
│       │   │   ├── network/CaptionForgeApi.kt
│       │   │   ├── ui/editor/*           # EditorScreen, Timeline, ToolsTrays, SubtitleOverlayCanvas
│       │   │   ├── ui/home/HomeScreen.kt
│       │   │   ├── ui/theme/Theme.kt
│       │   │   └── viewmodel/CaptionForgeViewModel.kt
│       │   └── res/font/*.ttf            # Bundled Vazirmatn, Noto Sans Arabic, Nastaliq Urdu, Lalezar, Amiri, Inter, Bebas Neue
│       └── test/java/com/captionforge/android/
│           ├── CaptionForgeModelsTest.kt
│           └── CaptionForgeRobolectricTest.kt
├── build.gradle.kts                      # Root Gradle build file
├── settings.gradle.kts                   # Root Gradle settings (includes :app)
├── gradle/libs.versions.toml             # Centralized Gradle Version Catalog
├── captionforge/                         # UNTOUCHED Python FastAPI + FFmpeg + Faster-Whisper Backend
├── captionforge_mobile/                  # UNTOUCHED Original Flutter/Dart Project
└── publish_kotlin_android_to_github.sh   # Safe script to push to a separate GitHub branch
```

---

## 2. Opening in Android Studio

1. In Google AI Studio, click **Download / Export ZIP** (or use **Push to GitHub** in the top menu).
2. Extract the ZIP archive on your computer.
3. Open **Android Studio** (Ladybug / Meerkat or newer with JDK 21) -> **Open** -> select the extracted root folder (`CaptionForge_AI`).
4. Android Studio will automatically detect `settings.gradle.kts` and sync the `:app` module.
5. Select an Android Emulator or physical USB device and click **Run (`▶`)**.

---

## 3. Running with the Full Python FastAPI + FFmpeg Backend

While the Android app runs standalone with **On-Device Media3 Transformer MP4 rendering**, **ExoPlayer playback**, **RTL captioning**, and **bundled `.wav` SFX**, you can connect it to your Python FastAPI backend for **Faster-Whisper AI speech transcription**, **neural translation**, **OpenCV motion tracking**, and **FFmpeg + `libass` burned-in rendering**:

```bash
cd captionforge
pip install -r requirements.txt
# Optional for Faster-Whisper & OpenAI translation:
# pip install -r requirements-ai.txt
uvicorn app:app --host 0.0.0.0 --port 8000 --reload
```

Then in the Android app:
- **Android Emulator**: Uses `http://10.0.2.2:8000` by default (already pre-configured).
- **Physical Phone over USB**: Run `adb reverse tcp:8000 tcp:8000` on your computer, then tap the **Server Status chip** on the Home screen and select **USB ADB (`http://127.0.0.1:8000`)**.
- **Physical Phone over Wi-Fi**: Enter your computer's LAN IP (e.g., `http://192.168.1.50:8000`) in the Server Status sheet.

---

## 4. Publishing to a Separate GitHub Branch (`native-android-kotlin`)

Because the AI Studio cloud build container does not store your personal GitHub credentials or `.git` history on disk, changes made here are stored in your **AI Studio workspace** until you either:
- Use AI Studio's built-in **GitHub Sync / Export** button in the top toolbar, OR
- Run the included `publish_kotlin_android_to_github.sh` script after downloading the project:

```bash
chmod +x publish_kotlin_android_to_github.sh
./publish_kotlin_android_to_github.sh https://github.com/jamshidnabizada7-boop/CaptionForge_AI.git native-android-kotlin
```
