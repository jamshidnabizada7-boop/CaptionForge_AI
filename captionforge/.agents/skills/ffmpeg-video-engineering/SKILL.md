---
name: ffmpeg-video-engineering
description: Use this skill when writing, debugging, or optimizing FFmpeg command pipelines for video transcoding, subtitle burning, audio processing, color grading, and SFX mixing in CaptionForge.
---

# FFmpeg Video Engineering Guide for CaptionForge

This guide establishes production rules, filter graph constructions, and commands for FFmpeg media processing in CaptionForge.

## Standards & Architecture

CaptionForge relies on FFmpeg for authoritative video export, audio extraction, noise suppression, color grading, sound effect mixing, and subtitle burning.

### Key Video Constraints
- **Container**: MP4 (`-f mp4 -movflags +faststart`).
- **Video Codec**: `libx264` (universal compatibility across iOS and Android).
- **Pixel Format**: `yuv420p` (mandatory for mobile playback hardware decoders).
- **Audio Codec**: `aac` at 192k bitrate, 48kHz sampling rate.
- **Color Range & Scaling**: 9:16 target aspect ratio (`1080x1920` or `720x1280`) using `scale` and `pad` filters:
  ```text
  scale=w=1080:h=1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2:black
  ```

## Filter Graph Construction

### 1. Subtitle Burning (ASS)
Burn formatted Advanced SubStation Alpha subtitles into video:
```bash
ffmpeg -i input.mp4 -vf "ass='subtitles.ass':fontsdir='assets/fonts'" -c:v libx264 -preset fast -crf 22 -c:a aac -b:a 192k output.mp4
```
*Note for Windows paths*: Escape backslashes and colons: `ass='subtitles.ass'` with `/` forward slashes.

### 2. Audio Enhancement & Noise Reduction
Use `afftdn` (FFT-based audio noise reduction) with calibrated levels:
- **Light**: `afftdn=nf=-25`
- **Balanced**: `afftdn=nf=-20`
- **Strong**: `afftdn=nf=-15:om=o`

Equalization for voice clarity:
```text
highpass=f=80,lowpass=f=12000,equalizer=f=3000:t=q:w=1.5:g=3
```

### 3. Caption Sound Effects Mixing
Mix sound effects at exact millisecond timestamps using `adelay` and `amix`:
```text
[1:a]adelay=1500|1500,volume=0.8[sfx1];[0:a][sfx1]amix=inputs=2:duration=first:dropout_transition=2[aout]
```

### 4. Video Color Grading Filters
Apply brightness, contrast, saturation, and unsharp masking:
- **Vivid**: `eq=contrast=1.15:saturation=1.25:brightness=0.02,unsharp=5:5:0.8:5:5:0.0`
- **Warm**: `colorbalance=rs=0.1:gs=0.0:bs=-0.1,eq=contrast=1.05:saturation=1.1`
- **Cool**: `colorbalance=rs=-0.1:gs=0.0:bs=0.15,eq=contrast=1.05:saturation=1.05`
- **Monochrome**: `eq=saturation=0.0:contrast=1.1`

## Essential Diagnostic Commands

```bash
# Verify FFmpeg binary and installed codecs
ffmpeg -version

# Probe container, streams, duration, and dimensions
ffprobe -v error -show_entries format=duration,size,bit_rate -show_entries stream=codec_name,width,height,r_frame_rate -of json input.mp4

# Extract audio as 16kHz mono WAV for AI transcription
ffmpeg -y -i input.mp4 -vn -acodec pcm_s16le -ar 16000 -ac 1 audio.wav
```

## Verification Checklist

- [ ] Output video plays back on both Android and iOS without black screens.
- [ ] Subtitle text is crisp, correctly shaped, and synchronized within 50ms of audio cues.
- [ ] Mixed sound effects do not cause clipping or distort speech audio.
- [ ] Video renders in 9:16 portrait format with proper letterboxing when input is landscape.
- [ ] Process runs non-interactively (`-y -nostdin`) and reports progress to stderr.
