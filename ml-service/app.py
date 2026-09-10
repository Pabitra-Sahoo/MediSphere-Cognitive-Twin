"""MediSphere Cognitive Twin — Python ML Service

Provides runtime clinical risk prediction, SHAP-based feature explainability,
and model catalog metadata for the MediSphere platform.
"""

import sys
import os
import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Any, List, Optional

from fastapi import FastAPI, Request, HTTPException, Security, Depends, status
from fastapi.security.api_key import APIKeyHeader
from fastapi.middleware.cors import CORSMiddleware

from config import RANDOM_SEED, FEATURE_NAMES, SAVED_MODELS_DIR
from data.schema import (
    get_schema_summary,
    RiskInferenceRequest,
    RiskInferenceResponse,
    ModelSummaryItem,
)
from data.dataset_loader import DatasetLoader
from explainability.clinical_explainer import ClinicalExplainer

app = FastAPI(
    title="MediSphere Cognitive Twin ML Service",
    description=(
        "AI/ML and Federated Learning service for MediSphere Cognitive Twin. "
        "NOTICE: Model outputs and feature schemas are designed exclusively for "
        "academic research and educational demonstration. They do not constitute a medical diagnosis."
    ),
    version="1.0.0-phase14",
)

# CORS middleware for local development
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ---------------------------------------------------------------------------
# Internal Service-Key Authentication
# ---------------------------------------------------------------------------
# In production, ML_SERVICE_INTERNAL_KEY is strictly required.
# In development, a documented fallback dev key is used if not explicitly set.
INTERNAL_KEY_HEADER_NAME = "X-Internal-Service-Key"
api_key_header = APIKeyHeader(name=INTERNAL_KEY_HEADER_NAME, auto_error=False)

IS_PRODUCTION = os.getenv("ENVIRONMENT", os.getenv("ENV", "development")).lower() in ["production", "prod"]
CONFIGURED_INTERNAL_KEY = os.getenv("ML_SERVICE_INTERNAL_KEY")

if IS_PRODUCTION and not CONFIGURED_INTERNAL_KEY:
    raise RuntimeError("CRITICAL: ML_SERVICE_INTERNAL_KEY environment variable is required in production.")

DEV_DEFAULT_KEY = "medisphere-dev-internal-key-change-in-prod"
EFFECTIVE_INTERNAL_KEY = CONFIGURED_INTERNAL_KEY or DEV_DEFAULT_KEY


def verify_internal_service_key(header_key: Optional[str] = Security(api_key_header)) -> str:
    """Verifies that the request originates from an authorized internal service (Spring Boot)."""
    if not header_key or header_key != EFFECTIVE_INTERNAL_KEY:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unauthorized: invalid or missing internal service key",
        )
    return header_key


# ---------------------------------------------------------------------------
# In-Memory Model Cache & Defaults
# ---------------------------------------------------------------------------
_EXPLAINER_CACHE: Dict[str, ClinicalExplainer] = {}

PRODUCTION_DEFAULT_MODELS = {
    "CARDIOVASCULAR": "cardiovascular_federated_logistic_regression",
    "DIABETES": "diabetes_complications_federated_logistic_regression",
    "DIABETES_COMPLICATIONS": "diabetes_complications_federated_logistic_regression",
}


def get_cached_explainer(model_name: str) -> ClinicalExplainer:
    """Retrieves or loads a ClinicalExplainer from the model artifact directory."""
    if model_name not in _EXPLAINER_CACHE:
        model_dir = SAVED_MODELS_DIR / model_name
        if not model_dir.exists() or not (model_dir / "estimator.joblib").exists():
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Model '{model_name}' not found in saved registry.",
            )
        try:
            _EXPLAINER_CACHE[model_name] = ClinicalExplainer.load(model_dir)
        except Exception as ex:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Failed to load model '{model_name}': {str(ex)}",
            )
    return _EXPLAINER_CACHE[model_name]


# ---------------------------------------------------------------------------
# System & Schema Endpoints (Unauthenticated / Monitoring)
# ---------------------------------------------------------------------------
@app.get("/health", tags=["System"])
def health_check() -> Dict[str, Any]:
    """Health check endpoint for container orchestrators and service monitoring."""
    return {
        "status": "UP",
        "service": "medisphere-ml-service",
        "phase": "Phase 10: Foundation & Data Pipeline",
        "pythonVersion": sys.version.split()[0],
        "randomSeed": RANDOM_SEED,
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "disclaimer": (
            "MediSphere ML service is intended for academic research and educational demonstration. "
            "It does not provide medical diagnosis or treatment guidance."
        ),
    }


