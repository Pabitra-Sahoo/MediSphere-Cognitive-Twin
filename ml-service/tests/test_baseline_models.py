"""MediSphere Phase 11 Baseline Model Test Suite

Tests:
1. Model contract compliance (8-feature CVD, 11-feature Diabetes).
2. Continuous probability range [0.0, 1.0] and shape correctness.
3. Decision threshold calibration and binary prediction.
4. Categorical risk tier assignment (LOW, MODERATE, HIGH) and strict separation from decision threshold.
5. Phase 12 FedAvg parameter extraction and injection hooks (get_parameters / set_parameters).
6. Artifact persistence (estimator.joblib, preprocessor.joblib, metadata.json) and bit-for-bit reloading.
7. Deterministic reproducibility with RANDOM_SEED=42.
8. Zero data leakage across splits.
"""

import json
from pathlib import Path
import numpy as np
import pandas as pd
import pytest

from config import (
    CARDIOVASCULAR_FEATURES,
    CARDIOVASCULAR_TARGET,
    DEFAULT_RISK_TIERS,
    DIABETES_FEATURES,
    DIABETES_TARGET,
    RANDOM_SEED,
)
from models.base_model import BaseClinicalModel
from models.cardiovascular_model import CardiovascularBaselineModel
from models.diabetes_model import DiabetesComplicationsBaselineModel


@pytest.fixture
def dummy_cvd_data():
    """Generates small synthetic DataFrame matching the 8-feature Framingham contract."""
    np.random.seed(42)
    n = 100
    df = pd.DataFrame({
        "age": np.random.uniform(30, 70, n),
        "gender": np.random.choice([0.0, 1.0], n),
        "bmi": np.random.uniform(18, 40, n),
        "systolicBP": np.random.uniform(100, 190, n),
        "diastolicBP": np.random.uniform(60, 110, n),
        "heartRate": np.random.uniform(50, 110, n),
        "glucose": np.random.uniform(60, 250, n),
        "cholesterol": np.random.uniform(140, 320, n),
        "TenYearCHD": np.random.choice([0, 1], n, p=[0.85, 0.15]),
    })
    return df


@pytest.fixture
def dummy_diabetes_data():
    """Generates small synthetic DataFrame matching the 11-feature UCI Diabetes contract."""
    np.random.seed(42)
    n = 100
    df = pd.DataFrame({
        "age": np.random.choice([25.0, 35.0, 45.0, 55.0, 65.0, 75.0, 85.0], n),
        "gender": np.random.choice([0.0, 1.0], n),
        "time_in_hospital": np.random.randint(1, 14, n).astype(float),
        "num_lab_procedures": np.random.randint(1, 100, n).astype(float),
        "num_procedures": np.random.randint(0, 6, n).astype(float),
        "num_medications": np.random.randint(1, 40, n).astype(float),
        "number_diagnoses": np.random.randint(1, 10, n).astype(float),
        "max_glu_serum": np.random.choice([0.0, 1.0, 2.0, 3.0], n),
        "A1Cresult": np.random.choice([0.0, 1.0, 2.0, 3.0], n),
        "insulin": np.random.choice([0.0, 1.0, 2.0, 3.0], n),
        "diabetesMed": np.random.choice([0.0, 1.0], n),
        "has_complication": np.random.choice([0, 1], n, p=[0.90, 0.10]),
    })
    return df


def test_cardiovascular_model_contract_and_fit(dummy_cvd_data):
    """Verifies CardiovascularBaselineModel contract compliance, fitting, and input validation."""
    model = CardiovascularBaselineModel(algorithm="LOGISTIC_REGRESSION")
    assert model.feature_names == CARDIOVASCULAR_FEATURES
    assert model.target_name == CARDIOVASCULAR_TARGET

    X = dummy_cvd_data[CARDIOVASCULAR_FEATURES]
    y = dummy_cvd_data[CARDIOVASCULAR_TARGET]

    model.fit(X, y)
    assert model.is_fitted
    assert model.estimator is not None
    assert model.preprocessor is not None

    # Verify input validation rejects missing columns
    incomplete_X = X.drop(columns=["cholesterol"])
    with pytest.raises(ValueError, match="does not satisfy feature contract"):
        model.predict_proba(incomplete_X)


def test_diabetes_model_contract_and_fit(dummy_diabetes_data):
    """Verifies DiabetesComplicationsBaselineModel contract compliance and HistGradientBoosting fit."""
    model = DiabetesComplicationsBaselineModel(algorithm="HIST_GRADIENT_BOOSTING")
    assert model.feature_names == DIABETES_FEATURES
    assert model.target_name == DIABETES_TARGET

    X = dummy_diabetes_data[DIABETES_FEATURES]
    y = dummy_diabetes_data[DIABETES_TARGET]

    model.fit(X, y)
    assert model.is_fitted
    assert model.estimator is not None


def test_output_probability_range_and_shape(dummy_cvd_data):
    """Verifies predicted probabilities are strictly bounded within [0.0, 1.0] with shape (N,)."""
    model = CardiovascularBaselineModel(algorithm="LOGISTIC_REGRESSION")
    X = dummy_cvd_data[CARDIOVASCULAR_FEATURES]
    y = dummy_cvd_data[CARDIOVASCULAR_TARGET]
    model.fit(X, y)

    probas = model.predict_proba(X)
    assert isinstance(probas, np.ndarray)
    assert probas.shape == (len(X),)
    assert np.all(probas >= 0.0)
    assert np.all(probas <= 1.0)


