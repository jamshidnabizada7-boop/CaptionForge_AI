"""CaptionForge: local-first mobile-friendly caption editor and cinematic FFmpeg exporter."""
from __future__ import annotations
import json, os, re, shutil, subprocess, threading, uuid, wave, math, struct
from pathlib import Path
from datetime import datetime, timezone
from typing import Any, List, Optional

# PyAV 19+ compatibility patch for faster-whisper
try:
    import av
    _orig_av_open = av.open
    def _compat_av_open(*args, **kwargs):
        kwargs.pop('metadata_errors', None)
        return _orig_av_open(*args, **kwargs)
    av.open = _compat_av_open
except Exception:
    pass

from fastapi import FastAPI, File, UploadFile, HTTPException, Body
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel

try:
    from sfx_library import export_all_sfx, get_pcm_for_preset, SFX_REGISTRY, SAMPLE_RATE as SFX_SAMPLE_RATE
    from compositor import interpolate_keyframes, track_motion, generate_subject_depth_matte, composite_project_video
    from fonts_manager import list_all_fonts, register_custom_font, FONTS_DIR
except ImportError:
    from captionforge.sfx_library import export_all_sfx, get_pcm_for_preset, SFX_REGISTRY, SAMPLE_RATE as SFX_SAMPLE_RATE
    from captionforge.compositor import interpolate_keyframes, track_motion, generate_subject_depth_matte, composite_project_video
    from captionforge.fonts_manager import list_all_fonts, register_custom_font, FONTS_DIR

ROOT = Path(__file__).resolve().parent
DATA = Path(os.getenv('CAPTIONFORGE_DATA', ROOT / 'data')).resolve()
DATA.mkdir(parents=True, exist_ok=True)
MAX_BYTES = 500 * 1024 * 1024

app = FastAPI(title='CaptionForge', version='0.2.0')
app.add_middleware(
    CORSMiddleware,
    allow_origins=['*'],
    allow_credentials=True,
    allow_methods=['*'],
    allow_headers=['*'],
)
app.mount('/static', StaticFiles(directory=ROOT / 'static'), name='static')
JOBS: dict[str, dict] = {}
MODEL = None
MODEL_LOCK = threading.Lock()
SUPPORTED_VIDEO = {'.mp4', '.mov', '.webm', '.m4v', '.mkv'}


def run(cmd: list[str], timeout: int = 90, cwd: Path | None = None) -> subprocess.CompletedProcess:
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout, cwd=cwd)
    except (subprocess.TimeoutExpired, FileNotFoundError) as e:
        raise RuntimeError(f'Command failed or timed out: {e}') from e
    if result.returncode:
        raise RuntimeError((result.stderr or result.stdout)[-2200:])
    return result


def probe(path: Path) -> dict:
    result = run(['ffprobe', '-v', 'error', '-show_entries', 'format=duration:stream=codec_type,width,height', '-of', 'json', str(path)])
    info = json.loads(result.stdout)
    video = next((s for s in info.get('streams', []) if s.get('codec_type') == 'video'), None)
    if not video:
        raise RuntimeError('The file contains no supported video stream')
    return dict(duration=round(float(info.get('format', {}).get('duration') or 0), 3),
                width=video.get('width', 0), height=video.get('height', 0),
                hasAudio=any(s.get('codec_type') == 'audio' for s in info.get('streams', [])))


def folder(pid: str) -> Path:
    if not re.fullmatch(r'[a-f0-9]{32}', pid) and not pid.startswith('sample_project_'):
        raise HTTPException(404, 'Project not found')
    p = DATA / pid
    if not (p / 'project.json').is_file():
        raise HTTPException(404, 'Project not found')
    return p


def load(pid: str) -> dict:
    pr = json.loads((folder(pid) / 'project.json').read_text('utf-8'))
    pr.setdefault('overlays', [])
    pr.setdefault('audioClips', [])
    pr.setdefault('customFonts', [])
    return pr


def save(project: dict) -> dict:
    p = folder(project['id'])
    project['updatedAt'] = datetime.now(timezone.utc).isoformat()
    tmp = p / 'project.tmp'
    tmp.write_text(json.dumps(project, ensure_ascii=False, indent=2), 'utf-8')
    tmp.replace(p / 'project.json')
    return project


def default_style():
    return dict(preset='Bold', font='Noto Sans', fontSize=25, textColor='#ffffff', bgColor='#181124', bgEnabled=True,
                outline=2, yPos=0.76, align='center', animation='Pop', early=0.0, oneLine=False,
                overlay='None', karaoke=False, followFace=False, opacity=100,
                letterSpacing=0, lineSpacing=1.0, glow=0.0, glowColor='#00F0FF', gradient=False, secondaryColor='#FF6A00')


def new_track(name='Original', lang='auto', color='#7352ff'):
    return dict(id=uuid.uuid4().hex[:10], name=name, language=lang, color=color, visible=True, locked=False,
                style=default_style(), captions=[])


def normalized_caption(c, i):
    s = max(0, float(c.get('start', 0)))
    e = max(s + 0.08, float(c.get('end', s + 1)))
    return dict(id=str(c.get('id') or f'c{i}'), start=round(s, 3), end=round(e, 3),
                text=str(c.get('text', '')), words=c.get('words', []))


@app.get('/')
def index():
    return FileResponse(ROOT / 'static' / 'index.html')


@app.get('/api/config')
def config():
    try:
        from translation import get_provider_info
    except ImportError:
        from captionforge.translation import get_provider_info
    p_info = get_provider_info()
    return {'transcription': 'faster-whisper' if _whisper_available() else 'install faster-whisper',
            'translation': f"ready ({p_info['active']})",
            'translationProvider': p_info,
            'render': shutil.which('ffmpeg') is not None,
            'faceTracking': _cv2_available(),
            'sfxCategories': list(SFX_REGISTRY.keys()),
            'fontsCount': len(list_all_fonts())}


def _whisper_available():
    try:
        import importlib.util
        return importlib.util.find_spec('faster_whisper') is not None
    except Exception:
        return False


def _cv2_available():
    try:
        import importlib.util
        return importlib.util.find_spec('cv2') is not None
    except Exception:
        return False


@app.get('/api/projects')
def list_projects():
    results = []
    for p in DATA.glob('*/project.json'):
        try:
            x = json.loads(p.read_text('utf-8'))
            results.append({k: x.get(k) for k in ('id', 'name', 'createdAt', 'updatedAt', 'duration', 'source', 'width', 'height')})
        except Exception:
            continue
    return sorted(results, key=lambda x: x.get('updatedAt') or '', reverse=True)