@app.get("/api/ml/schema", tags=["Schema"])
def get_feature_schema() -> Dict[str, Any]:
    """Returns the canonical 11-feature clinical vector specification for the HealthTwin."""
    return get_schema_summary()


@app.get("/api/ml/datasets", tags=["Datasets"])
def get_datasets_info() -> Dict[str, Any]:
    """Returns metadata for the evaluated clinical reference datasets."""
    try:
        cardio_info = DatasetLoader.get_dataset_metadata("CARDIOVASCULAR")
        diabetes_info = DatasetLoader.get_dataset_metadata("DIABETES_COMPLICATIONS")
        return {
            "cardiovascularDataset": cardio_info,
            "diabetesComplicationsDataset": diabetes_info,
            "totalFeatures": len(FEATURE_NAMES),
            "features": list(FEATURE_NAMES),
        }
    except Exception as ex:
        return {
            "status": "ERROR",
            "message": f"Failed to retrieve dataset metadata: {str(ex)}",
        }


# ---------------------------------------------------------------------------
# Runtime Inference & Explainability (Internal Key Protected)
# ---------------------------------------------------------------------------
@app.post(
    "/api/ml/predict-and-explain",
    response_model=RiskInferenceResponse,
    tags=["Inference"],
    dependencies=[Depends(verify_internal_service_key)],
)
def predict_and_explain(req: RiskInferenceRequest) -> RiskInferenceResponse:
    """Computes risk prediction and local SHAP feature attributions in additive log-odds space."""
    task_normalized = req.task_type.upper()
    if task_normalized not in ["CARDIOVASCULAR", "DIABETES", "DIABETES_COMPLICATIONS"]:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Unsupported task_type '{req.task_type}'. Must be CARDIOVASCULAR or DIABETES.",
        )

    # Determine model
    model_name = req.model_name
    if not model_name:
        model_name = PRODUCTION_DEFAULT_MODELS[task_normalized]

    explainer = get_cached_explainer(model_name)

    # Validate feature contract
    required_features = explainer.feature_names
    missing_features = [f for f in required_features if f not in req.features or req.features[f] is None]
    if missing_features:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail={
                "error": "INSUFFICIENT_CLINICAL_DATA",
                "message": f"Missing required clinical features: {missing_features}",
                "missing_features": missing_features,
            },
        )

    try:
        result = explainer.explain_instance(req.features)
        result["model_name"] = model_name
        return RiskInferenceResponse(**result)
    except Exception as ex:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Inference execution failure: {str(ex)}",
        )


@app.get(
    "/api/ml/models",
    response_model=List[ModelSummaryItem],
    tags=["Registry"],
    dependencies=[Depends(verify_internal_service_key)],
)
def list_models() -> List[ModelSummaryItem]:
    """Returns metadata for all registered models in the saved directory."""
    models: List[ModelSummaryItem] = []
    for model_dir in sorted(SAVED_MODELS_DIR.iterdir()):
        meta_file = model_dir / "metadata.json"
        if model_dir.is_dir() and meta_file.exists():
            try:
                with open(meta_file, "r", encoding="utf-8") as f:
                    meta = json.load(f)
                m_name = model_dir.name
                task = meta.get("task_type", "UNKNOWN")
                is_prod = (
                    m_name == PRODUCTION_DEFAULT_MODELS.get("CARDIOVASCULAR")
                    or m_name == PRODUCTION_DEFAULT_MODELS.get("DIABETES")
                )
                models.append(
                    ModelSummaryItem(
                        model_name=m_name,
                        task_type=task,
                        algorithm=meta.get("algorithm", "UNKNOWN"),
                        version=meta.get("version", "1.0.0"),
                        decision_threshold=meta.get("decision_threshold", 0.50),
                        feature_contract=meta.get("feature_contract", []),
                        is_production_default=is_prod,
                    )
                )
            except Exception:
                continue
    return models


@app.get(
    "/api/ml/models/{model_name}/global-explanation",
    tags=["Registry"],
    dependencies=[Depends(verify_internal_service_key)],
)
def get_global_explanation(model_name: str) -> Dict[str, Any]:
    """Returns the precomputed validation-cohort global SHAP feature importance artifact."""
    expl_file = SAVED_MODELS_DIR / model_name / "global_explanation.json"
    if not expl_file.exists():
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Global explanation artifact not found for model '{model_name}'.",
        )
    with open(expl_file, "r", encoding="utf-8") as f:
        return json.load(f)


if __name__ == "__main__":
    import uvicorn
    from config import SERVICE_HOST, SERVICE_PORT
    uvicorn.run("app:app", host=SERVICE_HOST, port=SERVICE_PORT, reload=True)
