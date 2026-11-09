# CaptionForge System Architecture

## 1. System Overview

CaptionForge is an AI-powered video captioning and kinetic typography mobile application engineered for high-impact short-form video creation (TikTok, Instagram Reels, YouTube Shorts). The system combines a responsive cross-platform Flutter frontend with a high-throughput Python FastAPI backend powered by Faster-Whisper, OpenCV face tracking, and FFmpeg filtergraphs.

```
┌─────────────────────────────────────────────────────────────────┐
│                    Flutter Mobile Client                        │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │   UI Layer: EditorScreen, TimelineView, SubtitleOverlay   │  │
│  │   Contextual Trays: Style, Format, Anim, SFX, Templates   │  │
│  └─────────────────────────────┬─────────────────────────────┘  │
│                                │                                │
│  ┌─────────────────────────────┴─────────────────────────────┐  │
│  │   State & Domain Layer: Project, Track, Cue, Overlay      │  │
│  │   Services: ApiService (HTTP/REST), Native VideoPlayer    │  │
│  └─────────────────────────────┬─────────────────────────────┘  │
└────────────────────────────────┼────────────────────────────────┘
                                 │ REST / Multipart HTTP (JSON/MP4)
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                    FastAPI Backend Engine                       │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │   REST API Gateway & Background Task Queue (Thread Pool)  │  │
│  └──────┬──────────────────────┬──────────────────────┬──────┘  │
│         │                      │                      │         │
│         ▼                      ▼                      ▼         │
│  ┌──────────────┐      ┌──────────────┐      ┌──────────────┐   │
│  │Faster-Whisper│      │ Translation  │      │Mathematical  │   │
│  │ASR Engine    │      │ Multi-Engine │      │SFX Synthesis │   │
│  │(CTranslate2) │      │ (5 Providers)│      │(9 Presets)   │   │
│  └──────┬───────┘      └──────┬───────┘      └──────┬───────┘   │
│         │                      │                      │         │
│         └──────────────────────┼──────────────────────┘         │
│                                ▼                                │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              FFmpeg Video Engineering Filtergraph         │  │
│  │  - Video Grading (eq, unsharp, scale, pad)                │  │
│  │  - Audio Denoiser (afftdn) & Multi-Track SFX Mixer (amix) │  │
│  │  - Multi-Layer ASS Subtitle & Graphic Overlay Renderer    │  │
│  └─────────────────────────────┬─────────────────────────────┘  │
└────────────────────────────────┼────────────────────────────────┘
                                 │
                                 ▼
                    Output Artifacts (H.264/AAC MP4)
```

---

## 2. Flutter Mobile Architecture (`captionforge_mobile`)

### A. Presentation Layer
- **`HomeScreen` (`lib/features/home/home_screen.dart`):** Responsive dashboard displaying project cards, video thumbnail previews, project management actions (rename, duplicate, delete), new project ingestion modal, and interactive server connectivity diagnostic modal.
- **`EditorScreen` (`lib/features/editor/editor_screen.dart`):** Central production workspace featuring:
  - Real-time video preview with aspect ratio constraint switching (9:16 portrait, 16:9 landscape, 1:1 square).
  - Native `VideoPlayerController` (`video_player: ^2.9.2`) integration with synchronized scrubbing, play/pause, seek debounce locks, and trim boundaries.
  - Dynamic `ColorFiltered` wrapper reflecting brightness, contrast, and saturation adjustments in real time without video re-encoding.
- **`SubtitleOverlay` (`lib/features/editor/widgets/subtitle_overlay.dart`):** Multi-layered kinetic typography canvas:
  - **Layer 0 (Background Depth):** Floating Persian background words (`soft_bg_text`) at low opacity with motion blur.
  - **Layer 1 & 2 (Subtitle Tracks):** Primary and secondary language subtitle tracks with live entrance animations (Pop, Fade, Bounce, Slide, Zoom), karaoke word highlighting, custom borders, and translucent backgrounds.
  - **Layer 3 (Foreground Overlays):** Interactive graphic overlays (`glow_hologram`, `metallic_banner`, `light_leak`, `flag_graphic`, `end_accent`) positioned with normalized coordinates.
- **`TimelineView` (`lib/features/editor/widgets/timeline_view.dart`):** Multi-track timeline displaying independent stacked subtitle lanes, interactive trim markers, playhead scrubber, cue splitting (`onSplitCue`), and cue merging.
- **`ToolsTrays` (`lib/features/editor/widgets/tools_trays.dart`):** Contextual tabbed drawers for:
  - *Templates:* 1-tap styles inspired by Video 1 & Video 2 (Orange Headline, Hand Hologram, etc.).
  - *Styles:* Font family, font size, text color, outline thickness, background box toggle.
  - *Animations:* Kinetic entrance animation selector (Fade, Pop, Bounce, Slide up/down, Zoom).
  - *SFX:* 9 sound effect presets, volume slider, and Sentence vs. Word trigger mode.
  - *Translation:* Pluggable AI engine selector and dual-track configuration.
  - *Color Grading & Audio:* Brightness, contrast, saturation, and Clear Voice denoiser.