@app.post('/api/projects')
async def create_project(file: UploadFile = File(...)):
    ext = Path(file.filename or '').suffix.lower()
    if ext not in SUPPORTED_VIDEO:
        raise HTTPException(400, 'Please upload MP4, MOV, WebM, M4V or MKV')
    pid = uuid.uuid4().hex
    p = DATA / pid
    p.mkdir()
    dest = p / f'original{ext}'
    size = 0
    try:
        with dest.open('wb') as f:
            while chunk := await file.read(1024 * 1024):
                size += len(chunk)
                if size > MAX_BYTES:
                    raise HTTPException(413, '500 MB max video size in this local build')
                f.write(chunk)
        info = probe(dest)
        if not info['duration'] or info['duration'] > 1800:
            raise HTTPException(400, 'Please upload a video up to 30 minutes')
    except Exception as e:
        shutil.rmtree(p, ignore_errors=True)
        if isinstance(e, HTTPException):
            raise e
        raise HTTPException(400, f'Cannot read video: {e}') from e
    now = datetime.now(timezone.utc).isoformat()
    project = dict(id=pid, name=Path(file.filename).stem[:120], source=dest.name,
                   duration=info['duration'], width=info['width'], height=info['height'], hasAudio=info['hasAudio'],
                   trim=dict(start=0, end=info['duration']), resolution=720, aspectRatio='9:16',
                   effects=dict(brightness=0, contrast=1, saturation=1, sharpness=0, clearVoice='Off',
                                sfx='None', sfxMode='Sentence', sfxVolume=40),
                   tracks=[new_track()], overlays=[], audioClips=[], customFonts=[], createdAt=now, updatedAt=now)
    (p / 'project.json').write_text(json.dumps(project, ensure_ascii=False, indent=2), 'utf-8')
    return project


@app.get('/api/projects/{pid}')
def get_project(pid: str):
    return load(pid)


@app.put('/api/projects/{pid}')
def update_project(pid: str, patch: dict = Body(...)):
    project = load(pid)
    allowed = {'name', 'trim', 'tracks', 'overlays', 'audioClips', 'customFonts', 'effects', 'resolution', 'aspectRatio'}
    for k in allowed:
        if k in patch:
            project[k] = patch[k]
    project['trim']['start'] = min(max(0, float(project['trim'].get('start', 0))), project['duration'])
    project['trim']['end'] = min(project['duration'], max(project['trim']['start'] + 0.1, float(project['trim'].get('end', project['duration']))))
    if len(project['tracks']) > 5:
        raise HTTPException(400, 'Up to five caption tracks are supported')
    for tr in project['tracks']:
        if len(tr.get('captions', [])) > 1500:
            raise HTTPException(400, 'Track exceeds 1500 subtitle cues')
        tr['captions'] = sorted([normalized_caption(c, i) for i, c in enumerate(tr.get('captions', []))], key=lambda x: x['start'])
    return save(project)


@app.get('/api/projects/{pid}/video')
def source_video(pid: str):
    pr = load(pid)
    return FileResponse(folder(pid) / pr['source'], media_type='video/mp4')


@app.delete('/api/projects/{pid}')
def delete_project(pid: str):
    p = folder(pid)
    shutil.rmtree(p)
    return {'ok': True}


@app.get('/api/sfx')
def list_sfx_catalog():
    sfx_dir = ROOT / 'static' / 'sfx'
    catalog = export_all_sfx(sfx_dir)
    return {'categories': list(SFX_REGISTRY.keys()), 'catalog': catalog}


@app.get('/api/fonts')
def get_font_library():
    return {'fonts': list_all_fonts()}


@app.post('/api/fonts')
async def upload_custom_font(file: UploadFile = File(...)):
    content = await file.read()
    try:
        font_info = register_custom_font(file.filename or 'custom.ttf', content)
        return font_info
    except Exception as e:
        raise HTTPException(400, str(e))


class TrackMotionRequest(BaseModel):
    trackType: str = 'face'
    start: float = 0.0
    end: float = 5.0


@app.post('/api/projects/{pid}/track')
def track_project_motion(pid: str, body: TrackMotionRequest):
    pr = load(pid)
    video_path = folder(pid) / pr['source']
    points = track_motion(video_path, body.trackType, body.start, body.end, pr.get('width', 720), pr.get('height', 1280))
    return {'trackType': body.trackType, 'start': body.start, 'end': body.end, 'points': points}


class AddAudioClipRequest(BaseModel):
    name: str
    category: str = 'sfx'
    source: str
    start: float = 0.0
    duration: float = 1.0
    trimStart: float = 0.0
    volume: float = 1.0
    fadeIn: float = 0.0
    fadeOut: float = 0.0
    ducking: bool = False


@app.post('/api/projects/{pid}/audio')
def add_audio_clip(pid: str, body: AddAudioClipRequest):
    pr = load(pid)
    clip = {
        'id': uuid.uuid4().hex[:10],
        'name': body.name,
        'category': body.category,
        'source': body.source,
        'start': max(0.0, float(body.start)),
        'duration': max(0.05, float(body.duration)),
        'trimStart': max(0.0, float(body.trimStart)),
        'volume': max(0.0, min(2.0, float(body.volume))),
        'fadeIn': max(0.0, float(body.fadeIn)),
        'fadeOut': max(0.0, float(body.fadeOut)),
        'ducking': bool(body.ducking),
    }
    pr.setdefault('audioClips', []).append(clip)
    save(pr)
    return {'clip': clip, 'project': pr}


@app.post('/api/projects/{pid}/srt')
async def import_srt(pid: str, file: UploadFile = File(...)):
    pr = load(pid)
    if not (file.filename or '').lower().endswith(('.srt', '.vtt')):
        raise HTTPException(400, 'Upload .srt or .vtt file')
    content = (await file.read(2_000_000)).decode('utf-8-sig', errors='replace')
    captions = parse_srt(content)
    if not captions:
        raise HTTPException(400, 'No readable subtitle cues')
    track = new_track('Imported subtitles', 'auto', '#38c5b3')
    track['captions'] = captions
    pr['tracks'].append(track)
    save(pr)
    return {'track': track, 'project': pr}


def parse_srt(content: str) -> list[dict]:
    content = content.replace('\r\n', '\n').replace('\r', '\n').replace('WEBVTT', '')
    pattern = re.compile(r'(\d\d:\d\d:\d\d[,.]\d+|\d\d:\d\d[,.]\d+)\s*-->\s*(\d\d:\d\d:\d\d[,.]\d+|\d\d:\d\d[,.]\d+)[^\n]*\n([^\n]+(?:\n(?!\s*\n|\d+\s*\n\d\d:|\d\d:\d\d:)[^\n]+)*)', re.M)
    def sec(s):
        parts = s.replace(',', '.').split(':')
        return sum(float(x) * 60**i for i, x in enumerate(reversed(parts)))
    out = []
    for i, m in enumerate(pattern.finditer(content)):
        text = re.sub(r'<[^>]+>', '', m.group(3)).strip()
        if text:
            out.append(normalized_caption(dict(start=sec(m.group(1)), end=sec(m.group(2)), text=text), i))
    return out


