"""CaptionForge Multi-Script Font Library & Custom Font Manager.

Supports English, Persian (Afghanistan/Dari), Pashto, Urdu, and Arabic.
Provides font metadata, preview sample texts, font file registry, and custom TTF/OTF imports.
"""
from __future__ import annotations
import json, os, shutil
from pathlib import Path
from typing import Dict, List, Optional

FONTS_DIR = Path(__file__).resolve().parent / 'data' / 'fonts'
FONTS_DIR.mkdir(parents=True, exist_ok=True)

# Comprehensive licensed font library catalog
DEFAULT_FONTS: List[Dict[str, Any]] = [
    {
        'family': 'Vazirmatn',
        'displayName': 'Vazirmatn (وزیرمتن)',
        'category': 'Modern Sans',
        'scripts': ['Arabic', 'Persian', 'Pashto', 'Urdu', 'Latin'],
        'languages': ['Persian (Dari/Farsi)', 'Pashto', 'Urdu', 'Arabic', 'English'],
        'license': 'SIL Open Font License 1.1',
        'weights': [100, 300, 400, 500, 700, 900],
        'sample': 'کپشن‌فورج - زیبایی متن فارسی و پشتو',
        'isCustom': False,
    },
    {
        'family': 'Noto Sans Arabic',
        'displayName': 'Noto Sans Arabic',
        'category': 'Clean Sans',
        'scripts': ['Arabic', 'Persian', 'Pashto', 'Urdu', 'Latin'],
        'languages': ['Arabic', 'Persian', 'Pashto', 'Urdu', 'English'],
        'license': 'SIL Open Font License 1.1',
        'weights': [300, 400, 600, 700, 900],
        'sample': 'خط عربی و پشتو پاک و خوانا',
        'isCustom': False,
    },
    {
        'family': 'Noto Nastaliq Urdu',
        'displayName': 'Noto Nastaliq Urdu (نستعلیق)',
        'category': 'Calligraphic Nastaliq',
        'scripts': ['Urdu', 'Pashto', 'Persian'],
        'languages': ['Urdu', 'Pashto', 'Persian'],
        'license': 'SIL Open Font License 1.1',
        'weights': [400, 700],
        'sample': 'کپشن فورج اردو نستعلیق خوبصورت ڈیزائن',
        'isCustom': False,
    },
    {
        'family': 'Lalezar',
        'displayName': 'Lalezar (لاله‌زار)',
        'category': 'Cinematic Display Bold',
        'scripts': ['Persian', 'Arabic', 'Latin'],
        'languages': ['Persian', 'Arabic', 'English'],
        'license': 'SIL Open Font License 1.1',
        'weights': [400, 800],
        'sample': 'عنوان سینمایی ویژه و جذاب',
        'isCustom': False,
    },
    {
        'family': 'Amiri',
        'displayName': 'Amiri (امیری)',
        'category': 'Classical Naskh',
        'scripts': ['Arabic', 'Persian', 'Urdu'],
        'languages': ['Arabic', 'Persian', 'Urdu'],
        'license': 'SIL Open Font License 1.1',
        'weights': [400, 700],
        'sample': 'بسم الله الرحمن الرحیم - کپشن فورج',
        'isCustom': False,
    },
    {
        'family': 'Impact',
        'displayName': 'Impact',
        'category': 'Heavy Cinematic Title',
        'scripts': ['Latin'],
        'languages': ['English'],
        'license': 'System Core Font',
        'weights': [700, 900],
        'sample': 'AUSTRALIA CINEMATIC TITLE',
        'isCustom': False,
    },
    {
        'family': 'Bebas Neue',
        'displayName': 'Bebas Neue',
        'category': 'Tall Condensed Display',
        'scripts': ['Latin'],
        'languages': ['English'],
        'license': 'SIL Open Font License 1.1',
        'weights': [400, 700],
        'sample': 'BOLD HORIZONTAL LETTERBOX BANNER',
        'isCustom': False,
    },
    {
        'family': 'Segoe UI',
        'displayName': 'Segoe UI',
        'category': 'Universal System Sans',
        'scripts': ['Latin', 'Arabic'],
        'languages': ['English', 'Persian', 'Arabic'],
        'license': 'System Core Font',
        'weights': [300, 400, 600, 700],
        'sample': 'Clean Subtitles & Glowing Holograms',
        'isCustom': False,
    },
    {
        'family': 'Tahoma',
        'displayName': 'Tahoma',
        'category': 'Legacy RTL & Latin Sans',
        'scripts': ['Arabic', 'Persian', 'Urdu', 'Latin'],
        'languages': ['English', 'Persian', 'Arabic', 'Urdu'],
        'license': 'System Core Font',
        'weights': [400, 700],
        'sample': 'زیرنویس دقیق و هماهنگ',
        'isCustom': False,
    },
    {
        'family': 'Arial',
        'displayName': 'Arial',
        'category': 'Standard Sans',
        'scripts': ['Latin', 'Arabic'],
        'languages': ['English', 'Arabic'],
        'license': 'System Core Font',
        'weights': [400, 700],
        'sample': 'Universal Cross-Platform Subtitles',
        'isCustom': False,
    },
]

def list_all_fonts() -> List[Dict[str, Any]]:
    """Returns combined list of default fonts and imported custom fonts."""
    fonts = list(DEFAULT_FONTS)
    for font_file in FONTS_DIR.glob('*.*'):
        if font_file.suffix.lower() in ('.ttf', '.otf'):
            stem = font_file.stem
            # Prevent duplicate listing
            if not any(f['family'].lower() == stem.lower() for f in fonts):
                fonts.append({
                    'family': stem,
                    'displayName': f'{stem} (Custom)',
                    'category': 'Imported Custom Font',
                    'scripts': ['Custom'],
                    'languages': ['Universal'],
                    'license': 'User Uploaded Font',
                    'weights': [400, 700],
                    'sample': f'Custom Font Preview: {stem}',
                    'isCustom': True,
                    'filePath': str(font_file),
                })
    return fonts

def is_valid_font(file_bytes: bytes) -> bool:
    """Validates TrueType / OpenType font binary header signature."""
    if len(file_bytes) < 256:
        return False
    magic = file_bytes[:4]
    return magic in (b'\x00\x01\x00\x00', b'OTTO', b'true', b'typ1', b'ttcf')

def register_custom_font(filename: str, file_bytes: bytes) -> Dict[str, Any]:
    """Saves uploaded TTF/OTF font file and registers it after verifying validity."""
    safe_name = Path(filename).name
    ext = Path(safe_name).suffix.lower()
    if ext not in ('.ttf', '.otf'):
        raise ValueError('Only .ttf and .otf font files are supported')
    if not is_valid_font(file_bytes):
        raise ValueError('Corrupted or invalid font: header signature does not match TTF/OTF specification')
    dest = FONTS_DIR / safe_name
    dest.write_bytes(file_bytes)
    return {
        'family': dest.stem,
        'displayName': f'{dest.stem} (Custom)',
        'category': 'Imported Custom Font',
        'scripts': ['Custom'],
        'languages': ['Universal'],
        'license': 'User Uploaded Font',
        'weights': [400, 700],
        'sample': f'Custom Font Preview: {dest.stem}',
        'isCustom': True,
        'filePath': str(dest),
    }
