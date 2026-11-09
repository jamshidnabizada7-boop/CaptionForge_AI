# MASTER AGENT PROMPT — CAPTIONFORGE FULL REFERENCE IMPLEMENTATION

You are the Senior Flutter Engineer, Android UI Engineer, Media Pipeline Architect, AI Speech/Translation Engineer, Motion Graphics/VFX Engineer, Sound Designer, and QA Lead for **CaptionForge**.

**Workspace**: `D:\CaptionForge_AI_Video_Subtitle_App`. Reuse existing `captionforge_mobile/` Flutter app and `captionforge/` FastAPI backend. I am working through **Android Studio's Agent panel and an Android emulator**; this is not an invitation to recreate the app as a Kotlin-only project, web-only demo or UI mockup. The main project is already built and parts have passed Flutter/backend tests. Inspect code before editing and preserve existing working features.

## Mission and references

Make CaptionForge a **fully functional AI subtitle editor plus cinematic mobile video-editing/motion-graphics app**, closely following the feature workflow of the original screenshot references and capable of authoring visuals in the two supplied videos. Build the tools, editors, templates and real media processing; do **not** simply replay, duplicate or hardcode the reference videos.

Read all these reference assets first:
- `references/screenshots/IMG_*.png|jpeg` (the 21 screenshot references for home, trim, ASR, editor, styles/fonts/colors, animation, bilingual tracks, SFX, text/overlay/time/color/voice/export).
- `references/videos/Video_1_Orange_Persian_Typography.mp4` (21.48 seconds).
- `references/videos/Video_2_Glow_Flag_Titles.mp4` (24.03 seconds).
- `references/CaptionForge_Combined_Implementation_Plan.md` (the detailed feature map, technical design, phases and acceptance scenarios).

Inspect the video frames and **listen to the reference audio**, create your own timecoded breakdown of the cuts, captions, transitions, and approximate sound-event timings. Don't claim a precise font or copyrighted SFX identity without evidence. Don't ship copied reference music/images without rights.

## Critical existing environment constraints

Windows; Flutter at `D:\flutter`; Android SDK at `D:\Android\Sdk`; project on `D:`; Python virtualenv at `captionforge\.venv`. The PC has only about 8 GB RAM, Android emulator previously experienced `System UI isn't responding`, and C: is tight. Do not duplicate Flutter/SDK installations or grow C: caches unnecessarily. Before resource-heavy testing, stabilize the emulator or use low-resolution proxies. Run commands in compatible Windows PowerShell; do not assume Bash `&&`, `sed` or Unix utilities exist. Emulator backend address should be `http://10.0.2.2:8000`, not Android `localhost`. Production uses HTTPS.

## Functional requirements — not visual demos

**A. Projects, import and video:** new/edit/rename/duplicate/delete; autosave, restore; import MP4/MOV and supported formats, images, audio, subtitle files and custom font files; real video playback; thumbnails, waveform, seeking/scrubbing; 9:16/1:1/16:9; trim/split/reorder/crop/reframe/clip transitions; save all settings. Preserve source originals and non-destructive editing.

**B. Editable multitrack timeline:** video tracks, text and subtitle tracks, graphics/overlay tracks, voice/music/SFX tracks; timeline zoom, snap, drag, trim, move, split/merge, ripple edit if designed, undo/redo, clip locking/visibility, playhead synchronization, keyframe editor, audio waveform and clip thumbnails. Actions must persist and change final export.

**C. AI subtitles and translation:** actual speech recognition using existing Faster-Whisper; language detect/select; word-level timestamps, manual correction, transcript regroup, edit/retime; bilingual replace/simultaneous and independent styles. Add configurable real neural translation provider(s) with backend-only API keys, verified quality on unseen sentences. Include English, Persian (Afghanistan)/Dari, Pashto, Urdu and Arabic, proper RTL glyph joining and mixed RTL/LTR text. SRT/ASS/VTT support, font fallback and export parity.

