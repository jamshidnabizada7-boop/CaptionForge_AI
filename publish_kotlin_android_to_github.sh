#!/usr/bin/env bash
# ==============================================================================
# CaptionForge — Safe GitHub Publisher for Native Kotlin Android Migration
# ==============================================================================
# This script safely initializes or updates a dedicated Git branch
# ("native-android-kotlin") and pushes the native Kotlin Android app + original
# FastAPI backend + original Flutter project without overwriting your "main" branch.
#
# Usage:
#   chmod +x publish_kotlin_android_to_github.sh
#   ./publish_kotlin_android_to_github.sh [REMOTE_REPO_URL] [BRANCH_NAME]
#
# Example (push to a separate branch on your existing repository):
#   ./publish_kotlin_android_to_github.sh https://github.com/jamshidnabizada7-boop/CaptionForge_AI.git native-android-kotlin
# ==============================================================================

set -euo pipefail

REMOTE_URL="${1:-https://github.com/jamshidnabizada7-boop/CaptionForge_AI.git}"
TARGET_BRANCH="${2:-native-android-kotlin}"

echo "========================================================"
echo " CaptionForge Native Kotlin Android — GitHub Publisher"
echo " Remote : $REMOTE_URL"
echo " Branch : $TARGET_BRANCH"
echo "========================================================"

# 1. Verify original folders are intact
if [ ! -f "captionforge/app.py" ]; then
  echo "ERROR: captionforge/app.py is missing! Aborting to protect repository."
  exit 1
fi

if [ ! -f "captionforge_mobile/pubspec.yaml" ]; then
  echo "ERROR: captionforge_mobile/pubspec.yaml is missing! Aborting to protect repository."
  exit 1
fi

if [ ! -f "app/build.gradle.kts" ]; then
  echo "ERROR: app/build.gradle.kts is missing! Run this script from the workspace root."
  exit 1
fi

# 2. Initialize git repository if exported as a ZIP from Google AI Studio
if [ ! -d ".git" ]; then
  echo "[1/5] Initializing Git repository..."
  git init
fi

# 3. Configure remote origin safely
if git remote get-url origin >/dev/null 2>&1; then
  git remote set-url origin "$REMOTE_URL"
else
  git remote add origin "$REMOTE_URL"
fi

# 4. Create/switch to the dedicated branch
echo "[2/5] Switching to branch '$TARGET_BRANCH'..."
git checkout -B "$TARGET_BRANCH"

# 5. Stage all Kotlin Android, FastAPI, and Flutter files (respecting .gitignore)
echo "[3/5] Staging files..."
git add .

# 6. Commit with a descriptive message
echo "[4/5] Creating commit..."
if git diff --cached --quiet; then
  echo "No new changes to commit."
else
  git commit -m "feat(android): add native Kotlin Jetpack Compose CaptionForge app alongside FastAPI and Flutter"
fi

COMMIT_HASH="$(git rev-parse --short HEAD)"
echo "Local Commit Hash: $COMMIT_HASH"

# 7. Push to remote branch
echo "[5/5] Pushing '$TARGET_BRANCH' to $REMOTE_URL..."
git push -u origin "$TARGET_BRANCH"

echo "========================================================"
echo " SUCCESS! Pushed commit $COMMIT_HASH to $REMOTE_URL ($TARGET_BRANCH)"
echo "========================================================"
