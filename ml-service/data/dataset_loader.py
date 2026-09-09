"""MediSphere Authentic Clinical Dataset Loader

Loads and validates authentic public clinical benchmark datasets for Phase 11:
1. Framingham Heart Study Teaching / Public Benchmark Dataset -> Cardiovascular Risk (8 native features)
2. UCI Diabetes 130-US Hospitals (1999-2008) -> Diabetes Secondary Complications (11 encounter features)

Enforces strict provenance rules:
- Zero fabricated features injected into native feature vectors.
- Target labels extracted strictly from empirical source outcomes.
- Raw datasets must exist in data/raw/ or FileNotFoundError is raised.
"""

from typing import Dict, Any, List, Optional
from pathlib import Path
import numpy as np
import pandas as pd

from config import (
    RAW_DATA_DIR,
    SYNTHETIC_FIXTURES_DIR,
    CARDIOVASCULAR_FEATURES,
    CARDIOVASCULAR_TARGET,
    DIABETES_FEATURES,
    DIABETES_TARGET,
    FEATURE_NAMES,
)


class DatasetLoader:
    """Provides validated loading and metadata inspection for authentic clinical datasets."""

    # Complication ICD-9 diagnosis prefixes (Secondary diabetic microvascular/macrovascular damage)
    COMPLICATION_ICD9_PREFIXES = (
        "250.4",  # Diabetic nephropathy / renal manifestations
        "585",    # Chronic kidney disease (CKD associated with diabetes)
        "250.5",  # Diabetic retinopathy / ophthalmic manifestations
        "362.0",  # Diabetic retinopathy specific codes
        "250.6",  # Diabetic neuropathy / neurological manifestations
        "357.2",  # Neuropathy in diabetes
        "250.7",  # Diabetic peripheral circulatory disorders / PVD
        "443.81", # Peripheral angiopathy in diabetes
        "443.9",  # Peripheral vascular disease, unspecified
        "443",    # Other peripheral vascular disease (category code in truncated UCI data)
    )

    @classmethod
    def load_cardiovascular_dataset(cls, data_dir: Optional[Path] = None) -> pd.DataFrame:
        """Loads the authentic Framingham Heart Study dataset.

        Exposes exactly 8 native features and the TenYearCHD target.
        Does NOT inject synthetic SpO2, creatinine, or hemoglobin measurements.
        """
        raw_dir = data_dir or RAW_DATA_DIR
        csv_path = raw_dir / "framingham.csv"

        if not csv_path.exists():
            raise FileNotFoundError(
                f"Authentic Framingham dataset not found at '{csv_path}'. "
                f"Please run 'python data/download_datasets.py' to obtain the authentic benchmark dataset."
            )

        df_raw = pd.read_csv(csv_path)

        # Verify raw columns exist
        required_raw = ["male", "age", "BMI", "sysBP", "diaBP", "heartRate", "glucose", "totChol", CARDIOVASCULAR_TARGET]
        missing_raw = [col for col in required_raw if col not in df_raw.columns]
        if missing_raw:
            raise ValueError(f"Framingham raw data is missing required source columns: {missing_raw}")

        # Map to canonical model feature names
        df_mapped = pd.DataFrame({
            "age": pd.to_numeric(df_raw["age"], errors="coerce"),
            "gender": pd.to_numeric(df_raw["male"], errors="coerce"),
            "bmi": pd.to_numeric(df_raw["BMI"], errors="coerce"),
            "systolicBP": pd.to_numeric(df_raw["sysBP"], errors="coerce"),
            "diastolicBP": pd.to_numeric(df_raw["diaBP"], errors="coerce"),
            "heartRate": pd.to_numeric(df_raw["heartRate"], errors="coerce"),
            "glucose": pd.to_numeric(df_raw["glucose"], errors="coerce"),
            "cholesterol": pd.to_numeric(df_raw["totChol"], errors="coerce"),
            CARDIOVASCULAR_TARGET: pd.to_numeric(df_raw[CARDIOVASCULAR_TARGET], errors="coerce").fillna(0).astype(int),
        })

        # Ensure no fake measurements were added
        forbidden_fields = ["oxygenSaturation", "creatinine", "hemoglobin"]
        for f in forbidden_fields:
            if f in df_mapped.columns:
                raise RuntimeError(f"Forbidden fabricated field '{f}' detected in cardiovascular dataset.")

        cls._validate_contract(df_mapped, CARDIOVASCULAR_FEATURES, CARDIOVASCULAR_TARGET)
        return df_mapped

    @classmethod
    def load_diabetes_complications_dataset(cls, data_dir: Optional[Path] = None) -> pd.DataFrame:
        """Loads the authentic UCI Diabetes 130-US Hospitals dataset.

        Extracts secondary diabetic complication labels from ICD-9 diagnosis codes (diag_1, diag_2, diag_3).
        Exposes exactly 11 authentic encounter features and the has_complication target.
        Does NOT fabricate blood pressure, heart rate, or SpO2 values.
        """
        raw_dir = data_dir or RAW_DATA_DIR
        csv_path = raw_dir / "diabetic_data.csv"

        if not csv_path.exists():
            raise FileNotFoundError(
                f"Authentic UCI Diabetes dataset not found at '{csv_path}'. "
                f"Please run 'python data/download_datasets.py' to obtain the authentic benchmark dataset."
            )

        df_raw = pd.read_csv(csv_path, low_memory=False)

        # Verify diagnosis columns exist
        diag_cols = ["diag_1", "diag_2", "diag_3"]
        for col in diag_cols:
            if col not in df_raw.columns:
                raise ValueError(f"UCI Diabetes raw data is missing ICD-9 diagnosis column '{col}'")

        # Complication extraction rule
        def is_complication(val: Any) -> bool:
            s = str(val).strip()
            return s.startswith(cls.COMPLICATION_ICD9_PREFIXES)

        has_complication = (
            df_raw["diag_1"].apply(is_complication)
            | df_raw["diag_2"].apply(is_complication)
            | df_raw["diag_3"].apply(is_complication)
        ).astype(int)

        # Demographics mapping
        age_map = {
            "[0-10)": 5.0, "[10-20)": 15.0, "[20-30)": 25.0, "[30-40)": 35.0,
            "[40-50)": 45.0, "[50-60)": 55.0, "[60-70)": 65.0, "[70-80)": 75.0,
            "[80-90)": 85.0, "[90-100)": 95.0,
        }
        age = df_raw["age"].map(age_map).fillna(55.0)
        gender = df_raw["gender"].map({"Male": 1.0, "Female": 0.0})  # Unknown becomes NaN (imputed on train split)

        # Encounter diagnostic / medication metrics
        time_in_hospital = pd.to_numeric(df_raw["time_in_hospital"], errors="coerce")
        num_lab_procedures = pd.to_numeric(df_raw["num_lab_procedures"], errors="coerce")
        num_procedures = pd.to_numeric(df_raw["num_procedures"], errors="coerce")
        num_medications = pd.to_numeric(df_raw["num_medications"], errors="coerce")
        number_diagnoses = pd.to_numeric(df_raw["number_diagnoses"], errors="coerce")

        # Metabolic & medication categories
        glu_map = {"None": 0.0, "Norm": 1.0, ">200": 2.0, ">300": 3.0}
        max_glu = df_raw["max_glu_serum"].map(glu_map).fillna(0.0)

        a1c_map = {"None": 0.0, "Norm": 1.0, ">7": 2.0, ">8": 3.0}
        a1c = df_raw["A1Cresult"].map(a1c_map).fillna(0.0)

        insulin_map = {"No": 0.0, "Steady": 1.0, "Up": 2.0, "Down": 3.0}
        insulin = df_raw["insulin"].map(insulin_map).fillna(0.0)

        med_map = {"Yes": 1.0, "No": 0.0}
        diabetes_med = df_raw["diabetesMed"].map(med_map).fillna(0.0)

        df_mapped = pd.DataFrame({
            "age": age,
            "gender": gender,
            "time_in_hospital": time_in_hospital,
            "num_lab_procedures": num_lab_procedures,
            "num_procedures": num_procedures,
            "num_medications": num_medications,
            "number_diagnoses": number_diagnoses,
            "max_glu_serum": max_glu,
            "A1Cresult": a1c,
            "insulin": insulin,
            "diabetesMed": diabetes_med,
            DIABETES_TARGET: has_complication,
        })

        # Ensure no fake measurements were added
        forbidden_fields = ["systolicBP", "diastolicBP", "heartRate", "oxygenSaturation"]
        for f in forbidden_fields:
            if f in df_mapped.columns:
                raise RuntimeError(f"Forbidden fabricated field '{f}' detected in diabetes dataset.")

        cls._validate_contract(df_mapped, DIABETES_FEATURES, DIABETES_TARGET)
        return df_mapped

    @classmethod
    def load_synthetic_fixture(cls, task_type: str) -> pd.DataFrame:
        """Loads a synthetic test fixture strictly for unit and integration testing.

        WARNING: Not derived from real patient records. Never use for clinical model training.
        """
        task_upper = task_type.upper()
        if task_upper == "CARDIOVASCULAR":
            path = SYNTHETIC_FIXTURES_DIR / "cardiovascular_reference.csv"
            target_col = "target"
        elif task_upper in ["DIABETES", "DIABETES_COMPLICATIONS"]:
            path = SYNTHETIC_FIXTURES_DIR / "diabetes_complications_reference.csv"
            target_col = "has_complication"
        else:
            raise ValueError(f"Unknown fixture task type: {task_type}")

        if not path.exists():
            raise FileNotFoundError(f"Synthetic fixture not found at '{path}'")

        df = pd.read_csv(path)
        return df

    @classmethod
    def get_dataset_metadata(cls, task_type: str) -> Dict[str, Any]:
        """Returns metadata, sample counts, and feature summaries for the requested authentic dataset."""
        task_upper = task_type.upper()
        if task_upper == "CARDIOVASCULAR":
            df = cls.load_cardiovascular_dataset()
            target_col = CARDIOVASCULAR_TARGET
            features = list(CARDIOVASCULAR_FEATURES)
            name = "Framingham Heart Study Teaching / Public Benchmark Dataset"
            source = "https://raw.githubusercontent.com/matackett/sta210/master/data/framingham.csv"
        elif task_upper in ["DIABETES", "DIABETES_COMPLICATIONS"]:
            df = cls.load_diabetes_complications_dataset()
            target_col = DIABETES_TARGET
            features = list(DIABETES_FEATURES)
            name = "UCI Diabetes 130-US Hospitals (Secondary Complications Cohort)"
            source = "https://archive.ics.uci.edu/static/public/296/diabetes+130-us+hospitals+for+years+1999-2008.zip"
        else:
            raise ValueError(f"Unknown task type: {task_type}. Expected CARDIOVASCULAR or DIABETES_COMPLICATIONS.")

        pos_count = int((df[target_col] == 1).sum())
        neg_count = int((df[target_col] == 0).sum())
        total = len(df)

        return {
            "datasetName": name,
            "sourceUrl": source,
            "taskType": task_upper,
            "targetColumn": target_col,
            "totalSamples": total,
            "positiveSamples": pos_count,
            "negativeSamples": neg_count,
            "positiveRate": round(pos_count / total, 4) if total > 0 else 0.0,
            "featureCount": len(features),
            "features": features,
        }

    @staticmethod
    def _validate_contract(df: pd.DataFrame, expected_features: List[str], target_col: str) -> None:
        """Verifies that all required features and the target column exist in the DataFrame."""
        missing = [f for f in expected_features if f not in df.columns]
        if missing:
            raise ValueError(f"Dataset missing required contract features: {missing}")
        if target_col not in df.columns:
            raise ValueError(f"Dataset missing required target column: '{target_col}'")