def sec_stamp(s: float) -> str:
    s = max(0, s); hh = int(s // 3600); mm = int(s % 3600 // 60); ss = int(s % 60); ms = int(round((s - int(s)) * 1000))
    return f'{hh:02}:{mm:02}:{ss:02},{ms:03}'


@app.get('/api/projects/{pid}/srt')
def export_srt(pid: str, track: str = ''):
    pr = load(pid)
    tr = next((x for x in pr['tracks'] if x['id'] == track), None) or pr['tracks'][0]
    lines = []
    for i, c in enumerate(tr['captions'], 1):
        lines.append(f"{i}\n{sec_stamp(c['start'])} --> {sec_stamp(c['end'])}\n{c['text']}\n")
    path = folder(pid) / 'export.srt'
    path.write_text('\n'.join(lines), encoding='utf-8')
    return FileResponse(path, filename=f"{pr['name']}_captions.srt", media_type='text/plain')


class TranscribeRequest(BaseModel):
    language: str | None = None
    model: str = 'tiny'


@app.post('/api/projects/{pid}/transcribe')
def transcribe(pid: str, body: TranscribeRequest):
    pr = load(pid)
    if not pr['hasAudio']:
        raise HTTPException(400, 'This video has no audio')
    if not _whisper_available():
        raise HTTPException(503, 'Install optional AI dependency: pip install faster-whisper')
    jobid = start_job('transcribe', _run_transcription, pid, body.language, body.model)
    return {'jobId': jobid}


def _run_transcription(jobid: str, pid: str, language: str | None, model_name: str):
    global MODEL
    if model_name not in ('tiny', 'base', 'small', 'medium', 'large-v3'):
        model_name = 'tiny'
    pr = load(pid); p = folder(pid)
    audio = p / 'speech.wav'
    start = float(pr['trim']['start']); length = float(pr['trim']['end']) - start
    run(['ffmpeg', '-y', '-v', 'error', '-ss', str(start), '-t', str(length), '-i', str(p / pr['source']), '-vn', '-ar', '16000', '-ac', '1', str(audio)], timeout=180)
    from faster_whisper import WhisperModel
    with MODEL_LOCK:
        if MODEL is None or getattr(MODEL, '_cf_size', None) != model_name:
            try:
                MODEL = WhisperModel(model_name, device='cpu', compute_type='int8')
                MODEL._cf_size = model_name
            except Exception:
                MODEL = WhisperModel('tiny', device='cpu', compute_type='int8')
                MODEL._cf_size = 'tiny'
        segments, info = MODEL.transcribe(str(audio), language=None if language in (None, 'auto', '') else language,
                                          word_timestamps=True, vad_filter=True, beam_size=5)
        segments = list(segments)
    word_list = []
    for seg in segments:
        if seg.words:
            word_list.extend(dict(start=round(start + float(w.start), 3), end=round(start + float(w.end), 3), text=w.word.strip()) for w in seg.words if w.word.strip())
        elif seg.text.strip():
            word_list.append(dict(start=round(start + float(seg.start), 3), end=round(start + float(seg.end), 3), text=seg.text.strip()))
    cues = []; group = []
    for w in word_list:
        if group and (len(group) >= 4 or w['end'] - group[0]['start'] > 2.6 or w['start'] - group[-1]['end'] > 0.8):
            cues.append(group); group = []
        group.append(w)
        if re.search(r'[.!?؟。]$', w['text']) and group:
            cues.append(group); group = []
    if group: cues.append(group)
    output = []
    for i, g in enumerate(cues):
        output.append(normalized_caption(dict(start=g[0]['start'], end=g[-1]['end'],
                                              text=' '.join(w['text'] for w in g), words=g), i))
    pr = load(pid); target = pr['tracks'][0]
    target['captions'] = output
    target['language'] = info.language
    save(pr)
    return {'project': pr, 'captions': len(output), 'language': info.language}


class TranslateRequest(BaseModel):
    trackId: str
    language: str
    mode: str = 'Simultaneous'
    provider: str | None = None


@app.post('/api/projects/{pid}/translate')
def translate(pid: str, body: TranslateRequest):
    pr = load(pid)
    src = next((t for t in pr['tracks'] if t['id'] == body.trackId), None)
    if not src:
        raise HTTPException(404, 'Caption track not found')
    if not src['captions']:
        raise HTTPException(400, 'Generate or import subtitles first')
    jobid = start_job('translate', _run_translation, pid, body.trackId, body.language, body.mode, body.provider)
    return {'jobId': jobid}


def _run_translation(jobid: str, pid: str, track_id: str, language: str, mode: str, provider: str | None = None):
    pr = load(pid)
    src = next(t for t in pr['tracks'] if t['id'] == track_id)
    texts = [c['text'] for c in src['captions']]
    try:
        from translation import translate_batch, get_active_provider
    except ImportError:
        from captionforge.translation import translate_batch, get_active_provider

    src_lang = src.get('language') or 'en'
    active_provider = get_active_provider(provider)
    translations = translate_batch(texts, language, source_lang=src_lang, provider=provider)
    if len(translations) != len(texts):
        try:
            from translation import translate_text
        except ImportError:
            from captionforge.translation import translate_text
        translations = [translate_text(t, language, source_lang=src_lang, provider=provider) for t in texts]

    pr = load(pid)
    src = next(t for t in pr['tracks'] if t['id'] == track_id)
    new = new_track(language, language, ['#ffac3e', '#33c6bd', '#8a59ff'][len(pr['tracks']) % 3])
    new['style']['yPos'] = min(0.91, src['style'].get('yPos', 0.76) + 0.10)
    for i, (a, t) in enumerate(zip(src['captions'], translations)):
        new['captions'].append(normalized_caption(dict(start=a['start'], end=a['end'], text=str(t)), i))
    if mode.lower() == 'replace':
        pr['tracks'] = [t for t in pr['tracks'] if t['id'] != track_id]
    pr['tracks'].append(new)
    save(pr)
    return {'project': pr, 'trackId': new['id'], 'provider': active_provider.name}


class ExportRequest(BaseModel):
    resolution: int = 720
    aspectRatio: str | None = None


@app.post('/api/projects/{pid}/export')
def export_video(pid: str, body: ExportRequest):
    load(pid)
    if not shutil.which('ffmpeg'):
        raise HTTPException(503, 'FFmpeg is required for rendering')
    jobid = start_job('export', _run_export, pid, body.resolution, body.aspectRatio)
    return {'jobId': jobid}


@app.get('/api/jobs/{jobid}')
def job_status(jobid: str):
    if jobid not in JOBS:
        raise HTTPException(404, 'Job not found. Server may have restarted.')
    return JOBS[jobid]


@app.get('/api/projects/{pid}/download')
def get_render(pid: str):
    p = folder(pid); f = p / 'rendered.mp4'
    if not f.is_file():
        raise HTTPException(404, 'No exported video yet')
    return FileResponse(f, filename=f'{load(pid)["name"]}_captionforge.mp4', media_type='video/mp4')


def start_job(kind, fn, *args):
    jid = uuid.uuid4().hex
    JOBS[jid] = {'id': jid, 'type': kind, 'status': 'running', 'message': 'Processing...'}
    def worker():
        try:
            data = fn(jid, *args)
            JOBS[jid] = {'id': jid, 'type': kind, 'status': 'complete', 'result': data}
        except Exception as e:
            JOBS[jid] = {'id': jid, 'type': kind, 'status': 'error', 'message': str(e)[:1500]}
    threading.Thread(target=worker, daemon=True).start()
    return jid


def ass_color(c: str, alpha='00'):
    c = str(c).lstrip('#')
    if not re.fullmatch(r'[0-9a-fA-F]{6}', c): c = 'FFFFFF'
    return f'&H{alpha}{c[4:6]}{c[2:4]}{c[0:2]}'


def ass_text(s):
    return str(s).replace('\\', '').replace('{', '(').replace('}', ')').replace('\n', r'\N')


def detect_face_trajectory(video: Path, cue_start: float, cue_end: float, trim_start: float, width: int, height: int):
    if not _cv2_available():
        return None
    import cv2
    cap = cv2.VideoCapture(str(video))
    cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')

    def _sample(ts: float):
        actual_ts = max(0.0, ts)
        cap.set(cv2.CAP_PROP_POS_MSEC, actual_ts * 1000)
        ok, frame = cap.read()
        if not ok: return None
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        faces = cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=4, minSize=(28, 28))
        if len(faces):
            x, y, w, h = max(faces, key=lambda f: f[2] * f[3])
            fw, fh = frame.shape[1], frame.shape[0]
            scale = min(width / fw, height / fh)
            offx = (width - fw * scale) / 2
            offy = (height - fh * scale) / 2
            xx = int(offx + (x + w / 2) * scale)
            yy = int(offy + (y + h + 24) * scale)
            return (max(60, min(width - 60, xx)), max(100, min(height - 100, yy)))
        return None

    pos1 = _sample(cue_start + 0.05)
    pos2 = _sample(max(cue_start + 0.1, cue_end - 0.05))
    cap.release()

    if pos1 and pos2:
        if math.hypot(pos2[0] - pos1[0], pos2[1] - pos1[1]) >= 12:
            return (pos1[0], pos1[1], pos2[0], pos2[1])
        return pos1
    return pos1 or pos2


