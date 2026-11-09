# CaptionForge Mobile (Flutter)

Cross-platform mobile application for **CaptionForge AI Video Subtitle Editor**, built with Flutter 3.47 and Dart 3.13.

## Features
- **Interactive 9:16 Preview Canvas**: Live aspect ratio switching (9:16, 1:1, 16:9) with real-time bidirectional subtitle rendering.
- **Draggable Video Timeline**: Multi-track lanes supporting up to 5 simultaneous subtitle tracks, scrubbing playhead, and contextual actions (Split, Merge, Edit, Delete).
- **Video Import & Trimming**: Native `FilePicker` video selection modal with segment start/end handles and duration preview.
- **AI Integration**: Triggers FastAPI backend Faster-Whisper ASR and AI translation services.
- **Export Flow**: Authoritative 480p/720p/1080p MP4 rendering with burned-in ASS captions and optional SRT download.

## Build and Test

```powershell
# Fetch dependencies
flutter pub get

# Static analysis
flutter analyze

# Run unit and widget test suite
flutter test

# Build debug APK
flutter build apk --debug
```
