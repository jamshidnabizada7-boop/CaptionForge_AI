---
name: video-ai-effects
description: Use this skill when implementing, refining, or troubleshooting face tracking, smart caption positioning, visual overlays, and clear voice audio enhancements in CaptionForge.
---

# Video AI Effects Guide for CaptionForge

This guide covers computer vision face tracking, dynamic subtitle placement, and audio voice clarity processing.

## Face Tracking & Smart Caption Positioning

### Principles
- Keep subtitles anchored dynamically above or below the speaker's face, or avoid obstructing the speaker's face.
- Prevent jitter: Subtitle coordinates must be filtered with an Exponential Moving Average (EMA) or Kalman Filter across video frames.

### Face Tracking Algorithm
```python
import cv2
import numpy as np

class FaceTracker:
    def __init__(self, smoothing_factor=0.85):
        # MediaPipe Face Detection or Haar Cascade / DNN
        self.face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')
        self.last_pos = None
        self.alpha = smoothing_factor

    def track_face(self, frame):
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        faces = self.face_cascade.detectMultiScale(gray, scaleFactor=1.2, minNeighbors=5, minSize=(80, 80))
        if len(faces) == 0:
            return self.last_pos  # Retain previous position or fallback
        
        # Select the primary (largest) face
        primary_face = max(faces, key=lambda f: f[2] * f[3])
        x, y, w, h = primary_face
        center_x = x + w / 2.0
        bottom_y = y + h
        
        target_pos = (center_x, bottom_y)
        if self.last_pos is None:
            self.last_pos = target_pos
        else:
            self.last_pos = (
                self.alpha * self.last_pos[0] + (1 - self.alpha) * target_pos[0],
                self.alpha * self.last_pos[1] + (1 - self.alpha) * target_pos[1]
            )
        return self.last_pos
```

### Positioning & Safety Clamps
- Subtitle vertical position is computed as:
  `subtitle_y = face_bottom_y + offset_y`
- **Safe Area Enforcements**:
  - `min_y = frame_height * 0.10` (stay clear of top navigation/header)
  - `max_y = frame_height * 0.85` (stay clear of bottom controls and TikTok/Reels caption areas)
  - `min_x = frame_width * 0.10`, `max_x = frame_width * 0.90`

## Clear Voice & Audio Enhancement

1. **Noise Profiling**: Detect stationary noise during non-speech intervals.
2. **Frequency Filtering**:
   - `highpass=f=85`: Eliminates microphone rumble and low-end air conditioning hum.
   - `lowpass=f=11000`: Cuts high-frequency hiss.
   - `equalizer=f=2800:t=q:w=1.2:g=3.5`: Boosts speech intelligibility and presence.
3. **Dynamic Range Compression**: Even out whisper and shouting levels so voices remain audible without clipping.

## Verification Checklist

- [ ] Face coordinates update smoothly across cuts without jarring leaps.
- [ ] Subtitle never renders outside screen bounds or over the speaker's eyes/mouth.
- [ ] Clear Voice does not introduce robotic or watery phaser artifacts to natural speech.
- [ ] Processing runs efficiently (target < 0.2x real-time on GPU, < 1.0x on CPU).
