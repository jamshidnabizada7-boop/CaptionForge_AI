"""Tests for CaptionForge Advanced Cinematic Motion Graphics, Font Library, and Multitrack SFX."""
from __future__ import annotations
import io, json, os, sys, wave
from pathlib import Path
from fastapi.testclient import TestClient

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT))

from app import app, build_ass, write_sfx, mix_multitrack_audio
from sfx_library import SFX_REGISTRY, get_pcm_for_preset
from compositor import ease_value, interpolate_keyframes, track_motion
from fonts_manager import list_all_fonts

client = TestClient(app)

def test_sfx_catalog_api_returns_all_11_categories():
    r = client.get('/api/sfx')
    assert r.status_code == 200
    data = r.json()
    assert 'categories' in data
    assert 'catalog' in data
    expected_categories = ['Pop', 'Click', 'Whoosh', 'Shimmer', 'Impact', 'Riser', 'Bass', 'Glitch', 'Light Sweep', 'Transition', 'Ambient']
    for cat in expected_categories:
        assert cat in data['categories'], f"Category {cat} missing from catalog"
        assert cat in data['catalog'], f"Category {cat} missing from items"
        assert len(data['catalog'][cat]) >= 1

def test_sfx_preset_synthesis_all_11_categories():
    categories = ['Pop', 'Click', 'Whoosh', 'Shimmer', 'Impact', 'Riser', 'Bass', 'Glitch', 'Light Sweep', 'Transition', 'Ambient']
    for cat in categories:
        pcm = get_pcm_for_preset(cat)
        assert len(pcm) > 100
        # PCM should be 16-bit signed little-endian (divisible by 2)
        assert len(pcm) % 2 == 0

def test_font_library_api_and_custom_font_upload():
    r = client.get('/api/fonts')
    assert r.status_code == 200
    fonts = r.json()['fonts']
    assert len(fonts) >= 7
    font_names = [f['family'] for f in fonts]
    assert 'Vazirmatn' in font_names
    assert 'Noto Sans Arabic' in font_names
    assert 'Impact' in font_names

    # Test uploading a custom font
    fake_ttf_bytes = b'\x00\x01\x00\x00' + b'FAKE_TTF_FONT_HEADER_FOR_TESTING' * 20
    upload_r = client.post('/api/fonts', files={'file': ('CustomCinematicFont.ttf', fake_ttf_bytes, 'font/ttf')})
    assert upload_r.status_code == 200
    res = upload_r.json()
    assert res['family'] == 'CustomCinematicFont'
    assert res['isCustom'] is True

def test_keyframe_interpolation_and_easing():
    keyframes = [
        {'time': 0.0, 'xPos': 0.2, 'yPos': 0.3, 'scale': 1.0, 'rotation': 0.0, 'opacity': 0.0, 'easing': 'easeInOut'},
        {'time': 2.0, 'xPos': 0.8, 'yPos': 0.7, 'scale': 2.0, 'rotation': 45.0, 'opacity': 1.0, 'easing': 'bounce'},
    ]
    defaults = {'xPos': 0.5, 'yPos': 0.5, 'scale': 1.0, 'rotation': 0.0, 'opacity': 1.0, 'blur': 0.0, 'glow': 0.0}

    # At start
    k_start = interpolate_keyframes(keyframes, 0.0, defaults)
    assert abs(k_start['xPos'] - 0.2) < 0.01
    assert abs(k_start['opacity'] - 0.0) < 0.01

    # At midpoint (1.0s)
    k_mid = interpolate_keyframes(keyframes, 1.0, defaults)
    assert 0.2 < k_mid['xPos'] < 0.8
    assert 1.0 < k_mid['scale'] < 2.0

    # At end (2.0s)
    k_end = interpolate_keyframes(keyframes, 2.0, defaults)
    assert abs(k_end['xPos'] - 0.8) < 0.01
    assert abs(k_end['rotation'] - 45.0) < 0.01

