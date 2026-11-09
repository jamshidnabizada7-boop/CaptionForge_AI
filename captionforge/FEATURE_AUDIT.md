# Screenshot-to-feature audit — 21 references

Status legend: **Built** (functional in local MVP); **AI setup** (functional code needs separately installed model/key); **Basic** (functional approximation, not production parity); **Next phase** (not implemented).

| Reference | Feature observed | Current status | Notes |
|---|---|---|---|
| IMG_9611 | Home, new project, attractive purple theme, premium banner | Built/Basic | Home and new project implemented; premium is informational, not a subscription purchase. |
| IMG_9612 | Import clip, choose segment / use full video | Built | Trim start and end in `Trim`; no drag-handle thumbnail trimmer yet. |
| IMG_9613–9615 | Upload progress, background generation, AI transcription | AI setup | Local background job + progress overlay; faster-whisper optional. |
| IMG_9616 | Video preview, 720p, export, timeline, selected clips | Built | Editing, multiple tracks, export, 480/720/1080 options. |
| IMG_9617 | Multiple subtitle style templates | Basic | Nine adjustable presets; reference thumbnail previews not replicated. |
| IMG_9618 | Font gallery with different scripts | Built/Basic | Font picker; rendering depends on system fonts. |
| IMG_9619 | Text/background colors and palettes | Built | Palette, custom hex color picker, translucent/opaque box control. |
| IMG_9620 | Subtitle transition/entrance animation gallery | Basic | Fade, Pop, Slide up/down, Bounce, Zoom in preview/export; subtler proprietary motion unavailable. |
| IMG_9621 | Translate subtitles, replace/simultaneous, language list | AI setup | Server-side translation route, includes Persian/Dari, Pashto, Urdu, Arabic, English, others. |
| IMG_9622–9624 | Bilingual subtitle timeline, independent tracks | Built | Up to 5 tracks, independent styles and positions. |
| IMG_9624 | Word or sentence SFX and sound volume | Built | Click/Pop/Soft sound synthesized/mixed into MP4. |
| IMG_9625 | One-line and alignment controls | Built | One-line switch, left/center/right alignment. |
| IMG_9626 | Shift caption start earlier | Built | 0–1s early adjustment. |
| IMG_9627 | Track text-flow and custom display | Basic | Track font size and text-flow; advanced adaptive per-word line breaks pending. |
| IMG_9628 | Multiple overlay presets, hide/lock/delete | Basic | Preset positions/boxes, hide/lock/delete work. Smart/depth/masked visual overlays need compositing/object segmentation. |
| IMG_9629 | Video color filters and fine adjustments | Built/Basic | Original/Vivid/Warm/Cool/Muted presets, brightness/contrast/saturation/sharpness. |
| IMG_9630 | Clear voice / noise reduction intensity | Built/Basic | Off/Light/Balanced/Strong via FFmpeg `afftdn`, not AI stem separation. |
| Across screenshots | Follow-face captions | Basic/AI setup | Experimental OpenCV detection once per subtitle, requires `opencv-python-headless`, not frame-by-frame smooth tracking. |
| Across screenshots | Caption text corrections, timing, undo and redo | Built | Timeline click, text edit, split, add, delete, cue times, history. |
| Across screenshots | Pro lock, purchases and product billing | Next phase | No payment or subscription backend (requires Apple/Google in-app purchase compliance). |
| Across screenshots | Production native iOS UI / Android APK | Next phase | Current build is responsive browser/PWA, NOT a compiled native app. |

## Quality standards for a full release

- Word-level multilingual ASR benchmark on 2–3 hours of clean/noisy speech per language, human correction UI, readable right-to-left text.
- Frame-accurate transcript/visual timing, trim behavior, 9:16/1:1/16:9 crops, safe areas.
- Native GPU-accelerated previews that visually match final export, stress-tested on Android/iOS devices.
- Smooth frame-by-frame face detection/tracking and depth-aware subtitle placement; on-device privacy controls.
- Background render queue, cancellation/retry, export progress, offline recovery, cloud project storage if desired.
- Licensed fonts and template assets, design system replacing proprietary content, subscription/payment implementation and app-store requirements.
