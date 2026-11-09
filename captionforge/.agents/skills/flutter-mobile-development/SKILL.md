---
name: flutter-mobile-development
description: Use this skill when developing, building, analyzing, or testing Flutter and Dart cross-platform mobile applications for CaptionForge on Android and iOS.
---

# Flutter Mobile Development Guide for CaptionForge

This guide defines the standards, architecture, and workflows for building and maintaining the CaptionForge mobile application with Flutter and Dart.

## Architecture & Code Organization

CaptionForge follows feature-first clean architecture:

```text
lib/
├── main.dart                   # App entrypoint, ProviderScope, initialization
├── app.dart                    # MaterialApp, dark theme configuration, router
├── core/
│   ├── constants/              # Theme colors, dimensions, typography, API URLs
│   ├── network/                # ApiClient, HTTP helpers, error interceptors
│   ├── database/               # Local SQLite / Drift database for projects & cues
│   └── utils/                  # Time formatters, file helpers, BiDi utilities
├── features/
│   ├── home/                   # Project list, new project button, empty state
│   │   ├── presentation/       # Screens, widgets, state providers
│   │   └── data/               # Project repository
│   ├── media_picker/           # Native gallery/file import, video thumbnail strip
│   ├── editor/                 # Main editor shell, preview canvas, playback bar
│   ├── timeline/               # Multi-lane caption timeline, scrubber, zoom, trim
│   ├── caption_styles/         # Style presets, font gallery, color palettes
│   ├── animations/             # Pop, bounce, slide, fade, karaoke controllers
│   ├── translation/            # Multilingual track manager, simultaneous display
│   ├── ai_effects/             # Face tracking toggle, clear voice, video filters
│   └── export/                 # Resolution picker, progress sheet, share dialog
└── models/                     # Project, Track, CaptionCue, SubtitleStyle, VideoMeta
```

## State Management with Riverpod

1. **State Isolation**: Use `NotifierProvider` or `AsyncNotifierProvider` for stateful business logic. Keep UI widgets dumb consumers (`ConsumerWidget` / `ConsumerStatefulWidget`).
2. **Immutable Models**: All data models (`Project`, `Track`, `CaptionCue`, `SubtitleStyle`) must be immutable with `copyWith()`, `toJson()`, and `fromJson()`.
3. **Undo/Redo Command Stack**: Track modifications through an editor history stack so changes can be reverted instantly without network roundtrips.

## Mobile Layout Standards

- **Theme**: Dark purple aesthetic (`#0D0B14` base background, `#1A162B` surface cards, `#8B5CF6` vibrant primary purple accent, `#A78BFA` highlight purple).
- **Responsive Video Preview**: Maintain a fixed 9:16 or aspect-fitted container using `AspectRatio` or `FittedBox` with letterboxing/pillarboxing support.
- **Safe Area**: Always wrap overlays in `SafeArea` to prevent notch, camera cutout, and gesture bar collisions.
- **Touch Targets**: Minimum 44x44 dp touch targets for all timeline handles, buttons, and switches.

## Essential Commands

```bash
# Verify environment and connected tools
flutter doctor -v

# Fetch dependencies
flutter pub get

# Static analysis and linting
flutter analyze

# Execute test suite
flutter test

# Run app on target device
flutter run -d <device_id>

# Build release APK for Android
flutter build apk --release
```

## Verification Checklist

- [ ] `flutter pub get` completes without dependency conflicts.
- [ ] `flutter analyze` reports zero errors and zero warnings.
- [ ] `flutter test` passes all unit and widget tests.
- [ ] Video preview renders smoothly at 60 fps without main thread jank.
- [ ] Draggable timeline handles scrub without frame drops.
- [ ] Back button triggers autosave confirmation or seamless local persist.