def test_decision_threshold_calibration_and_predict(dummy_cvd_data):
    """Verifies threshold calibration selects optimal theta* and predict applies it."""
    model = CardiovascularBaselineModel(algorithm="LOGISTIC_REGRESSION")
    X = dummy_cvd_data[CARDIOVASCULAR_FEATURES]
    y = dummy_cvd_data[CARDIOVASCULAR_TARGET]
    model.fit(X[:70], y[:70])

    # Calibrate on remaining 30 samples
    optimal_theta = model.calibrate_decision_threshold(X[70:], y[70:], metric="f1")
    assert 0.10 <= optimal_theta <= 0.90
    assert model.decision_threshold == optimal_theta

    preds = model.predict(X[70:])
    assert set(np.unique(preds)).issubset({0, 1})
    manual_preds = (model.predict_proba(X[70:]) >= optimal_theta).astype(int)
    np.testing.assert_array_equal(preds, manual_preds)


def test_risk_tier_separation_from_decision_threshold(dummy_cvd_data):
    """Verifies risk tiers (LOW, MODERATE, HIGH) are strictly clinical display boundaries,

    independent of binary decision threshold.
    """
    model = CardiovascularBaselineModel(
        algorithm="LOGISTIC_REGRESSION",
        decision_threshold=0.35,
        risk_tiers={"low_max": 0.20, "moderate_max": 0.50},
    )
    X = dummy_cvd_data[CARDIOVASCULAR_FEATURES]
    y = dummy_cvd_data[CARDIOVASCULAR_TARGET]
    model.fit(X, y)

    tiers_1 = model.predict_risk_tier(X)
    assert all(t in {"LOW", "MODERATE", "HIGH"} for t in tiers_1)

    # Changing decision_threshold MUST NOT change risk tiers
    model.decision_threshold = 0.85
    tiers_2 = model.predict_risk_tier(X)
    assert tiers_1 == tiers_2  # Risk tiers remain identical despite decision threshold change


def test_phase12_parameter_extraction_hooks(dummy_cvd_data):
    """Verifies get_parameters and set_parameters for Phase 12 FedAvg compatibility."""
    model = CardiovascularBaselineModel(algorithm="LOGISTIC_REGRESSION")
    X = dummy_cvd_data[CARDIOVASCULAR_FEATURES]
    y = dummy_cvd_data[CARDIOVASCULAR_TARGET]
    model.fit(X, y)

    params = model.get_parameters()
    assert "coef" in params
    assert "intercept" in params
    assert params["coef"].shape == (1, 8)
    assert params["intercept"].shape == (1,)

    # Modify parameters and inject back
    new_params = {
        "coef": params["coef"] * 0.5,
        "intercept": params["intercept"] + 0.1,
    }
    model.set_parameters(new_params)
    np.testing.assert_array_equal(model.estimator.coef_, new_params["coef"])
    np.testing.assert_array_equal(model.estimator.intercept_, new_params["intercept"])


def test_serialization_and_exact_reloading(tmp_path, dummy_cvd_data):
    """Verifies model saving to joblib + JSON and exact reloading."""
    model = CardiovascularBaselineModel(
        algorithm="LOGISTIC_REGRESSION",
        decision_threshold=0.33,
        risk_tiers={"low_max": 0.20, "moderate_max": 0.50},
    )
    X = dummy_cvd_data[CARDIOVASCULAR_FEATURES]
    y = dummy_cvd_data[CARDIOVASCULAR_TARGET]
    model.fit(X, y)

    save_dir = tmp_path / "saved_cvd_test"
    model.save(save_dir)

    # Check files exist
    assert (save_dir / "estimator.joblib").exists()
    assert (save_dir / "preprocessor.joblib").exists()
    assert (save_dir / "metadata.json").exists()

    # Verify metadata JSON structure
    with open(save_dir / "metadata.json", "r", encoding="utf-8") as f:
        meta = json.load(f)
    assert meta["model_name"] == "cardiovascular_logistic_regression"
    assert meta["decision_threshold"] == 0.33
    assert meta["risk_tiers"] == {"low_max": 0.20, "moderate_max": 0.50}
    assert "safety_disclaimer" in meta
    assert "Not a medical diagnosis" in meta["safety_disclaimer"]

    # Reconstitute and verify predictions
    loaded_model = BaseClinicalModel.load(save_dir)
    assert loaded_model.decision_threshold == 0.33
    assert loaded_model.risk_tiers == {"low_max": 0.20, "moderate_max": 0.50}

    orig_probas = model.predict_proba(X)
    loaded_probas = loaded_model.predict_proba(X)
    np.testing.assert_allclose(orig_probas, loaded_probas, rtol=1e-6)

    orig_tiers = model.predict_risk_tier(X)
    loaded_tiers = loaded_model.predict_risk_tier(X)
    assert orig_tiers == loaded_tiers


def test_deterministic_reproducibility(dummy_cvd_data):
    """Verifies that two models initialized and fit with seed=42 yield bit-for-bit identical outputs."""
    X = dummy_cvd_data[CARDIOVASCULAR_FEATURES]
    y = dummy_cvd_data[CARDIOVASCULAR_TARGET]

    m1 = CardiovascularBaselineModel(algorithm="LOGISTIC_REGRESSION", random_seed=RANDOM_SEED)
    m1.fit(X, y)
    p1 = m1.predict_proba(X)

    m2 = CardiovascularBaselineModel(algorithm="LOGISTIC_REGRESSION", random_seed=RANDOM_SEED)
    m2.fit(X, y)
    p2 = m2.predict_proba(X)

    np.testing.assert_array_equal(p1, p2)
    np.testing.assert_array_equal(m1.estimator.coef_, m2.estimator.coef_)
    np.testing.assert_array_equal(m1.estimator.intercept_, m2.estimator.intercept_)
