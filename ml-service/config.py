"""MediSphere ML Service Configuration

Central configuration module ensuring deterministic execution, reproducible seeds,
and standard path resolutions across all data and model modules.
"""

import os
from pathlib import Path

# Base directories
BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"
RAW_DATA_DIR = DATA_DIR / "raw"
PROCESSED_DATA_DIR = DATA_DIR / "processed"
FIXTURES_DIR = DATA_DIR / "fixtures"
SYNTHETIC_FIXTURES_DIR = FIXTURES_DIR / "synthetic_pipeline_test_data"
MODELS_DIR = BASE_DIR / "models"
SAVED_MODELS_DIR = MODELS_DIR / "saved"
REGISTRY_DIR = BASE_DIR / "registry"

# Categorical risk-tier display boundaries (strictly separated from binary decision threshold)
DEFAULT_RISK_TIERS = {
    "low_max": 0.20,        # P < 0.20 -> LOW
    "moderate_max": 0.50,   # 0.20 <= P < 0.50 -> MODERATE; P >= 0.50 -> HIGH
}

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

# Model 1: Cardiovascular Risk (Framingham authentic 8-feature native contract)
CARDIOVASCULAR_FEATURES = [
    "age",
    "gender",
    "bmi",
    "systolicBP",
    "diastolicBP",
    "heartRate",
    "glucose",
    "cholesterol",
]
CARDIOVASCULAR_TARGET = "TenYearCHD"

# Model 2: Diabetes Complications (UCI Diabetes 130-US Hospitals authentic 11-feature encounter contract)
DIABETES_FEATURES = [
    "age",
    "gender",
    "time_in_hospital",
    "num_lab_procedures",
    "num_procedures",
    "num_medications",
    "number_diagnoses",
    "max_glu_serum",
    "A1Cresult",
    "insulin",
    "diabetesMed",
]
DIABETES_TARGET = "has_complication"

# Partitioning ratios
TRAIN_RATIO = 0.70
VAL_RATIO = 0.15
TEST_RATIO = 0.15

# Federated client split ratios (of training cohort)
CLIENT_SPLIT_RATIOS = [0.40, 0.35, 0.25]
CLIENT_IDS = ["hospital_alpha", "clinic_beta", "regional_gamma"]
