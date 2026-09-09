@echo off
REM ============================================================
REM MediSphere ML Service — Local Virtual Environment Setup
REM ============================================================
REM Creates an isolated virtualenv using the host's Python.
REM Does NOT install packages into global Python environment.

setlocal enabledelayedexpansion

echo [INFO] Setting up isolated Python virtual environment for MediSphere ML Service...

REM Check for python
where python >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Python was not found on PATH. Please install Python 3.10+ or use Docker.
    exit /b 1
)

REM Navigate to ml-service root
cd /d "%~dp0\.."

REM Create .venv if it does not exist
if not exist ".venv" (
    echo [INFO] Creating .venv in %cd%\.venv...
    python -m venv .venv
    if %ERRORLEVEL% NEQ 0 (
        echo [ERROR] Failed to create virtual environment.
        exit /b 1
    )
)

echo [INFO] Activating virtual environment...
call .venv\Scripts\activate.bat

echo [INFO] Upgrading pip...
python -m pip install --upgrade pip

echo [INFO] Installing requirements from requirements-dev.txt...
pip install -r requirements-dev.txt

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Virtual environment setup completed.
    echo [INFO] To activate in the future, run: ml-service\.venv\Scripts\activate.bat
    echo [INFO] To run tests, run: pytest
    echo [INFO] To start the service, run: uvicorn app:app --reload --port 8000
) else (
    echo [ERROR] Package installation encountered an error.
    exit /b %ERRORLEVEL%
)
