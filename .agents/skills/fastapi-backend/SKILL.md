---
name: fastapi-backend
description: Use this skill when building, running, extending, or testing the FastAPI backend, REST endpoints, background rendering jobs, and media management for CaptionForge.
---

# FastAPI Backend Guide for CaptionForge

This guide defines the standards, modular organization, and testing protocols for the CaptionForge Python backend.

## Architectural Structure

```text
captionforge/
├── app.py                      # FastAPI app initialization, routes, and middleware
├── services/
│   ├── transcription.py        # Faster-Whisper ASR and word alignment service
│   ├── translation.py          # Multilingual translation service (OpenAI / local)
│   ├── render.py               # ASS generation and FFmpeg export service
│   ├── audio.py                # Audio extraction, SFX mixing, Clear Voice denoising
│   ├── face_tracker.py         # OpenCV / MediaPipe face tracking service
│   └── project_store.py        # Project persistence (JSON/SQLite/Postgres)
├── models/
│   ├── schemas.py              # Pydantic models for requests and responses
│   └── domain.py               # Internal domain entities (Project, Track, Cue)
├── data/                       # Local media cache and project stores
└── tests/
    ├── test_smoke.py           # End-to-end API and render smoke tests
    ├── test_transcription.py   # ASR unit tests
    └── test_export.py          # Video export and filter tests
```

## API Design Standards

1. **Structured Schemas**: Use Pydantic V2 models for all request bodies and responses.
2. **Background Tasks**:
   - Long-running jobs (AI transcription, 4K rendering) must return an immediate HTTP 202 with a `job_id`.
   - Provide `/api/jobs/{job_id}` polling endpoint for progress percentage (`0-100%`), status (`queued`, `processing`, `completed`, `failed`), and download URL.
3. **Graceful Error Handling**:
   - Return clean JSON error payloads: `{"detail": "Descriptive error message", "error_code": "RENDER_FAILED"}`.
   - Use `HTTPException` with appropriate status codes (`400`, `404`, `422`, `500`).
4. **File Safety & Sanitization**:
   - Validate file MIME types and size boundaries.
   - Sanitize filenames to prevent path traversal (`os.path.basename` / `secure_filename`).
   - Clean up temporary scratch files after export finishes or fails.

## Essential Commands

```bash
# Activate virtual environment
.venv\Scripts\activate   # Windows
source .venv/bin/activate # Linux/macOS

# Install dependencies
pip install -r requirements.txt
pip install -r requirements-ai.txt

# Run development server with reload
uvicorn app:app --host 127.0.0.1 --port 8000 --reload

# Run test suite
pytest tests/
```

## Verification Checklist

- [ ] All routes return valid JSON with appropriate HTTP status codes.
- [ ] Large file uploads (up to 500 MB) stream to disk without memory exhaustion.
- [ ] Export jobs report real-time percentage progress.
- [ ] Pytest suite completes 100% green without unexpected warnings.
- [ ] Backend runs properly on Windows, Linux, and macOS.
