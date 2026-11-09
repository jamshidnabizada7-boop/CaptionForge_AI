# Android + iOS native application roadmap

## Product brief

Build **CaptionForge**, a creator-oriented AI video subtitle editor. The supplied 21 screenshots are **UI/feature references**, not reusable branded assets. Match their dark-purple visual hierarchy, control density, vertical preview, colored caption timelines, bottom editing tray, and one-tap workflows, while using original branding and independently authored style assets.

## Recommended architecture

**Mobile UI:** Flutter (single Android/iOS project), with platform-native media picker, secure project cache, portrait editing display, haptic trim handles, local preview compositor. Split by feature: `home`, `import`, `transcription`, `editor`, `caption_styles`, `translation`, `audio`, `export`, `account`.

**State:** Riverpod or Bloc with immutable project graph; local store SQLite/Drift; synced backend project saves as needed; command stack for undo/redo.

**Media engine:** Native AVFoundation (iOS) / Media3 + MediaCodec (Android) for decode and preview; FFmpeg-based background worker where legally/distribution-appropriate, server rendering otherwise. Ensure preview/export parity. Render captions with complex script shaping (Harfbuzz/libass or native text layout), correct Unicode BiDi and glyph fallback.

**AI:** Faster-whisper or a high-accuracy managed transcription API, force-alignment correction for word timestamps, diarization optional. Translation by server-side API, batched by caption IDs. Optional language-adaptive ML (models should be validated for Persian Afghanistan and Pashto).

**Object tracking:** MediaPipe/OpenCV face/body tracker, smoothing with Kalman filters, object masks for depth-aware overlays, predictable default placement when no face is visible.

**Backend:** This FastAPI proof-of-concept → Postgres metadata, object storage with signed URLs, Redis/Celery or managed job queue, ASR GPU worker, translation worker, FFmpeg render worker, auth and quota service, webhook processing. Per-user project files and encrypted secrets. Retention and deletion controls.

**Payments:** RevenueCat with Apple StoreKit and Google Play Billing for optional premium templates/export. Test subscription restoration, transparency, privacy policy, cancellation.

## Screens to implement

1. **Home:** recent projects, purple new project button, subscription teaser/settings.
2. **Media import & trim:** gallery/files permissions, native picker, thumbnail strip, move handles, caption selected range.
3. **Processing state:** upload/transcript progress, recoverable retry, completion notifications.
4. **Editor:** editable preview, scrubber, clip and multi-track caption timeline, font size rail, undo/redo, output resolution, export.
5. **Style:** caption presets with animated mini previews.
6. **Font:** original licensed font families, script-specific previews.
7. **Color:** text/box/outline/swatch, opacity.
8. **Animation:** fade, pop, bounce, slide, karaoke, each with timing curves.
9. **Follow:** enable face/object follow, position/avoid subject, fallback.
10. **Translate:** Replace vs Simultaneous; multilingual track list; edit translated text.
11. **SFX:** word/sentence trigger, sound choices, volume.
12. **Position & Timing:** horizontal/vertical position, line constraints, early display, start/end editing.
13. **Text flow/Overlay:** smart line breaking, overlays, depth or masks, background rectangles.
14. **Tracks:** visibility, lock, duplicate, delete, language/color assignments.
15. **Filters:** preview and render color grading.
16. **Clear voice:** level selector, noise reduction before/after.
17. **Export:** resolution/frame rate, preview, render percentage, save/share gallery, SRT file.

## Phased delivery

**Phase A (current prototype):** local web editor, persistent projects, key tools and MP4 export. **Phase B:** polished Flutter Android client, backend integration and true preview parity. **Phase C:** advanced tracking/depth effects, native render optimization, iOS rollout, subscriptions, cloud scalability and security audits.

## Definition of done for full parity

Each referenced screen must be matched to a running UI and a functional service. No blank buttons. A QA matrix should cover: Android + iPhone, high/low spec devices, 720/1080 exports, videos with no audio, videos with music, Persian/Dari and Pashto right-to-left shaping, clips with multiple people, translations with mismatched lengths, offline/error states and resume after crash. Verify exported video and the timeline are frame-aligned within a target tolerance set by QA.
