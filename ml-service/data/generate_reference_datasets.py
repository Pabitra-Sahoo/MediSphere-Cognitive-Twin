"""Deterministic Generator for MediSphere Synthetic Pipeline Test Fixtures

WARNING:
Synthetic test fixture only. Not derived from UCI or Framingham patient records.
Do not use for clinical ML model training or baseline evaluation.

Generates small, reproducible synthetic datasets used exclusively for fast unit testing
of schema serialization and pipeline mock endpoints.
All samples are generated deterministically using RANDOM_SEED = 42.
"""

import os
from pathlib import Path
import numpy as np
import pandas as pd

from config import RANDOM_SEED, SYNTHETIC_FIXTURES_DIR, FEATURE_NAMES


def generate_cardiovascular_dataset(n_samples: int = 400) -> pd.DataFrame:
    """Generates a reproducible clinical reference cohort reflecting the UCI Heart Disease distribution.
    
    Target:
      1: High model-estimated risk / presence of significant coronary artery disease (>=50% vessel narrowing)
      0: Low model-estimated risk / absence of significant disease
    """
    rng = np.random.default_rng(RANDOM_SEED)

    # Base demographics
    age = rng.normal(loc=54.5, scale=9.0, size=n_samples).clip(29, 77).round(1)
    gender = rng.binomial(n=1, p=0.68, size=n_samples).astype(float)  # 68% male in UCI cohort
    bmi = rng.normal(loc=26.5, scale=4.2, size=n_samples).clip(18.0, 48.0).round(1)

    # Hemodynamics
    systolic_bp = rng.normal(loc=131.0, scale=17.5, size=n_samples).clip(94, 200).round(1)
    diastolic_bp = (systolic_bp * 0.65 + rng.normal(0, 5, size=n_samples)).clip(60, 115).round(1)
    heart_rate = rng.normal(loc=149.0, scale=23.0, size=n_samples).clip(71, 202).round(1)
    oxygen_sat = rng.normal(loc=97.5, scale=1.4, size=n_samples).clip(88.0, 100.0).round(1)

    # Laboratory diagnostics
    cholesterol = rng.normal(loc=246.0, scale=51.0, size=n_samples).clip(126, 564).round(1)
    fasting_glucose = rng.normal(loc=105.0, scale=35.0, size=n_samples).clip(65, 380).round(1)
    creatinine = rng.normal(loc=1.0, scale=0.3, size=n_samples).clip(0.5, 3.5).round(2)
    hemoglobin = rng.normal(loc=14.5, scale=1.5, size=n_samples).clip(9.5, 18.0).round(1)

    # Realistic clinical risk logit based on Framingham / Cleveland coefficients
    # Older age, male gender, elevated systolic BP, elevated cholesterol, lower SpO2, high glucose increase CAD probability
    logit = (
        -7.8
        + 0.055 * (age - 50)
        + 0.85 * gender
        + 0.04 * (bmi - 25)
        + 0.035 * (systolic_bp - 120)
        + 0.015 * (cholesterol - 200)
        + 0.012 * (fasting_glucose - 100)
        - 0.08 * (oxygen_sat - 98)
    )
    prob = 1.0 / (1.0 + np.exp(-logit))
    target = (rng.uniform(0, 1, size=n_samples) < prob).astype(int)

    df = pd.DataFrame({
        "age": age,
        "gender": gender,
        "bmi": bmi,
        "systolicBP": systolic_bp,
        "diastolicBP": diastolic_bp,
        "heartRate": heart_rate,
        "oxygenSaturation": oxygen_sat,
        "glucose": fasting_glucose,
        "cholesterol": cholesterol,
        "creatinine": creatinine,
        "hemoglobin": hemoglobin,
        "target": target,
    })

    return df


