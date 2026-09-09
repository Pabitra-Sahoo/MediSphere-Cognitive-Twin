"""MediSphere Clinical Feature Schema

Defines the standard 11-feature vector shared between the Spring Boot HealthTwin
and the Python ML Service for risk estimation.
"""

from typing import Dict, Any, List, Optional
from pydantic import BaseModel, Field, field_validator


class ClinicalFeatureVector(BaseModel):
    """Normalized clinical feature vector matching MediSphere HealthTwin.
    
    Contains 11 biometric parameters spanning demographics, vital signs, and laboratory diagnostics.
    """
    age: float = Field(..., ge=0.0, le=125.0, description="Patient age in years")
    gender: float = Field(..., ge=0.0, le=1.0, description="Gender (1.0 = Male, 0.0 = Female)")
    bmi: float = Field(..., ge=10.0, le=75.0, description="Body Mass Index in kg/m^2")
    systolicBP: float = Field(..., ge=60.0, le=250.0, description="Systolic Blood Pressure in mmHg")
    diastolicBP: float = Field(..., ge=30.0, le=150.0, description="Diastolic Blood Pressure in mmHg")
    heartRate: float = Field(..., ge=30.0, le=220.0, description="Heart Rate in beats per minute")
    oxygenSaturation: float = Field(..., ge=65.0, le=100.0, description="Oxygen Saturation (SpO2) percentage")
    glucose: float = Field(..., ge=30.0, le=600.0, description="Blood Glucose in mg/dL")
    cholesterol: float = Field(..., ge=50.0, le=600.0, description="Total Serum Cholesterol in mg/dL")
    creatinine: float = Field(..., ge=0.1, le=20.0, description="Serum Creatinine in mg/dL")
    hemoglobin: float = Field(..., ge=3.0, le=25.0, description="Hemoglobin in g/dL")

    @field_validator("diastolicBP")
    @classmethod
    def validate_bp_ratio(cls, v: float, info) -> float:
        """Ensures diastolic BP does not exceed systolic BP when systolic is present."""
        systolic = info.data.get("systolicBP")
        if systolic is not None and v > systolic:
            raise ValueError(f"diastolicBP ({v}) cannot exceed systolicBP ({systolic})")
        return v

    def to_feature_list(self) -> List[float]:
        """Returns ordered list of numeric features matching canonical FEATURE_NAMES order."""
        return [
            float(self.age),
            float(self.gender),
            float(self.bmi),
            float(self.systolicBP),
            float(self.diastolicBP),
            float(self.heartRate),
            float(self.oxygenSaturation),
            float(self.glucose),
            float(self.cholesterol),
            float(self.creatinine),
            float(self.hemoglobin),
        ]


# Canonical metadata dictionary describing each clinical feature
SCHEMA_METADATA: Dict[str, Dict[str, Any]] = {
    "age": {
        "type": "numeric",
        "unit": "years",
        "range": [0, 125],
        "normalRange": [18, 65],
        "source": "demographics.age",
        "description": "Patient chronological age in years",
    },
    "gender": {
        "type": "binary",
        "unit": "encoded",
        "range": [0, 1],
        "mapping": {"0": "Female", "1": "Male"},
        "source": "demographics.gender",
        "description": "Biological sex encoded as 1.0 (Male) or 0.0 (Female)",
    },
    "bmi": {
        "type": "numeric",
        "unit": "kg/m^2",
        "range": [10.0, 75.0],
        "normalRange": [18.5, 24.9],
        "source": "demographics.bmi",
        "description": "Body Mass Index calculated from height and weight",
    },
    "systolicBP": {
        "type": "numeric",
        "unit": "mmHg",
        "range": [60.0, 250.0],
        "normalRange": [90.0, 120.0],
        "source": "latestVitals.systolicBP",
        "description": "Systolic arterial blood pressure",
    },
    "diastolicBP": {
        "type": "numeric",
        "unit": "mmHg",
        "range": [30.0, 150.0],
        "normalRange": [60.0, 80.0],
        "source": "latestVitals.diastolicBP",
        "description": "Diastolic arterial blood pressure",
    },
    "heartRate": {
        "type": "numeric",
        "unit": "bpm",
        "range": [30.0, 220.0],
        "normalRange": [60.0, 100.0],
        "source": "latestVitals.heartRate",
        "description": "Resting heart rate in beats per minute",
    },
    "oxygenSaturation": {
        "type": "numeric",
        "unit": "%",
        "range": [65.0, 100.0],
        "normalRange": [95.0, 100.0],
        "source": "latestVitals.oxygenSaturation",
        "description": "Pulse oximetry blood oxygen saturation percentage",
    },
    "glucose": {
        "type": "numeric",
        "unit": "mg/dL",
        "range": [30.0, 600.0],
        "normalRange": [70.0, 99.0],
        "source": "latestLabs.glucose",
        "description": "Blood glucose concentration",
    },
    "cholesterol": {
        "type": "numeric",
        "unit": "mg/dL",
        "range": [50.0, 600.0],
        "normalRange": [125.0, 200.0],
        "source": "latestLabs.cholesterol",
        "description": "Total serum cholesterol concentration",
    },
    "creatinine": {
        "type": "numeric",
        "unit": "mg/dL",
        "range": [0.1, 20.0],
        "normalRange": [0.6, 1.2],
        "source": "latestLabs.creatinine",
        "description": "Serum creatinine renal filtration biomarker",
    },
    "hemoglobin": {
        "type": "numeric",
        "unit": "g/dL",
        "range": [3.0, 25.0],
        "normalRange": [12.0, 17.5],
        "source": "latestLabs.hemoglobin",
        "description": "Blood hemoglobin concentration",
    },
}


def get_schema_summary() -> Dict[str, Any]:
    """Generates schema specification payload for API inspection."""
    return {
        "totalFeatures": len(SCHEMA_METADATA),
        "features": SCHEMA_METADATA,
        "featureOrder": list(SCHEMA_METADATA.keys()),
        "targetModels": ["CARDIOVASCULAR", "DIABETES_COMPLICATIONS"],
        "disclaimer": (
            "MediSphere feature schema and model outputs are designed for academic research "
            "and educational demonstration only. They do not constitute a medical diagnosis."
        ),
    }
