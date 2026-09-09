"""Tests for ClinicalFeatureVector and schema validation"""

import pytest
from pydantic import ValidationError

from data.schema import ClinicalFeatureVector, get_schema_summary
from config import FEATURE_NAMES


def test_valid_clinical_feature_vector():
    """Verifies that a valid clinical vector creates properly and exports ordered features."""
    valid_data = {
        "age": 45.0,
        "gender": 1.0,
        "bmi": 24.5,
        "systolicBP": 120.0,
        "diastolicBP": 80.0,
        "heartRate": 72.0,
        "oxygenSaturation": 98.0,
        "glucose": 98.0,
        "cholesterol": 185.0,
        "creatinine": 0.9,
        "hemoglobin": 14.2,
    }
    vector = ClinicalFeatureVector(**valid_data)
    feature_list = vector.to_feature_list()

    assert len(feature_list) == 11
    assert feature_list[0] == 45.0  # age
    assert feature_list[1] == 1.0   # gender
    assert feature_list[3] == 120.0 # systolicBP
    assert feature_list[4] == 80.0  # diastolicBP


def test_diastolic_exceeding_systolic_rejected():
    """Verifies that invalid blood pressure ratios (diastolic > systolic) raise ValidationError."""
    invalid_data = {
        "age": 45.0,
        "gender": 1.0,
        "bmi": 24.5,
        "systolicBP": 80.0,
        "diastolicBP": 120.0,  # Invalid: diastolic exceeds systolic
        "heartRate": 72.0,
        "oxygenSaturation": 98.0,
        "glucose": 98.0,
        "cholesterol": 185.0,
        "creatinine": 0.9,
        "hemoglobin": 14.2,
    }
    with pytest.raises(ValidationError) as exc_info:
        ClinicalFeatureVector(**invalid_data)
    assert "diastolicBP (120.0) cannot exceed systolicBP (80.0)" in str(exc_info.value)


def test_out_of_bounds_features_rejected():
    """Verifies that physiologically impossible values are caught by schema validation."""
    # Negative age
    with pytest.raises(ValidationError):
        ClinicalFeatureVector(
            age=-5.0, gender=1.0, bmi=24.5, systolicBP=120.0, diastolicBP=80.0,
            heartRate=72.0, oxygenSaturation=98.0, glucose=98.0, cholesterol=185.0,
            creatinine=0.9, hemoglobin=14.2
        )

    # Impossible SpO2 (> 100%)
    with pytest.raises(ValidationError):
        ClinicalFeatureVector(
            age=45.0, gender=1.0, bmi=24.5, systolicBP=120.0, diastolicBP=80.0,
            heartRate=72.0, oxygenSaturation=105.0, glucose=98.0, cholesterol=185.0,
            creatinine=0.9, hemoglobin=14.2
        )


def test_schema_summary_structure():
    """Verifies that get_schema_summary contains all 11 features and expected metadata."""
    summary = get_schema_summary()
    assert summary["totalFeatures"] == 11
    assert summary["featureOrder"] == FEATURE_NAMES
    for feature in FEATURE_NAMES:
        assert feature in summary["features"]
        meta = summary["features"][feature]
        assert "type" in meta
        assert "unit" in meta
        assert "range" in meta
        assert "source" in meta