def generate_diabetes_complications_dataset(n_samples: int = 500) -> pd.DataFrame:
    """Generates a reproducible clinical reference cohort reflecting the Diabetes 130-US Hospitals complication distribution.
    
    Target:
      1: Diagnosed secondary diabetic complication (nephropathy, retinopathy, neuropathy, or vascular complication)
      0: Controlled diabetes without documented secondary organ complications
    """
    rng = np.random.default_rng(RANDOM_SEED + 100)

    # Diabetic cohort demographics (inpatient distribution)
    age = rng.normal(loc=62.0, scale=12.0, size=n_samples).clip(25, 89).round(1)
    gender = rng.binomial(n=1, p=0.52, size=n_samples).astype(float)
    bmi = rng.normal(loc=31.5, scale=6.5, size=n_samples).clip(19.0, 58.0).round(1)

    # Hemodynamics
    systolic_bp = rng.normal(loc=136.0, scale=19.0, size=n_samples).clip(95, 210).round(1)
    diastolic_bp = (systolic_bp * 0.62 + rng.normal(0, 6, size=n_samples)).clip(58, 120).round(1)
    heart_rate = rng.normal(loc=78.0, scale=12.0, size=n_samples).clip(50, 130).round(1)
    oxygen_sat = rng.normal(loc=96.8, scale=1.8, size=n_samples).clip(85.0, 100.0).round(1)

    # Diabetes-specific diagnostics (higher glucose and elevated creatinine in complicated cases)
    glucose = rng.normal(loc=165.0, scale=62.0, size=n_samples).clip(70, 480).round(1)
    cholesterol = rng.normal(loc=215.0, scale=45.0, size=n_samples).clip(110, 420).round(1)
    creatinine = rng.exponential(scale=0.6, size=n_samples) + 0.7  # Skewed renal marker
    creatinine = creatinine.clip(0.6, 6.5).round(2)
    hemoglobin = rng.normal(loc=12.8, scale=1.8, size=n_samples).clip(8.0, 17.5).round(1)

    # Clinical risk logit for diabetic complications (renal, retinal, neuropathic)
    # Strongly driven by chronically elevated glucose, elevated creatinine (renal damage), high systolic BP, and age
    logit = (
        -4.2
        + 0.04 * (age - 55)
        + 0.012 * (glucose - 140)
        + 1.45 * (creatinine - 1.0)  # Strong nephropathy marker
        + 0.025 * (systolic_bp - 130)
        + 0.03 * (bmi - 28)
    )
    prob = 1.0 / (1.0 + np.exp(-logit))
    has_complication = (rng.uniform(0, 1, size=n_samples) < prob).astype(int)

    df = pd.DataFrame({
        "age": age,
        "gender": gender,
        "bmi": bmi,
        "systolicBP": systolic_bp,
        "diastolicBP": diastolic_bp,
        "heartRate": heart_rate,
        "oxygenSaturation": oxygen_sat,
        "glucose": glucose,
        "cholesterol": cholesterol,
        "creatinine": creatinine,
        "hemoglobin": hemoglobin,
        "has_complication": has_complication,
    })

    return df


def ensure_synthetic_fixtures():
    """Ensures synthetic fixtures exist on disk; generates them deterministically if missing."""
    SYNTHETIC_FIXTURES_DIR.mkdir(parents=True, exist_ok=True)
    cardio_path = SYNTHETIC_FIXTURES_DIR / "cardiovascular_reference.csv"
    diabetes_path = SYNTHETIC_FIXTURES_DIR / "diabetes_complications_reference.csv"

    if not cardio_path.exists():
        df_cardio = generate_cardiovascular_dataset(n_samples=400)
        df_cardio.to_csv(cardio_path, index=False)

    if not diabetes_path.exists():
        df_diabetes = generate_diabetes_complications_dataset(n_samples=500)
        df_diabetes.to_csv(diabetes_path, index=False)


if __name__ == "__main__":
    ensure_synthetic_fixtures()
    print("[SUCCESS] Synthetic test fixtures generated deterministically.")
