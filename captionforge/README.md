# CaptionForge — AI Video Subtitle Editor

**An original, mobile-first subtitle editor inspired by the 21 supplied screenshots.** This package includes a functioning **browser/PWA application** and a **Python/FastAPI + FFmpeg processing backend**. It is an initial build, **not a finished iOS App Store/Android Play Store app**, and it does not claim pixel-perfect parity with the reference product.

## What you can use today

- Upload MP4/MOV/WebM/M4V/MKV (500 MB or 30-minute cap for this local build), choose a clip, and save projects.
- View and edit up to five timed caption tracks; add, split, edit, duplicate, hide, lock, delete; undo/redo; import and export `.srt`.
- Mobile-style 9:16 preview, play/pause, timeline, and track selection.
- Style presets, Arabic-script text, fonts, colors, backgrounds, alignment, size, position, opacity, and basic preview animations.
- Offline, local AI transcription **once you install `faster-whisper`**. Generates word timestamps and short grouped cues. First usage downloads a model. Voice recognition accuracy varies by language; review Persian/Pashto/Urdu manually.
- AI translation to several languages using an optional server-side OpenAI API key. Supports replacement or simultaneous subtitle tracks.
- Export MP4 at 480p, 720p, 1080p, with embedded ASS captions and basic animation. Portrait 9:16 export, letterboxing landscape videos.
- Add short caption sound effects in exported video; video brightness/contrast/saturation/sharpness adjustments; FFmpeg noise reduction.
- Experimental face-following caption placement via optional OpenCV **sampled once per caption** on export. This is not robust continuous object tracking.

## Fast start: Windows 10/11

1. Install [Python 3.11+](https://www.python.org/downloads/) with **Add Python to PATH**.
2. Install [FFmpeg](https://ffmpeg.org/download.html) so `ffmpeg` and `ffprobe` are on PATH (test in Command Prompt using `ffmpeg -version`). For Windows, `winget install Gyan.FFmpeg` is a possible option.
3. Unzip this archive. Double-click **`run_windows.bat`** (or run it from Command Prompt). The launcher creates a Python virtual environment and installs core dependencies.
4. In the browser, open **http://127.0.0.1:8000**.
5. Choose **New project**. Upload a clip. In **Tracks** or the quick toolbar, press **Generate AI** (after AI setup), or **Import SRT**. Customize subtitles and press **Export**.

### Enable automatic voice-to-captions

Open Command Prompt **inside this folder** after running the launcher at least once:

```bat
.venv\Scripts\activate
pip install -r requirements-ai.txt
```

The first transcription loads/downloads the selected model (`small` by default). You need Internet access for the first model download, and memory/CPU for inference. You can change `model:'small'` in `static/app.js` to `base` or `tiny` on lower-end computers. The model is a separate dependency, not bundled in this archive.

### Enable AI translation

Set the API key **on the backend only**, never in client-side JS:

```bat
set OPENAI_API_KEY=your_key_here
run_windows.bat
```

The API key must be set in the Command Prompt that starts the server. Translating sends the **subtitle text** to the configured API service; the video itself is not sent by this translation function. Usage can incur provider charges. Set `OPENAI_TRANSLATION_MODEL` if you want to choose another supported model.

### Linux/macOS

Install Python 3.11+ and `ffmpeg`, then run:

```bash
bash run_linux_mac.sh
# Open http://127.0.0.1:8000
# Optional AI: source .venv/bin/activate && pip install -r requirements-ai.txt
# Optional translation: OPENAI_API_KEY=... uvicorn app:app --host 127.0.0.1 --port 8000
```

## Main workflow

**Home → New project → Clip/trim → AI subtitles or SRT import → Editor/timeline → Style/Fonts/Colors/Animations → Translate (optional) → Filters/SFX/Clear Voice → Export.**

Tap a caption block to edit the actual words and timing. Each track has its own design and display position. To add a second language without a key, import a second SRT and edit it manually. To create blank tracks, choose **Tracks → Add track**.

The preview shows text changes, effects and placement, but the **exported MP4 is authoritative** for final typography, FFmpeg filters, SFX and denoising. Available fonts depend on **server-installed fonts**. For Persian/Dari/Pashto/Urdu, install a correctly licensed Arabic-script font, e.g. Vazirmatn or Noto Sans Arabic, on the rendering machine.

## Project structure

```text
captionforge/
  app.py                  FastAPI REST, AI captioning, translation, ASS renderer, FFmpeg
  static/index.html       Mobile-first editor layout
  static/styles.css       Dark-purple visual design
  static/app.js           Timeline, editor controls, client interactions
  static/manifest.json    Installable web app metadata
  requirements.txt        Core dependencies
  requirements-ai.txt     Optional ASR + face tracking
  run_windows.bat         Windows launcher
  run_linux_mac.sh        Linux/macOS launcher
  FEATURE_AUDIT.md        Detailed screenshot-to-feature mapping
  NATIVE_APP_ROADMAP.md   Android/iOS production architecture
  tests/                  Small video export smoke tests
  data/                   Generated on first run, stores user videos + projects (not shipped)
```

## Run tests

```bash
python -m pip install -r requirements.txt pytest
python -m pytest -q tests/test_smoke.py
```

Tests cover upload, subtitle import, Unicode captions, style persistence, SRT export, MP4 rendering, audio effects, and denoising. The provided synthetic test clip is only 3 seconds, so the tests are fast. Model-based AI transcription, translation API, and native installation are **not tested** by these smoke tests.

## Scope and security

This is a **local developer prototype**: it has no accounts, payment system, moderation, storage encryption, service quotas, or production job queue. The server binds to `127.0.0.1` by default. Do **not** expose it directly to the public Internet without authentication, CORS/security review, upload malware/format checks, quotas, TLS, a database, persistent job queue, and isolated FFmpeg workers. Back up the `data/` directory if you need to retain projects.

### What still needs work for true screenshot parity

The reference product has sophisticated animated templates, object-aware overlays, fluid face/object tracking, polished per-word highlighting, paid premium subscriptions, and highly optimized native mobile workflows. This version provides the editor foundation and basic/experimental variants of those capabilities; details are itemized in `FEATURE_AUDIT.md` and `NATIVE_APP_ROADMAP.md`.