### B. Domain & State Layer
- **`Project` Model (`lib/models/project.dart`):** Complete immutable project state including ID, title, video metadata, aspect ratio, trim boundaries, subtitle tracks, video effects, and graphic overlays.
- **`Track` & `CaptionCue` Models:** Multi-track caption schema with millisecond timestamps, per-track styles, and word-level karaoke timings.
- **`GraphicOverlay` Model:** Coordinates (`xPos`, `yPos`), start/end timing, styling, animation, and overlay type.
- **`VideoEffects` Model:** Color adjustments (`brightness`, `contrast`, `saturation`, `sharpness`) and audio filters (`clearVoice`, `sfx`, `sfxVolume`).

### C. Data & Services Layer
- **`ApiService` (`lib/services/api_service.dart`):** Robust HTTP client managing REST requests, multipart video uploads, long-polling job status checks, and dynamic server endpoint switching (supporting Android emulator `10.0.2.2`, local USB `127.0.0.1`, LAN Wi-Fi, and production HTTPS).

---

## 3. FastAPI Backend Architecture (`captionforge`)

### A. API Gateway & Job Pipeline
- **`app.py`:** High-performance asynchronous FastAPI application exposing RESTful routes for project CRUD, media streaming, transcription, translation, and video rendering.
- **Non-Blocking Background Tasks:** Transcription and rendering jobs run on worker threads using `BackgroundTasks`, updating job status objects polled via `/api/jobs/{id}`.
- **PyAV 19 Compatibility Adapter:** Intercepts `av.open` calls to sanitize legacy `metadata_errors` arguments, ensuring compatibility with modern PyAV releases while running Faster-Whisper.

### B. Faster-Whisper ASR Engine
- **CTranslate2 Execution:** High-speed inference using quantized Faster-Whisper models with Silero VAD (Voice Activity Detection) filter.
- **Word-Level Alignment:** Extracts word-level timestamps (`word_timestamps=True`) and clusters words into natural conversational phrases based on punctuation and duration thresholds.

### C. Multilingual Pluggable Translation Engine (`captionforge/translation.py`)
- **Adapter Architecture:** `TranslationEngine` delegates to registered `TranslationProvider` implementations:
  1. `OpenAITranslationProvider`: GPT-4o-mini / GPT-4o structured translation.
  2. `AnthropicTranslationProvider`: Claude 3.5 Sonnet contextual subtitle adaptation.
  3. `DeepLTranslationProvider`: Neural machine translation.
  4. `TransformersTranslationProvider`: Local offline NLLB-200 / MarianMT models.
  5. `OfflineRuleBasedTranslationProvider`: In-memory dictionary and phrase engine with Arabic diacritics stripping (`strip_arabic_diacritics`) and RTL punctuation normalization (`؟`, `،`).
- **Bidirectional Support:** Full translation between English, Persian/Dari, Pashto, Urdu, Arabic, Spanish, French, and German.

### D. Mathematical SFX Audio Synthesis Engine (`write_sfx`)
CaptionForge includes an entirely self-contained, mathematically synthesized audio effect generation engine with 0 external sound asset dependencies. It generates pure 44.1 kHz 16-bit PCM WAV buffers on demand:
- **Pop:** Exponential pitch drop (900 Hz to 300 Hz) with 45 ms decay envelope.
- **Whoosh:** Bandpass noise sweep with bell-shaped amplitude envelope.
- **Impact:** Resonant 75 Hz sub-bass thump with 220 ms exponential decay.
- **Click:** Sharp 1400 Hz transient click with rapid 18 ms decay.
- **Shimmer:** Harmonic triad overtone (1200 Hz + 1850 Hz + 2450 Hz) with gentle 350 ms fade.
- **Riser:** Exponential pitch climb (180 Hz to 1100 Hz) over 400 ms.
- **Sweep:** Linear downward frequency modulation (1350 Hz to 280 Hz) over 280 ms.
- **Glitch:** Square-wave bit-crushed stutter burst.
- **Ambient / Soft:** Dual-sine mellow pad (260 Hz + 390 Hz) with soft 200 ms attack/release.

### E. FFmpeg Video Engineering & Subtitle Burning Pipeline
Video export combines multi-stage FFmpeg processing into an optimized single-pass pipeline:
1. **Video Pre-Processing:** Trimming (`-ss`, `-to`), color grading (`eq=brightness:contrast:saturation`), edge sharpening (`unsharp`), and aspect ratio re-formatting (`scale` and `pad` to 9:16, 16:9, or 1:1).
2. **Audio Processing:** Clear Voice denoising (`afftdn`), SFX audio overlay mixing (`amix=inputs=2:duration=first:dropout_transition=2`).
3. **Advanced ASS Subtitle Burning (`build_ass`):** Compiles multi-track subtitles and graphic overlays into an Advanced SubStation Alpha script with:
   - DirectWrite, FriBidi, and HarfBuzz bidirectional text shaping.
   - Micro-timed karaoke highlighting tags (`\k`).
   - Dynamic entrance animation tags (`\fad`, `\fscx\t`, `\move`).
   - Multi-layer depth ordering (Layer 0 background text, Layers 1-2 subtitles, Layer 3 foreground banners and holograms).
4. **Encoding & Container:** H.264 high-profile encoding with faststart metadata (`-movflags +faststart`) for instant streaming playback.
