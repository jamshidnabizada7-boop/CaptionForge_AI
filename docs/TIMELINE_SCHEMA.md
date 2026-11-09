# CaptionForge Timeline & Project Data Schema

## 1. Overview
This document specifies the exact JSON data interchange schema used between the Flutter mobile client (`captionforge_mobile`) and the FastAPI backend (`captionforge`). It defines the hierarchical structure for video projects, multi-track subtitle cues, kinetic animations, graphic overlays, audio parameters, and visual grading effects.

---

## 2. Root Project Schema (`Project`)

```json
{
  "$schema": "https://captionforge.app/schemas/project-v2.json",
  "id": "proj_9f8c12a0e4",
  "title": "Orange Persian Typography Reel",
  "videoPath": "d:/CaptionForge_AI_Video_Subtitle_App/captionforge/data/video.mp4",
  "duration": 21.48,
  "aspectRatio": "9:16",
  "trim": {
    "start": 0.0,
    "end": 21.48
  },
  "tracks": [
    {
      "id": "track_primary_en",
      "name": "English Primary",
      "language": "en",
      "visible": true,
      "locked": false,
      "style": {
        "fontFamily": "Segoe UI",
        "fontSize": 24.0,
        "primaryColor": "#FFFFFF",
        "outlineColor": "#000000",
        "outlineWidth": 3.0,
        "shadowColor": "#000000",
        "shadowOffset": 2.0,
        "yPos": 0.70,
        "alignment": "center",
        "bgEnabled": true,
        "bgColor": "#80000000",
        "bgCornerRadius": 8.0,
        "bgPadding": 10.0,
        "animation": "Pop",
        "karaoke": true,
        "karaokeColor": "#FFD700",
        "oneLine": false,
        "early": 0.05
      },
      "cues": [
        {
          "id": "cue_001",
          "start": 0.00,
          "end": 3.20,
          "text": "Discover the Future of Video Subtitles",
          "words": [
            {"word": "Discover", "start": 0.00, "end": 0.65},
            {"word": "the", "start": 0.65, "end": 0.85},
            {"word": "Future", "start": 0.85, "end": 1.40},
            {"word": "of", "start": 1.40, "end": 1.60},
            {"word": "Video", "start": 1.60, "end": 2.10},
            {"word": "Subtitles", "start": 2.10, "end": 3.20}
          ]
        }
      ]
    },
    {
      "id": "track_secondary_fa",
      "name": "Persian Secondary",
      "language": "fa",
      "visible": true,
      "locked": false,
      "style": {
        "fontFamily": "Vazirmatn",
        "fontSize": 28.0,
        "primaryColor": "#FF6A00",
        "outlineColor": "#1A1A1A",
        "outlineWidth": 3.5,
        "shadowColor": "#000000",
        "shadowOffset": 2.5,
        "yPos": 0.84,
        "alignment": "center",
        "bgEnabled": false,
        "bgColor": "#00000000",
        "bgCornerRadius": 0.0,
        "bgPadding": 0.0,
        "animation": "Pop",
        "karaoke": false,
        "karaokeColor": "#FFFFFF",
        "oneLine": false,
        "early": 0.00
      },
      "cues": [
        {
          "id": "cue_fa_001",
          "start": 0.00,
          "end": 3.20,
          "text": "آینده زیرنویس‌های ویدیویی را کشف کنید",
          "words": [
            {"word": "آینده", "start": 0.00, "end": 0.70},
            {"word": "زیرنویس‌های", "start": 0.70, "end": 1.50},
            {"word": "ویدیویی", "start": 1.50, "end": 2.20},
            {"word": "را", "start": 2.20, "end": 2.40},
            {"word": "کشف", "start": 2.40, "end": 2.80},
            {"word": "کنید", "start": 2.80, "end": 3.20}
          ]
        }
      ]
    }
  ],
  "effects": {
    "brightness": 0.02,
    "contrast": 1.08,
    "saturation": 1.15,
    "sharpness": 1.0,
    "clearVoice": "Balanced",
    "sfx": "Pop",
    "sfxVolume": 50,
    "sfxTrigger": "sentence"
  },
  "overlays": [
    {
      "id": "ovl_hologram_01",
      "type": "glow_hologram",
      "text": "AI CORE",
      "xPos": 0.50,
      "yPos": 0.32,
      "start": 0.50,
      "end": 3.80,
      "color": "#00F0FF",
      "fontSize": 20.0,
      "fontFamily": "Segoe UI",
      "animation": "pulse"
    },
    {
      "id": "ovl_metallic_02",
      "type": "metallic_banner",
      "text": "AUSTRALIA",
      "xPos": 0.50,
      "yPos": 0.18,
      "start": 12.00,
      "end": 16.50,
      "color": "#E2E8F0",
      "fontSize": 32.0,
      "fontFamily": "Impact",
      "animation": "fade"
    },
    {
      "id": "ovl_bg_words_03",
      "type": "soft_bg_text",
      "text": "کپشن فورج • هوش مصنوعی",
      "xPos": 0.50,
      "yPos": 0.45,
      "start": 3.00,
      "end": 10.00,
      "color": "#FFFFFF",
      "fontSize": 22.0,
      "fontFamily": "Vazirmatn",
      "animation": "none"
    }
  ]
}
```

