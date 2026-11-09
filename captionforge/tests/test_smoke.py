import sys, os, time
from pathlib import Path
from fastapi.testclient import TestClient

TEST_DIR = Path(__file__).resolve().parent
ROOT = TEST_DIR.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from app import app
c = TestClient(app)
SAMPLE_MP4 = TEST_DIR / 'assets' / 'sample.mp4'

def test_health():
    assert c.get('/health').json()['ok']
    assert c.get('/api/health').json()['ok']

def test_api_config():
    cfg = c.get('/api/config').json()
    assert 'transcription' in cfg
    assert 'translation' in cfg
    assert 'translationProvider' in cfg
    assert cfg['translationProvider']['active'] is not None
    assert 'offline' in cfg['translationProvider']['available_providers']

def test_import_srt_and_export():
    with open(SAMPLE_MP4, 'rb') as f:
        r=c.post('/api/projects',files={'file':('sample.mp4',f,'video/mp4')})
    assert r.status_code==200,r.text
    p=r.json(); pid=p['id']
    assert 3<p['duration']<4
    srt='1\n00:00:00,100 --> 00:00:01,400\nHello from CaptionForge!\n\n2\n00:00:01,500 --> 00:00:03,000\nسلام دنیا\n'
    imp=c.post(f'/api/projects/{pid}/srt',files={'file':('test.srt',srt.encode(),'text/plain')})
    assert imp.status_code==200,imp.text
    imported=imp.json()['track']
    assert len(imported['captions'])==2, imported
    # Update style and settings, then ensure they persist.
    proj=imp.json()['project'];proj['tracks'][1]['style'].update({'animation':'Fade','textColor':'#ffee44','font':'Noto Sans Arabic'})
    r=c.put(f'/api/projects/{pid}',json={'tracks':proj['tracks'],'effects':proj['effects']})
    assert r.status_code==200,r.text
    out=c.get(f'/api/projects/{pid}/srt?track={imported["id"]}')
    assert 'سلام دنیا' in out.text
    r=c.post(f'/api/projects/{pid}/export',json={'resolution':480})
    assert r.status_code==200,r.text
    job=r.json()['jobId']
    for _ in range(75):
        status=c.get('/api/jobs/'+job).json()
        if status['status']!='running':break
        time.sleep(.2)
    assert status['status']=='complete',status
    rendered=c.get(f'/api/projects/{pid}/download')
    assert rendered.status_code==200 and len(rendered.content)>10000
    print('Working video export bytes:',len(rendered.content),' project ID:',pid)

if __name__=='__main__':
    test_health();test_import_srt_and_export()

def test_sound_effects_noise_and_face_follow():
    with open(SAMPLE_MP4, 'rb') as f:
        r=c.post('/api/projects',files={'file':('sample.mp4',f,'video/mp4')})
    p=r.json();pid=p['id'];t=p['tracks'][0]
    t['style']['followFace']=True
    t['style']['animation']='Pop'
    t['captions']=[{'id':'a','start':0.2,'end':1.5,'text':'Test sound', 'words':[{'start':.2,'end':.5,'text':'Test'},{'start':.6,'end':1.5,'text':'sound'}]}]
    p['effects'].update({'sfx':'Pop','sfxMode':'Word','sfxVolume':50,'clearVoice':'Light'})
    r=c.put(f'/api/projects/{pid}',json={'tracks':p['tracks'],'effects':p['effects']})
    assert r.status_code==200,r.text
    r=c.post(f'/api/projects/{pid}/export',json={'resolution':480});assert r.status_code==200,r.text
    job=r.json()['jobId']
    for _ in range(100):
        status=c.get('/api/jobs/'+job).json()
        if status['status']!='running':break
        time.sleep(.2)
    assert status['status']=='complete',status
    assert len(c.get(f'/api/projects/{pid}/download').content)>10000

def test_ai_transcribe_end_to_end():
    with open(SAMPLE_MP4, 'rb') as f:
        r=c.post('/api/projects',files={'file':('sample.mp4',f,'video/mp4')})
    assert r.status_code==200
    pid=r.json()['id']
    tr=c.post(f'/api/projects/{pid}/transcribe',json={'language':'en','model':'tiny'})
    assert tr.status_code==200,tr.text
    jid=tr.json()['jobId']
    for _ in range(60):
        st=c.get(f'/api/jobs/{jid}').json()
        if st['status']!='running':break
        time.sleep(0.5)
    assert st['status']=='complete',st
    proj=c.get(f'/api/projects/{pid}').json()
    assert proj['tracks'][0]['language']=='en'

def test_trim_cue_and_sfx_exclusion():
    from app import build_ass, write_sfx
    p = {
        'id': 'dummy',
        'duration': 20.0,
        'source': 'sample.mp4',
        'trim': {'start': 5.0, 'end': 15.0},
        'effects': {'sfx': 'Pop', 'sfxMode': 'Sentence', 'sfxVolume': 50},
        'tracks': [{
            'id': 't1',
            'visible': True,
            'style': {'animation': 'Pop', 'early': 0.0},
            'captions': [
                {'id': 'c1', 'start': 1.0, 'end': 3.0, 'text': 'GHOST_BEFORE'},
                {'id': 'c2', 'start': 6.0, 'end': 8.0, 'text': 'VALID_INSIDE'},
                {'id': 'c3', 'start': 16.0, 'end': 18.0, 'text': 'GHOST_AFTER'},
            ]
        }]
    }
    ass_path = TEST_DIR / 'test_trim.ass'
    build_ass(p, ass_path, 720, 1280)
    ass_text = ass_path.read_text(encoding='utf-8')
    ass_path.unlink()
    assert 'GHOST_BEFORE' not in ass_text
    assert 'GHOST_AFTER' not in ass_text
    assert 'VALID_INSIDE' in ass_text
    assert '0:00:01.00,0:00:03.00' in ass_text

    # Verify write_sfx doesn't crash and generates file for valid cue inside trimmed window
    sfx_path = write_sfx(p, TEST_DIR, 10.0)
    assert sfx_path is not None and sfx_path.is_file()
    sfx_path.unlink()


