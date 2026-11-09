# CaptionForge Asset Licenses & Provenance Registry

## 1. Overview
This registry catalogs the intellectual property, copyright provenance, open-source licensing terms, and distribution rights for all typographic fonts, sound effects, animation templates, and external software dependencies utilized within CaptionForge.

---

## 2. Typographic Fonts

| Font Family | Script & Target Languages | License | Provenance / Upstream Repository | Commercial Use Terms |
|---|---|---|---|---|
| **Vazirmatn** | Arabic, Persian, Dari, Urdu, Kurdish | **SIL Open Font License 1.1 (OFL)** | [github.com/rastikerdar/vazirmatn](https://github.com/rastikerdar/vazirmatn) by Saber Rastikerdar | Permitted freely in commercial and open-source software; embedding and bundling permitted. |
| **Noto Sans Arabic** | Arabic, Persian, Pashto, Urdu, Sindhi | **SIL Open Font License 1.1 (OFL)** | [fonts.google.com/noto/specimen/Noto+Sans+Arabic](https://fonts.google.com/noto/specimen/Noto+Sans+Arabic) by Google Fonts | Permitted freely in commercial and open-source software; embedding and bundling permitted. |
| **Segoe UI** | Latin, Cyrillic, Greek | **Microsoft System Typography** | Standard Windows system font | Available via local system font rendering and DirectWrite. Fallback to sans-serif on non-Windows targets. |
| **Tahoma / Arial** | Multilingual Latin & Arabic fallback | **Standard System Typography** | Cross-platform core font | Rendering via system font interfaces across desktop and mobile engines. |

---

## 3. Sound Effects (SFX) Audio Engine: 100% Algorithmic Mathematical Synthesis

All 9 sound effect presets in CaptionForge (`Pop`, `Whoosh`, `Impact`, `Click`, `Shimmer`, `Riser`, `Sweep`, `Glitch`, `Soft`) are **100% original algorithmic waveform syntheses** generated directly in-memory via Python's standard `math` and `wave` libraries:
- **Zero Third-Party Samples:** No downloaded WAVs, proprietary stock audio libraries, or third-party SFX recordings are bundled in the repository or binary output.
- **Synthesized Waveform Mathematics:**
  - *Pop:* Pure exponential sine sweep $f(t) = 900 \cdot (300/900)^{t/D}$ with fast decay envelope.
  - *Whoosh:* Modulated pseudo-random noise passed through an amplitude window.
  - *Impact:* Sub-harmonic 75 Hz sinusoidal transient with exponential resonant dampening.
  - *Click:* 1400 Hz acute impulse transient (18 ms duration).
  - *Shimmer:* Three-part overtone triad ($f_1 = 1200\text{ Hz}, f_2 = 1850\text{ Hz}, f_3 = 2450\text{ Hz}$).
  - *Riser:* Exponential frequency modulation from 180 Hz climbing to 1100 Hz.
  - *Sweep:* Downward linear frequency modulation from 1350 Hz to 280 Hz.
  - *Glitch:* Bit-crushed square wave periodic burst.
  - *Ambient / Soft:* Dual-frequency pad ($f_1 = 260\text{ Hz}, f_2 = 390\text{ Hz}$) with smooth cosine attack/decay.
- **Copyright & License:** **Public Domain / Creative Commons Zero (CC0)** & **MIT License**. Freely usable in commercial productions with zero royalties or attribution requirements.

---

## 4. Cinematic Templates & Graphic Overlay Designs

The 8 preset styles inspired by Video 1 and Video 2 are original declarative vector and ASS tag compositions designed specifically for CaptionForge:
1. `Orange Headline` (Vazirmatn Black + Orange `#FF6A00` + Pop entrance)
2. `Orange/White Explainer` (Dual-tier hierarchical track layout)
3. `Purple Flare` (Translucent diagonal light leak overlay)
4. `Hand Hologram` (Cyan `#00F0FF` glowing text card with pulse animation)
5. `Soft Persian Words` (Layer 0 background floating text with soft blur)
6. `Flag with Subject` (Subject-anchored graphic badge)
7. `Metallic Reveal` (Silver `#E2E8F0` metallic banner with steel outline)
8. `End Accent Title` (Lower-third crimson call-to-action title)

**License:** **MIT License**. Unrestricted redistribution, modification, and commercial application.

---

## 5. Software Libraries & Frameworks

### A. Mobile Client (`captionforge_mobile`)
- **Flutter SDK & Dart Engine:** BSD 3-Clause License (Google LLC).
- **`video_player` (`^2.9.2`):** BSD 3-Clause License (Flutter Community / Google LLC).
- **`http` (`^1.2.0`):** BSD 3-Clause License (Dart Team).
- **`file_picker` (`^8.1.7`):** MIT License (Miguel Ruivo).
- **`path` / `path_provider`:** BSD 3-Clause License (Dart Team).

### B. Backend Engine (`captionforge`)
- **FastAPI:** MIT License (Tiangolo / Sebastián Ramírez).
- **Uvicorn:** BSD 3-Clause License (Encode OSS).
- **Faster-Whisper:** MIT License (SYSTRAN).
- **CTranslate2:** MIT License (SYSTRAN).
- **OpenCV Python Headless:** Apache License 2.0 (OpenCV Foundation).
- **PyAV (`av`):** BSD 2-Clause License (Mike Boers).
- **Pydantic:** MIT License (Samuel Colvin).
- **FFmpeg (External Tool):** LGPL v2.1+ / GPL v2+ (FFmpeg team). Packaged under standard dynamic executable invocation guidelines without static linking violations.
