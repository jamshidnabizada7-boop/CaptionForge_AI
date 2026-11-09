"""Live HTTP client-to-backend test connecting to http://127.0.0.1:8000."""
import os, sys, time, json
import urllib.request, urllib.parse

BASE_URL = "http://127.0.0.1:8000"

def test_live_http():
    print(f"Testing live HTTP connection to {BASE_URL}...")
    
    # 1. Health check
    with urllib.request.urlopen(f"{BASE_URL}/health") as resp:
        data = json.loads(resp.read().decode())
        assert data.get("ok") is True
        print(f"  [OK] /health -> {data}")

    # 2. Config check
    with urllib.request.urlopen(f"{BASE_URL}/api/config") as resp:
        cfg = json.loads(resp.read().decode())
        print(f"  [OK] /api/config -> {cfg}")
        assert cfg["render"] is True
        assert cfg["transcription"] == "faster-whisper"

    # 3. List projects
    with urllib.request.urlopen(f"{BASE_URL}/api/projects") as resp:
        projects = json.loads(resp.read().decode())
        print(f"  [OK] /api/projects -> {len(projects)} existing projects listed")

    print("\nLive backend HTTP connectivity verified with 0 mock responses!")

if __name__ == "__main__":
    test_live_http()
