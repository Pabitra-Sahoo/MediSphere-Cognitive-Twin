"""MediSphere ML Service Configuration

Central configuration module ensuring deterministic execution, reproducible seeds,
and standard path resolutions across all data and model modules.
"""

import os
from pathlib import Path

# Base directories
BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"
REFERENCE_DATA_DIR = DATA_DIR / "reference"
MODELS_DIR = BASE_DIR / "models"
REGISTRY_DIR = BASE_DIR / "registry"

# Deterministic random seed strictly enforced across all components
RANDOM_SEED: int = int(os.getenv("RANDOM_SEED", "42"))

# Service networking
SERVICE_HOST: str = os.getenv("HOST", "0.0.0.0")
SERVICE_PORT: int = int(os.getenv("PORT", "8000"))
DEBUG: bool = os.getenv("DEBUG", "false").lower() == "true"

# Standard 11-feature clinical schema matching MediSphere HealthTwin
FEATURE_NAMES = [
    "age",
    "gender",
    "bmi",
    "systolicBP",
    "diastolicBP",
    "heartRate",
    "oxygenSaturation",
    "glucose",
    "cholesterol",
    "creatinine",
    "hemoglobin",
]

# Partitioning ratios
TRAIN_RATIO = 0.70
VAL_RATIO = 0.15
TEST_RATIO = 0.15

# Federated client split ratios (of training cohort)
CLIENT_SPLIT_RATIOS = [0.40, 0.35, 0.25]
CLIENT_IDS = ["hospital_alpha", "clinic_beta", "regional_gamma"]
