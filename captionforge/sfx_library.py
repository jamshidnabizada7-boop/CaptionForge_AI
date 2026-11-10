"""
CaptionForge Studio Sound Effects & Music Loop Library (60 Distinct Presets across 23 Categories).
Synthesizes real 44.1kHz 16-bit PCM WAV files for both frontend preview and FFmpeg multi-track export.
"""

import math
import os
import random
import struct
import wave
from pathlib import Path
from typing import Any, Dict, List, Optional

SAMPLE_RATE = 44100

SFX_CATALOG: List[Dict[str, Any]] = [
    # 1. Soft Pop (3)
    {"id": "pop_crisp", "name": "Crisp Caption Pop", "category": "Soft Pop", "alias": "Pop", "duration": 0.16, "filename": "pop_crisp.wav", "description": "Warm rounded plosive pop for clean caption entrances"},
    {"id": "pop_bubble", "name": "Liquid Bubble Pop", "category": "Soft Pop", "alias": "Pop", "duration": 0.20, "filename": "pop_bubble.wav", "description": "Upward pitch-swept organic water drop pop"},
    {"id": "pop_velvet", "name": "Velvet Subtle Pop", "category": "Soft Pop", "alias": "Pop", "duration": 0.15, "filename": "pop_velvet.wav", "description": "Low-pass muted studio pop for documentary subtitles"},
    # 2. Punchy Pop (3)
    {"id": "pop_punch_snap", "name": "Punchy Creator Pop", "category": "Punchy Pop", "alias": "Pop", "duration": 0.18, "filename": "pop_punch_snap.wav", "description": "High-transient creator pop for Reference Video 1 hero words"},
    {"id": "pop_elastic_boing", "name": "Elastic Kinetic Pop", "category": "Punchy Pop", "alias": "Pop", "duration": 0.24, "filename": "pop_elastic_boing.wav", "description": "Bouncy pitch-bent pop for word-by-word kinetic reveals"},
    {"id": "pop_double_tap", "name": "Double Tap Pop", "category": "Punchy Pop", "alias": "Pop", "duration": 0.22, "filename": "pop_double_tap.wav", "description": "Rapid dual-transient pop accent for emphasis badges"},
    # 3. UI Click (3)
    {"id": "click_tactile", "name": "Tactile Mechanical Click", "category": "UI Click", "alias": "Click", "duration": 0.10, "filename": "click_tactile.wav", "description": "Crisp mechanical switch click for UI & HUD overlays"},
    {"id": "click_shutter", "name": "Camera Shutter Click", "category": "UI Click", "alias": "Click", "duration": 0.14, "filename": "click_shutter.wav", "description": "Double-transient mirror slap shutter sound"},
    {"id": "click_glass_tap", "name": "Glass UI Tap", "category": "UI Click", "alias": "Click", "duration": 0.12, "filename": "click_glass_tap.wav", "description": "Crystalline high-frequency glass tap"},
    # 4. Text Click (3)
    {"id": "click_typewriter_key", "name": "Typewriter Key Strike", "category": "Text Click", "alias": "Click", "duration": 0.11, "filename": "click_typewriter_key.wav", "description": "Authentic typewriter hammer strike for kinetic text"},
    {"id": "click_digital_tick", "name": "Digital Telemetry Tick", "category": "Text Click", "alias": "Click", "duration": 0.09, "filename": "click_digital_tick.wav", "description": "Precision micro-tick for rapid word-by-word captions"},
    {"id": "click_wood_rim", "name": "Warm Woodblock Click", "category": "Text Click", "alias": "Click", "duration": 0.13, "filename": "click_wood_rim.wav", "description": "Organic acoustic wood click for explainer bullet points"},
    # 5. Fast Whoosh (3)
    {"id": "whoosh_fast_air", "name": "Fast Air Whoosh", "category": "Fast Whoosh", "alias": "Whoosh", "duration": 0.36, "filename": "whoosh_fast_air.wav", "description": "Tight aerodynamic air rush for fast caption slide-ins"},
    {"id": "whoosh_razor_whip", "name": "Razor Whip Cut", "category": "Fast Whoosh", "alias": "Whoosh", "duration": 0.28, "filename": "whoosh_razor_whip.wav", "description": "Ultra-fast high-velocity whip cut for Reels & Shorts"},
    {"id": "whoosh_snap_pass", "name": "Snap Pass Whoosh", "category": "Fast Whoosh", "alias": "Whoosh", "duration": 0.32, "filename": "whoosh_snap_pass.wav", "description": "Snappy flyby for word transitions"},
    # 6. Deep Whoosh (2)
    {"id": "whoosh_deep_sub", "name": "Deep Sub Whoosh", "category": "Deep Whoosh", "alias": "Whoosh", "duration": 0.58, "filename": "whoosh_deep_sub.wav", "description": "Low-frequency chest-thumping cinematic air sweep"},
    {"id": "whoosh_dark_tunnel", "name": "Dark Tunnel Pass", "category": "Deep Whoosh", "alias": "Whoosh", "duration": 0.65, "filename": "whoosh_dark_tunnel.wav", "description": "Resonant dark formant whoosh for scene changes"},
    # 7. Cinematic Swoosh (3)
    {"id": "whoosh_cinematic_whip", "name": "Cinematic Whip Swoosh", "category": "Cinematic Swoosh", "alias": "Whoosh", "duration": 0.48, "filename": "whoosh_cinematic_whip.wav", "description": "Wide-band theatrical swoosh with sub-tail"},
    {"id": "swoosh_velvet_glide", "name": "Velvet Glide Swoosh", "category": "Cinematic Swoosh", "alias": "Whoosh", "duration": 0.52, "filename": "swoosh_velvet_glide.wav", "description": "Smooth silk-like swoosh for lower-third reveals"},
    {"id": "swoosh_orbital_spin", "name": "Orbital Spin Swoosh", "category": "Cinematic Swoosh", "alias": "Whoosh", "duration": 0.55, "filename": "swoosh_orbital_spin.wav", "description": "Doppler pitch-shifted rotational swoosh"},
    # 8. Heavy Impact (3)
    {"id": "impact_sub_slam", "name": "Sub Slam Impact", "category": "Heavy Impact", "alias": "Impact", "duration": 0.65, "filename": "impact_sub_slam.wav", "description": "Massive punchy sub slam with crisp transient crack"},
    {"id": "impact_heavy_thump", "name": "Heavy Chest Thump", "category": "Heavy Impact", "alias": "Impact", "duration": 0.55, "filename": "impact_heavy_thump.wav", "description": "Warm low-end punch for bold headline hits"},
    {"id": "impact_anvil_strike", "name": "Titan Anvil Hit", "category": "Heavy Impact", "alias": "Impact", "duration": 0.70, "filename": "impact_anvil_strike.wav", "description": "Aggressive trailer impact for hero title drops"},
    # 9. Bass Drop (3)
    {"id": "bass_808_drop", "name": "808 Sub Bass Drop", "category": "Bass Drop", "alias": "Bass", "duration": 0.85, "filename": "bass_808_drop.wav", "description": "Classic 808 pitch-dive sub drop with warm saturation"},
    {"id": "bass_deep_sub", "name": "Deep Cinema Sub Dive", "category": "Bass Drop", "alias": "Bass", "duration": 0.92, "filename": "bass_deep_sub.wav", "description": "Ultra-clean 110Hz to 32Hz sine sub dive"},
    {"id": "bass_reese_drop", "name": "Reese Neuro Bass Drop", "category": "Bass Drop", "alias": "Bass", "duration": 0.88, "filename": "bass_reese_drop.wav", "description": "Detuned dual-saw sub bass drop for tech edits"},
    # 10. Cinematic Boom (2)
    {"id": "boom_trailer_seismic", "name": "Seismic Trailer Boom", "category": "Cinematic Boom", "alias": "Impact", "duration": 1.15, "filename": "boom_trailer_seismic.wav", "description": "Long-decay theatrical boom with cavern rumble"},
    {"id": "boom_distant_thunder", "name": "Distant Horizon Boom", "category": "Cinematic Boom", "alias": "Impact", "duration": 1.05, "filename": "boom_distant_thunder.wav", "description": "Deep rolling documentary boom for chapter titles"},
    # 11. Riser (3)
    {"id": "riser_tension", "name": "Tension Pitch Riser", "category": "Riser", "alias": "Riser", "duration": 0.90, "filename": "riser_tension.wav", "description": "Accelerating harmonic tension build before a reveal"},
    {"id": "riser_hyper_riser", "name": "Hyper Shimmer Riser", "category": "Riser", "alias": "Riser", "duration": 0.85, "filename": "riser_hyper_riser.wav", "description": "Bright exponential sweep riser with air noise"},
    {"id": "riser_cyber_charge", "name": "Cyber Turbine Charge", "category": "Riser", "alias": "Riser", "duration": 0.80, "filename": "riser_cyber_charge.wav", "description": "Futuristic hologram power-up riser"},
    # 12. Reverse Riser (2)
    {"id": "reverse_crystal_suck", "name": "Reverse Crystal Suck", "category": "Reverse Riser", "alias": "Riser", "duration": 0.68, "filename": "reverse_crystal_suck.wav", "description": "Inward vacuum reverse swell leading into a cut"},
    {"id": "reverse_vocal_swell", "name": "Reverse Air Swell", "category": "Reverse Riser", "alias": "Riser", "duration": 0.72, "filename": "reverse_vocal_swell.wav", "description": "Smooth reverse harmonic inhale transition"},
    # 13. Glitch (3)
    {"id": "glitch_digital_byte", "name": "Digital Byte Glitch", "category": "Glitch", "alias": "Glitch", "duration": 0.30, "filename": "glitch_digital_byte.wav", "description": "Bit-crushed data corruption burst for tech titles"},
    {"id": "glitch_stutter_burst", "name": "Stutter Buffer Glitch", "category": "Glitch", "alias": "Glitch", "duration": 0.34, "filename": "glitch_stutter_burst.wav", "description": "Rapid gated square-wave buffer stutter"},
    {"id": "glitch_modem_chirp", "name": "Cyber Telemetry Chirp", "category": "Glitch", "alias": "Glitch", "duration": 0.26, "filename": "glitch_modem_chirp.wav", "description": "High-speed hologram data packet lock sound"},
    # 14. Electric Pulse (2)
    {"id": "pulse_neon_hum", "name": "Neon Plasma Pulse", "category": "Electric Pulse", "alias": "Glitch", "duration": 0.45, "filename": "pulse_neon_hum.wav", "description": "Warm 120Hz electric arc pulse for glowing borders"},
    {"id": "pulse_tesla_zap", "name": "Tesla Arc Zap", "category": "Electric Pulse", "alias": "Glitch", "duration": 0.32, "filename": "pulse_tesla_zap.wav", "description": "Snappy high-voltage electric spark accent"},
    # 15. Magical Shimmer (2)
    {"id": "shimmer_celestial", "name": "Celestial Hologram Shimmer", "category": "Magical Shimmer", "alias": "Shimmer", "duration": 0.85, "filename": "shimmer_celestial.wav", "description": "Lush harmonic chord shimmer for Reference Video 2 hologram"},
    {"id": "shimmer_aurora_veil", "name": "Aurora Dream Veil", "category": "Magical Shimmer", "alias": "Shimmer", "duration": 0.90, "filename": "shimmer_aurora_veil.wav", "description": "Floating angelic overtone shimmer for poetic RTL captions"},
    # 16. Sparkle (2)
    {"id": "shimmer_sparkle", "name": "Crystal Starlight Sparkle", "category": "Sparkle", "alias": "Shimmer", "duration": 0.58, "filename": "shimmer_sparkle.wav", "description": "Cascading bell arpeggio sparkle for luxury highlights"},
    {"id": "sparkle_fairy_chime", "name": "Diamond Twinkle Chime", "category": "Sparkle", "alias": "Shimmer", "duration": 0.50, "filename": "sparkle_fairy_chime.wav", "description": "Bright high-register bell chime for keyword emphasis"},
    # 17. Light Sweep (3)
    {"id": "light_sweep_laser_sheen", "name": "Laser Sheen Sweep", "category": "Light Sweep", "alias": "Light Sweep", "duration": 0.58, "filename": "light_sweep_laser_sheen.wav", "description": "Specular metallic light sweep across 3D titles"},
    {"id": "light_sweep_prism_sweep", "name": "Prism Flare Sweep", "category": "Light Sweep", "alias": "Light Sweep", "duration": 0.64, "filename": "light_sweep_prism_sweep.wav", "description": "Glass optical flare sweep for Purple Light Flare transition"},
    {"id": "light_sweep_anamorphic", "name": "Anamorphic Lens Streak", "category": "Light Sweep", "alias": "Light Sweep", "duration": 0.60, "filename": "light_sweep_anamorphic.wav", "description": "Warm horizontal cinema lens flare sweep"},
    # 18. Transition Hit (3)
    {"id": "transition_swoosh_hit", "name": "Swoosh Into Impact Hit", "category": "Transition Hit", "alias": "Transition", "duration": 0.58, "filename": "transition_swoosh_hit.wav", "description": "Fast rush culminating in a tight sub-punch"},
    {"id": "transition_downbeat", "name": "Downbeat Scene Drop", "category": "Transition Hit", "alias": "Transition", "duration": 0.62, "filename": "transition_downbeat.wav", "description": "Rhythmic downbeat accent for clip cuts"},
    {"id": "transition_whip_slam", "name": "Whip Flash Slam", "category": "Transition Hit", "alias": "Transition", "duration": 0.48, "filename": "transition_whip_slam.wav", "description": "High-energy whip transition matched to Purple Flash"},
    # 19. Atmospheric Ambience (2)
    {"id": "ambient_warm_pad", "name": "Warm Cinema Pad", "category": "Atmospheric Ambience", "alias": "Ambient", "duration": 1.25, "filename": "ambient_warm_pad.wav", "description": "Rich analog fifths pad for emotional storytelling"},
    {"id": "ambient_ethereal_drone", "name": "Ethereal Space Drone", "category": "Atmospheric Ambience", "alias": "Ambient", "duration": 1.35, "filename": "ambient_ethereal_drone.wav", "description": "Deep evolving atmospheric texture bed"},
    # 20. Soft Transition (2)
    {"id": "soft_trans_breeze", "name": "Silk Breeze Transition", "category": "Soft Transition", "alias": "Transition", "duration": 0.46, "filename": "soft_trans_breeze.wav", "description": "Gentle airy whisper transition for minimal captions"},
    {"id": "soft_trans_felted", "name": "Felted Piano Swell", "category": "Soft Transition", "alias": "Transition", "duration": 0.55, "filename": "soft_trans_felted.wav", "description": "Warm acoustic felt-note transition for editorial videos"},
    # 21. Metallic Impact (2)
    {"id": "metal_australia_clang", "name": "Metallic Title Reveal Hit", "category": "Metallic Impact", "alias": "Impact", "duration": 0.78, "filename": "metal_australia_clang.wav", "description": "Resonant forged steel impact for Reference Video 2 AUSTRALIA reveal"},
    {"id": "metal_sword_sheen", "name": "Chrome Blade Ring", "category": "Metallic Impact", "alias": "Light Sweep", "duration": 0.68, "filename": "metal_sword_sheen.wav", "description": "Bright metallic ring with harmonic tail"},
    # 22. Logo Reveal (2)
    {"id": "logo_studio_chime", "name": "CaptionForge Studio Signature", "category": "Logo Reveal", "alias": "Shimmer", "duration": 1.10, "filename": "logo_studio_chime.wav", "description": "Warm sub-foundation with crystalline major-9th studio chord"},
    {"id": "logo_sonic_bloom", "name": "Sonic Brand Bloom", "category": "Logo Reveal", "alias": "Transition", "duration": 1.00, "filename": "logo_sonic_bloom.wav", "description": "Modern streaming-platform style sonic end-card bloom"},
    # 23. Royalty-Free Background Music Loops (4)
    {"id": "music_lofi_doc_pulse", "name": "Lo-Fi Documentary Pulse Loop", "category": "Background Music", "alias": "Ambient", "duration": 2.40, "filename": "music_lofi_doc_pulse.wav", "description": "Warm rhythmic documentary pulse bed (100 BPM loopable)"},
    {"id": "music_persian_amber_pad", "name": "Persian Amber Strings Loop", "category": "Background Music", "alias": "Ambient", "duration": 2.40, "filename": "music_persian_amber_pad.wav", "description": "Cinematic D-minor Middle-Eastern harmonic progression"},
    {"id": "music_cyber_arp_loop", "name": "Cyber Hologram Arpeggio Loop", "category": "Background Music", "alias": "Ambient", "duration": 2.00, "filename": "music_cyber_arp_loop.wav", "description": "Futuristic synthwave arpeggio bed for tech & VFX reels"},
    {"id": "music_deep_cinema_bed", "name": "Deep Cinema Sub Bed Loop", "category": "Background Music", "alias": "Ambient", "duration": 2.50, "filename": "music_deep_cinema_bed.wav", "description": "Low atmospheric tension bed designed for voiceover ducking"},
]


