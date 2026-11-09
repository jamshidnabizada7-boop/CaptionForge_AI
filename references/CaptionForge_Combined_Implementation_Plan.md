# CaptionForge — Combined Full Implementation Plan

**Source references:** 21 original interface screenshots; `Video_1_Orange_Persian_Typography.mp4` (21.48 s); `Video_2_Glow_Flag_Titles.mp4` (24.03 s).  
**Target:** Existing Flutter `captionforge_mobile` application + existing Python/FastAPI `captionforge` backend in `D:\CaptionForge_AI_Video_Subtitle_App`.  
**Development environment:** Android Studio and its Agent panel, Android emulator, Windows PowerShell, Flutter in `D:\flutter`, Android SDK in `D:\Android\Sdk`.  
**Scope:** Genuine functional mobile editor capable of authoring footage with reference-style visual effects, not just a skin or a recreation of existing reference videos.

## 1. Observed reference techniques vs implementation proposals

### Video 1 (21.48 seconds): orange/white kinetic Persian typography

- **0–5 s:** Portrait interview with prominent, staggered bold orange Persian words layered with smaller white words; strong text shadow, glow/highlight and animated entrance; document shown to camera. Build editable text groups, independent word segments, anchor points, colors, emphasis, shadows, in/out keyframes.
- **6–9 s:** Closer framing; large emphasized Persian lettering; flag graphic appears behind/near the presenter with a brief purple-magenta lighting flare. Build overlay media clips (transparent PNG, WebP, alpha-capable MOV, and where supported WebM), tracked or keyed placements, flash/light-leak overlays, depth layer control.
- **10–15 s:** White multi-line explanatory subtitles and emphasized orange phrases appear as the shot changes. Build precise subtitle grouping, typography, per-word keyframes, smooth reveal/line breaking and auto-safe placement.
- **~19–21.5 s:** Reframed/wider shot, orange foreground headline, visually consistent palette. Build clip split/cut, reframing, timeline transitions, reusable text templates.

### Video 2 (24.03 seconds): hand-held glowing object + kinetic background and country reveal

- **~0–3 s:** Studio shot of seated male presenter; luminous miniature architecture-like graphic grows/brightens above his upturned hand. Build image/animated overlay import, glow/bloom, additive/screen blending, optional particle effects, keyframe scale/opacity, and an actual motion-tracking/hand attachment workflow. A rendered 2D overlay can create a 3D *look*; true interactive 3D needs a separate renderer and is an advanced optional phase.
- **~3–6 s:** Several faint Persian words appear around the presenter, with variations in size/opacity, blurred or behind subject. Build background text layers, animated placement, masking/subject segmentation, and controllable text depth order.
- **~6–10 s:** Large animated Malaysian flag and pole appear over/behind presenter, partly occluding background, while hand graphic remains. Build media overlay tracks, chroma/alpha compositing, foreground/background masking, tracked transform, animation path, playback parity.
- **~11–18 s:** Changing Persian words appear in space beside the presenter and near lower center with mixed sharpness and opacity. Build multi-object text animation, motion blur, in/out, fade, horizontal and vertical movement, per-element keyframes and templates.
- **~18–21 s:** An animated horizontal glitch/scanline metallic banner reveals a country title, **AUSTRALIA**, then clears. Build a title-template designer with wipe/mask reveal, crop, metallic look, distortion/glitch, motion blur, and timed intro/outro; use licensed/original assets.
- **~21–24 s:** Clean shot resumes, with a smaller red text emphasis near the bottom. Build font/color/size animation continuity and text safe areas.

Both sources contain audio tracks. **The precise identities/licenses of audible sound effects and music have not been established.** The editor must have a usable *original or properly licensed* sound library; users can also import their own audio and align it to visual events. Do not extract copyrighted SFX/music as reusable stock assets without authorization.

### Reference app screenshots (21 images)

The screenshot set establishes the UI workflow: home, project import and trim, processing and transcript review, video canvas, style/font/color/animation galleries, translation replace/simultaneous, independent multitrack captions, sound-effect modes and volume, one-line/alignment, early timing, text flow/split/merge, layers/overlays, color grading, clear voice and export. See `references/screenshots/` for the original images. Several images have duplicate filenames/variants; index the files actually supplied rather than assuming a missing `IMG_9631` image exists.

## 2. Product capabilities and acceptance criteria

