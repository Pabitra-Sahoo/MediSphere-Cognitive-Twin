"""Unit and Integration Tests for ML Service Runtime API

Verifies:
- Internal service-key authentication (401 on missing/invalid key)
- Default production federated model selection (CVD & Diabetes)
- Explicit benchmark model selection
- Missing clinical feature rejection (422 INSUFFICIENT_CLINICAL_DATA)
- Input feature validation and additive log-odds SHAP output
- Model catalog and global explanation endpoints
"""

import pytest
from starlette.testclient import TestClient

from app import app, DEV_DEFAULT_KEY, INTERNAL_KEY_HEADER_NAME


@pytest.fixture
def client():
    return TestClient(app)


@pytest.fixture
def valid_auth_headers():
    return {INTERNAL_KEY_HEADER_NAME: DEV_DEFAULT_KEY}


@pytest.fixture
def sample_cvd_features():
    return {
        "age": 55.0,
        "gender": 1.0,
        "bmi": 26.5,
        "systolicBP": 145.0,
        "diastolicBP": 92.0,
        "heartRate": 74.0,
        "glucose": 98.0,
        "cholesterol": 215.0,
    }


@pytest.fixture
def sample_diabetes_features():
    return {
        "age": 65.0,
        "gender": 0.0,
        "time_in_hospital": 4,
        "num_lab_procedures": 45,
        "num_procedures": 1,
        "num_medications": 14,
        "number_diagnoses": 7,
        "max_glu_serum": "norm",
        "A1Cresult": ">7",
        "insulin": "steady",
        "diabetesMed": "yes",
    }


def test_auth_missing_internal_key_returns_401(client, sample_cvd_features):
    """Verifies that direct calls without internal key are rejected with 401."""
    res = client.post(
        "/api/ml/predict-and-explain",
        json={"task_type": "CARDIOVASCULAR", "features": sample_cvd_features},
    )
    assert res.status_code == 401
    assert "internal service key" in res.json()["detail"].lower()


def test_auth_invalid_internal_key_returns_401(client, sample_cvd_features):
    """Verifies that calls with wrong internal key are rejected with 401."""
    res = client.post(
        "/api/ml/predict-and-explain",
        headers={INTERNAL_KEY_HEADER_NAME: "wrong-secret-key"},
        json={"task_type": "CARDIOVASCULAR", "features": sample_cvd_features},
    )
    assert res.status_code == 401


def test_predict_and_explain_cvd_default_federated(client, valid_auth_headers, sample_cvd_features):
    """Verifies CVD risk prediction using default global federated model."""
    res = client.post(
        "/api/ml/predict-and-explain",
        headers=valid_auth_headers,
        json={"task_type": "CARDIOVASCULAR", "features": sample_cvd_features},
    )
    assert res.status_code == 200
    data = res.json()

    assert data["task_type"] == "CARDIOVASCULAR"
    assert "federated" in data["model_name"]
    assert 0.0 <= data["model_estimated_risk_probability"] <= 1.0
    assert data["estimated_risk_tier"] in ["LOW", "MODERATE", "HIGH"]
    assert data["explanation_space"] == "log_odds"
    assert "base_value_log_odds" in data
    assert "total_log_odds" in data
    assert len(data["feature_attributions"]) == 8

    # Verify rank ordering
    ranks = [attr["rank"] for attr in data["feature_attributions"]]
    assert ranks == list(range(1, 9))
    for attr in data["feature_attributions"]:
        assert attr["direction"] in ["INCREASES_RISK", "DECREASES_RISK"]
        assert "feature_name" in attr
        assert "feature_value" in attr


def test_predict_and_explain_diabetes_default_federated(client, valid_auth_headers, sample_diabetes_features):
    """Verifies Diabetes complications prediction using default global federated model."""
    res = client.post(
        "/api/ml/predict-and-explain",
        headers=valid_auth_headers,
        json={"task_type": "DIABETES", "features": sample_diabetes_features},
    )
    assert res.status_code == 200
    data = res.json()

    assert data["task_type"] == "DIABETES"
    assert "federated" in data["model_name"]
    assert 0.0 <= data["model_estimated_risk_probability"] <= 1.0
    assert len(data["feature_attributions"]) == 11
    assert data["feature_attributions"][0]["rank"] == 1


def test_predict_and_explain_benchmark_model_override(client, valid_auth_headers, sample_cvd_features):
    """Verifies explicit benchmark model selection (e.g. HGB)."""
    res = client.post(
        "/api/ml/predict-and-explain",
        headers=valid_auth_headers,
        json={
            "task_type": "CARDIOVASCULAR",
            "model_name": "cardiovascular_hist_gradient_boosting",
            "features": sample_cvd_features,
        },
    )
    assert res.status_code == 200
    data = res.json()
    assert data["model_name"] == "cardiovascular_hist_gradient_boosting"
    assert len(data["feature_attributions"]) == 8


def test_missing_feature_rejected_with_422(client, valid_auth_headers, sample_cvd_features):
    """Verifies that missing required clinical measurements trigger 422 INSUFFICIENT_CLINICAL_DATA."""
    del sample_cvd_features["cholesterol"]
    res = client.post(
        "/api/ml/predict-and-explain",
        headers=valid_auth_headers,
        json={"task_type": "CARDIOVASCULAR", "features": sample_cvd_features},
    )
    assert res.status_code == 422
    err_detail = res.json()["detail"]
    assert err_detail["error"] == "INSUFFICIENT_CLINICAL_DATA"
    assert "cholesterol" in err_detail["missing_features"]


def test_unsupported_task_type_rejected_with_400(client, valid_auth_headers, sample_cvd_features):
    """Verifies that unknown task types are rejected with 400."""
    res = client.post(
        "/api/ml/predict-and-explain",
        headers=valid_auth_headers,
        json={"task_type": "ONCOLOGY", "features": sample_cvd_features},
    )
    assert res.status_code == 400


def test_model_not_found_returns_404(client, valid_auth_headers, sample_cvd_features):
    """Verifies that requests for unregistered models return 404."""
    res = client.post(
        "/api/ml/predict-and-explain",
        headers=valid_auth_headers,
        json={
            "task_type": "CARDIOVASCULAR",
            "model_name": "non_existent_neural_network",
            "features": sample_cvd_features,
        },
    )
    assert res.status_code == 404


def test_model_catalog_endpoint(client, valid_auth_headers):
    """Verifies that /api/ml/models lists all registered models with metadata."""
    res = client.get("/api/ml/models", headers=valid_auth_headers)
    assert res.status_code == 200
    models = res.json()
    assert len(models) >= 6
    names = [m["model_name"] for m in models]
    assert "cardiovascular_logistic_regression" in names
    assert "cardiovascular_hist_gradient_boosting" in names
    assert "cardiovascular_federated_logistic_regression" in names
    assert "diabetes_complications_federated_logistic_regression" in names
    assert "diabetes_complications_logistic_regression" in names
    assert "diabetes_complications_hist_gradient_boosting" in names


def test_global_explanation_endpoint(client, valid_auth_headers):
    """Verifies that /api/ml/models/{model_name}/global-explanation returns the precomputed artifact."""
    res = client.get(
        "/api/ml/models/cardiovascular_federated_logistic_regression/global-explanation",
        headers=valid_auth_headers,
    )
    assert res.status_code == 200
    data = res.json()
    assert data["explanation_space"] == "mean_absolute_log_odds"
    assert len(data["global_feature_importance"]) == 8
