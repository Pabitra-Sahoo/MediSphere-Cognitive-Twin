"""MediSphere Cognitive Twin — Python ML Service (Phase 10 Foundation)

Provides health monitoring, schema specifications, and clinical dataset metadata.
"""

import sys
import os
from datetime import datetime, timezone
from typing import Dict, Any

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware

from config import RANDOM_SEED, FEATURE_NAMES
from data.schema import get_schema_summary
from data.dataset_loader import DatasetLoader

app = FastAPI(
    title="MediSphere Cognitive Twin ML Service",
    description=(
        "AI/ML and Federated Learning service for MediSphere Cognitive Twin. "
        "NOTICE: Model outputs and feature schemas are designed exclusively for "
        "academic research and educational demonstration. They do not constitute a medical diagnosis."
    ),
    version="0.1.0-phase10",
)

# CORS middleware for local development
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


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


if __name__ == "__main__":
    import uvicorn
    from config import SERVICE_HOST, SERVICE_PORT
    uvicorn.run("app:app", host=SERVICE_HOST, port=SERVICE_PORT, reload=True)