| Module | Must implement | Completion evidence |
|---|---|---|
| Projects and media import | Create/rename/duplicate/delete, import video/audio/image/font/overlay, thumbnails and metadata, save/restore | Reopen real media project after app restart |
| Real video editor | Native-backed video frame preview, play/pause, seek, frame/time scrubbing, source trim, split/duplicate/reorder multiple clips, aspect crop/reframe, timelines | Test editor on Android emulator; preview contains actual moving source video |
| Timeline | Video track, multiple caption/text tracks, overlays, audio/SFX/music lanes, draggable segments, trim handles, waveform/thumbnails, snapping, zoom, keyframes, undo/redo | User gestures change persisted editor model and render |
| AI speech and subtitles | Faster-Whisper ASR, word timecodes, language selection, punctuation, editable grouping and corrections, re-align when necessary, SRT/VTT/ASS import/export | Compare word timecodes and export against spoken video |
| Languages and fonts | English, Persian (Afghanistan), Pashto, Urdu, Arabic; bidirectional shaping, proper punctuation, diverse licensed fonts, custom font import, fallback chains | Correct mixed English/RTL display in preview and exported frames |
| AI translation | Actual provider-backed context-aware translation (optional Gemini/OpenAI/etc) with secure server keys; original+translated tracks; language-specific formatting and correction | Translate previously unseen sentences; flag any unsupported offline provider |
| Text styling | Size, weight, color, gradient where supported, shadow, glow, outline, box, blur, opacity, stroke, letter/line spacing, position and responsive safe area | Visible correct output on Android and export |
| Subtitle animations | Pop, bounce, zoom, fade, slide, karaoke, word emphasis, tracking/timing, per-word stagger, custom in/out durations | Time-coded frame captures at beginning/mid/end of animation |
| Motion graphics | Independent text/media elements with transform/opacity keyframes, bezier easing, layer order, alpha, masks, blur, cinematic reveal templates | Recreate equivalent techniques from the two videos using editable user-supplied or licensed elements |
| Tracked VFX | Attach an asset to hand/face/object via real detection/tracking; manual keyframe override; glow, masking, subject-aware occlusion | Output stays aligned on a moving subject without noticeable drift in representative clips |
| Flag/title styles | Animated flag asset overlay, background Persian text, glitch metallic country-name bar, flash/light-leak transitions | Template demos exported as MP4 with editable layers |
| Audio | Voice, music and SFX tracks; licensed library (pop, whoosh, impact, click, shimmer, riser, sweep, glitch, ambient), import, drag/drop timing, trim, gain, fades, ducking, waveform | Each effect is heard at its actual timeline time and mixed into export |
| Sound sync | Attach sounds to specific caption words, sentence starts, markers, cuts or title reveals; avoid overlaps/clipping | A/B playback and exported audio proof |
| Voice processing | Denoising, enhancement with accessible intensity, preview-before/after when possible, preserve intelligibility | Compare waveforms/listenable outputs with effect on/off |
| Color and video effects | Exposure/brightness, saturation, contrast, temperature, sharpen, LUT/custom presets where supported, light leak, glow, blur, vignette | Preview close to rendered output; disclose proxy limitations |
| Export | 9:16/1:1/16:9, 480/720/1080, 4K when backend/hardware supports, video/audio codecs, MP4, SRT, progress/cancel/retry, save/share | MP4 `ffprobe`, visual frame comparison, audio check, correct duration, Android save/share |
| Reliability and privacy | Caching, autosave, crash recovery, permissions, server errors, upload limits, background rendering, project-specific access | Force restart and recover project; consistent file handling |

## 3. Architecture and edit model

**Maintain** Flutter as the Android client and FastAPI as the processing/export backend; do not rewrite as an unrelated Kotlin demo. Reuse stable existing functionality, including the live `video_player` integration and current ASR/export tests. Introduce a **versioned, non-destructive, JSON-editable timeline document** as the single source of truth, shared between mobile preview and server renderer. Model tracks and clips, clip-local and source times, overlapping clips, video/audio/caption/graphic/text layers, blend modes, masks, style IDs, assets, transformations, keyframe sequences, effect parameters, font resolution, transition graphs and output presets. Maintain a migration path from existing stored CaptionForge projects.

Use `video_player`/Android native-backed playback for actual frames; Flutter compositing/custom render for interactive preview; generated proxies for heavy VFX; server FFmpeg and appropriate compositing tools for authoritative render. An ASS subtitle renderer is suitable for many caption effects but **is not sufficient for every tracked VFX/mask/glitch**; use a deterministic frame compositor for effects unsupported by ASS, with transition/audio stages in FFmpeg. Avoid old retired FFmpegKit as a dependency without evaluating actively maintained options, licensing and long-term viability. For local Android testing, use `10.0.2.2:8000` to address the PC backend; production requires HTTPS and properly restricted network policy.

All imported fonts/media must have appropriate user rights; track attribution/licenses of bundled assets. Use cached previews and export parity profiles. Server-side project access must prevent users from reading each other's media; tokens/API secrets stay on the backend.

## 4. Phased execution (do not equate UI with completion)

**Phase 0 — Audit and baseline.** Inspect current repository and `IMPLEMENTATION_TRACKER.md`, existing tests, Android emulator, FastAPI setup, project models, prior bug reports, and reference bundle. Create per-reference storyboard + functional matrix. Run baseline tests and check git state. Preserve working code.

