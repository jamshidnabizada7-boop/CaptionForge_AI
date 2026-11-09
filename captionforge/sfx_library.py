"""CaptionForge 11-Category Mathematical Sound Effects Engine.

Pure mathematical synthesis using 44.1kHz 16-bit PCM audio. Zero external audio
dependencies or copyright restrictions (100% royalty-free CC0 math-generated assets).
"""
from __future__ import annotations
import math, os, random, struct, wave
from pathlib import Path
from typing import Dict, List, Tuple

SAMPLE_RATE = 44100

def _clamp(val: float, min_val: float = -1.0, max_val: float = 1.0) -> float:
    return max(min_val, min(max_val, val))

def _pack_pcm(samples: List[float]) -> bytearray:
    buf = bytearray(len(samples) * 2)
    for i, s in enumerate(samples):
        int_val = int(_clamp(s) * 32767.0)
        struct.pack_into('<h', buf, i * 2, max(-32768, min(32767, int_val)))
    return buf

def synthesize_pop(duration: float = 0.08, start_freq: float = 950.0, end_freq: float = 280.0) -> bytearray:
    """Crisp exponential drop transient pop."""
    n = int(SAMPLE_RATE * duration)
    samples = []
    for i in range(n):
        t = i / SAMPLE_RATE
        progress = i / n
        freq = start_freq * ((end_freq / start_freq) ** progress)
        env = (1.0 - progress) ** 2.8
        sample = math.sin(2.0 * math.pi * freq * t) * env * 0.85
        samples.append(sample)
    return _pack_pcm(samples)

def synthesize_click(duration: float = 0.025, freq: float = 1600.0) -> bytearray:
    """Sharp mechanical tactile click."""
    n = int(SAMPLE_RATE * duration)
    samples = []
    for i in range(n):
        progress = i / n
        env = (1.0 - progress) ** 5.0
        sample = math.sin(2.0 * math.pi * freq * (i / SAMPLE_RATE)) * env * 0.90
        # Phase modulation transient burst
        sample += 0.25 * (math.sin(2.0 * math.pi * (freq * 2.3) * (i / SAMPLE_RATE))) * env
        samples.append(sample)
    return _pack_pcm(samples)

def synthesize_whoosh(duration: float = 0.45, center_freq: float = 650.0) -> bytearray:
    """Cinematic air rush whoosh."""
    n = int(SAMPLE_RATE * duration)
    samples = []
    # Pseudo-random noise with sweeping bandpass envelope
    noise_seed = 12345
    for i in range(n):
        progress = i / n
        env = math.sin(math.pi * progress) ** 1.8
        # Frequency sweep up and down
        f = center_freq + 400.0 * math.sin(math.pi * progress)
        # Linear congruential noise
        noise_seed = (noise_seed * 1103515245 + 12345) & 0x7FFFFFFF
        noise = (noise_seed / 1073741824.0) - 1.0
        tonal = math.sin(2.0 * math.pi * f * (i / SAMPLE_RATE))
        sample = (tonal * 0.45 + noise * 0.55) * env * 0.75
        samples.append(sample)
    return _pack_pcm(samples)

def synthesize_shimmer(duration: float = 0.40) -> bytearray:
    """Harmonic celestial shimmer triad with sparkling resonance."""
    n = int(SAMPLE_RATE * duration)
    samples = []
    f1, f2, f3, f4 = 1150.0, 1720.0, 2300.0, 3100.0
    for i in range(n):
        t = i / SAMPLE_RATE
        progress = i / n
        env = (1.0 - progress) ** 1.4
        s1 = math.sin(2.0 * math.pi * f1 * t) * 0.40
        s2 = math.sin(2.0 * math.pi * f2 * t + 0.3) * 0.30
        s3 = math.sin(2.0 * math.pi * f3 * t + 0.7) * 0.20
        s4 = math.sin(2.0 * math.pi * f4 * t + 1.1) * 0.10
        samples.append((s1 + s2 + s3 + s4) * env * 0.85)
    return _pack_pcm(samples)