def _resolve_three_tiers(cue: dict) -> tuple[str, str, str]:
    t1 = str(cue.get('tier1Text') or '').strip()
    t2 = str(cue.get('tier2Text') or '').strip()
    t3 = str(cue.get('tier3Text') or '').strip()
    if t1 or t2 or t3:
        return (t1, t2 or str(cue.get('text', '')).strip(), t3)
    words = cue.get('words') or []
    if any(int(w.get('tier', 0) or 0) in (1, 2, 3) for w in words):
        w1 = ' '.join(str(w.get('text', '')) for w in words if int(w.get('tier', 0) or 0) == 1).strip()
        w2 = ' '.join(str(w.get('text', '')) for w in words if int(w.get('tier', 0) or 0) in (0, 2)).strip()
        w3 = ' '.join(str(w.get('text', '')) for w in words if int(w.get('tier', 0) or 0) == 3).strip()
        return (w1, w2 or str(cue.get('text', '')).strip(), w3)
    raw = str(cue.get('text', '')).strip()
    lines = [ln.strip() for ln in raw.split('\n') if ln.strip()]
    if len(lines) >= 3:
        return (lines[0], lines[1], ' '.join(lines[2:]))
    if len(lines) == 2:
        return (lines[0], lines[1], '')
    tokens = [tok for tok in raw.split() if tok]
    if len(tokens) >= 5:
        return (' '.join(tokens[:2]), ' '.join(tokens[2:-2]), ' '.join(tokens[-2:]))
    if len(tokens) == 4:
        return (tokens[0], ' '.join(tokens[1:3]), tokens[3])
    if len(tokens) == 3:
        return (tokens[0], tokens[1], tokens[2])
    if len(tokens) == 2:
        return (tokens[0], tokens[1], '')
    return ('', raw, '')


