"""MediSphere Dataset Loader

Loads and provides validated clinical reference datasets for model development and evaluation.
"""

from typing import Dict, Any, Tuple
from pathlib import Path
import pandas as pd

from config import REFERENCE_DATA_DIR, FEATURE_NAMES
from data.generate_reference_datasets import ensure_reference_datasets


class DatasetLoader:
    """Provides high-level loading and summary methods for MediSphere clinical datasets."""

    @classmethod
    def load_cardiovascular_dataset(cls) -> pd.DataFrame:
        """Loads the cardiovascular risk reference dataset (UCI Heart Disease benchmark distribution)."""
        ensure_reference_datasets()
        path = REFERENCE_DATA_DIR / "cardiovascular_reference.csv"
        df = pd.read_csv(path)
        cls._validate_dataset(df, target_col="target")
        return df

    @classmethod
    def load_diabetes_complications_dataset(cls) -> pd.DataFrame:
        """Loads the diabetes secondary complications reference dataset (Diabetes 130-US Hospitals distribution)."""
        ensure_reference_datasets()
        path = REFERENCE_DATA_DIR / "diabetes_complications_reference.csv"
        df = pd.read_csv(path)
        cls._validate_dataset(df, target_col="has_complication")
        return df

    @classmethod
    def get_dataset_metadata(cls, task_type: str) -> Dict[str, Any]:
        """Returns metadata, sample counts, and feature summaries for the requested dataset."""
        task_upper = task_type.upper()
        if task_upper == "CARDIOVASCULAR":
            df = cls.load_cardiovascular_dataset()
            target_col = "target"
            name = "UCI Heart Disease Multicenter Benchmark"
        elif task_upper in ["DIABETES", "DIABETES_COMPLICATIONS"]:
            df = cls.load_diabetes_complications_dataset()
            target_col = "has_complication"
            name = "UCI Diabetes 130-US Hospitals Secondary Complications Cohort"
        else:
            raise ValueError(f"Unknown task type: {task_type}. Expected CARDIOVASCULAR or DIABETES_COMPLICATIONS.")

        pos_count = int((df[target_col] == 1).sum())
        neg_count = int((df[target_col] == 0).sum())
        total = len(df)

        return {
            "datasetName": name,
            "taskType": task_upper,
            "targetColumn": target_col,
            "totalSamples": total,
            "positiveSamples": pos_count,
            "negativeSamples": neg_count,
            "positiveRate": round(pos_count / total, 4) if total > 0 else 0.0,
            "featureCount": len(FEATURE_NAMES),
            "features": list(FEATURE_NAMES),
        }

    @staticmethod
    def _validate_dataset(df: pd.DataFrame, target_col: str) -> None:
        """Verifies that all 11 required clinical features and the target column exist."""
        missing = [f for f in FEATURE_NAMES if f not in df.columns]
        if missing:
            raise ValueError(f"Dataset missing required clinical features: {missing}")
        if target_col not in df.columns:
            raise ValueError(f"Dataset missing required target column: '{target_col}'")
