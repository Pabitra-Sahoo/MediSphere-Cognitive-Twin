"""Tests for Authentic Dataset Pipeline, Column Mappings, and Complication Extraction"""

import pytest
import numpy as np
import pandas as pd
from pathlib import Path

from data.dataset_loader import DatasetLoader
from data.preprocessor import ClinicalPreprocessor
from data.partitioner import DataPartitioner
from config import (
    RAW_DATA_DIR,
    SYNTHETIC_FIXTURES_DIR,
    CARDIOVASCULAR_FEATURES,
    CARDIOVASCULAR_TARGET,
    DIABETES_FEATURES,
    DIABETES_TARGET,
)


def test_cardiovascular_authentic_loading_and_mapping():
    """Verifies that Framingham dataset is loaded with exactly 8 native features and TenYearCHD."""
    df = DatasetLoader.load_cardiovascular_dataset()
    
    assert len(df) == 4240
    assert CARDIOVASCULAR_TARGET in df.columns
    assert set(CARDIOVASCULAR_FEATURES).issubset(set(df.columns))

    # Verify no fake measurements were added
    forbidden = ["oxygenSaturation", "creatinine", "hemoglobin"]
    for f in forbidden:
        assert f not in df.columns, f"Forbidden fabricated column '{f}' found in cardiovascular dataset"

    # Verify authentic class distribution
    pos_count = (df[CARDIOVASCULAR_TARGET] == 1).sum()
    neg_count = (df[CARDIOVASCULAR_TARGET] == 0).sum()
    assert pos_count == 644
    assert neg_count == 3596
    assert abs(pos_count / len(df) - 0.1519) < 0.001


def test_diabetes_authentic_loading_and_icd9_extraction():
    """Verifies that UCI Diabetes 130-US Hospitals dataset extracts complications from ICD-9 codes."""
    df = DatasetLoader.load_diabetes_complications_dataset()

    assert len(df) == 101766
    assert DIABETES_TARGET in df.columns
    assert set(DIABETES_FEATURES).issubset(set(df.columns))

    # Verify no fake measurements were added
    forbidden = ["systolicBP", "diastolicBP", "heartRate", "oxygenSaturation"]
    for f in forbidden:
        assert f not in df.columns, f"Forbidden fabricated column '{f}' found in diabetes dataset"

    # Verify authentic secondary complication distribution
    pos_count = (df[DIABETES_TARGET] == 1).sum()
    neg_count = (df[DIABETES_TARGET] == 0).sum()
    assert pos_count == 10245
    assert neg_count == 91521
    assert abs(pos_count / len(df) - 0.1007) < 0.001


def test_complication_icd9_pattern_matching_rules():
    """Unit tests the ICD-9 prefix detection for diabetic organ complications."""
    prefixes = DatasetLoader.COMPLICATION_ICD9_PREFIXES

    # Nephropathy codes
    assert any(str("250.40").startswith(p) for p in prefixes)
    assert any(str("585.9").startswith(p) for p in prefixes)

    # Retinopathy codes
    assert any(str("250.51").startswith(p) for p in prefixes)
    assert any(str("362.01").startswith(p) for p in prefixes)

    # Neuropathy codes
    assert any(str("250.60").startswith(p) for p in prefixes)
    assert any(str("357.2").startswith(p) for p in prefixes)

    # Circulatory / PVD codes
    assert any(str("250.70").startswith(p) for p in prefixes)
    assert any(str("443.81").startswith(p) for p in prefixes)

    # Uncomplicated / General diabetes codes should NOT trigger complication
    assert not any(str("250.00").startswith(p) for p in prefixes)
    assert not any(str("250.02").startswith(p) for p in prefixes)
    assert not any(str("401.9").startswith(p) for p in prefixes)  # Hypertension
    assert not any(str("414.0").startswith(p) for p in prefixes)  # CAD


def test_missing_raw_dataset_raises_clear_error(tmp_path):
    """Verifies that attempting to load from an empty raw directory raises FileNotFoundError."""
    with pytest.raises(FileNotFoundError) as exc_info:
        DatasetLoader.load_cardiovascular_dataset(data_dir=tmp_path)
    assert "download_datasets.py" in str(exc_info.value)

    with pytest.raises(FileNotFoundError) as exc_info:
        DatasetLoader.load_diabetes_complications_dataset(data_dir=tmp_path)
    assert "download_datasets.py" in str(exc_info.value)


def test_synthetic_fixtures_isolated_from_training():
    """Verifies that synthetic fixtures are loaded only via explicit fixture loader."""
    # Synthetic fixtures exist in fixtures directory
    assert (SYNTHETIC_FIXTURES_DIR / "cardiovascular_reference.csv").exists()
    assert (SYNTHETIC_FIXTURES_DIR / "diabetes_complications_reference.csv").exists()

    df_synth = DatasetLoader.load_synthetic_fixture("CARDIOVASCULAR")
    assert len(df_synth) == 400
    assert "target" in df_synth.columns

    # Verify that default load_cardiovascular_dataset returns the real 4240 records, NOT the 400 synthetic rows
    df_real = DatasetLoader.load_cardiovascular_dataset()
    assert len(df_real) == 4240
    assert len(df_real) != len(df_synth)


def test_deterministic_partitioning_on_authentic_data():
    """Verifies that DataPartitioner produces reproducible splits on Framingham authentic data."""
    df = DatasetLoader.load_cardiovascular_dataset()
    p1 = DataPartitioner(random_seed=42)
    p2 = DataPartitioner(random_seed=42)

    train1, val1, test1 = p1.split_train_val_test(df, target_col=CARDIOVASCULAR_TARGET)
    train2, val2, test2 = p2.split_train_val_test(df, target_col=CARDIOVASCULAR_TARGET)

    pd.testing.assert_frame_equal(train1, train2)
    pd.testing.assert_frame_equal(val1, val2)
    pd.testing.assert_frame_equal(test1, test2)