---

## 3. Sub-Component Specifications

### A. `VideoTrim`
- **`start`** (`number`, seconds, default `0.0`): Start playback and export offset.
- **`end`** (`number`, seconds, default `video.duration`): End cutoff offset.
- **Validation Rule:** `0.0 <= start < end <= video.duration`. Clamped in UI and export pipeline.

### B. `SubtitleStyle`
| Field | Type | Default | Description |
|---|---|---|---|
| `fontFamily` | `string` | `"Segoe UI"` | Typography font family (`Vazirmatn`, `Noto Sans Arabic`, `Segoe UI`, `Tahoma`, `Arial`). |
| `fontSize` | `number` | `24.0` | Base font size in points (14.0 to 48.0). |
| `primaryColor` | `string` | `"#FFFFFF"` | Main font fill color (hex format `#RRGGBB`). |
| `outlineColor` | `string` | `"#000000"` | Subtitle outline stroke color. |
| `outlineWidth` | `number` | `3.0` | Outline thickness in pixels (0.0 to 6.0). |
| `shadowColor` | `string` | `"#000000"` | Drop shadow color. |
| `shadowOffset` | `number` | `2.0` | Drop shadow offset distance in pixels (0.0 to 5.0). |
| `yPos` | `number` | `0.78` | Normalized vertical screen position (0.0 = top, 1.0 = bottom). |
| `alignment` | `string` | `"center"` | Alignment (`"left"`, `"center"`, `"right"`). Mapped to ASS `\an4`, `\an5`, `\an6`. |
| `bgEnabled` | `boolean`| `false` | When true, renders a rectangular backdrop bounding box. |
| `bgColor` | `string` | `"#80000000"` | Hex ARGB or RGBA color code for the background box. |
| `animation` | `string` | `"Pop"` | Entrance kinetic transition (`"Fade"`, `"Pop"`, `"Bounce"`, `"Slide"`, `"Zoom"`). |
| `karaoke` | `boolean`| `false` | When true, enables kinetic word-by-word active highlighting. |
| `karaokeColor` | `string` | `"#FFD700"` | Highlight color applied to currently spoken words. |
| `oneLine` | `boolean`| `false` | When true, forces single-line presentation with ellipsis overflow. |
| `early` | `number` | `0.0` | Advance timing offset in seconds (0.0 to 1.0). |

### C. `CaptionCue` & `WordTimestamp`
- **`id`** (`string`): Unique cue identifier.
- **`start`** (`number`, seconds): Cue start timecode.
- **`end`** (`number`, seconds): Cue end timecode.
- **`text`** (`string`): Full cue textual string.
- **`words`** (`array` of objects):
  - `word` (`string`): Individual token.
  - `start` (`number`, seconds): Token start time.
  - `end` (`number`, seconds): Token end time.

### D. `GraphicOverlay`
- **`id`** (`string`): Unique overlay identifier.
- **`type`** (`string`): Preset archetype (`"metallic_banner"`, `"glow_hologram"`, `"light_leak"`, `"flag_graphic"`, `"soft_bg_text"`, `"end_accent"`).
- **`text`** (`string`): Display label or text content.
- **`xPos`** (`number`): Normalized horizontal coordinate (0.0 = left edge, 1.0 = right edge).
- **`yPos`** (`number`): Normalized vertical coordinate (0.0 = top edge, 1.0 = bottom edge).
- **`start`** (`number`): In-point timestamp in seconds.
- **`end`** (`number`): Out-point timestamp in seconds.
- **`color`** (`string`): Primary thematic hex color.
- **`fontSize`** (`number`): Text size in points.
- **`fontFamily`** (`string`): Assigned typography font family.
- **`animation`** (`string`): Dynamic animation mode (`"none"`, `"pulse"`, `"fade"`, `"pop"`).

### E. `VideoEffects`
- **`brightness`** (`number`, default `0.0`): Brightness multiplier offset (-0.5 to 0.5).
- **`contrast`** (`number`, default `1.0`): Contrast multiplier (0.5 to 2.0).
- **`saturation`** (`number`, default `1.0`): Saturation multiplier (0.0 to 2.0).
- **`sharpness`** (`number`, default `1.0`): Unsharp edge sharpening strength (0.0 to 2.5).
- **`clearVoice`** (`string`, default `"Off"`): Audio denoising mode (`"Off"`, `"Light"`, `"Balanced"`, `"Strong"`).
- **`sfx`** (`string`, default `"None"`): Audio transient SFX preset (`"None"`, `"Pop"`, `"Whoosh"`, `"Impact"`, `"Click"`, `"Shimmer"`, `"Riser"`, `"Sweep"`, `"Glitch"`, `"Soft"`).
- **`sfxVolume`** (`number`, default `50`): SFX mixing volume percentage (0 to 100).
- **`sfxTrigger`** (`string`, default `"sentence"`): Trigger cadence (`"sentence"`, `"word"`).