def _write_wav(path: Path, samples: List[float]):
    path.parent.mkdir(parents=True, exist_ok=True)
    peak = max((abs(s) for s in samples), default=1.0)
    scale = 0.92 / max(peak, 1e-6)
    with wave.open(str(path), "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(SAMPLE_RATE)
        frames = bytearray()
        for s in samples:
            val = int(max(-1.0, min(1.0, s * scale)) * 32767.0)
            frames.extend(struct.pack("<h", val))
        wf.writeframes(bytes(frames))


def _synthesize_preset(preset_id: str, duration: float, category: str) -> List[float]:
    n = int(SAMPLE_RATE * duration)
    rng = random.Random(preset_id)
    out: List[float] = []
    phase = 0.0
    lp = 0.0
    for i in range(n):
        t = i / SAMPLE_RATE
        p = min(1.0, t / max(0.01, duration))
        if "Pop" in category:
            f0, f1 = (150.0, 680.0) if "Punchy" in category else (180.0, 520.0)
            freq = f0 + (f1 - f0) * (p ** 0.65)
            phase += 2.0 * math.pi * freq / SAMPLE_RATE
            env = math.exp(-t * 18.0)
            val = math.tanh((math.sin(phase) + 0.3 * math.sin(phase * 2.0)) * 1.3) * env
        elif "Click" in category:
            env = math.exp(-t * 45.0)
            val = (0.65 * math.sin(2.0 * math.pi * 1350.0 * t) + 0.35 * rng.uniform(-1.0, 1.0)) * env
        elif "Whoosh" in category or "Swoosh" in category or "Soft Transition" in category:
            f_low = 80.0 if "Deep" in category else 220.0
            f_high = 320.0 if "Deep" in category else 1100.0
            freq = f_low + (f_high - f_low) * math.sin(p * math.pi)
            phase += 2.0 * math.pi * freq / SAMPLE_RATE
            lp = 0.78 * lp + 0.22 * rng.uniform(-1.0, 1.0)
            env = (math.sin(p * math.pi) ** 1.6)
            val = (0.58 * math.sin(phase) + 0.42 * lp) * env
        elif "Bass" in category or "Impact" in category or "Boom" in category or "Transition Hit" in category:
            f_start = 135.0 if "Bass" in category else 110.0
            f_end = 32.0
            freq = f_end + (f_start - f_end) * math.exp(-t * 8.0)
            phase += 2.0 * math.pi * freq / SAMPLE_RATE
            env = math.exp(-t * (3.5 if "Boom" in category else 5.5))
            metal = 0.35 * math.sin(2.0 * math.pi * 712.0 * t) * math.exp(-t * 7.0) if "Metallic" in category else 0.0
            val = math.tanh((math.sin(phase) + 0.25 * math.sin(phase * 2.0) + metal) * 1.4) * env
        elif "Riser" in category:
            rev = "Reverse" in category
            curve = ((1.0 - p) ** 1.6) if rev else (p ** 1.8)
            freq = 160.0 + 1150.0 * curve
            phase += 2.0 * math.pi * freq / SAMPLE_RATE
            env = math.sin(p * math.pi) if rev else (p ** 1.2)
            val = (0.78 * math.sin(phase) + 0.22 * rng.uniform(-1.0, 1.0)) * env
        elif "Glitch" in category or "Electric" in category:
            f = 180.0 if "Electric" in category else (240.0 * (1 + ((i // 700) % 6)))
            sq = 0.7 if math.sin(2.0 * math.pi * f * t) >= 0 else -0.7
            val = (sq + rng.uniform(-0.25, 0.25)) * math.sin(p * math.pi)
        elif "Background Music" in category:
            notes = [146.83, 174.61, 220.0, 261.63]
            idx = int(t / 0.30) % len(notes)
            hz = notes[idx]
            env = math.exp(-(t % 0.30) * 5.0)
            val = 0.45 * math.sin(2.0 * math.pi * notes[0] * t) + 0.45 * math.sin(2.0 * math.pi * (hz * 2.0) * t) * env
        else:
            # Shimmer, Sparkle, Light Sweep, Logo Reveal, Ambience
            chord = [261.63, 329.63, 392.0, 523.25, 659.25]
            env = (math.sin(p * math.pi) ** 0.6) * math.exp(-t * 1.2)
            val = sum(math.sin(2.0 * math.pi * hz * t) for hz in chord) / len(chord) * env
        out.append(val)
    return out


def ensure_sfx_library(static_dir: Path) -> List[Dict[str, Any]]:
    sfx_dir = static_dir / "sfx"
    sfx_dir.mkdir(parents=True, exist_ok=True)
    result = []
    for item in SFX_CATALOG:
        wav_path = sfx_dir / item["filename"]
        if not wav_path.exists() or wav_path.stat().st_size < 100:
            samples = _synthesize_preset(item["id"], float(item["duration"]), item["category"])
            _write_wav(wav_path, samples)
        entry = dict(item)
        entry["url"] = f"/static/sfx/{item['filename']}"
        entry["path"] = str(wav_path)
        result.append(entry)
    return result


def get_sfx_path_for_category(static_dir: Path, category_or_id: str) -> Optional[Path]:
    if not category_or_id or category_or_id == "None":
        return None
    ensure_sfx_library(static_dir)
    sfx_dir = static_dir / "sfx"
    norm = category_or_id.strip().lower()
    for item in SFX_CATALOG:
        if (
            item["id"].lower() == norm
            or item["name"].lower() == norm
            or item["category"].lower() == norm
            or item.get("alias", "").lower() == norm
            or item["filename"].lower() == norm
        ):
            candidate = sfx_dir / item["filename"]
            if candidate.exists():
                return candidate
    return None


SAMPLE_RATE = 44100
SFX_REGISTRY = {item["id"]: item for item in SFX_CATALOG}
_PCM_CACHE: Dict[str, bytes] = {}


def export_all_sfx(sfx_dir: Optional[Path] = None) -> List[Dict[str, Any]]:
    if sfx_dir is None:
        static_dir = Path(__file__).resolve().parent / "static"
    elif sfx_dir.name == "sfx":
        static_dir = sfx_dir.parent
    else:
        static_dir = sfx_dir
    return ensure_sfx_library(static_dir)


def get_pcm_for_preset(preset_key: str) -> bytes:
    if not preset_key or preset_key == "None":
        return b""
    norm = preset_key.strip().lower()
    if norm in _PCM_CACHE:
        return _PCM_CACHE[norm]

    matched = None
    for item in SFX_CATALOG:
        if (
            item["id"].lower() == norm
            or item["name"].lower() == norm
            or item["category"].lower() == norm
            or item.get("alias", "").lower() == norm
            or item["filename"].lower() == norm
        ):
            matched = item
            break
    if matched is None:
        matched = SFX_CATALOG[0]

    samples = _synthesize_preset(matched["id"], float(matched["duration"]), matched["category"])
    buf = bytearray(len(samples) * 2)
    for idx, val in enumerate(samples):
        clamped = max(-1.0, min(1.0, val))
        struct.pack_into("<h", buf, idx * 2, int(clamped * 32767.0))
    pcm = bytes(buf)
    _PCM_CACHE[norm] = pcm
    return pcm

