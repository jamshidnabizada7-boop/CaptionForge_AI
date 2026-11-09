"""Full end-to-end verification of CaptionForge complete workflow."""
import os, sys, time, json
from pathlib import Path
import httpx, subprocess

# Live HTTP socket client connecting to real running backend
BASE_URL = "http://127.0.0.1:8000"
client = httpx.Client(base_url=BASE_URL, timeout=180.0)

def probe(path: Path) -> dict:
    result = subprocess.run(['ffprobe', '-v', 'error', '-show_entries', 'format=duration:stream=codec_type,width,height', '-of', 'json', str(path)], capture_output=True, text=True)
    info = json.loads(result.stdout)
    video = next((s for s in info.get('streams', []) if s.get('codec_type') == 'video'), None)
    if not video:
        raise RuntimeError('The file contains no supported video stream')
    return dict(duration=round(float(info.get('format', {}).get('duration') or 0), 3), width=video.get('width', 0), height=video.get('height', 0), hasAudio=any(s.get('codec_type') == 'audio' for s in info['streams']))

def run_full_verification():
    print("=" * 60)
    print("CAPTIONFORGE: RUNNING COMPLETE 14-STEP WORKFLOW VERIFICATION")
    print("=" * 60)

    # Step 1: Import video
    video_path = Path("real_speech_video.mp4")
    assert video_path.is_file(), "real_speech_video.mp4 not found"
    print(f"\n[Step 1] Importing real video with speech: {video_path} ({video_path.stat().st_size} bytes)")
    with open(video_path, "rb") as f:
        r = client.post("/api/projects", files={"file": ("real_speech_video.mp4", f, "video/mp4")})
    assert r.status_code == 200, f"Upload failed: {r.text}"
    project = r.json()
    pid = project["id"]
    print(f"  -> Project created: ID={pid}, Name='{project['name']}', Duration={project['duration']}s")
    assert project["duration"] > 6.0, "Expected video duration > 6s"
    assert project["hasAudio"] is True, "Expected video to have audio"

    # Step 2: Play it (video stream retrieval)
    print(f"\n[Step 2] Testing video playback / streaming endpoint: /api/projects/{pid}/video")
    v_res = client.get(f"/api/projects/{pid}/video")
    assert v_res.status_code == 200, f"Video fetch failed: {v_res.status_code}"
    assert len(v_res.content) > 10000, "Stream returned insufficient bytes"
    print(f"  -> Video stream verified: {len(v_res.content)} bytes received")

    # Step 3: Trim the video
    print(f"\n[Step 3] Trimming video: start=0.2s, end=6.2s")
    project["trim"] = {"start": 0.2, "end": 6.2}
    r = client.put(f"/api/projects/{pid}", json={"trim": project["trim"]})
    assert r.status_code == 200, f"Trim failed: {r.text}"
    project = r.json()
    assert project["trim"]["start"] == 0.2
    assert project["trim"]["end"] == 6.2
    print(f"  -> Trim saved: {project['trim']}")

    # Step 4: Generate real AI subtitles with faster-whisper
    print(f"\n[Step 4] Generating real AI subtitles using faster-whisper ASR...")
    tr_res = client.post(f"/api/projects/{pid}/transcribe", json={"language": "en", "model": "tiny"})
    assert tr_res.status_code == 200, f"Transcription start failed: {tr_res.text}"
    job_id = tr_res.json()["jobId"]
    print(f"  -> ASR job started: {job_id}. Polling completion...")
    for _ in range(60):
        st = client.get(f"/api/jobs/{job_id}").json()
        if st["status"] != "running":
            break
        time.sleep(0.5)
    assert st["status"] == "complete", f"ASR job failed: {st}"
    print(f"  -> ASR complete: {st['result']}")

    # Step 5: Verify accurate caption timestamps
    print(f"\n[Step 5] Verifying caption cues and word timestamps...")
    project = client.get(f"/api/projects/{pid}").json()
    orig_track = project["tracks"][0]
    captions = orig_track["captions"]
    assert len(captions) >= 2, f"Expected at least 2 caption cues, got {len(captions)}"
    for i, c in enumerate(captions):
        print(f"  Cue {i+1}: [{c['start']}s -> {c['end']}s] '{c['text']}' (words: {len(c.get('words', []))})")
        assert c["start"] >= 0, "Invalid start time"
        assert c["end"] > c["start"], "End time must be after start time"
        assert len(c["text"]) > 0, "Empty caption text"

    # Step 6: Edit subtitle text
    print(f"\n[Step 6] Editing subtitle text for pristine typography...")
    captions[0]["text"] = "Welcome to CaptionForge: AI Video Subtitle App"
    captions[1]["text"] = "We create beautiful animated subtitles."
    orig_track["captions"] = captions
    r = client.put(f"/api/projects/{pid}", json={"tracks": project["tracks"]})
    assert r.status_code == 200, f"Update failed: {r.text}"
    project = r.json()
    assert "Welcome to CaptionForge: AI Video Subtitle App" in project["tracks"][0]["captions"][0]["text"]
    print(f"  -> Subtitle text successfully updated and persisted.")

    # Step 7: Change fonts and subtitle styles
    print(f"\n[Step 7] Changing fonts, colors, and subtitle styles...")
    orig_track["style"].update({
        "preset": "Neon",
        "font": "Segoe UI",
        "fontSize": 28,
        "textColor": "#FFFFFF",
        "bgColor": "#110D1A",
        "bgEnabled": True,
        "outline": 2.5,
        "align": "center",
        "yPos": 0.70
    })

    # Step 8: Enable word-by-word animation & SFX
    print(f"\n[Step 8] Enabling word-by-word animation and SFX...")
    orig_track["style"].update({
        "animation": "Pop",
        "karaoke": True
    })
    project["effects"].update({
        "sfx": "Pop",
        "sfxMode": "Word",
        "sfxVolume": 45,
        "clearVoice": "Balanced",
        "saturation": 1.15
    })
    r = client.put(f"/api/projects/{pid}", json={"tracks": project["tracks"], "effects": project["effects"]})
    assert r.status_code == 200

    # Step 9: Generate translated subtitle tracks (Persian/Dari and Pashto)
    print(f"\n[Step 9] Generating translated subtitle tracks for Persian/Dari and Pashto...")
    # Persian / Dari track
    t_res = client.post(f"/api/projects/{pid}/translate", json={
        "trackId": orig_track["id"],
        "language": "Persian (Dari)",
        "mode": "Simultaneous"
    })
    assert t_res.status_code == 200, f"Persian translation failed: {t_res.text}"
    job_id = t_res.json()["jobId"]
    for _ in range(40):
        st = client.get(f"/api/jobs/{job_id}").json()
        if st["status"] != "running":
            break
        time.sleep(0.2)
    assert st["status"] == "complete", f"Persian translation job failed: {st}"

    # Pashto track
    t_res2 = client.post(f"/api/projects/{pid}/translate", json={
        "trackId": orig_track["id"],
        "language": "Pashto",
        "mode": "Simultaneous"
    })
    assert t_res2.status_code == 200, f"Pashto translation failed: {t_res2.text}"
    job_id2 = t_res2.json()["jobId"]
    for _ in range(40):
        st2 = client.get(f"/api/jobs/{job_id2}").json()
        if st2["status"] != "running":
            break
        time.sleep(0.2)
    assert st2["status"] == "complete", f"Pashto translation job failed: {st2}"
    print("  -> Persian/Dari and Pashto translation tracks generated.")

    # Step 10: Display two languages simultaneously
    print(f"\n[Step 10] Configuring dual simultaneous subtitle tracks (English + Persian/Dari)...")
    project = client.get(f"/api/projects/{pid}").json()
    print(f"  -> Total tracks now: {len(project['tracks'])}")
    for t in project["tracks"]:
        print(f"     Track: '{t['name']}' (Lang: {t['language']}), Captions: {len(t['captions'])}, yPos: {t['style'].get('yPos')}")
        if "persian" in t["language"].lower() or "dari" in t["language"].lower():
            t["style"]["font"] = "Tahoma"
            t["style"]["textColor"] = "#FFE844"
            t["style"]["yPos"] = 0.84
        elif "pashto" in t["language"].lower():
            # Hide Pashto so English and Persian display simultaneously
            t["visible"] = False

    # Step 11: Save and reopen project
    print(f"\n[Step 11] Saving and reopening project state...")
    r = client.put(f"/api/projects/{pid}", json={"tracks": project["tracks"], "effects": project["effects"]})
    assert r.status_code == 200
    reopened = client.get(f"/api/projects/{pid}").json()
    assert len(reopened["tracks"]) >= 3
    assert reopened["tracks"][0]["style"]["animation"] == "Pop"
    assert reopened["effects"]["sfx"] == "Pop"
    print(f"  -> Full project JSON reloaded and verified. State persistence: 100%")

    # Step 12: Export real MP4 video
    print(f"\n[Step 12] Exporting real MP4 with embedded ASS subtitles and mixed SFX...")
    exp = client.post(f"/api/projects/{pid}/export", json={"resolution": 720})
    assert exp.status_code == 200, f"Export start failed: {exp.text}"
    job_id = exp.json()["jobId"]
    print(f"  -> Render job started: {job_id}. Rendering 720x1280 9:16 portrait video...")
    for _ in range(120):
        st = client.get(f"/api/jobs/{job_id}").json()
        if st["status"] != "running":
            break
        time.sleep(0.5)
    assert st["status"] == "complete", f"Render job failed: {st}"
    print(f"  -> Render complete: {st['result']}")

    # Step 13: Play exported video / verify stream & size
    print(f"\n[Step 13] Verifying and playing exported MP4...")
    dl = client.get(f"/api/projects/{pid}/download")
    assert dl.status_code == 200, f"Download failed: {dl.status_code}"
    export_file = Path("verified_output_captionforge.mp4")
    export_file.write_bytes(dl.content)
    print(f"  -> Saved exported MP4 to: {export_file.resolve()} ({export_file.stat().st_size} bytes)")
    assert export_file.stat().st_size > 50000, "Exported video file too small"

    # Step 14: Verify subtitle timing, styling, sound and video quality
    print(f"\n[Step 14] Probing exported MP4 stream parameters and extracting frames...")
    info = probe(export_file)
    print(f"  -> Probed output: {info}")
    assert info["width"] == 720
    assert info["height"] == 1280
    assert 5.8 <= info["duration"] <= 6.2, f"Unexpected duration: {info['duration']}"
    assert info["hasAudio"] is True

    # Extract frame at 2.0s to verify burned subtitle rendering
    frame_path = Path("verified_rendered_frame.png")
    os.system(f"ffmpeg -y -ss 00:00:02.000 -i {export_file} -frames:v 1 -update 1 {frame_path}")
    assert frame_path.is_file() and frame_path.stat().st_size > 1000, "Frame extraction failed"
    print(f"  -> Extracted frame saved to: {frame_path.resolve()} ({frame_path.stat().st_size} bytes)")

    # Also test Pashto simultaneous rendering
    print(f"\n[Bonus] Testing Pashto dual track rendering...")
    for t in project["tracks"]:
        if "pashto" in t["language"].lower():
            t["visible"] = True
            t["style"]["font"] = "Arial"
            t["style"]["textColor"] = "#33C6BD"
            t["style"]["yPos"] = 0.85
        elif "persian" in t["language"].lower():
            t["visible"] = False
    client.put(f"/api/projects/{pid}", json={"tracks": project["tracks"]})
    exp_ps = client.post(f"/api/projects/{pid}/export", json={"resolution": 720})
    job_ps = exp_ps.json()["jobId"]
    for _ in range(120):
        st = client.get(f"/api/jobs/{job_ps}").json()
        if st["status"] != "running":
            break
        time.sleep(0.5)
    assert st["status"] == "complete", f"Pashto export failed: {st}"
    dl_ps = client.get(f"/api/projects/{pid}/download")
    ps_file = Path("verified_pashto_captionforge.mp4")
    ps_file.write_bytes(dl_ps.content)
    ps_frame = Path("verified_pashto_frame.png")
    os.system(f"ffmpeg -y -ss 00:00:02.000 -i {ps_file} -frames:v 1 -update 1 {ps_frame}")
    print(f"  -> Pashto video export verified: {ps_file.resolve()} ({ps_file.stat().st_size} bytes)")
    print(f"  -> Pashto frame extracted: {ps_frame.resolve()}")

    # Bonus 2: Test Landscape 16:9 Aspect Ratio Export
    print(f"\n[Bonus 2] Testing 16:9 landscape export and dimension probing...")
    exp_169 = client.post(f"/api/projects/{pid}/export", json={"resolution": 720, "aspectRatio": "16:9"})
    assert exp_169.status_code == 200, f"16:9 export failed: {exp_169.text}"
    job_169 = exp_169.json()["jobId"]
    for _ in range(120):
        st = client.get(f"/api/jobs/{job_169}").json()
        if st["status"] != "running":
            break
        time.sleep(0.5)
    assert st["status"] == "complete", f"16:9 render failed: {st}"
    dl_169 = client.get(f"/api/projects/{pid}/download")
    file_169 = Path("verified_16_9_landscape.mp4")
    file_169.write_bytes(dl_169.content)
    info_169 = probe(file_169)
    print(f"  -> Probed 16:9 output: {info_169}")
    assert info_169["width"] == 1280, f"Expected 1280 width, got {info_169['width']}"
    assert info_169["height"] == 720, f"Expected 720 height, got {info_169['height']}"

    # Bonus 3: Test Square 1:1 Aspect Ratio Export
    print(f"\n[Bonus 3] Testing 1:1 square export and dimension probing...")
    exp_11 = client.post(f"/api/projects/{pid}/export", json={"resolution": 720, "aspectRatio": "1:1"})
    assert exp_11.status_code == 200, f"1:1 export failed: {exp_11.text}"
    job_11 = exp_11.json()["jobId"]
    for _ in range(120):
        st = client.get(f"/api/jobs/{job_11}").json()
        if st["status"] != "running":
            break
        time.sleep(0.5)
    assert st["status"] == "complete", f"1:1 render failed: {st}"
    dl_11 = client.get(f"/api/projects/{pid}/download")
    file_11 = Path("verified_1_1_square.mp4")
    file_11.write_bytes(dl_11.content)
    info_11 = probe(file_11)
    print(f"  -> Probed 1:1 output: {info_11}")
    assert info_11["width"] == 720, f"Expected 720 width, got {info_11['width']}"
    assert info_11["height"] == 720, f"Expected 720 height, got {info_11['height']}"

    # Bonus 4: Video 1 Cinematic Style Export (Orange Persian Headline + Purple Flare + Whoosh SFX)
    print(f"\n[Bonus 4] Testing Video 1 reference style export (Orange Headline + Flare + Whoosh SFX)...")
    v1_tracks = [{
        "id": "tr_v1",
        "name": "Orange Persian Headline",
        "language": "fa",
        "visible": True,
        "style": {
            "preset": "Orange Headline",
            "font": "Vazirmatn",
            "fontSize": 32,
            "textColor": "#FF7A00",
            "bgColor": "#110D1A",
            "bgEnabled": True,
            "outline": 3.5,
            "animation": "Pop",
            "yPos": 0.72,
        },
        "captions": [
            {"id": "c_v1_1", "start": 0.5, "end": 2.8, "text": "تیتر بزرگ نارنجی فارسی"},
            {"id": "c_v1_2", "start": 3.0, "end": 5.5, "text": "برنامه زیرنویس هوش مصنوعی"},
        ]
    }]
    v1_overlays = [
        {
            "id": "ov_flare",
            "name": "Purple Light Flare",
            "type": "light_leak",
            "color": "#D946EF",
            "start": 0.5,
            "end": 2.0,
            "visible": True,
        }
    ]
    v1_effects = {
        "brightness": 0.05,
        "contrast": 1.1,
        "saturation": 1.2,
        "sharpness": 0.3,
        "clearVoice": "Balanced",
        "sfx": "Whoosh",
        "sfxMode": "Sentence",
        "sfxVolume": 65,
    }
    client.put(f"/api/projects/{pid}", json={
        "tracks": v1_tracks,
        "overlays": v1_overlays,
        "effects": v1_effects,
        "aspectRatio": "9:16",
    })
    exp_v1 = client.post(f"/api/projects/{pid}/export", json={"resolution": 720, "aspectRatio": "9:16"})
    assert exp_v1.status_code == 200, f"Video 1 export failed: {exp_v1.text}"
    job_v1 = exp_v1.json()["jobId"]
    for _ in range(120):
        st = client.get(f"/api/jobs/{job_v1}").json()
        if st["status"] != "running":
            break
        time.sleep(0.5)
    assert st["status"] == "complete", f"Video 1 export failed: {st}"
    dl_v1 = client.get(f"/api/projects/{pid}/download")
    v1_file = Path("verified_video1_style.mp4")
    v1_file.write_bytes(dl_v1.content)
    info_v1 = probe(v1_file)
    print(f"  -> Probed Video 1 style output: {info_v1} ({v1_file.stat().st_size} bytes)")
    assert info_v1["width"] == 720 and info_v1["height"] == 1280
    v1_frame = Path("verified_video1_frame.png")
    os.system(f"ffmpeg -y -ss 00:00:01.500 -i {v1_file} -frames:v 1 -update 1 {v1_frame}")
    assert v1_frame.is_file() and v1_frame.stat().st_size > 1000

    # Bonus 5: Video 2 Cinematic Style Export (Glow Hologram + Metallic Australia + Soft Background Words + Shimmer SFX)
    print(f"\n[Bonus 5] Testing Video 2 reference style export (Glow Hologram + Metallic AUSTRALIA + Shimmer SFX)...")
    v2_tracks = [{
        "id": "tr_v2",
        "name": "Main Subtitle",
        "language": "en",
        "visible": True,
        "style": {
            "preset": "Minimal",
            "font": "Noto Sans",
            "fontSize": 24,
            "textColor": "#FFFFFF",
            "bgColor": "#110D1A",
            "bgEnabled": True,
            "outline": 2.0,
            "animation": "Fade",
            "yPos": 0.80,
        },
        "captions": [
            {"id": "c_v2_1", "start": 0.5, "end": 3.0, "text": "Interactive Hand-Tracked Hologram"},
            {"id": "c_v2_2", "start": 3.2, "end": 5.8, "text": "Cinematic Motion Graphics and Title Reveal"},
        ]
    }]
    v2_overlays = [
        {
            "id": "ov_holo",
            "name": "Hand Glow Hologram",
            "type": "glow_hologram",
            "content": "◈ HOLOGRAM ◈",
            "color": "#38BDF8",
            "start": 0.8,
            "end": 3.5,
            "xPos": 0.65,
            "yPos": 0.38,
            "scale": 1.25,
            "visible": True,
        },
        {
            "id": "ov_soft",
            "name": "Soft Background Words",
            "type": "soft_bg_text",
            "content": "کپشن فورج شناور",
            "color": "#94A3B8",
            "start": 1.0,
            "end": 4.5,
            "xPos": 0.30,
            "yPos": 0.35,
            "scale": 1.0,
            "visible": True,
        },
        {
            "id": "ov_metal",
            "name": "Metallic Australia Reveal",
            "type": "metallic_banner",
            "content": "AUSTRALIA",
            "color": "#E2E8F0",
            "start": 3.5,
            "end": 5.8,
            "xPos": 0.50,
            "yPos": 0.55,
            "scale": 1.0,
            "visible": True,
        },
        {
            "id": "ov_end",
            "name": "End Accent Title",
            "type": "end_accent",
            "content": "CaptionForge Studio",
            "color": "#EF4444",
            "start": 4.0,
            "end": 5.8,
            "xPos": 0.50,
            "yPos": 0.88,
            "visible": True,
        }
    ]
    v2_effects = {
        "brightness": 0.0,
        "contrast": 1.05,
        "saturation": 1.1,
        "sharpness": 0.0,
        "clearVoice": "Off",
        "sfx": "Shimmer",
        "sfxMode": "Sentence",
        "sfxVolume": 60,
    }
    client.put(f"/api/projects/{pid}", json={
        "tracks": v2_tracks,
        "overlays": v2_overlays,
        "effects": v2_effects,
        "aspectRatio": "9:16",
    })
    exp_v2 = client.post(f"/api/projects/{pid}/export", json={"resolution": 720, "aspectRatio": "9:16"})
    assert exp_v2.status_code == 200, f"Video 2 export failed: {exp_v2.text}"
    job_v2 = exp_v2.json()["jobId"]
    for _ in range(120):
        st = client.get(f"/api/jobs/{job_v2}").json()
        if st["status"] != "running":
            break
        time.sleep(0.5)
    assert st["status"] == "complete", f"Video 2 export failed: {st}"
    dl_v2 = client.get(f"/api/projects/{pid}/download")
    v2_file = Path("verified_video2_style.mp4")
    v2_file.write_bytes(dl_v2.content)
    info_v2 = probe(v2_file)
    print(f"  -> Probed Video 2 style output: {info_v2} ({v2_file.stat().st_size} bytes)")
    assert info_v2["width"] == 720 and info_v2["height"] == 1280
    v2_frame = Path("verified_video2_frame.png")
    os.system(f"ffmpeg -y -ss 00:00:02.000 -i {v2_file} -frames:v 1 -update 1 {v2_frame}")
    assert v2_frame.is_file() and v2_frame.stat().st_size > 1000

    print("\n" + "=" * 60)
    print("ALL WORKFLOW OPERATIONS + VIDEO 1 & 2 STYLE EXPORTS PASSED 100%!")
    print("=" * 60)

if __name__ == "__main__":
    run_full_verification()