def build_ass(pr: dict, dest: Path, width: int, height: int, skip_procedural_overlays: bool = False):
    header = f"""[Script Info]
Title: CaptionForge Cinematic
ScriptType: v4.00+
PlayResX: {width}
PlayResY: {height}
WrapStyle: 2
ScaledBorderAndShadow: yes

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
"""
    styles = []
    events = []
    trim_start = float(pr.get('trim', {}).get('start', 0))
    trim_end = float(pr.get('trim', {}).get('end', pr.get('duration', 10)))
    length = max(0.1, trim_end - trim_start)

    for i, tr in enumerate(pr.get('tracks', [])):
        if not tr.get('visible', True) or not tr.get('captions'):
            continue
        st = tr.get('style', {})
        size = max(13, min(96, float(st.get('fontSize', 25)))) * (width / 360)
        font = st.get('font', 'Vazirmatn').replace(',', '')
        color = ass_color(st.get('textColor', '#ffffff'))
        sec_color = ass_color(st.get('secondaryColor', '#FF7A00'))
        outline_col = ass_color(st.get('outlineColor', '#000000'))
        overlay = st.get('overlay', 'None')
        if overlay == 'Solid':
            bgcolor = '&H00000000&'
            border = 3
        elif overlay == 'Translucent':
            bgcolor = '&H80000000&'
            border = 3
        else:
            bgcolor = ass_color(st.get('bgColor', '#181124'), '55')
            border = 3 if st.get('bgEnabled') else 1
        style = f'track{i}'
        align = {'left': 4, 'center': 5, 'right': 6}.get(st.get('align', 'center'), 5)
        bold_flag = -1 if int(st.get('fontWeight', 700)) >= 600 or st.get('preset') in ('Bold', 'Orange Headline', 'Orange Persian Kinetic', 'Neon') else 0
        spacing = int(float(st.get('letterSpacing', 0)))
        outline_w = max(0.0, float(st.get('outline', 2.5)))
        shadow_d = max(0.0, float(st.get('shadowDepth', 2.0)) + float(st.get('extrusion3D', 0.0)) * 0.6)
        styles.append(f'Style: {style},{font},{size:.1f},{color},{sec_color},{outline_col},{bgcolor},{bold_flag},0,0,0,100,100,{spacing},0,{border},{outline_w:.1f},{shadow_d:.1f},{align},20,20,30,1')
        visible = [c for c in tr['captions'] if c.get('text', '').strip()]

        for ci, cue in enumerate(visible):
            cue_st = cue.get('customStyle') or st
            c_start = float(cue.get('start', 0))
            c_end = float(cue.get('end', c_start + 1.0))
            early = max(0.0, float(cue_st.get('early', 0)))

            if trim_start > 0 and c_end <= trim_start: continue
            if c_start >= trim_end: continue

            rel_start = max(0.0, c_start - trim_start) - early
            rel_end = min(length, c_end - trim_start)

            if rel_end <= 0 or rel_start >= length: continue

            start = max(0.0, round(rel_start, 3))
            end = min(length, max(start + 0.08, round(rel_end, 3)))

            custom_x = cue_st.get('xPos')
            if custom_x is not None:
                x = int(width * max(0.08, min(0.92, float(custom_x))))
            else:
                x = {'left': int(width * 0.09), 'center': width // 2, 'right': int(width * 0.91)}.get(cue_st.get('align', 'center'), width // 2)
            y = int(height * max(0.10, min(0.93, float(cue_st.get('yPos', 0.76)))))

            face_traj = detect_face_trajectory(dest.parent / pr['source'], c_start, c_end, trim_start, width, height) if cue_st.get('followFace') else None
            dur_ms = int(max(100, (end - start) * 1000))

            if face_traj:
                if len(face_traj) == 4:
                    x1, y1, x2, y2 = face_traj
                    prefix = f'{{\\move({x1},{y1},{x2},{y2},0,{dur_ms})}}'
                else:
                    x, y = face_traj
                    prefix = f'{{\\pos({x},{y})}}'
            else:
                prefix = f'{{\\pos({x},{y})}}'

            rot_deg = float(cue_st.get('rotation', 0.0))
            if abs(rot_deg) > 0.1:
                prefix += f'{{\\frz{int(rot_deg)}}}'

            animate = cue_st.get('animation', 'None')
            exit_anim = cue_st.get('exitAnimation', 'Fade')
            fade_out_ms = 140 if exit_anim in ('Fade', 'Blur Out', 'Scale Down') else 0
            if animate == 'Fade':
                prefix += f'{{\\fad(150,{fade_out_ms})}}'
            elif animate == 'Pop':
                prefix += r'{\fscx75\fscy75\t(0,120,\fscx115\fscy115)\t(120,220,\fscx100\fscy100)}'
                if fade_out_ms:
                    prefix += f'{{\\fad(0,{fade_out_ms})}}'
            elif animate == 'Zoom':
                prefix += r'{\fscx30\fscy30\t(0,200,\fscx100\fscy100)}'
            elif animate == 'Slide up' and not (face_traj and len(face_traj) == 4):
                prefix += f'{{\\move({x},{y+25},{x},{y},0,180)}}'
            elif animate == 'Slide down' and not (face_traj and len(face_traj) == 4):
                prefix += f'{{\\move({x},{y-25},{x},{y},0,180)}}'
            elif animate == 'Bounce':
                prefix += r'{\fscx85\fscy85\t(0,100,\fscx120\fscy120)\t(100,240,\fscx100\fscy100)}'

            if float(cue_st.get('glow', 0)) > 0:
                glow_r = int(float(cue_st.get('glow', 0)))
                glow_c = ass_color(cue_st.get('glowColor', '#00F0FF'))
                prefix += f'{{\\blur{glow_r}\\3c{glow_c}}}'

            is_three_tier = bool(cue_st.get('threeTierLayout')) or cue_st.get('preset') == 'Orange Persian Kinetic' or bool(cue.get('tier1Text') or cue.get('tier2Text'))
            if is_three_tier:
                t1, t2, t3 = _resolve_three_tiers(cue)
                c1 = ass_color(cue_st.get('tier1Color', '#FFFFFF'))
                c2 = ass_color(cue_st.get('tier2Color', '#FF7A00'))
                c3 = ass_color(cue_st.get('tier3Color', '#FACC15'))
                fs_t1 = int(size * 0.78)
                fs_t2 = int(size * 1.32)
                fs_t3 = int(size * 0.86)
                parts = []
                if t1:
                    parts.append(f'{{\\fs{fs_t1}\\c{c1}\\b1}}{ass_text(t1)}')
                if t2:
                    parts.append(f'{{\\fs{fs_t2}\\c{c2}\\b1}}{ass_text(t2)}')
                if t3:
                    parts.append(f'{{\\fs{fs_t3}\\c{c3}\\b1}}{ass_text(t3)}')
                text = r'\N'.join(parts)
            else:
                text = ass_text(cue['text'])
                if cue_st.get('oneLine'):
                    text = text.replace(r'\N', ' ')

                if cue_st.get('karaoke') and cue.get('words'):
                    items = []
                    for w in cue['words']:
                        w_start = float(w.get('start', c_start))
                        w_end = float(w.get('end', w_start + 0.1))
                        rel_ws = max(0.0, w_start - trim_start)
                        rel_we = min(length, max(rel_ws + 0.05, w_end - trim_start))
                        ws = max(start, min(end, rel_ws))
                        we = max(ws + 0.05, min(end, rel_we))
                        span = int(max(1, (we - ws) * 100))
                        w_tag = f'\\k{span}'
                        if w.get('customColor'):
                            w_tag += f'\\c{ass_color(w["customColor"])}'
                        boost = float(w.get('scaleBoost', 1.0) or 1.0)
                        if abs(boost - 1.0) > 0.04:
                            sc_pct = int(boost * 100)
                            w_tag += f'\\fscx{sc_pct}\\fscy{sc_pct}'
                        items.append(f'{{{w_tag}}}' + ass_text(w.get('text', '')))
                    text = ' '.join(items)

            events.append((start, f'Dialogue: {i+1},{ass_time(start)},{ass_time(end)},{style},,0,0,0,,{prefix}{text}'))

    styles.append('Style: Default,Vazirmatn,25,&H00FFFFFF,&H0000E8FF,&H00000000&,&H55181124,-1,0,0,0,100,100,0,0,1,2,0,5,20,20,30,1')
    output = header + '\n'.join(styles) + '\n\n[Events]\nFormat: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n'

    # Advanced Keyframe Motion Graphics & Composited Overlays
    for ov in pr.get('overlays', []):
        if not ov.get('visible', True):
            continue
        ov_type = ov.get('type', 'template')
        if skip_procedural_overlays and ov_type in (
            'glow_hologram', 'light_leak', 'metallic_banner', 'flag_graphic', 'waving_flag',
            'soft_bg_text', 'persian_orange_headline', 'persian_orange'
        ):
            continue
        o_start = float(ov.get('start', 0))
        o_end = float(ov.get('end', o_start + 3.0))
        if trim_start > 0 and o_end <= trim_start: continue
        if o_start >= trim_end: continue
        rel_start = max(0.0, o_start - trim_start)
        rel_end = min(length, max(rel_start + 0.1, o_end - trim_start))
        if rel_end <= 0 or rel_start >= length: continue

        keyframes = ov.get('keyframes', [])
        content = ass_text(ov.get('content') or ov.get('name') or '')
        depth_layer = 0 if ov.get('isBackground') or ov.get('depthLayer') == 0 or ov_type == 'soft_bg_text' else 3
        font_name = 'Cinzel' if ov_type == 'metallic_banner' else ('Orbitron' if ov_type == 'glow_hologram' else 'Vazirmatn')

        if keyframes and len(keyframes) >= 2:
            sorted_kf = sorted(keyframes, key=lambda k: float(k.get('time', 0.0)))
            t_first = float(sorted_kf[0].get('time', 0.0))
            t_last = float(sorted_kf[-1].get('time', 0.0))
            if abs(t_last - t_first) < 1e-4:
                k0 = sorted_kf[0]
                x = int(width * float(k0.get('xPos', 0.5)))
                y = int(height * float(k0.get('yPos', 0.5)))
                sc = int(float(k0.get('scale', 1.0)) * 100)
                rot = int(float(k0.get('rotation', 0.0)))
                rot_tag = f'\\frz{rot}' if rot != 0 else ''
                c = ass_color(k0.get('color', ov.get('color', '#FF7A00')))
                tag = f'{{\\pos({x},{y})\\an5\\fn{font_name}\\fs{int(32 * width/360)}\\c{c}\\3c&H000000&\\bord3\\shad2\\fscx{sc}\\fscy{sc}{rot_tag}}}'
                events.append((rel_start, f'Dialogue: {depth_layer},{ass_time(rel_start)},{ass_time(rel_end)},Default,,0,0,0,,{tag}{content}'))
            else:
                for ki in range(len(sorted_kf) - 1):
                    k1, k2 = sorted_kf[ki], sorted_kf[ki + 1]
                    t1 = rel_start + float(k1.get('time', 0.0))
                    t2 = rel_start + float(k2.get('time', 0.0))
                    if t1 >= rel_end or t2 <= rel_start or t2 <= t1: continue
                    seg_start = max(rel_start, t1)
                    seg_end = min(rel_end, t2)
                    if seg_end <= seg_start: continue
                    dur_ms = int(max(50, (seg_end - seg_start) * 1000))

                    x1, y1 = int(width * float(k1.get('xPos', 0.5))), int(height * float(k1.get('yPos', 0.5)))
                    x2, y2 = int(width * float(k2.get('xPos', 0.5))), int(height * float(k2.get('yPos', 0.5)))
                    sc1 = int(float(k1.get('scale', 1.0)) * 100)
                    sc2 = int(float(k2.get('scale', 1.0)) * 100)
                    r1 = int(float(k1.get('rotation', 0.0)))
                    r2 = int(float(k2.get('rotation', 0.0)))
                    op1 = f"&H{int((1.0 - float(k1.get('opacity', 1.0))) * 255):02X}&"
                    op2 = f"&H{int((1.0 - float(k2.get('opacity', 1.0))) * 255):02X}&"
                    c1 = ass_color(k1.get('color', ov.get('color', '#FF7A00')))

                    tag = f'{{\\move({x1},{y1},{x2},{y2},0,{dur_ms})\\an5\\fn{font_name}\\fs{int(32 * width/360)}\\c{c1}\\3c&H000000&\\bord3\\shad2\\fscx{sc1}\\fscy{sc1}\\frz{r1}\\alpha{op1}\\t(0,{dur_ms},\\fscx{sc2}\\fscy{sc2}\\frz{r2}\\alpha{op2})}}'
                    events.append((seg_start, f'Dialogue: {depth_layer},{ass_time(seg_start)},{ass_time(seg_end)},Default,,0,0,0,,{tag}{content}'))
        else:
            ox = int(width * float(ov.get('xPos', 0.5)))
            oy = int(height * float(ov.get('yPos', 0.5)))
            scale_pct = int(float(ov.get('scale', 1.0)) * 100)
            rot = int(float(ov.get('rotation', 0.0)))
            ov_color = ass_color(ov.get('color', '#FF7A00'))
            rot_tag = f'\\frz{rot}' if rot != 0 else ''

            if ov_type in ('persian_orange_headline', 'persian_orange'):
                title = content or 'فنلاند'
                tag = f'{{\\pos({ox},{oy})\\an5\\fnVazirmatn\\b1\\fs{int(44 * width/360)}\\c&H006AFF&\\3c&H1A1A1A&\\bord5\\shad4\\blur1\\fad(100,100)\\fscx{scale_pct}\\fscy{scale_pct}{rot_tag}}}'
                events.append((rel_start, f'Dialogue: 3,{ass_time(rel_start)},{ass_time(rel_end)},Default,,0,0,0,,{tag}{title}'))
            elif ov_type == 'metallic_banner':
                title_text = content or 'AUSTRALIA'
                events.append((rel_start, f'Dialogue: 3,{ass_time(rel_start)},{ass_time(rel_end)},Default,,0,0,0,,{{\\pos({ox},{oy})\\an5\\fnCinzel\\fs{int(42 * width/360)}\\c&HE2E8F0&\\3c&H1E293B&\\bord5\\shad3\\fad(100,120)\\fsp4\\fscx{scale_pct}\\fscy{scale_pct}{rot_tag}}}{title_text}'))
            elif ov_type == 'glow_hologram':
                holo_text = content or '◈ ILLUMINATED HOLOGRAM ◈'
                events.append((rel_start, f'Dialogue: {depth_layer},{ass_time(rel_start)},{ass_time(rel_end)},Default,,0,0,0,,{{\\pos({ox},{oy})\\an5\\fnOrbitron\\fs{int(32 * width/360)}\\c&H00F0FF&\\3c&H00A0E0&\\bord6\\shad0\\blur8\\fad(150,150)\\fscx{scale_pct}\\fscy{scale_pct}{rot_tag}}}{holo_text}'))
                events.append((rel_start, f'Dialogue: {depth_layer+1},{ass_time(rel_start)},{ass_time(rel_end)},Default,,0,0,0,,{{\\pos({ox},{oy})\\an5\\fnOrbitron\\fs{int(32 * width/360)}\\c&HFFFFFF&\\3c&H004080&\\bord2\\shad0\\fad(150,150)\\fscx{scale_pct}\\fscy{scale_pct}{rot_tag}}}{holo_text}'))
            elif ov_type == 'light_leak':
                events.append((rel_start, f'Dialogue: 3,{ass_time(rel_start)},{ass_time(rel_end)},Default,,0,0,0,,{{\\pos({ox},{oy})\\an5\\fnInter\\fs{int(30 * width/360)}\\c&HD946EF&\\3c&H9333EA&\\bord4\\shad4\\fad(80,140)\\alpha&H40&{rot_tag}}}{content or "✦ LIGHT FLARE ✦"}'))
            elif ov_type in ('flag_graphic', 'waving_flag'):
                events.append((rel_start, f'Dialogue: {depth_layer},{ass_time(rel_start)},{ass_time(rel_end)},Default,,0,0,0,,{{\\pos({ox},{oy})\\an5\\fnInter\\fs{int(30 * width/360)}\\c&HFFFFFF&\\3c&H1E3A8A&\\bord4\\shad2\\fad(120,120){rot_tag}}}{content or "⚑ FLAG OVERLAY ⚑"}'))
            elif ov_type == 'soft_bg_text':
                events.append((rel_start, f'Dialogue: 0,{ass_time(rel_start)},{ass_time(rel_end)},Default,,0,0,0,,{{\\pos({ox},{oy})\\an5\\fnVazirmatn\\fs{int(32 * width/360)}\\c&HCCCCCC&\\3c&H000000&\\bord2\\blur6\\alpha&H88&\\fad(200,200){rot_tag}}}{content or "برج‌های دوگانه بلند ترین"}'))
            elif ov_type == 'end_accent':
                events.append((rel_start, f'Dialogue: 3,{ass_time(rel_start)},{ass_time(rel_end)},Default,,0,0,0,,{{\\pos({ox},{oy})\\an5\\fnInter\\fs{int(22 * width/360)}\\c&H3539E5&\\3c&H000000&\\bord2\\shad1\\fad(100,100){rot_tag}}}{content or "CaptionForge Studio"}'))
            else:
                events.append((rel_start, f'Dialogue: {depth_layer},{ass_time(rel_start)},{ass_time(rel_end)},Default,,0,0,0,,{{\\pos({ox},{oy})\\an5\\fnVazirmatn\\fs{int(26 * width/360)}\\c{ov_color}\\3c&H000000&\\bord3\\shad2\\fad(120,120){rot_tag}}}{content}'))

    output += '\n'.join(line for _, line in sorted(events, key=lambda x: x[0])) + '\n'
    dest.write_text(output, encoding='utf-8')


def ass_time(s):
    cs = int(round(max(0, s) * 100))
    return f'{cs//360000}:{cs//6000%60:02}:{cs//100%60:02}.{cs%100:02}'


def mix_multitrack_audio(pr: dict, p: Path, length: float) -> Path | None:
    """Mixes 60 studio SFX presets (global, cue-entrance/exit, word-level) and timeline audio clips with ducking."""
    rate = SFX_SAMPLE_RATE  # 44100
    limit = min(length, 1800)
    samples = int(rate * limit)
    buf = bytearray(samples * 2)
    has_audio = False

    fx = pr.get('effects', {})
    typ = fx.get('sfx', 'None')
    sfx_mode = fx.get('sfxMode', 'Sentence')
    base_gain = max(0.0, min(1.0, float(fx.get('sfxVolume', 60)) / 100.0)) * 0.28

    # Collect (relative_time_sec, sfx_preset_name) triggers
    sfx_triggers: list[tuple[float, str]] = []
    trim_start = float(pr.get('trim', {}).get('start', 0))
    trim_end = float(pr.get('trim', {}).get('end', pr.get('duration', 10)))

    for t in pr.get('tracks', []):
        if not t.get('visible', True): continue
        for c in t.get('captions', []):
            cs = float(c.get('start', 0))
            ce = float(c.get('end', cs + 1.0))
            if trim_start > 0 and ce <= trim_start: continue
            if cs >= trim_end: continue
            rel_cs = max(0.0, cs - trim_start)
            rel_ce = max(0.0, ce - trim_start)

            cue_ent = c.get('entranceSfx', 'None')
            if cue_ent and cue_ent != 'None' and 0 <= rel_cs < length:
                sfx_triggers.append((rel_cs, cue_ent))
            elif typ != 'None' and sfx_mode in ('Sentence', 'Both') and 0 <= rel_cs < length:
                sfx_triggers.append((rel_cs, typ))

            cue_exit = c.get('exitSfx', 'None')
            if cue_exit and cue_exit != 'None' and 0 <= rel_ce < length:
                sfx_triggers.append((rel_ce, cue_exit))

            for w in c.get('words', []):
                ws = float(w.get('start', cs))
                rel_ws = max(0.0, ws - trim_start)
                w_sfx = w.get('sfx', 'None')
                if w_sfx and w_sfx != 'None' and 0 <= rel_ws < length:
                    sfx_triggers.append((rel_ws, w_sfx))
                elif typ != 'None' and sfx_mode == 'Word' and 0 <= rel_ws < length:
                    sfx_triggers.append((rel_ws, typ))

    for (tm, preset_key) in sfx_triggers[:900]:
        pcm = get_pcm_for_preset(preset_key)
        pcm_samples = len(pcm) // 2
        start_idx = int(tm * rate)
        for j in range(pcm_samples):
            k = start_idx + j
            if k >= samples: break
            raw_s = struct.unpack_from('<h', pcm, j * 2)[0] / 32768.0
            val = raw_s * base_gain
            prev = struct.unpack_from('<h', buf, k * 2)[0]
            mixed = max(-32768, min(32767, prev + int(val * 32767.0)))
            struct.pack_into('<h', buf, k * 2, mixed)
        has_audio = True

    # Precalculate active caption speech intervals for audio ducking
    caption_intervals = []
    for t in pr.get('tracks', []):
        if not t.get('visible', True): continue
        for c in t.get('captions', []):
            cs = float(c.get('start', 0))
            ce = float(c.get('end', cs + 1.0))
            if trim_start > 0 and ce <= trim_start: continue
            if cs >= trim_end: continue
            rel_s = max(0.0, cs - trim_start)
            rel_e = min(length, ce - trim_start)
            if rel_e > rel_s:
                caption_intervals.append((rel_s, rel_e))

    def get_ducking(t_sec: float) -> float:
        for (cs, ce) in caption_intervals:
            if cs - 0.1 <= t_sec <= ce + 0.15:
                return 0.25  # -12 dB attenuation during speech
        return 1.0

    # 2. Project Timeline Audio Clips (SFX, Music Loops, Voiceover)
    for clip in pr.get('audioClips', []):
        c_start = float(clip.get('start', 0.0))
        c_dur = float(clip.get('duration', 1.0))
        c_vol = float(clip.get('volume', 1.0))
        c_cat = clip.get('category', 'Soft Pop')
        c_src = clip.get('source', '')
        enable_ducking = bool(clip.get('ducking', False))

        pcm = get_pcm_for_preset(c_src or c_cat)
        pcm_samples = max(1, len(pcm) // 2)
        dur_samples = int(c_dur * rate)
        start_idx = int(c_start * rate)

        fade_in_samples = int(float(clip.get('fadeIn', 0.0)) * rate)
        fade_out_samples = int(float(clip.get('fadeOut', 0.0)) * rate)

        for j in range(dur_samples):
            k = start_idx + j
            if k >= samples: break
            raw_s = struct.unpack_from('<h', pcm, (j % pcm_samples) * 2)[0] / 32768.0

            env = 1.0
            if fade_in_samples > 0 and j < fade_in_samples:
                env *= (j / fade_in_samples)
            if fade_out_samples > 0 and (dur_samples - j) < fade_out_samples:
                env *= ((dur_samples - j) / fade_out_samples)

            duck = get_ducking(k / rate) if enable_ducking else 1.0
            val = raw_s * c_vol * env * duck * 0.35
            prev = struct.unpack_from('<h', buf, k * 2)[0]
            mixed = max(-32768, min(32767, prev + int(val * 32767.0)))
            struct.pack_into('<h', buf, k * 2, mixed)
            has_audio = True

    if not has_audio:
        return None

    path = p / 'sfx.wav'
    with wave.open(str(path), 'wb') as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(rate)
        wf.writeframes(buf)
    return path


write_sfx = mix_multitrack_audio


def _run_export(jobid: str, pid: str, resolution: int, aspect_ratio: str | None = None):
    pr = load(pid)
    p = folder(pid)
    if resolution not in (480, 720, 1080):
        resolution = 720
    aspect = (aspect_ratio or pr.get('aspectRatio') or '9:16').strip()
    if aspect == '16:9':
        width = 2 * round((resolution * 16 / 9) / 2)
        height = resolution
    elif aspect == '1:1':
        width = resolution
        height = resolution
    else:
        width = resolution
        height = 2 * round((resolution * 16 / 9) / 2)

    trim_start = float(pr['trim']['start'])
    trim_end = float(pr['trim']['end'])
    length = max(0.1, trim_end - trim_start)

    # 1. Procedural Video Compositor (Motion Graphics, Depth Occlusion, Tracking Overlays)
    composited_video = p / 'composited.mp4'
    has_comp = False
    if pr.get('overlays'):
        try:
            has_comp = composite_project_video(p / pr['source'], composited_video, pr, width, height, trim_start, length)
        except Exception:
            has_comp = False

    ass = p / 'captions.ass'
    build_ass(pr, ass, width, height, skip_procedural_overlays=has_comp)
    fx = pr.get('effects', {})
    brightness = max(-0.8, min(0.8, float(fx.get('brightness', 0))))
    contrast = max(0.5, min(2.0, float(fx.get('contrast', 1))))
    saturation = max(0.0, min(3.0, float(fx.get('saturation', 1))))
    sharpness = max(0.0, min(2.0, float(fx.get('sharpness', 0))))

    filters = [
        f'scale={width}:{height}:force_original_aspect_ratio=decrease:force_divisible_by=2',
        f'pad={width}:{height}:(ow-iw)/2:(oh-ih)/2:black',
        f'eq=brightness={brightness}:contrast={contrast}:saturation={saturation}'
    ]
    if sharpness:
        filters.append(f'unsharp=5:5:{sharpness}')
    if (DATA / 'fonts').is_dir() and any((DATA / 'fonts').iterdir()):
        filters.append('ass=captions.ass:fontsdir=../fonts')
    else:
        filters.append('ass=captions.ass')

    sfx = mix_multitrack_audio(pr, p, length)
    clear = fx.get('clearVoice', 'Off')
    denoise = {'Light': 'afftdn=nr=8', 'Balanced': 'afftdn=nr=14', 'Strong': 'afftdn=nr=22'}.get(clear, 'anull')

    # Check whether source video actually contains an audio stream
    source_has_audio = False
    try:
        source_probe = probe(p / pr['source'])
        source_has_audio = bool(source_probe.get('hasAudio'))
    except Exception:
        source_has_audio = bool(pr.get('hasAudio', False))

    if has_comp and composited_video.is_file():
        cmd = ['ffmpeg', '-y', '-v', 'error', '-t', str(length), '-i', str(composited_video)]
        if source_has_audio:
            cmd += ['-ss', str(trim_start), '-t', str(length), '-i', str(p / pr['source'])]
        if sfx:
            cmd += ['-i', str(sfx)]

        cmd += ['-vf', ','.join(filters)]

        if source_has_audio and sfx:
            cmd += ['-filter_complex', f'[1:a]{denoise}[clean];[clean][2:a]amix=inputs=2:duration=first:normalize=0,alimiter=limit=0.95:attack=5:release=50[aout]', '-map', '0:v:0', '-map', '[aout]']
        elif source_has_audio:
            if clear != 'Off':
                cmd += ['-af', denoise]
            cmd += ['-map', '0:v:0', '-map', '1:a:0']
        elif sfx:
            cmd += ['-af', 'alimiter=limit=0.95:attack=5:release=50', '-map', '0:v:0', '-map', '1:a:0']
        else:
            cmd += ['-an', '-map', '0:v:0']
    else:
        cmd = ['ffmpeg', '-y', '-v', 'error', '-ss', str(trim_start), '-t', str(length), '-i', str(p / pr['source'])]
        if sfx:
            cmd += ['-i', str(sfx)]
        cmd += ['-vf', ','.join(filters)]
        if source_has_audio and sfx:
            cmd += ['-filter_complex', f'[0:a]{denoise}[clean];[clean][1:a]amix=inputs=2:duration=first:normalize=0,alimiter=limit=0.95:attack=5:release=50[aout]', '-map', '0:v:0', '-map', '[aout]']
        elif source_has_audio:
            if clear != 'Off':
                cmd += ['-af', denoise]
            cmd += ['-map', '0:v:0', '-map', '0:a:0']
        elif sfx:
            cmd += ['-af', 'alimiter=limit=0.95:attack=5:release=50', '-map', '0:v:0', '-map', '1:a:0']
        else:
            cmd += ['-an']

    cmd += ['-t', str(length), '-c:v', 'libx264', '-preset', 'veryfast', '-crf', '22', '-pix_fmt', 'yuv420p',
            '-c:a', 'aac', '-b:a', '128k', '-movflags', '+faststart', str(p / 'rendered.mp4')]
    run(cmd, timeout=1800, cwd=p)
    output = probe(p / 'rendered.mp4')
    return {'download': f'/api/projects/{pid}/download', 'duration': output['duration'],
            'resolution': resolution, 'aspectRatio': aspect}


@app.get('/health')
@app.get('/api/health')
def health():
    return {'ok': True}