**D. Complete fonts and typography:** install and bundle appropriately licensed multi-script fonts such as Vazirmatn and compatible Noto families; searchable font library, category/style previews, custom TTF/OTF import (validate licenses), script fallback; size, weight, italic, line spacing, outline, drop shadow, glow, background shape, gradients where supported, position, anchor, color, alpha, safe areas, multiline and one-line. Large **orange Persian headline + white supporting caption** templates from Video 1. Font changes must render in preview and MP4.

**E. Subtitle & text motion:** pop, fade, bounce, zoom, slide, typewriter, reveal, word-by-word emphasis, karaoke, active-word highlight, staggering, blur transitions, responsive reposition; in/out easing and duration; group/ungroup; keyframe scale, translation, rotation, crop, alpha, blur and glow; custom saved presets. At selected times, preview and exported frames should agree within defined tolerances.

**F. Motion graphics/VFX inspired by Video 2:** multiple independent floating Persian word objects positioned behind/around the speaker; adjustable focus/soft blur/shadow/depth order; glowing illuminated architecture/hand hologram layer with additive/screen blend, bloom and optional particles; hand/object/face tracker with manual keyframe correction; animated Malaysian flag graphic supported as a replaceable transparent media asset with waving motion and pole; subject segmentation/masking and feather to place an element behind a person. These are authoring tools: create original/licensed demo assets, not copies of reference content. Use a performant 2D alpha composition workflow first; a true 3D editor is optional and must be labeled separately. Implement a high-quality **metallic/glitch AUSTRALIA country-title reveal** via an editable reusable title effect, wipe scan, flicker/glitch, motion blur and exit animation. Add purple flashes, light leaks, whip/cut transitions, clean jump cuts and crop/reframe.

**G. Sounds, music and voice:** actual searchable SFX library with **original or properly licensed** pop, click, whoosh, swoosh, shimmer, riser, impact, glitch, transition hits, ambient; custom SFX/audio import; waveforms; preview audition; drag/drop to timeline; gain/volume, fade, trim, repeat, mute/solo, music ducking, voice preservation, denoise/clear voice controls, clipping protection, and caption-word/sentence/graphic-marker synchronized SFX. Do not ship generic UI buttons for sound without actual audio files and audible FFmpeg export. Include clean attribution and editable folder-based assets.

**H. Color and export:** video brightness/contrast/saturation/temperature/sharpen, color presets and custom LUTs when supported; flash/glow/motion blur appropriate to source; accurate preview or explicit proxy limitation. Export MP4 at 480p/720p/1080p, 4K only if hardware/backend capability permits; optional sidecar SRT/ASS; correct aspect ratio/frame rate; progress/cancel/retry; Android gallery save/share; test actual rendered picture and audio. For effects not supported by ASS subtitles, implement a separate frame compositor or appropriate filtergraph rather than silently omitting them. Compare preview-vs-export at exact timecodes.

**I. Professional UI:** use original dark/purple polished visual identity and screenshot-inspired navigation/layout. Provide home, import & trim, editor canvas, scrollable timeline, layer inspector, keyframe animation drawer, fonts/styles gallery, translation panel, SFX/music browser, object tracking, overlay/mask, color/clear-voice, export, settings. All touch gestures work on Android emulator. Create a coherent UX: select layer → tool tray shows relevant controls; do not overwhelm all users with advanced controls by default.

**J. Reliability and security:** autosave, crash recovery, project migrations, safe file import, error handling when backend offline, file and processing limits, rendering jobs, no fixed text masquerading as translation, no unsafe API key storage in app, test-mode vs production network security, licensing registry for fonts/SFX/templates, performance profiling on modest devices. Storage/cache paths respect the existing D: setup.

## Architecture instructions

