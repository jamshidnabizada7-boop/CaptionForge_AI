---
name: multilingual-subtitles
description: Use this skill when managing bidirectional text rendering, Arabic-script shaping, translation tracks, and simultaneous bilingual subtitle layouts in CaptionForge.
---

# Multilingual Subtitles Guide for CaptionForge

This guide covers bidirectional (BiDi) text layout, font shaping, translation modes, and dual-track synchronization.

## Bidirectional (BiDi) & Script Shaping

Languages like Persian/Dari, Pashto, Urdu, and Arabic are written Right-to-Left (RTL). When mixed with numbers or Latin text, standard string operations can reverse words or disconnect glyphs.

### Rules for RTL Text Rendering
1. **HarfBuzz / Complex Text Shaping**: Never reverse character strings manually in code (e.g. `s[::-1]`). Arabic glyphs must connect contextually (isolated, initial, medial, final).
2. **Font Bundling**: Bundle high-legibility, open-source fonts:
   - **Vazirmatn** (OFL license) for Persian, Dari, and general Arabic-script content.
   - **Noto Sans Arabic** / **Noto Nastaliq Urdu** for Arabic and Urdu.
   - **Montserrat** / **Inter** for Latin scripts.
3. **ASS Alignment (`\an`)**:
   - For RTL captions centered at bottom: `Alignment=2` (bottom center), `MarginL=40`, `MarginR=40`.
   - For left/right justified tracks: Set alignment flags appropriately (`\an1`, `\an3`).

## Dual Translation Modes

### 1. Replace Mode
- Replaces the active track's text with translated text.
- Maintains exact original cue timestamps (`start`, `end`).
- If translated text is significantly longer, splits into sub-cues or scales down font size.

### 2. Simultaneous (Bilingual) Mode
- Displays two independent subtitle tracks at once (e.g. English + Persian).
- **Track Stacking**: Primary language positioned at bottom (`Alignment=2`, `MarginV=120`), Secondary language positioned above it (`Alignment=2`, `MarginV=210`) or at top (`Alignment=8`, `MarginV=100`).
- Each track retains independent style:
  - Font family, size, text color, outline, background highlight.
  - Independent visibility toggle, lock toggle, and export flag.

## SRT / VTT / ASS Interchange Format

### ASS Style Header Example:
```ini
[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: PrimaryFa,Vazirmatn,54,&H00FFFFFF,&H000000FF,&H00000000,&H80000000,1,0,0,0,100,100,0,0,1,3,0,2,40,40,120,1
Style: SecondaryEn,Montserrat,38,&H0000D4FF,&H000000FF,&H00000000,&H801A162B,1,0,0,0,100,100,0,0,1,2,0,2,40,40,210,1
```

## Verification Checklist

- [ ] Persian/Arabic text connects letters smoothly without isolated glyph bugs.
- [ ] Mixed English and Arabic text renders in the correct logical reading direction.
- [ ] Simultaneous subtitle tracks do not overlap or collide vertically.
- [ ] Font fallback renders without boxes or missing glyph squares (tofu).
- [ ] Exported ASS file loads and plays accurately in standard media players (VLC, mpv).