**Phase 1 — Stabilize mobile core.** Correct emulator ANRs/System UI instability, audio/video playback, correct server settings, upload/import, saving, persistent project state, permission model and debug logs. Use low-end emulator/proxies on 8 GB RAM Windows PC. Keep C: space low and caches on D:.

**Phase 2 — Non-destructive multitrack timeline.** Add actual video clip tracks and editing, trim/split/reorder, independent graphic/text/overlay/audio tracks, gesture handles, waveform, playhead synchronization, undo/redo, autosave. Implement time mapping for cuts, offsets and effects correctly.

**Phase 3 — Fonts, captions and translation.** Implement font catalog, custom font installation, Unicode/RTL export parity, ASR correction and timestamps, real translation provider, two+ subtitle lanes, karaoke and per-word text animators, font/outline/glow presets. Create reference-inspired orange and white animated Persian styles.

**Phase 4 — Sound and color.** Ship original/licensed audio effect pack with metadata; music import and mix; volume envelopes, fades, ducking, markers, auto-attach SFX to text animation, denoise, color presets and immediate useful previews.

**Phase 5 — Motion graphics and compositing.** Add keyframes/easing, glow/shadow/blur/blend masks, text behind subject, motion tracking and override, transparent flag overlays, camera motion/reframe, purple flashes, light leaks, animated glitch/scanning metallic title bar and layered architecture/hologram graphic. Begin with robust importable assets and 2D composites before optional high-cost true 3D or AI segmentation; do not claim complete without frame-tested outputs.

**Phase 6 — Templates and parity.** Add editable presets: Video1 Orange Persian Headline, Orange/White Explainer, Purple Flag Transition, Video2 Hand Glow, Soft Background Persian Words, Flag With Subject, Metallic AUSTRALIA Reveal, End Accent Title. Make styles editable and reusable; do not include unlicensed copies of reference assets.

**Phase 7 — QA, optimization, delivery.** On Android emulator, use real test videos and UI taps: import → edit → add captions → style → add 2–4 FX layers → audio/SFX → export → open MP4, repeat in Persian/Pashto/English, then compare rendered video against preview at timed frame checkpoints. Run Flutter analyze/test, backend pytest, full HTTP test, export ffprobe and visual/audio evidence; compile APK; produce honest functional status by module. Fix emulator memory ANRs. Real-phone evidence stays *not tested* unless a physical device was actually used.

## 5. Concrete end-to-end acceptance scenarios

1. **Subtitle portrait:** create a 9:16 video with English + Persian simultaneous word-timed subtitles, orange emphasis, white text, bounce highlight and soft pop SFX. Save, close, reopen, export, then verify timings and no Arabic-script glyph defects.
2. **Video 1 technique:** import a different lawful user video, edit headline words, cut and reframe, add orange/white kinetic text, translucent/flag overlay and short purple light transition. Timeline must remain editable. Export 1080p and show screenshots at 1, 8, 15 and 20 seconds.
3. **Video 2 technique:** on a lawful test clip, add a glowing object above a hand with keyed track positions, animated flag behind a subject with segmentation or manual mask, floating background Persian text, metallic/glitch AUSTRALIA reveal and end title; attach different whoosh/riser/impact cues. Export and inspect 2, 7, 15, 19 and 23 seconds.
4. **Robustness:** reopen/edit projects after crash/restart, cancel/retry export, handle missing font, offline backend, invalid media, API failure, long clips, rapid scrubbing and emulator low memory without silent loss.
5. **Audio:** voices audible and uncorrupted; all SFX at exact timeline cues; no clipping; music ducking behind voice works and persists in export.

Each acceptance scenario is `PASS` only with actual on-emulator or produced-media evidence. Mark all others `PARTIAL`, `BLOCKED`, or `NOT TESTED` honestly.

## 6. Output files the Android Studio coding agent must maintain

- `IMPLEMENTATION_TRACKER.md` — module status, evidence, gaps.
- `docs/REFERENCE_BREAKDOWN.md` — timecoded clips and 21 screenshot feature mapping.
- `docs/ARCHITECTURE.md` and `docs/TIMELINE_SCHEMA.md` — data model, render graph, migration, preview/export parity.
- `docs/ASSET_LICENSES.md` — all shipped fonts, SFX, templates and their licenses.
- `docs/ANDROID_EMULATOR_QA_REPORT.md` — actual emulator tests and ANR evidence.
- `docs/RELEASE_CHECKLIST.md` — signing and secure server requirements.
- Output APK in the Flutter build directory, a set of editable templates, screenshots and at least two real exported test MP4s.

## 7. Definition of done

A feature is only **DONE** if: (a) controls exist, (b) state persists and reopening works, (c) real-time preview is usable, (d) final export applies the feature, (e) the agent tested actual representative cases with an artifact or reproducible log. If one condition is missing, mark `PARTIAL` and specify what remains. Billing, advanced tracked 3D, background/offline neural translation, on-device export and cloud hosting are not to be marked finished without their actual services/requirements.
