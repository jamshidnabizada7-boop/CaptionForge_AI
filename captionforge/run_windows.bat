@echo off
setlocal
cd /d "%~dp0"
where python >nul 2>&1 || (echo Please install Python 3.11+ and add it to PATH. & pause & exit /b 1)
where ffmpeg >nul 2>&1 || (echo Please install FFmpeg and add it to PATH. & pause & exit /b 1)
if not exist .venv (python -m venv .venv || exit /b 1)
call .venv\Scripts\activate.bat
python -m pip install -r requirements.txt || (pause & exit /b 1)
echo.
echo CaptionForge is available at http://127.0.0.1:8000
start http://127.0.0.1:8000
python -m uvicorn app:app --host 0.0.0.0 --port 8000
pause
