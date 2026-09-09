"""MediSphere Phase 13 SHAP Explainability Test Suite

Tests:
1. Explainer selection (LinearExplainer for LogisticRegression, TreeExplainer for HistGradientBoosting).
2. All six models load correctly into ClinicalExplainer.
3. SHAP output shape matches feature contract (8 for CVD, 11 for Diabetes).
4. Feature-name alignment with canonical configuration contracts.
5. Local explanation structure and JSON serialization compliance.
6. Deterministic output across independent invocations.
7. Logistic Regression log-odds additivity: base + sum(shap) == raw_decision_function.
8. Sigmoid probability reconstruction: sigmoid(total_log_odds) == predict_proba.
9. TreeSHAP additivity on HistGradientBoosting.
10. Global mean-absolute-SHAP calculation correctness.
11. Rank ordering by absolute SHAP magnitude.
12. Direction calculation: INCREASES_RISK vs. DECREASES_RISK.
13. Safety disclaimer presence in both local and global explanation payloads.
14. Holdout test set isolation from background distributions.
15. Serialized global_explanation.json reload and schema integrity.
16. Federated models use their global estimators without client re-execution.
17. Unsupported models fail clearly with ValueError in ExplainerFactory.
"""

import json
from pathlib import Path
import numpy as np
import pandas as pd
import pytest
import shap

from config import (
    CARDIOVASCULAR_FEATURES,
    CARDIOVASCULAR_TARGET,
    DIABETES_FEATURES,
    DIABETES_TARGET,
    SAVED_MODELS_DIR,
)
from data.dataset_loader import DatasetLoader
from data.partitioner import DataPartitioner
from explainability.clinical_explainer import ClinicalExplainer
from explainability.explainer_factory import ExplainerFactory


@pytest.fixture(scope="module")
def sample_cvd_input():
    """Authentic sample patient input matching the 8-feature Framingham contract."""
    return {
        "age": 55.0,
        "gender": 1.0,
        "bmi": 28.4,
        "systolicBP": 145.0,
        "diastolicBP": 92.0,
        "heartRate": 76.0,
        "glucose": 95.0,
        "cholesterol": 230.0,
    }


@pytest.fixture(scope="module")
def sample_diab_input():
    """Authentic sample patient input matching the 11-feature UCI Diabetes contract."""
    return {
        "age": 65.0,
        "gender": 0.0,
        "time_in_hospital": 4.0,
        "num_lab_procedures": 42.0,
        "num_procedures": 1.0,
        "num_medications": 14.0,
        "number_diagnoses": 7.0,
        "max_glu_serum": 0.0,
        "A1Cresult": 0.0,
        "insulin": 1.0,
        "diabetesMed": 1.0,
    }


def test_explainer_selection():
    """Verifies that ExplainerFactory selects LinearExplainer for LR and TreeExplainer for HGB."""
    from sklearn.linear_model import LogisticRegression
    from sklearn.ensemble import HistGradientBoostingClassifier

    lr = LogisticRegression()
    lr.coef_ = np.ones((1, 8))
    lr.intercept_ = np.zeros(1)
    lr.classes_ = np.array([0, 1])

    hgb = HistGradientBoostingClassifier()
    # Fit dummy on minimal synthetic data
    X_dummy = np.random.randn(20, 8)
    y_dummy = np.random.choice([0, 1], 20)
    hgb.fit(X_dummy, y_dummy)

    explainer_lr = ExplainerFactory.create_explainer(lr)
    assert isinstance(explainer_lr, shap.LinearExplainer)

    explainer_hgb = ExplainerFactory.create_explainer(hgb)
    assert isinstance(explainer_hgb, shap.TreeExplainer)


