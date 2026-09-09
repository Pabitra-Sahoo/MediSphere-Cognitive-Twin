"""Tests for FastAPI endpoints in app.py"""

import pytest
from fastapi.testclient import TestClient
from app import app


@pytest.fixture
def client():
    return TestClient(app)


def test_health_endpoint(client):
    """Verifies that /health returns UP, python version, and academic disclaimer."""
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert data["service"] == "medisphere-ml-service"
    assert data["phase"] == "Phase 10: Foundation & Data Pipeline"
    assert "pythonVersion" in data
    assert data["randomSeed"] == 42
    assert "timestamp" in data
    assert "academic research" in data["disclaimer"].lower()


def test_schema_endpoint(client):
    """Verifies that /api/ml/schema returns 11 features with metadata and bounds."""
    response = client.get("/api/ml/schema")
    assert response.status_code == 200
    data = response.json()
    assert data["totalFeatures"] == 11
    assert len(data["features"]) == 11
    assert "age" in data["features"]
    assert "systolicBP" in data["features"]
    assert "glucose" in data["features"]
    assert "cholesterol" in data["features"]
    assert "creatinine" in data["features"]
    assert len(data["featureOrder"]) == 11
    assert "CARDIOVASCULAR" in data["targetModels"]
    assert "DIABETES_COMPLICATIONS" in data["targetModels"]


def test_datasets_info_endpoint(client):
    """Verifies that /api/ml/datasets returns evaluated reference dataset details."""
    response = client.get("/api/ml/datasets")
    assert response.status_code == 200
    data = response.json()
    assert "cardiovascularDataset" in data
    assert "diabetesComplicationsDataset" in data

    cardio = data["cardiovascularDataset"]
    assert cardio["totalSamples"] > 0
    assert cardio["featureCount"] == 11
    assert cardio["targetColumn"] == "target"

    diabetes = data["diabetesComplicationsDataset"]
    assert diabetes["totalSamples"] > 0
    assert diabetes["featureCount"] == 11
    assert diabetes["targetColumn"] == "has_complication"
