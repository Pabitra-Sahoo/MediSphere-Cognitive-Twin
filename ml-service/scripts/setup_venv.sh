#!/usr/bin/env bash
# ============================================================
# MediSphere ML Service — Local Virtual Environment Setup
# ============================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

echo "[INFO] Setting up isolated Python virtual environment for MediSphere ML Service..."

if ! command -v python3 &>/dev/null; then
    echo "[ERROR] python3 not found on PATH."
    exit 1
fi

if [ ! -d ".venv" ]; then
    echo "[INFO] Creating .venv in $(pwd)/.venv..."
    python3 -m venv .venv
fi

echo "[INFO] Activating virtual environment..."
source .venv/bin/activate

echo "[INFO] Upgrading pip..."
pip install --upgrade pip

echo "[INFO] Installing requirements..."
pip install -r requirements-dev.txt

echo "[SUCCESS] Virtual environment setup completed."
echo "[INFO] Run 'pytest' to execute test suite."
