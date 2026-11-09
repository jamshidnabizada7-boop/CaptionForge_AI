# CaptionForge Reference Breakdown & Parity Matrix

## Overview
This document provides a comprehensive technical breakdown of the reference materials extracted from the three reference packages:
1. `CaptionForge_Screenshots_and_Plan.zip` (`/goal`)
2. `CaptionForge_Video_1_References.zip` (`/teamwork-preview`)
3. `CaptionForge_Video_2_References.zip` (`/boost`)

It documents the visual, typographic, animation, audio, and architectural specifications reverse-engineered from each reference asset, alongside the corresponding implementation in CaptionForge.

---

## 1. Reference Video Analysis

### A. Reference Video 1: Orange Persian Kinetic Typography (`Video_1_Orange_Persian_Typography.mp4`)
- **Container & Codec:** MP4 container, H.264 (High Profile), AAC audio.
- **Resolution & Aspect Ratio:** 512x910 pixels (9:16 vertical short-form format).
- **Framerate & Duration:** 30.00 fps, 21.48 seconds.
- **Speech & ASR Detection:** Faster-Whisper detected Persian (`fa`) with 0.96 confidence.

#### Timecoded Breakdown & Visual Effects
| Timecode | Visual / Audio Element | Design & ASS Implementation | Flutter Editor Parity |
|---|---|---|---|
| `00:00 - 00:04` | **Hero Persian Headline** | Extra-bold Persian font (Vazirmatn Black), vibrant orange fill (`#FF6A00` / `\c&H006AFF&`), thick dark outline (`\3c&H1A1A1A&`, 3.5px), deep shadow offset, Pop scale entrance (`\fscx85\fscy85\t(0,180,\fscx100\fscy100)`). | Implemented in `_buildTemplatesTray` as preset `"Orange Headline"`. Live animated transform via `SubtitleOverlay`. |
| `00:04 - 00:09` | **Dual-Tiered Explainer Hierarchy** | Top line: Large orange Persian focal word (`yPos: 0.68`). Bottom line: Smaller white secondary explainer subtitle (`#FFFFFF`, `yPos: 0.76`). | Supported via dual-track timeline architecture (`tracks[0]` & `tracks[1]`) with independent vertical offsets and font sizing. |
| `00:09 - 00:15` | **Purple Flare Transition** | Translucent magenta-purple light leak overlay (`#D946EF`, 25% alpha, `\alpha&H40&`) wiped diagonally across camera angle change. | Graphic overlay type `light_leak` rendered in `SubtitleOverlay` and exported as ASS dialogue with radial flash alpha blending. |
| `00:15 - 00:21.48` | **Kinetic Word Punch & Punctuation** | Rapid word-by-word active highlight (`karaoke: true`), Persian-specific question mark (`؟`) and comma (`،`) shaping, synchronized transient audio clicks. | Active word karaoke timing synced with math-synthesized `Pop` and `Click` SFX engine in `write_sfx`. |

---

### B. Reference Video 2: Glow Hologram & Metallic Title Cards (`Video_2_Glow_Flag_Titles.mp4`)
- **Container & Codec:** MP4 container, H.264 (High Profile), AAC audio.
- **Resolution & Aspect Ratio:** 512x910 pixels (9:16 vertical short-form format).
- **Framerate & Duration:** 30.00 fps, 24.03 seconds.
- **Speech & ASR Detection:** Faster-Whisper detected Persian (`fa`) with 0.88 confidence.

#### Timecoded Breakdown & Visual Effects
| Timecode | Visual / Audio Element | Design & ASS Implementation | Flutter Editor Parity |
|---|---|---|---|
| `00:00 - 00:03` | **Hand Hologram Effect** | Cyan glowing text (`#00F0FF`, `\c&H00F0FF&`) anchored directly over speaker's upturned palm (`xPos: 0.50`, `yPos: 0.32`). Dual-pass halo glow (`\blur8\3c&H00F0FF&`). | Graphic overlay type `glow_hologram` with animated pulse scale and cyan glow box in `SubtitleOverlay`. |
| `00:03 - 00:07` | **Floating Background Words** | Faint Persian words drifting slowly behind speaker at 40% opacity (`\alpha&H88&`, `\blur6`, Layer 0). Non-intrusive ambient depth typography. | Graphic overlay type `soft_bg_text` rendered underneath subtitle tracks in both Flutter canvas and ASS layer 0. |
| `00:07 - 00:12` | **Floating Flag Banner** | Subject-anchored waving flag graphic badge (`flag_graphic`) with gold-cyan border and subtle breathing pulse animation. | Graphic overlay type `flag_graphic` anchored at `xPos: 0.78, yPos: 0.22` with badge icon and country identifier. |
| `00:12 - 00:18` | **Metallic Scanline Reveal** | Silver metallic title banner (`metallic_banner`, "AUSTRALIA") using silver fill (`\c&HE2E8F0&`), dark slate border (`\3c&H1E293B&`), Impact font, letter-tracking, and Shimmer SFX. | Graphic overlay type `metallic_banner` with shimmer gradient highlight, border stroke, and math-synthesized 3-harmonic chord SFX. |
| `00:18 - 00:24.03` | **Crimson End Accent Title** | Bottom-anchored bold red title callout (`end_accent`, `\c&H3539E5&`, `yPos: 0.88`) with pulse entrance for call-to-action impact. | Graphic overlay type `end_accent` anchored at lower third with high-contrast red badge styling. |