def test_ass_generation_with_keyframed_overlay():
    dest = ROOT / 'data' / 'test_keyframed.ass'
    proj = {
        'id': 'test_kf_proj',
        'duration': 5.0,
        'source': 'test.mp4',
        'trim': {'start': 0.0, 'end': 5.0},
        'tracks': [],
        'overlays': [
            {
                'id': 'kf_ov1',
                'name': 'Animated Hologram',
                'type': 'glow_hologram',
                'content': 'PETRONAS TOWERS',
                'start': 0.0,
                'end': 4.0,
                'visible': True,
                'keyframes': [
                    {'time': 0.0, 'xPos': 0.3, 'yPos': 0.4, 'scale': 0.8, 'rotation': 0.0, 'opacity': 0.0},
                    {'time': 2.0, 'xPos': 0.5, 'yPos': 0.35, 'scale': 1.2, 'rotation': 10.0, 'opacity': 1.0},
                    {'time': 4.0, 'xPos': 0.7, 'yPos': 0.4, 'scale': 1.0, 'rotation': 0.0, 'opacity': 0.8},
                ]
            }
        ]
    }
    build_ass(proj, dest, 720, 1280)
    ass_text = dest.read_text(encoding='utf-8')
    dest.unlink()

    assert 'PETRONAS TOWERS' in ass_text
    assert '\\move(' in ass_text
    assert '\\t(' in ass_text

def test_multitrack_audio_clip_mixing():
    temp_dir = ROOT / 'data' / 'temp_audio_test'
    temp_dir.mkdir(parents=True, exist_ok=True)
    proj = {
        'id': 'test_audio_proj',
        'duration': 6.0,
        'trim': {'start': 0.0, 'end': 6.0},
        'effects': {'sfx': 'Pop', 'sfxMode': 'Sentence', 'sfxVolume': 40},
        'tracks': [{
            'id': 't1', 'visible': True, 'style': {},
            'captions': [{'id': 'c1', 'start': 1.0, 'end': 2.5, 'text': 'Cue with sound'}]
        }],
        'audioClips': [
            {
                'id': 'ac1', 'name': 'Impact Hit', 'category': 'Impact', 'source': 'Impact',
                'start': 0.5, 'duration': 1.0, 'volume': 0.8, 'fadeIn': 0.1, 'fadeOut': 0.2
            },
            {
                'id': 'ac2', 'name': 'Whoosh Sweep', 'category': 'Whoosh', 'source': 'Whoosh',
                'start': 3.0, 'duration': 0.8, 'volume': 0.6, 'fadeIn': 0.0, 'fadeOut': 0.1
            }
        ]
    }
    wav_path = mix_multitrack_audio(proj, temp_dir, 6.0)
    assert wav_path is not None and wav_path.is_file()
    assert wav_path.stat().st_size > 50000
    wav_path.unlink()
    temp_dir.rmdir()


def test_font_library_corrupted_upload_rejected():
    # 1. Reject invalid header
    r_bad = client.post('/api/fonts', files={'file': ('bad.ttf', b'THIS_IS_NOT_A_FONT_BINARY', 'font/ttf')})
    assert r_bad.status_code == 400
    assert 'Corrupted or invalid font' in r_bad.json()['detail']

    # 2. Reject empty 0-byte font
    r_empty = client.post('/api/fonts', files={'file': ('empty.ttf', b'', 'font/ttf')})
    assert r_empty.status_code == 400