def test_expanded_sfx_and_graphic_overlays():
    from app import build_ass, write_sfx
    sound_types = ['Pop', 'Whoosh', 'Impact', 'Click', 'Shimmer', 'Riser', 'Sweep', 'Glitch', 'Ambient']

    for snd in sound_types:
        proj = {
            'id': f'test_{snd.lower()}',
            'duration': 10.0,
            'source': 'sample.mp4',
            'trim': {'start': 0.0, 'end': 10.0},
            'effects': {'sfx': snd, 'sfxMode': 'Sentence', 'sfxVolume': 60},
            'tracks': [{
                'id': 't1',
                'visible': True,
                'style': {'animation': 'Pop'},
                'captions': [{'id': 'c1', 'start': 1.0, 'end': 3.0, 'text': f'Testing {snd}'}]
            }]
        }
        sfx_path = write_sfx(proj, TEST_DIR, 10.0)
        assert sfx_path is not None and sfx_path.is_file(), f"Failed for {snd}"
        assert sfx_path.stat().st_size > 1000
        sfx_path.unlink()

    # Test overlays rendering in ASS
    proj_overlays = {
        'id': 'test_ov_export',
        'duration': 10.0,
        'source': 'sample.mp4',
        'trim': {'start': 0.0, 'end': 10.0},
        'tracks': [{
            'id': 't1',
            'visible': True,
            'style': {'font': 'Noto Sans', 'animation': 'Fade'},
            'captions': [{'id': 'c1', 'start': 0.5, 'end': 3.5, 'text': 'Main Subtitle'}]
        }],
        'overlays': [
            {'id': 'ov1', 'type': 'metallic_banner', 'content': 'AUSTRALIA', 'start': 1.0, 'end': 3.5, 'visible': True},
            {'id': 'ov2', 'type': 'glow_hologram', 'content': 'HOLOGRAM OBJECT', 'start': 2.0, 'end': 5.0, 'visible': True},
            {'id': 'ov3', 'type': 'light_leak', 'start': 0.5, 'end': 1.2, 'visible': True},
            {'id': 'ov4', 'type': 'flag_graphic', 'start': 3.0, 'end': 6.0, 'visible': True},
            {'id': 'ov5', 'type': 'soft_bg_text', 'content': 'کپشن فورج شناور', 'start': 1.5, 'end': 4.5, 'visible': True},
            {'id': 'ov6', 'type': 'end_accent', 'content': 'Final Accent', 'start': 8.0, 'end': 9.5, 'visible': True},
        ]
    }
    ass_path = TEST_DIR / 'test_overlays.ass'
    build_ass(proj_overlays, ass_path, 720, 1280)
    content = ass_path.read_text(encoding='utf-8')
    ass_path.unlink()

    assert 'AUSTRALIA' in content
    assert 'HOLOGRAM OBJECT' in content
    assert 'LIGHT FLARE' in content
    assert 'FLAG OVERLAY' in content
    assert 'کپشن فورج شناور' in content
    assert 'Final Accent' in content


def test_trim_boundary_straddle_and_empty_track_sfx():
    from app import build_ass, write_sfx
    p = {
        'id': 'straddle_test',
        'duration': 20.0,
        'source': 'sample.mp4',
        'trim': {'start': 5.0, 'end': 15.0},
        'effects': {'sfx': 'Pop', 'sfxMode': 'Sentence', 'sfxVolume': 50},
        'tracks': [
            {
                'id': 't0',
                'visible': True,
                'captions': []
            },
            {
                'id': 't1',
                'visible': True,
                'style': {'animation': 'None', 'early': 0.0},
                'captions': [
                    {'id': 'c_straddle', 'start': 4.0, 'end': 7.0, 'text': 'STRADDLE_CUE'}
                ]
            }
        ],
        'overlays': [
            {'id': 'ov_straddle', 'type': 'metallic_banner', 'content': 'STRADDLE_OV', 'start': 4.0, 'end': 7.0, 'visible': True},
            {'id': 'ov_bg', 'type': 'soft_bg_text', 'content': 'BG_DEPTH', 'start': 6.0, 'end': 9.0, 'visible': True}
        ]
    }
    ass_path = TEST_DIR / 'test_straddle.ass'
    build_ass(p, ass_path, 720, 1280)
    text = ass_path.read_text(encoding='utf-8')
    ass_path.unlink()

    # Straddle cue should start at 0.0s (not 4.0s) and end at 2.0s
    assert '0:00:00.00,0:00:02.00' in text
    assert 'STRADDLE_CUE' in text
    assert 'STRADDLE_OV' in text
    # Verify layer depths: soft_bg_text is Layer 0, subtitle track is Layer 2, metallic_banner is Layer 3
    assert 'Dialogue: 0,' in text and 'BG_DEPTH' in text
    assert 'Dialogue: 2,' in text and 'STRADDLE_CUE' in text
    assert 'Dialogue: 3,' in text and 'STRADDLE_OV' in text

    # Verify write_sfx finds captions on track 1 even when track 0 is empty
    sfx_path = write_sfx(p, TEST_DIR, 10.0)
    assert sfx_path is not None and sfx_path.is_file()
    assert sfx_path.stat().st_size > 1000
    sfx_path.unlink()