---

## 2. Screenshot Parity Matrix (All 21 References: IMG_9611 to IMG_9631)

| Screenshot ID | Captured Feature in Reference | Feature Category | Mobile Client Implementation | Backend Engine Implementation | Verification Evidence |
|---|---|---|---|---|---|
| **IMG_9611** | Project management dashboard, project cards, thumbnail previews, New Project FAB, Pro badge | Workspace / Navigation | `HomeScreen` with responsive SliverAppBar, project list/grid, New Project modal, Pro membership dialog, duplicate/rename/delete actions. | `GET /api/projects`, `POST /api/projects`, `DELETE /api/projects/{pid}` with JSON persistence. | `models_test.dart`, `screenshot_mobile_home.png` |
| **IMG_9612** | Video import & segment trim handles, duration clamp, start/end timestamps | Media Ingest & Trim | Draggable double-ended trim scrubber in `EditorScreen` with `VideoTrim` model and live timestamp indicators. | Clamped range passed to FFmpeg export (`-ss` and `-to`) and video metadata extractor. | `test_full_workflow.py` Step 1 & 3 (`trim: {0.2s, 6.2s}`) |
| **IMG_9613** | File upload progress sheet, streaming percentage, file size | Ingestion Pipeline | Multipart streaming upload in `ApiService.createProject` with progress callback and cancel handle. | Fast streaming file receiver with 500 MB limit in `app.py`. | Verified in live HTTP sockets test |
| **IMG_9614** | AI transcription loading modal, spinning ring, estimated time remaining | Background Jobs | Async polling modal in Flutter (`pollJob`) with animated spinner and status message. | Non-blocking thread pool execution with `/api/jobs/{id}` status tracking (`QUEUED`, `PROCESSING`, `COMPLETED`). | `test_full_workflow.py` Step 4 |
| **IMG_9615** | Transcript review drawer, word-level confidence chips, text editor | ASR Verification | Interactive transcript editor with word-by-word timestamp chips, editable text fields, and split/merge actions. | Faster-Whisper ASR with `word_timestamps=True`, PyAV 19 compatibility layer, grouping algorithm. | Transcribed 11 words with 0.992 confidence |
| **IMG_9616** | 9:16 vertical editor canvas, play/pause controls, playhead scrubber, resolution switcher | Editor Canvas | Native `video_player: ^2.9.2` integration, interactive timeline scrubber, resolution badge (480p/720p/1080p), aspect ratio selector (9:16, 16:9, 1:1). | Aspect ratio-aware video filtergraph in FFmpeg (`scale=w:h:force_original_aspect_ratio=decrease,pad=w:h:(ow-iw)/2:(oh-ih)/2`). | `video_player_integration_test.dart`, multi-aspect exports |
| **IMG_9617** | Subtitle style preset gallery (Bold, Neon, Minimal, Karaoke, etc.) | Typography Presets | Scrollable preset gallery in `tools_trays.dart` with instant 1-tap style application and live canvas refresh. | Style definitions serialized into ASS script headers with custom font, margin, border, and color properties. | `models_test.dart`, `test_full_workflow.py` Step 7 |
| **IMG_9618** | Multi-script font drawer (Latin, Arabic, Persian, Urdu) | Font Management | Font selector drawer with Vazirmatn, Noto Sans Arabic, Segoe UI, Tahoma, Arial with live RTL script preview. | DirectWrite + FriBidi + HarfBuzz bidirectional text shaping in FFmpeg libass export. | `test_noto.ass`, `test_multilingual.ass` |
| **IMG_9619** | Text color, background box, outline thickness, padding controls | Style Customizer | Hex color picker, background box toggle (`bgEnabled`), background color picker, outline width slider (0.0 to 6.0px). | Compiled into ASS `\c&H...&`, `\3c&H...&`, `\bord`, and `\shad` tags. | `verified_output_captionforge.mp4`, frame inspection |
| **IMG_9620** | Entrance animation selector (Fade, Pop, Bounce, Slide, Zoom) | Kinetic Typography | Animation picker tray with animated preview cards. Real-time Flutter animation transforms in `SubtitleOverlay`. | Exact ASS override tags (`\fad`, `\fscx\t`, `\move`) matching Flutter curve and duration. | `test_full_workflow.py` Step 8 |
| **IMG_9621** | AI translation sheet (Replace vs. Simultaneous mode, language selector) | Multilingual Engine | Translation sheet supporting Persian/Dari, Pashto, Urdu, Arabic, Spanish, French, German with mode toggle. | Pluggable translation engine (`captionforge/translation.py`): OpenAI, Anthropic, DeepL, Transformers, Offline Rule-Based. | `test_translation.py`, dual-track exports |
| **IMG_9622** | Dual stacked timeline tracks (Primary + Secondary) | Timeline Architecture | Multi-lane timeline view in `timeline_view.dart` with independent track headers, drag handles, and cue blocks. | Multi-track ASS dialogue generation with distinct layer indices (`Layer 1`, `Layer 2`). | `screenshot_mobile_editor.png`, `verified_rendered_frame.png` |
| **IMG_9623** | Independent track styling & vertical position offsets | Multi-Track Layout | Per-track styling drawer with independent vertical position slider (`yPos` 0.0 to 1.0) and track color schemes. | ASS `\pos(x, y)` calculation using track-specific `yPos` and video height. | Dual tracks at `yPos: 0.70` (English) and `yPos: 0.84` (Persian) |
| **IMG_9624** | Caption sound effects (Pop, Click, Soft) with volume slider & trigger mode | Audio Synthesis | Expanded SFX tray with 9 synthesized sound effect chips, volume slider, and Sentence vs. Word trigger selector. | Real-time mathematical audio synthesis (`write_sfx`) for 9 SFX presets mixed via FFmpeg `amix`. | `test_smoke.py`, `test_expanded_sfx_and_graphic_overlays` |
| **IMG_9625** | One-line switch, text alignment (Left, Center, Right) | Text Layout | Format tray with `oneLine` toggle switch and segmented alignment buttons (Left, Center, Right). | ASS alignment tags (`\an4`, `\an5`, `\an6`) and whitespace collapsing. | Verified in `models_test.dart` and `test_smoke.py` |
| **IMG_9626** | Early subtitle display offset slider (0.0s to 1.0s) | Synchronization | Early timing slider in timing tray (`early` parameter) with millisecond-precision readout. | Offsets cue start times by `-early` seconds in timeline rendering and ASS script generation. | Tested in `models_test.dart` and `app.py` |
| **IMG_9627** | Split subtitle, merge subtitle, max characters per line, font size | Cue Manipulation | Split cue action on long press, merge cue button (`Icons.merge_type`), font size slider (14pt to 48pt). | Dynamic word regrouping and timecode re-allocation in backend models. | `widget_test.dart` |
| **IMG_9628** | Graphic overlay manager, track lock, track hide, track delete | Layer Control | Overlay templates tray with 8 Video 1 & Video 2 presets, track visibility toggle, track lock, track delete. | Backend `overlays: List[dict]` support with ASS multi-layer dialogue compiler. | `test_full_workflow.py` Step 10 & Bonus 4/5 |
| **IMG_9629** | Video color editing (Vivid, Warm, Cool, Muted, Brightness, Saturation) | Color Grading | Live 4x5 color matrix preview via `ColorFiltered` widget in Flutter with brightness/contrast/saturation sliders. | FFmpeg `eq=brightness=...:contrast=...:saturation=...` and `unsharp` video filter chain. | `verified_video1_style.mp4`, `verified_video2_style.mp4` |
| **IMG_9630** | Clear Voice noise reduction (Off, Light, Balanced, Strong) | Audio Processing | Clear Voice selector tray with real-time status badge in editor preview header. | FFmpeg `afftdn` FFT noise reduction filter (`nr=8`, `nr=14`, `nr=22`). | `test_smoke.py`, `test_full_workflow.py` Step 8 |
| **IMG_9631** | Export settings modal (Resolution, Aspect Ratio, Bitrate, Save to Gallery) | Final Rendering | Export modal with resolution chips (480p, 720p, 1080p), aspect ratios (9:16, 16:9, 1:1), and download action. | FFmpeg libx264 multi-aspect ratio rendering with faststart MP4 container flags. | 4 exported MP4s probed and validated |