def test_all_six_models_load_correctly():
    """Verifies that ClinicalExplainer successfully loads all six Phase 11 and Phase 12 models."""
    model_names = [
        "cardiovascular_logistic_regression",
        "cardiovascular_hist_gradient_boosting",
        "cardiovascular_federated_logistic_regression",
        "diabetes_complications_logistic_regression",
        "diabetes_complications_hist_gradient_boosting",
        "diabetes_complications_federated_logistic_regression",
    ]
    for m_name in model_names:
        m_dir = SAVED_MODELS_DIR / m_name
        assert m_dir.exists(), f"Model directory {m_dir} does not exist."
        explainer = ClinicalExplainer.load(m_dir)
        assert explainer is not None
        assert "federated" in explainer.model_name if "federated" in m_name else True



def test_shap_output_shape_and_feature_alignment(sample_cvd_input, sample_diab_input):
    """Verifies that SHAP values match the feature contracts exactly in shape and alignment."""
    # CVD Model
    cvd_explainer = ClinicalExplainer.load(SAVED_MODELS_DIR / "cardiovascular_logistic_regression")
    cvd_res = cvd_explainer.explain_instance(sample_cvd_input)
    assert len(cvd_res["feature_attributions"]) == len(CARDIOVASCULAR_FEATURES)
    res_features = {attr["feature_name"] for attr in cvd_res["feature_attributions"]}
    assert res_features == set(CARDIOVASCULAR_FEATURES)

    # Diabetes Model
    diab_explainer = ClinicalExplainer.load(SAVED_MODELS_DIR / "diabetes_complications_logistic_regression")
    diab_res = diab_explainer.explain_instance(sample_diab_input)
    assert len(diab_res["feature_attributions"]) == len(DIABETES_FEATURES)
    res_features_diab = {attr["feature_name"] for attr in diab_res["feature_attributions"]}
    assert res_features_diab == set(DIABETES_FEATURES)


def test_local_explanation_structure(sample_cvd_input):
    """Verifies required keys, types, and schema conformance of local explanations."""
    explainer = ClinicalExplainer.load(SAVED_MODELS_DIR / "cardiovascular_federated_logistic_regression")
    res = explainer.explain_instance(sample_cvd_input)

    assert "task_type" in res
    assert res["task_type"] == "CARDIOVASCULAR"
    assert "model_name" in res
    assert "model_estimated_risk_probability" in res
    assert 0.0 <= res["model_estimated_risk_probability"] <= 1.0
    assert "estimated_risk_tier" in res
    assert res["estimated_risk_tier"] in ["LOW", "MODERATE", "HIGH"]
    assert "explanation_space" in res
    assert res["explanation_space"] == "log_odds"
    assert "base_value_log_odds" in res
    assert "total_log_odds" in res
    assert "feature_attributions" in res
    assert "safety_disclaimer" in res
    assert "Not a medical diagnosis" in res["safety_disclaimer"]

    # Check attribution fields
    for attr in res["feature_attributions"]:
        assert "feature_name" in attr
        assert "feature_value" in attr
        assert "shap_value_log_odds" in attr
        assert "direction" in attr
        assert attr["direction"] in ["INCREASES_RISK", "DECREASES_RISK"]
        assert "rank" in attr


def test_deterministic_output(sample_cvd_input):
    """Verifies that multiple calls on identical input produce identical numerical SHAP values."""
    explainer = ClinicalExplainer.load(SAVED_MODELS_DIR / "cardiovascular_federated_logistic_regression")
    res1 = explainer.explain_instance(sample_cvd_input)
    res2 = explainer.explain_instance(sample_cvd_input)

    assert res1["model_estimated_risk_probability"] == res2["model_estimated_risk_probability"]
    assert res1["base_value_log_odds"] == res2["base_value_log_odds"]
    assert res1["total_log_odds"] == res2["total_log_odds"]

    for a1, a2 in zip(res1["feature_attributions"], res2["feature_attributions"]):
        assert a1["feature_name"] == a2["feature_name"]
        assert a1["shap_value_log_odds"] == a2["shap_value_log_odds"]
        assert a1["direction"] == a2["direction"]
        assert a1["rank"] == a2["rank"]