def synthesize_impact(duration: float = 0.55, sub_freq: float = 72.0) -> bytearray:
    """Heavy cinematic sub-bass impact hit with acoustic body."""
    n = int(SAMPLE_RATE * duration)
    samples = []
    for i in range(n):
        t = i / SAMPLE_RATE
        progress = i / n
        env = math.exp(-5.5 * progress)
        # Sub pitch drops from 95Hz to sub_freq
        f = sub_freq + 28.0 * (1.0 - progress)
        sub = math.sin(2.0 * math.pi * f * t) * 0.75
        # Soft click transient at attack
        click = math.sin(2.0 * math.pi * 850.0 * t) * ((1.0 - min(1.0, progress * 15)) ** 3) * 0.4
        samples.append((sub + click) * env * 0.95)
    return _pack_pcm(samples)

def synthesize_riser(duration: float = 0.60, start_freq: float = 140.0, end_freq: float = 1200.0) -> bytearray:
    """Tension build riser with exponential pitch escalation."""
    n = int(SAMPLE_RATE * duration)
    samples = []
    for i in range(n):
        t = i / SAMPLE_RATE
        progress = i / n
        freq = start_freq + (end_freq - start_freq) * (progress ** 2.2)
        env = (progress ** 1.1) * 0.85
        sample = math.sin(2.0 * math.pi * freq * t) * env
        samples.append(sample)
    return _pack_pcm(samples)

def synthesize_bass(duration: float = 0.70, freq: float = 55.0) -> bytearray:
    """Deep 808 resonant bass drop."""
    n = int(SAMPLE_RATE * duration)
    samples = []
    for i in range(n):
        t = i / SAMPLE_RATE
        progress = i / n
        env = math.exp(-3.2 * progress)
        current_freq = freq * (1.0 + 0.3 * math.exp(-12.0 * progress))
        # Fundamental + slight 2nd harmonic for warmth
        sample = (math.sin(2.0 * math.pi * current_freq * t) * 0.85 +
                  math.sin(4.0 * math.pi * current_freq * t) * 0.15) * env * 0.95
        samples.append(sample)
    return _pack_pcm(samples)

def synthesize_glitch(duration: float = 0.16) -> bytearray:
    """Digital data buffer stutter glitch."""
    n = int(SAMPLE_RATE * duration)
    samples = []
    carrier = 380.0
    for i in range(n):
        progress = i / n
        # Periodic modulation
        mod = math.sin(2.0 * math.pi * 32.0 * (i / SAMPLE_RATE))
        f = carrier + 450.0 * mod
        # Quantized / bit-crushed square wave
        wave_val = 1.0 if math.sin(2.0 * math.pi * f * (i / SAMPLE_RATE)) > 0 else -1.0
        env = (1.0 - progress) * 0.75
        samples.append(wave_val * env)
    return _pack_pcm(samples)

def synthesize_light_sweep(duration: float = 0.38) -> bytearray:
    """Crystalline laser light sweep."""
    n = int(SAMPLE_RATE * duration)
    samples = []
    for i in range(n):
        progress = i / n
        freq = 2400.0 - 1850.0 * (progress ** 0.8)
        env = math.sin(math.pi * progress) ** 1.2
        sample = math.sin(2.0 * math.pi * freq * (i / SAMPLE_RATE)) * env * 0.80
        samples.append(sample)
    return _pack_pcm(samples)

def synthesize_transition(duration: float = 0.50) -> bytearray:
    """Cinematic riser-to-downbeat transition."""
    n = int(SAMPLE_RATE * duration)
    samples = []
    for i in range(n):
        t = i / SAMPLE_RATE
        progress = i / n
        if progress < 0.6:
            # Riser portion
            sub_prog = progress / 0.6
            f = 220.0 + 750.0 * (sub_prog ** 2)
            env = sub_prog * 0.70
            s = math.sin(2.0 * math.pi * f * t) * env
        else:
            # Impact falloff
            sub_prog = (progress - 0.6) / 0.4
            f = 110.0 - 45.0 * sub_prog
            env = (1.0 - sub_prog) ** 2 * 0.85
            s = math.sin(2.0 * math.pi * f * t) * env
        samples.append(s)
    return _pack_pcm(samples)

def synthesize_ambient(duration: float = 0.80) -> bytearray:
    """Warm ethereal analog pad ambient tone."""
    n = int(SAMPLE_RATE * duration)
    samples = []
    f1, f2 = 220.0, 330.0  # A3 + E4 fifth chord
    for i in range(n):
        t = i / SAMPLE_RATE
        progress = i / n
        env = math.sin(math.pi * progress) ** 1.1
        s1 = math.sin(2.0 * math.pi * f1 * t) * 0.55
        s2 = math.sin(2.0 * math.pi * f2 * t + 0.4) * 0.40
        samples.append((s1 + s2) * env * 0.70)
    return _pack_pcm(samples)

