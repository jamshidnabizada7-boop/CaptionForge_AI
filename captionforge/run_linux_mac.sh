#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
command -v ffmpeg >/dev/null || { echo 'Install ffmpeg first'; exit 1; }
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
echo 'Open http://127.0.0.1:8000'
uvicorn app:app --host 127.0.0.1 --port 8000