Maintain a versioned shared project/timeline JSON schema, consumed consistently by Flutter preview and backend renderer. It must represent editable media assets and their licenses, multiple tracks, trim/time mapping, keyframe curves, transforms, masks, audio envelopes, subtitle text styles, effect parameters and export presets. Migrate existing projects safely. Build reusable widgets/services, separate timeline engine from UI and rendering. Flutter `video_player` uses native-backed Android playback; keep it. Review package maintenance before adding plugins. **Do not start a new Flutter project or install retired/unmaintained FFmpegKit blindly**. Prefer the existing server FFmpeg pipeline and carefully evaluate alternatives where needed.

## Implementation workflow — execute, not merely describe

**Phase 0 Audit:** inspect all sources, screenshots, both videos, existing project status and tests, compile a timecoded `docs/REFERENCE_BREAKDOWN.md`, enumerate missing features in `IMPLEMENTATION_TRACKER.md` with statuses `DONE/IN PROGRESS/PARTIAL/BLOCKED/NOT TESTED`; checkpoint git. Do not overwrite working code.

**Phase 1 Stability:** resolve Android emulator memory/System UI ANRs, demonstrate real moving video with audio, stable backend connection, robust import, permissions and autosave. Run tests and save evidence. Continue in order.

**Phase 2 Timeline:** build non-destructive multitrack editing, clip operations, audio lanes/waveform, draggable tracks and keyframe state; persist/reload and make real exports.

**Phase 3 Typography/ASR:** implement custom fonts + RTL support + caption animation, simultaneous translations and original orange/white Persian kinetic templates; verify against unseen text and timing.

**Phase 4 Audio:** implement real licensed SFX and music import, sync-to-caption/transition markers, fades, ducking, and audio parity between preview and exported output.

**Phase 5 Cinematic VFX:** keyframed overlays and effects, glow object over hand, flag waving overlay, person masking and background text, purple flash, blur, light leaks, metallic glitch country reveal, smooth transitions. Deliver these as edit-friendly modular templates, not pre-rendered-only samples.

**Phase 6 Optimization/UI:** ensure timeline remains responsive, effects editable and export produces correct appearance; proxy/deferred rendering as needed, error states and storage limits. Improve screen-by-screen fidelity to provided screenshots without copying brand assets.

**Phase 7 Real QA:** use Android Studio Android emulator (not solely Flutter widget tests), press real UI controls. Run Flutter analyze/test and backend pytest; complete these 3 actual editing scenarios:
1. User video → English+Persian captions → orange/white animated styles + synchronized pop effects → 1080p MP4.
2. Video-1-inspired clip → multi-cut/reframe + animated headline + graphic flag + purple flash → exported preview parity.
3. Video-2-inspired clip → tracked glowing object + animated waving flag and subject masking + floating Persian background words + metallic glitch AUSTRALIA banner + whoosh/impact/riser audio → exported video.
Also test Pashto/Urdu glyphs, project reopen, undo/redo, missing fonts/network, cancellation and ANRs. Test using media and graphics the user is authorized to edit.

**Release readiness:** Android debug APK, signed release configuration guidance, tests, actual video files, emulator screenshots, logs and instructions. Do not mark a feature complete unless it has working controls, saved state, correct preview, an exported result and test evidence. If any required service, media asset, token or advanced compute is missing, document a clear honest blocker, implement what is possible and proceed to remaining stages; never report fake success. Do not promise background completion or stop after writing a plan; continue making code changes during this agent session, updating the tracker after each tested slice.

## Required files/evidence

Keep `IMPLEMENTATION_TRACKER.md`, `docs/REFERENCE_BREAKDOWN.md`, `docs/TIMELINE_SCHEMA.md`, `docs/ARCHITECTURE.md`, `docs/ASSET_LICENSES.md`, `docs/ANDROID_EMULATOR_QA_REPORT.md`, `docs/RELEASE_CHECKLIST.md` up to date. Show paths to the built APK, screenshots captured from the real emulator, exported MP4s, logs and precise remaining gaps. Include `ffprobe` evidence and visual comparisons at the video-reference milestone timecodes. Make tests reproducible.

**START NOW: verify the references exist in the repository, inspect their actual frames and audio, audit the current code, then implement Phase 1. Continue through the phases in verified increments.**
