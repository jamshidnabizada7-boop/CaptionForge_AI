---
name: ai-speech-transcription
description: Use this skill when implementing, configuring, or evaluating AI speech recognition, word-level alignment, language detection, and audio preprocessing with Faster-Whisper.
---

# AI Speech Transcription Guide for CaptionForge

This guide establishes the workflow and architecture for local and server-side speech-to-text processing in CaptionForge.

## Core Transcription Engine: Faster-Whisper

CaptionForge utilizes `faster-whisper` powered by CTranslate2 for high-speed, quantized inference on CPU and GPU.

### Audio Preprocessing Pipeline
1. Ingest input video/audio of any supported format.
2. Extract clean 16 kHz, 16-bit, single-channel (mono) PCM WAV:
   ```bash
   ffmpeg -y -i input_video.mp4 -vn -acodec pcm_s16le -ar 16000 -ac 1 temp_audio.wav
   ```
3. Pass clean PCM buffer directly to `BatchedInferencePipeline` or `WhisperModel`.

### Model Selection & Initialization
```python
from faster_whisper import WhisperModel

# Initialize model (cpu int8 for compatibility, cuda float16 if GPU is present)
model = WhisperModel("small", device="auto", compute_type="default")

# Perform transcription with word timestamps and VAD
segments, info = model.transcribe(
    "temp_audio.wav",
    beam_size=5,
    word_timestamps=True,
    vad_filter=True,
    vad_parameters=dict(min_silence_duration_ms=500),
    language=None  # Auto-detect language
)
```

## Word-Level Cue Grouping Heuristic

Raw ASR segments often group 15-30 words together. Mobile video captions require punchy 2 to 5-word chunks for readability.

Grouping algorithm rules:
1. **Duration Limit**: Max 2.5 seconds per cue.
2. **Word Limit**: Max 4–5 words per cue for English, 3–4 words for Arabic-script languages.
3. **Punctuation Break**: Always break after periods, question marks, commas, or exclamation marks.
4. **Natural Pauses**: Split if silence between consecutive words exceeds 350ms.

## Multilingual Support Matrix

- **English (`en`)**: Supported out of the box with high accuracy.
- **Persian / Dari (`fa`)**: Supported. Requires normalized text formatting (half-spaces/zwnj handling).
- **Pashto (`ps`)**: Low-resource language. Use beam size >= 5 and temperature fallback.
- **Urdu (`ur`)**: Supported. Preserve Nastaliq or Naskh font compatibility.
- **Arabic (`ar`)**: Supported with diacritic handling.

## Verification Checklist

- [ ] Audio extraction outputs 16,000 Hz mono PCM without clipping.
- [ ] Language detection accurately identifies language code and confidence level.
- [ ] Word timestamps match audible speech with < 100ms offset.
- [ ] Grouped cues fit comfortably within the 9:16 mobile video viewport.
- [ ] Inference executes asynchronously without blocking the web or API server.
