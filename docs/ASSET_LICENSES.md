# CaptionForge Asset Licenses & Attribution

All font and audio assets bundled in the CaptionForge native Android application (`app/src/main/res/font/` and `app/src/main/assets/sfx/`) and the Python FastAPI compositor (`captionforge/data/fonts/` and `captionforge/static/sfx/`) are royalty-free and cleared for commercial and open-source redistribution.

---

## 1. Curated Multi-Script Font Library (26 Verified Fonts)

All 26 bundled `.ttf` typefaces are sourced from Google Fonts and distributed under the **SIL Open Font License (OFL) 1.1** or **Apache License 2.0**.

### English / Latin Display, Sans, Serif & Monospace (12 Fonts)

| Font Family | Resource File (`app/src/main/res/font/`) | Studio Category | Supported Weights | License |
| :--- | :--- | :--- | :--- | :--- |
| **Inter** | `inter.ttf` | Modern Sans-Serif | 400, 500, 600, 700, 800, 900 | SIL OFL 1.1 |
| **Montserrat** | `montserrat.ttf` | Bold Cinematic | 400, 600, 700, 800, 900 | SIL OFL 1.1 |
| **Poppins** | `poppins.ttf` | Social-Media Captions | 400, 600, 700, 800, 900 | SIL OFL 1.1 |
| **Bebas Neue** | `bebas_neue.ttf` | Condensed Headlines | 400, 700 | SIL OFL 1.1 |
| **Anton** | `anton.ttf` | Bold Cinematic | 400, 700, 900 | SIL OFL 1.1 |
| **Oswald** | `oswald.ttf` | Condensed Headlines | 400, 500, 600, 700 | SIL OFL 1.1 |
| **League Spartan** | `league_spartan.ttf` | Social-Media Captions | 400, 600, 700, 800, 900 | SIL OFL 1.1 |
| **Archivo Black** | `archivo_black.ttf` | Bold Cinematic | 400, 700, 900 | SIL OFL 1.1 |
| **Playfair Display** | `playfair_display.ttf` | Documentary | 400, 600, 700, 800 | SIL OFL 1.1 |
| **Cinzel** | `cinzel.ttf` | Metallic Cinematic Titles | 400, 600, 700, 800, 900 | SIL OFL 1.1 |
| **Orbitron** | `orbitron.ttf` | Neon/Glowing Titles | 400, 600, 700, 800, 900 | SIL OFL 1.1 |
| **JetBrains Mono** | `jetbrains_mono.ttf` | Minimal Captions | 400, 500, 700, 800 | SIL OFL 1.1 |

### Persian / Dari, Pashto, Urdu & Arabic Multi-Script RTL Fonts (14 Fonts)

| Font Family | Resource File (`app/src/main/res/font/`) | Studio Category | Scripts Supported | License |
| :--- | :--- | :--- | :--- | :--- |
| **Vazirmatn** | `vazirmatn.ttf` | Modern Dari/Persian | `fa`, `ps`, `ar`, `ur`, `en` | SIL OFL 1.1 |
| **Lalezar** | `lalezar.ttf` | Bold Cinematic | `fa`, `ps`, `ar`, `ur`, `en` | SIL OFL 1.1 |
| **Noto Sans Arabic** | `noto_sans_arabic.ttf` | Modern Dari/Persian | `fa`, `ps`, `ar`, `ur` | SIL OFL 1.1 |
| **Noto Naskh Arabic** | `noto_naskh_arabic.ttf` | Pashto Typography | `ps`, `fa`, `ar`, `ur` | SIL OFL 1.1 |
| **Noto Nastaliq Urdu** | `noto_nastaliq_urdu.ttf` | Urdu Nastaliq | `ur`, `fa`, `ps`, `ar` | SIL OFL 1.1 |
| **Amiri** | `amiri.ttf` | Elegant Persian | `fa`, `ar`, `ps`, `ur`, `en` | SIL OFL 1.1 |
| **Cairo** | `cairo.ttf` | Arabic Calligraphy | `ar`, `fa`, `ps`, `ur`, `en` | SIL OFL 1.1 |
| **Tajawal** | `tajawal.ttf` | Modern Dari/Persian | `ar`, `fa`, `ps`, `en` | SIL OFL 1.1 |
| **IBM Plex Sans Arabic** | `ibm_plex_sans_arabic.ttf` | Documentary | `ar`, `fa`, `ps`, `ur`, `en` | SIL OFL 1.1 |
| **Reem Kufi** | `reem_kufi.ttf` | Arabic Calligraphy | `ar`, `fa`, `ps`, `en` | SIL OFL 1.1 |
| **Markazi Text** | `markazi_text.ttf` | Elegant Persian | `fa`, `ps`, `ar`, `ur`, `en` | SIL OFL 1.1 |
| **Lateef** | `lateef.ttf` | Pashto Typography | `ps`, `ur`, `fa`, `ar`, `en` | SIL OFL 1.1 |
| **Mirza** | `mirza.ttf` | Elegant Persian | `fa`, `ps`, `ur`, `ar`, `en` | SIL OFL 1.1 |
| **Scheherazade New** | `scheherazade_new.ttf` | Pashto Typography | `ps`, `fa`, `ar`, `ur`, `en` | SIL OFL 1.1 |

