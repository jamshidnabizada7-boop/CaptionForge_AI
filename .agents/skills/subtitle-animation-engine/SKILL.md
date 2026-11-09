---
name: subtitle-animation-engine
description: Use this skill when implementing or tuning subtitle entrance/exit animations, active-word highlighting, and karaoke kinetic typography across Flutter preview and FFmpeg ASS export.
---

# Subtitle Animation Engine Guide for CaptionForge

This guide establishes the animation primitives and mapping between Flutter real-time UI animation and FFmpeg ASS override tags.

## Core Animation Types

| Animation | Flutter Preview Implementation | ASS Tag Export Mapping |
| :--- | :--- | :--- |
| **Fade** | `FadeTransition` with `Curves.easeInOut` | `{\fad(150,150)}` |
| **Pop** | Scale `0.7 -> 1.15 -> 1.0` via `TweenSequence` | `{\t(0,80,\fscx115\fscy115)\t(80,160,\fscx100\fscy100)}` |
| **Bounce** | Elastic curve bounce overshoot | `{\t(0,100,\fscy130)\t(100,200,\fscy90)\t(200,300,\fscy100)}` |
| **Slide Up** | `SlideTransition` offset `(0, 0.4) -> (0, 0)` | `{\move(x, y+60, x, y, 0, 150)}` |
| **Slide Down** | `SlideTransition` offset `(0, -0.4) -> (0, 0)` | `{\move(x, y-60, x, y, 0, 150)}` |
| **Zoom** | Zoom from `0.2 -> 1.0` | `{\t(0,150,\fscx100\fscy100)\fscx20\fscy20}` |
| **Karaoke** | Dynamic word highlight color sweep | `{\k<centiseconds>}` or active word color tags `{\c&H00D4FF&}` |

## Active-Word Highlighting (Karaoke)

When word-level timestamps are available:
1. **Flutter Live Preview**:
   - Compare `currentPlaybackPositionMs` against `cue.words[i].startMs` and `cue.words[i].endMs`.
   - The active word is styled with `highlightColor` (e.g. Vibrant Purple or Amber Yellow) and enlarged slightly (`fontSize * 1.1`).
   - Inactive words display the normal `textColor`.
2. **Exported ASS**:
   - Split cue into segments using ASS timed color tags:
   ```text
   {\c&H00D4FF&}ActiveWord{\c&HFFFFFF&} NormalWord NormalWord
   ```
   Or use standard ASS karaoke tags `{\k25}Word1 {\k40}Word2`.

## Timing & Frame Synchronization

- Target frame rate: 30 fps or 60 fps.
- Subtitle animations must be calculated relative to cue start time:
  `t_local = (current_video_time - cue.start_time)`.
- If local time is past animation duration (e.g. 150ms), settle at default rest state (scale 1.0, opacity 1.0, translation 0).

## Verification Checklist

- [ ] Flutter animation runs at 60 fps without dropped frames.
- [ ] Exported MP4 visually matches preview timing curves within 1 frame (< 33ms).
- [ ] Karaoke highlight follows spoken words accurately without jumping ahead.
- [ ] Entrance animations do not flash before their cue start timestamp.