def test_identical_keyframes_boundary_tolerance():
    proj = {
        'id': 'test_identical_kf',
        'duration': 5.0,
        'source': 'test.mp4',
        'trim': {'start': 0.0, 'end': 5.0},
        'tracks': [],
        'overlays': [
            {
                'id': 'ov_same_time',
                'name': 'Same Time Overlay',
                'type': 'metallic_banner',
                'content': 'AUSTRALIA',
                'start': 1.0,
                'end': 4.0,
                'visible': True,
                'keyframes': [
                    {'time': 1.0, 'xPos': 0.5, 'yPos': 0.5, 'scale': 1.2},
                    {'time': 1.0, 'xPos': 0.5, 'yPos': 0.5, 'scale': 1.2},
                ]
            }
        ]
    }
    dest = ROOT / 'data' / 'test_identical.ass'
    build_ass(proj, dest, 720, 1280)
    text = dest.read_text(encoding='utf-8')
    dest.unlink()

    # Overlay MUST NOT vanish when keyframes have identical timestamps!
    dialogues = [line for line in text.splitlines() if line.startswith('Dialogue:')]
    assert len(dialogues) >= 1
    assert 'AUSTRALIA' in dialogues[0]
    assert '\\fnImpact' in dialogues[0]


def test_audio_looping_and_ducking():
    import struct
    temp_dir = ROOT / 'data' / 'temp_ducking_test'
    temp_dir.mkdir(parents=True, exist_ok=True)
    proj = {
        'id': 'test_ducking_proj',
        'duration': 4.0,
        'trim': {'start': 0.0, 'end': 4.0},
        'tracks': [{
            'id': 'trk_speech', 'visible': True, 'style': {},
            'captions': [{'id': 'c1', 'start': 1.0, 'end': 2.0, 'text': 'Spoken sentence'}]
        }],
        'audioClips': [
            {
                'id': 'ambient_bg', 'name': 'Ambient Loop', 'category': 'Ambient', 'source': 'Ambient',
                'start': 0.0, 'duration': 4.0, 'volume': 1.0, 'fadeIn': 0.0, 'fadeOut': 0.0, 'ducking': True
            }
        ]
    }
    wav_path = mix_multitrack_audio(proj, temp_dir, 4.0)
    assert wav_path is not None and wav_path.is_file()

    with wave.open(str(wav_path)) as wf:
        frames = wf.readframes(wf.getnframes())
        samples = struct.unpack(f'<{len(frames)//2}h', frames)
        # Check non-zero amplitude in all 4 seconds (verifying looping)
        sec0 = samples[0:44100]
        sec1 = samples[44100:88200]
        sec2 = samples[88200:132300]
        sec3 = samples[132300:176400]

        max0 = max(abs(s) for s in sec0)
        max1 = max(abs(s) for s in sec1)  # Speech caption active here (ducked!)
        max2 = max(abs(s) for s in sec2)
        max3 = max(abs(s) for s in sec3)

        assert max0 > 1000, "Second 0 should have audible sound"
        assert max3 > 1000, "Second 3 should have audible sound (looped, not cutoff!)"
        # Ducking attenuation verification: speech in second 1 should duck volume
        assert max1 < max0, f"Ducking failed: sec 1 amplitude {max1} should be lower than sec 0 {max0}"

    wav_path.unlink()
    temp_dir.rmdir()


def test_compositor_procedural_asset_drawers():
    import numpy as np
    from compositor import draw_waving_flag, draw_illuminated_hologram, draw_purple_flare, draw_metallic_banner

    canvas = np.zeros((640, 360, 3), dtype=np.uint8)

    # 1. Waving Flag
    draw_waving_flag(canvas, 180, 200, 160, 100, t=0.5, scale=1.0, opacity=1.0)
    assert np.count_nonzero(canvas) > 5000

    # 2. Illuminated Hologram
    draw_illuminated_hologram(canvas, 180, 320, 160, 160, t=0.5, scale=1.0, opacity=1.0)
    assert np.count_nonzero(canvas) > 15000

    # 3. Metallic Banner
    draw_metallic_banner(canvas, 500, 360, 640, 'AUSTRALIA', scale=1.0, opacity=1.0)
    assert np.count_nonzero(canvas) > 25000

    # 4. Purple Flare
    draw_purple_flare(canvas, 360, 640, t=0.5, opacity=0.8)
    assert canvas.mean() > 10