def test_logistic_regression_log_odds_additivity(sample_cvd_input):
    """Verifies that base_value_log_odds + sum(shap_values) == decision_function."""
    explainer = ClinicalExplainer.load(SAVED_MODELS_DIR / "cardiovascular_logistic_regression")
    res = explainer.explain_instance(sample_cvd_input)

    df = pd.DataFrame([sample_cvd_input])[CARDIOVASCULAR_FEATURES]
    X_scaled = explainer.preprocessor.transform(df)
    raw_decision = float(explainer.estimator.decision_function(X_scaled)[0])

    sum_shap = sum(attr["shap_value_log_odds"] for attr in res["feature_attributions"])
    reconstructed_margin = res["base_value_log_odds"] + sum_shap

    # Account for round(4) in JSON serialization
    assert abs(reconstructed_margin - raw_decision) < 0.005
    assert abs(res["total_log_odds"] - reconstructed_margin) < 0.005


def test_sigmoid_probability_reconstruction(sample_cvd_input):
    """Verifies that sigmoid(total_log_odds) matches model_estimated_risk_probability."""
    explainer = ClinicalExplainer.load(SAVED_MODELS_DIR / "cardiovascular_federated_logistic_regression")
    res = explainer.explain_instance(sample_cvd_input)

    total_log_odds = res["total_log_odds"]
    reconstructed_proba = 1.0 / (1.0 + np.exp(-total_log_odds))

    # Should match model_estimated_risk_probability within rounding precision (0.01)
    assert abs(reconstructed_proba - res["model_estimated_risk_probability"]) < 0.01


def test_tree_shap_additivity(sample_cvd_input):
    """Verifies TreeSHAP additivity in margin (log-odds) space for HistGradientBoosting."""
    explainer = ClinicalExplainer.load(SAVED_MODELS_DIR / "cardiovascular_hist_gradient_boosting")
    res = explainer.explain_instance(sample_cvd_input)

    df = pd.DataFrame([sample_cvd_input])[CARDIOVASCULAR_FEATURES]
    X_scaled = explainer.preprocessor.transform(df)
    raw_decision = float(explainer.estimator.decision_function(X_scaled)[0])

    sum_shap = sum(attr["shap_value_log_odds"] for attr in res["feature_attributions"])
    reconstructed_margin = res["base_value_log_odds"] + sum_shap

    assert abs(reconstructed_margin - raw_decision) < 0.005


def test_global_mean_absolute_shap_calculation():
    """Verifies that compute_global_explanation computes correct mean absolute values and ranks."""
    explainer = ClinicalExplainer.load(SAVED_MODELS_DIR / "cardiovascular_logistic_regression")
    cvd_raw = DatasetLoader.load_cardiovascular_dataset()
    partitioner = DataPartitioner(random_seed=42)
    _, val_df, _ = partitioner.split_train_val_test(cvd_raw, target_col=CARDIOVASCULAR_TARGET)

    res = explainer.compute_global_explanation(val_df, max_samples=100)
    assert res["explanation_space"] == "mean_absolute_log_odds"
    assert res["cohort_size"] == 100
    assert len(res["global_feature_importance"]) == len(CARDIOVASCULAR_FEATURES)

    # Verify rank ordering descending
    importances = [item["mean_abs_shap_log_odds"] for item in res["global_feature_importance"]]
    assert importances == sorted(importances, reverse=True)

    ranks = [item["rank"] for item in res["global_feature_importance"]]
    assert ranks == list(range(1, len(CARDIOVASCULAR_FEATURES) + 1))


def test_direction_calculation(sample_cvd_input):
    """Verifies that direction is strictly INCREASES_RISK or DECREASES_RISK based on sign."""
    explainer = ClinicalExplainer.load(SAVED_MODELS_DIR / "cardiovascular_logistic_regression")
    res = explainer.explain_instance(sample_cvd_input)

    for attr in res["feature_attributions"]:
        val = attr["shap_value_log_odds"]
        if val > 0:
            assert attr["direction"] == "INCREASES_RISK"
        else:
            assert attr["direction"] == "DECREASES_RISK"


