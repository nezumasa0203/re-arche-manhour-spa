#!/bin/bash
# ============================================
# Codespace post-create setup
# ============================================
set -e

echo "[setup] Installing Claude Code..."
npm install -g @anthropic-ai/claude-code

echo "[setup] Installing frontend dependencies..."
cd frontend && npm install && cd ..

echo "[setup] Setting up Gradle wrapper..."
cd backend && chmod +x gradlew 2>/dev/null || true && cd ..

echo "[setup] Ready. Run 'claude' to start Claude Code."