SFX_REGISTRY = {
    'Pop': [('Crisp', 0.08, lambda: synthesize_pop(0.08, 950.0, 280.0)),
            ('Bubble', 0.12, lambda: synthesize_pop(0.12, 1100.0, 180.0))],
    'Click': [('Tactile', 0.025, lambda: synthesize_click(0.025, 1600.0)),
              ('Shutter', 0.035, lambda: synthesize_click(0.035, 1200.0))],
    'Whoosh': [('Fast Air', 0.35, lambda: synthesize_whoosh(0.35, 750.0)),
               ('Cinematic Whip', 0.50, lambda: synthesize_whoosh(0.50, 520.0))],
    'Shimmer': [('Celestial', 0.40, lambda: synthesize_shimmer(0.40)),
                ('Sparkle', 0.30, lambda: synthesize_shimmer(0.30))],
    'Impact': [('Sub Slam', 0.55, lambda: synthesize_impact(0.55, 68.0)),
               ('Heavy Thump', 0.45, lambda: synthesize_impact(0.45, 82.0))],
    'Riser': [('Tension', 0.60, lambda: synthesize_riser(0.60, 140.0, 1200.0)),
              ('Hyper Riser', 0.45, lambda: synthesize_riser(0.45, 220.0, 1600.0))],
    'Bass': [('808 Drop', 0.70, lambda: synthesize_bass(0.70, 55.0)),
             ('Deep Sub', 0.60, lambda: synthesize_bass(0.60, 44.0))],
    'Glitch': [('Digital Byte', 0.16, lambda: synthesize_glitch(0.16)),
               ('Stutter Burst', 0.12, lambda: synthesize_glitch(0.12))],
    'Light Sweep': [('Laser Sheen', 0.38, lambda: synthesize_light_sweep(0.38)),
                    ('Prism Sweep', 0.30, lambda: synthesize_light_sweep(0.30))],
    'Transition': [('Swoosh Hit', 0.50, lambda: synthesize_transition(0.50)),
                   ('Downbeat', 0.40, lambda: synthesize_transition(0.40))],
    'Ambient': [('Warm Pad', 0.80, lambda: synthesize_ambient(0.80)),
                ('Ethereal Drone', 0.70, lambda: synthesize_ambient(0.70))],
}

def export_all_sfx(output_dir: Path) -> Dict[str, List[dict]]:
    """Generates all 11-category WAV files on disk and returns catalog metadata."""
    output_dir.mkdir(parents=True, exist_ok=True)
    catalog = {}
    for category, items in SFX_REGISTRY.items():
        catalog[category] = []
        for name, dur, synth_fn in items:
            filename = f'{category.lower().replace(" ", "_")}_{name.lower().replace(" ", "_")}.wav'
            filepath = output_dir / filename
            if not filepath.is_file():
                pcm_data = synth_fn()
                with wave.open(str(filepath), 'wb') as wf:
                    wf.setnchannels(1)
                    wf.setsampwidth(2)
                    wf.setframerate(SAMPLE_RATE)
                    wf.writeframes(pcm_data)
            catalog[category].append({
                'category': category,
                'name': name,
                'duration': dur,
                'filename': filename,
                'url': f'/static/sfx/{filename}',
            })
    return catalog

def get_pcm_for_preset(preset_name: str) -> bytearray:
    """Direct memory synthesis by preset name."""
    clean = preset_name.strip()
    if clean in ('Pop', 'pop'):
        return synthesize_pop()
    elif clean in ('Click', 'click'):
        return synthesize_click()
    elif clean in ('Whoosh', 'whoosh'):
        return synthesize_whoosh()
    elif clean in ('Shimmer', 'shimmer'):
        return synthesize_shimmer()
    elif clean in ('Impact', 'impact'):
        return synthesize_impact()
    elif clean in ('Riser', 'riser'):
        return synthesize_riser()
    elif clean in ('Bass', 'bass'):
        return synthesize_bass()
    elif clean in ('Glitch', 'glitch'):
        return synthesize_glitch()
    elif clean in ('Light Sweep', 'light_sweep', 'Sweep', 'sweep'):
        return synthesize_light_sweep()
    elif clean in ('Transition', 'transition'):
        return synthesize_transition()
    elif clean in ('Ambient', 'ambient', 'Soft', 'soft'):
        return synthesize_ambient()
    return synthesize_pop()
