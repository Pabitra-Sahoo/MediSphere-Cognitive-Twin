"""MediSphere Clinical Feature Preprocessor

Handles deterministic validation, imputation, and standard scaling of clinical feature vectors.
Prevents data leakage by computing statistics strictly on the designated training split.
"""

from typing import Dict, Any, List, Optional, Tuple
import numpy as np
import pandas as pd

from config import FEATURE_NAMES


class ClinicalPreprocessor:
    """Deterministic preprocessor for tabular HealthTwin clinical feature vectors."""

    def __init__(self, feature_names: Optional[List[str]] = None):
        self.feature_names: List[str] = list(feature_names) if feature_names is not None else list(FEATURE_NAMES)
        self.means_: Dict[str, float] = {}
        self.scales_: Dict[str, float] = {}
        self.medians_: Dict[str, float] = {}
        self.is_fitted: bool = False

    def fit(self, df: pd.DataFrame) -> "ClinicalPreprocessor":
        """Fits preprocessor by computing medians, means, and std deviations strictly on training data."""
        self._validate_columns(df)

        for col in self.feature_names:
            series = pd.to_numeric(df[col], errors="coerce")
            median_val = float(series.median())
            self.medians_[col] = median_val

            # Fill missing with median before calculating mean/std
            filled = series.fillna(median_val)
            mean_val = float(filled.mean())
            std_val = float(filled.std(ddof=0))
            # Guard against zero variance
            if std_val < 1e-7 or np.isnan(std_val):
                std_val = 1.0

            self.means_[col] = mean_val
            self.scales_[col] = std_val

        self.is_fitted = True
        return self

    def transform(self, df: pd.DataFrame) -> np.ndarray:
        """Transforms a DataFrame of clinical features into a scaled NumPy array."""
        if not self.is_fitted:
            raise RuntimeError("ClinicalPreprocessor must be fitted before calling transform().")

        self._validate_columns(df)
        transformed_cols = []

        for col in self.feature_names:
            series = pd.to_numeric(df[col], errors="coerce")
            filled = series.fillna(self.medians_[col])
            scaled = (filled - self.means_[col]) / self.scales_[col]
            transformed_cols.append(scaled.to_numpy(dtype=np.float32))

        return np.column_stack(transformed_cols)

    def fit_transform(self, df: pd.DataFrame) -> np.ndarray:
        """Convenience method to fit and transform in a single call."""
        return self.fit(df).transform(df)

    def transform_single_vector(self, vector_dict: Dict[str, Any]) -> np.ndarray:
        """Transforms a single patient's feature dictionary into a scaled 2D array of shape (1, 11)."""
        df = pd.DataFrame([vector_dict])
        return self.transform(df)

    def get_state(self) -> Dict[str, Any]:
        """Returns preprocessor parameters for serialization and audit inspection."""
        return {
            "feature_names": self.feature_names,
            "means": self.means_,
            "scales": self.scales_,
            "medians": self.medians_,
            "is_fitted": self.is_fitted,
        }

    def _validate_columns(self, df: pd.DataFrame) -> None:
        """Verifies that all required clinical features exist in the DataFrame."""
        missing = [col for col in self.feature_names if col not in df.columns]
        if missing:
            raise ValueError(f"Missing required clinical features in input DataFrame: {missing}")
