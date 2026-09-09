"""Tests for ClinicalPreprocessor"""

import pytest
import numpy as np
import pandas as pd

from data.preprocessor import ClinicalPreprocessor
from data.dataset_loader import DatasetLoader
from config import FEATURE_NAMES, CARDIOVASCULAR_FEATURES


def test_preprocessor_fit_transform():
    """Verifies that the preprocessor computes means/scales and normalizes features."""
    df = DatasetLoader.load_cardiovascular_dataset()
    preprocessor = ClinicalPreprocessor(feature_names=CARDIOVASCULAR_FEATURES)

    X_scaled = preprocessor.fit_transform(df[CARDIOVASCULAR_FEATURES])

    assert preprocessor.is_fitted is True
    assert X_scaled.shape == (len(df), len(CARDIOVASCULAR_FEATURES))
    # Normalized features should have approximately zero mean and unit variance
    np.testing.assert_allclose(X_scaled.mean(axis=0), 0.0, atol=1e-4)
    np.testing.assert_allclose(X_scaled.std(axis=0), 1.0, atol=1e-4)


def test_unfitted_transform_raises_error():
    """Verifies that calling transform before fit raises RuntimeError."""
    df = DatasetLoader.load_cardiovascular_dataset()
    preprocessor = ClinicalPreprocessor(feature_names=CARDIOVASCULAR_FEATURES)
    with pytest.raises(RuntimeError):
        preprocessor.transform(df[CARDIOVASCULAR_FEATURES])


def test_missing_value_imputation():
    """Verifies that missing values are imputed using the training median without error."""
    df = DatasetLoader.load_cardiovascular_dataset().copy()
    # Introduce deliberate missing values
    df.loc[0, "cholesterol"] = np.nan
    df.loc[1, "glucose"] = np.nan

    preprocessor = ClinicalPreprocessor(feature_names=CARDIOVASCULAR_FEATURES)
    X_scaled = preprocessor.fit_transform(df[CARDIOVASCULAR_FEATURES])

    assert not np.isnan(X_scaled).any(), "Scaled output must not contain NaN after median imputation."


def test_single_vector_transformation():
    """Verifies that transform_single_vector produces an array matching feature count."""
    df = DatasetLoader.load_cardiovascular_dataset()
    preprocessor = ClinicalPreprocessor(feature_names=CARDIOVASCULAR_FEATURES)
    preprocessor.fit(df[CARDIOVASCULAR_FEATURES])

    sample_dict = df[CARDIOVASCULAR_FEATURES].iloc[0].to_dict()
    scaled_vector = preprocessor.transform_single_vector(sample_dict)

    assert scaled_vector.shape == (1, len(CARDIOVASCULAR_FEATURES))
    assert not np.isnan(scaled_vector).any()


def test_missing_columns_validation():
    """Verifies that a DataFrame missing required features raises ValueError."""
    df = DatasetLoader.load_cardiovascular_dataset()
    df_missing = df.drop(columns=["systolicBP"])
    preprocessor = ClinicalPreprocessor(feature_names=CARDIOVASCULAR_FEATURES)

    with pytest.raises(ValueError) as exc_info:
        preprocessor.fit(df_missing)
    assert "systolicBP" in str(exc_info.value)