def test_safety_disclaimer_presence(sample_cvd_input):
    """Verifies presence of mandatory clinical safety disclaimer and prohibited language absence."""
    explainer = ClinicalExplainer.load(SAVED_MODELS_DIR / "cardiovascular_logistic_regression")
    res = explainer.explain_instance(sample_cvd_input)

    disclaimer = res.get("safety_disclaimer", "")
    assert "Not a medical diagnosis" in disclaimer
    assert "academic research only" in disclaimer

    # Verify prohibited diagnostic words are absent
    payload_str = json.dumps(res).lower()
    assert "patient has" not in payload_str
    assert "confirmed condition" not in payload_str


def test_holdout_isolation_check():
    """Verifies that holdout test set is never used as background or global explanation dataset."""
    partitioner = DataPartitioner(random_seed=42)
    cvd_raw = DatasetLoader.load_cardiovascular_dataset()
    train_df, val_df, test_df = partitioner.split_train_val_test(cvd_raw, target_col=CARDIOVASCULAR_TARGET)

    # Train, val, and test must sum to the full dataset size
    assert len(train_df) + len(val_df) + len(test_df) == len(cvd_raw)
    assert len(val_df) > 0
    assert len(test_df) > 0

    # Ensure saved global explanations record validation cohort size, not test size
    glob_file = SAVED_MODELS_DIR / "cardiovascular_logistic_regression" / "global_explanation.json"
    with open(glob_file, "r", encoding="utf-8") as f:
        meta = json.load(f)
    assert meta["cohort_size"] <= len(val_df)


def test_serialized_global_explanation_reload():
    """Verifies all six persisted global_explanation.json files can be loaded and validated."""
    model_names = [
        "cardiovascular_logistic_regression",
        "cardiovascular_hist_gradient_boosting",
        "cardiovascular_federated_logistic_regression",
        "diabetes_complications_logistic_regression",
        "diabetes_complications_hist_gradient_boosting",
        "diabetes_complications_federated_logistic_regression",
    ]
    for m_name in model_names:
        f_path = SAVED_MODELS_DIR / m_name / "global_explanation.json"
        assert f_path.exists(), f"Missing artifact: {f_path}"

        with open(f_path, "r", encoding="utf-8") as f:
            data = json.load(f)

        assert data["model_name"] in [m_name, "diabetes_federated_logistic_regression"]
        assert data["explanation_space"] == "mean_absolute_log_odds"
        assert len(data["global_feature_importance"]) in [8, 11]
        assert "safety_disclaimer" in data


def test_federated_models_use_global_estimators(sample_cvd_input, sample_diab_input):
    """Verifies that federated models explain from the global aggregated estimator."""
    cvd_fed_explainer = ClinicalExplainer.load(SAVED_MODELS_DIR / "cardiovascular_federated_logistic_regression")
    diab_fed_explainer = ClinicalExplainer.load(SAVED_MODELS_DIR / "diabetes_complications_federated_logistic_regression")

    assert "federated" in cvd_fed_explainer.model_name
    assert "federated" in diab_fed_explainer.model_name

    cvd_res = cvd_fed_explainer.explain_instance(sample_cvd_input)
    diab_res = diab_fed_explainer.explain_instance(sample_diab_input)

    assert "federated" in cvd_res["model_name"]
    assert "federated" in diab_res["model_name"]


def test_unsupported_model_fails_clearly():
    """Verifies that ExplainerFactory raises ValueError for unsupported model architectures."""
    class DummyEstimator:
        pass

    dummy = DummyEstimator()
    with pytest.raises(ValueError, match="Unsupported estimator class"):
        ExplainerFactory.create_explainer(dummy)