---

## 2. Studio Sound Effects & Background Music Library (60 Assets)

All 60 sound effects and background music loops are procedurally synthesized from pure mathematical DSP primitives (FM/subtractive oscillators, exponential pitch sweeps, harmonic chimes, resonant sub-bass envelopes, and filtered noise bursts) inside `SfxPlayerManager.kt` (Android runtime) and `captionforge/sfx_library.py` (Python FastAPI backend).

- **License**: **CC0 1.0 Universal (Public Domain Dedication)** — original procedural DSP synthesis authored for CaptionForge.
- **Categories Covered (23 Categories / 60 Distinct Presets)**:
  1. `Soft Pop` (`pop_bubble.wav`, `pop_velvet_air.wav`, `pop_warm_drop.wav`)
  2. `Punchy Pop` (`pop_crisp.wav`, `pop_punch_snap.wav`, `pop_kinetic_accent.wav`)
  3. `UI Click` (`click_tactile.wav`, `click_shutter.wav`, `click_glass_tap.wav`)
  4. `Text Click` (`text_typewriter_key.wav`, `text_mechanical_tick.wav`, `text_digital_blip.wav`)
  5. `Fast Whoosh` (`whoosh_fast_air.wav`, `whoosh_razor_cut.wav`, `whoosh_snap_whip.wav`)
  6. `Deep Whoosh` (`whoosh_deep_sub.wav`, `whoosh_dark_wind.wav`, `whoosh_heavy_pass.wav`)
  7. `Cinematic Swoosh` (`whoosh_cinematic_whip.wav`, `swoosh_velvet_pan.wav`, `swoosh_silk_banner.wav`)
  8. `Heavy Impact` (`impact_heavy_thump.wav`, `impact_sub_slam.wav`, `impact_anvil_strike.wav`)
  9. `Bass Drop` (`bass_808_drop.wav`, `bass_deep_sub.wav`, `bass_reese_dive.wav`)
  10. `Cinematic Boom` (`boom_trailer_quake.wav`, `boom_distant_thunder.wav`, `boom_epic_finale.wav`)
  11. `Riser` (`riser_tension.wav`, `riser_hyper_riser.wav`, `riser_cyber_charge.wav`)
  12. `Reverse Riser` (`reverse_crystal_suck.wav`, `reverse_air_vacuum.wav`)
  13. `Glitch` (`glitch_digital_byte.wav`, `glitch_stutter_burst.wav`, `glitch_matrix_tear.wav`)
  14. `Electric Pulse` (`pulse_neon_zap.wav`, `pulse_plasma_arc.wav`, `pulse_synth_throb.wav`)
  15. `Magical Shimmer` (`shimmer_celestial.wav`, `shimmer_golden_dust.wav`)
  16. `Sparkle` (`shimmer_sparkle.wav`, `sparkle_diamond_chime.wav`)
  17. `Light Sweep` (`light_sweep_laser_sheen.wav`, `light_sweep_prism_sweep.wav`, `light_sweep_flare_flash.wav`)
  18. `Transition Hit` (`transition_swoosh_hit.wav`, `transition_downbeat.wav`)
  19. `Atmospheric Ambience` (`ambient_ethereal_drone.wav`, `ambient_warm_pad.wav`)
  20. `Soft Transition` (`soft_breeze_fade.wav`, `soft_silk_turn.wav`)
  21. `Metallic Impact` (`metal_sword_ring.wav`, `metal_titanium_clang.wav`)
  22. `Logo Reveal` (`logo_cinematic_halo.wav`, `logo_quantum_bloom.wav`)
  23. `Background Music` (`music_cyber_pulse_loop.wav`, `music_persian_cinematic_strings.wav`, `music_documentary_ambient_bed.wav`, `music_trap_reel_beat.wav`)
