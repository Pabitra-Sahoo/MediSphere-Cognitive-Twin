"""MediSphere Clinical Explainer

Generates local per-patient feature attributions and population-level global feature importance
for centralized baseline and federated consensus clinical models.

Output Space Separation:
- Predictions operate in probability space: model_estimated_risk_probability in [0.0, 1.0]
- Attributions operate in additive log-odds space:
    total_log_odds = base_value_log_odds + sum(shap_value_log_odds)
    model_estimated_risk_probability = sigmoid(total_log_odds)
"""

import json
from pathlib import Path
from typing import Any, Dict, List, Optional, Union

import joblib
import numpy as np
import pandas as pd

from config import DEFAULT_RISK_TIERS, RANDOM_SEED
from data.preprocessor import ClinicalPreprocessor
from explainability.explainer_factory import ExplainerFactory


class ClinicalExplainer:
    """Explains clinical risk models using model-specific SHAP explainers."""

    DISCLAIMER = (
        "Model feature contributions reflect statistical model behavior for academic research only. "
        "Not a medical diagnosis or clinical interpretation."
    )

    def __init__(
        self,
        estimator: Any,
        preprocessor: ClinicalPreprocessor,
        feature_names: List[str],
        task_type: str = "CARDIOVASCULAR",
        model_name: str = "clinical_model",
        model_version: str = "1.0.0",
        decision_threshold: float = 0.50,
        risk_tiers: Optional[Dict[str, float]] = None,
        background_data: Optional[np.ndarray] = None,
    ):
        self.estimator = estimator
        self.preprocessor = preprocessor
        self.feature_names = list(feature_names)
        self.task_type = str(task_type).upper()
        self.model_name = str(model_name)
        self.model_version = str(model_version)
        self.decision_threshold = float(decision_threshold)
        self.risk_tiers = dict(risk_tiers or DEFAULT_RISK_TIERS)
        self.background_data = background_data

        # Instantiate specialized SHAP explainer
        self.explainer = ExplainerFactory.create_explainer(
            estimator=self.estimator,
            background_data=self.background_data,
        )

    @classmethod
    def load(
        cls,
        model_dir: Union[str, Path],
        background_data: Optional[np.ndarray] = None,
    ) -> "ClinicalExplainer":
        """Loads a ClinicalExplainer from a saved model artifact directory."""
        in_path = Path(model_dir)
        estimator_path = in_path / "estimator.joblib"
        preprocessor_path = in_path / "preprocessor.joblib"
        metadata_path = in_path / "metadata.json"

        if not (estimator_path.exists() and preprocessor_path.exists() and metadata_path.exists()):
            raise FileNotFoundError(
                f"Missing required artifact files in '{in_path}'. "
                "Expected estimator.joblib, preprocessor.joblib, and metadata.json."
            )

        estimator = joblib.load(estimator_path)
        preprocessor = joblib.load(preprocessor_path)
        with open(metadata_path, "r", encoding="utf-8") as f:
            metadata = json.load(f)

        feature_names = metadata.get("feature_contract", preprocessor.feature_names)
        task_type = metadata.get("task_type", "CARDIOVASCULAR")
        model_name = metadata.get("model_name", in_path.name)
        model_version = metadata.get("version", "1.0.0")
        decision_threshold = metadata.get("decision_threshold", 0.50)
        risk_tiers = metadata.get("risk_tiers", DEFAULT_RISK_TIERS)

        return cls(
            estimator=estimator,
            preprocessor=preprocessor,
            feature_names=feature_names,
            task_type=task_type,
            model_name=model_name,
            model_version=model_version,
            decision_threshold=decision_threshold,
            risk_tiers=risk_tiers,
            background_data=background_data,
        )

    def explain_instance(
        self,
        features_input: Union[Dict[str, Any], pd.DataFrame],
    ) -> Dict[str, Any]:
        """Generates local feature attributions for a single clinical observation vector.

        Output Space Separation:
        - model_estimated_risk_probability: probability from predict_proba in [0.0, 1.0]
        - base_value_log_odds: baseline log-odds reference
        - shap_value_log_odds: additive contribution in log-odds
        - total_log_odds = base_value_log_odds + sum(shap_value_log_odds)
        """
        # 1. Format input as DataFrame
        if isinstance(features_input, dict):
            df = pd.DataFrame([features_input])
        elif isinstance(features_input, pd.DataFrame):
            df = features_input.copy()
            if len(df) != 1:
                df = df.iloc[[0]]
        else:
            raise ValueError(f"features_input must be a dict or pd.DataFrame, got {type(features_input)}")

        # Validate feature contract
        missing = [f for f in self.feature_names if f not in df.columns]
        if missing:
            raise ValueError(f"Input data missing required clinical features: {missing}")

        X_subset = df[self.feature_names]
        X_scaled = self.preprocessor.transform(X_subset)

        # 2. Compute model estimated risk probability & tier
        proba = float(self.estimator.predict_proba(X_scaled)[0, 1])
        low_max = self.risk_tiers.get("low_max", 0.20)
        mod_max = self.risk_tiers.get("moderate_max", 0.50)

        if proba < low_max:
            tier = "LOW"
        elif proba < mod_max:
            tier = "MODERATE"
        else:
            tier = "HIGH"

        # 3. Compute SHAP values
        explanation = self.explainer(X_scaled)
        raw_shap = explanation.values
        if len(raw_shap.shape) == 3:
            # Multi-class output shape: (1, d, 2) -> take positive class
            shap_vec = raw_shap[0, :, 1]
        else:
            shap_vec = raw_shap[0]

        # Extract base value scalar
        expected_val = getattr(self.explainer, "expected_value", 0.0)
        if isinstance(expected_val, (list, tuple, np.ndarray)):
            flat = np.asarray(expected_val).ravel()
            base_val_scalar = float(flat[-1] if len(flat) > 1 else flat[0])
        else:
            base_val_scalar = float(expected_val)

        total_log_odds = float(base_val_scalar + np.sum(shap_vec))

        # 4. Construct ranked feature attributions
        attributions = []
        for j, feat_name in enumerate(self.feature_names):
            raw_val = df[feat_name].iloc[0]
            val_float = float(raw_val) if pd.notna(raw_val) else 0.0
            shap_float = float(shap_vec[j])

            direction = "INCREASES_RISK" if shap_float > 0 else "DECREASES_RISK"
            attributions.append({
                "feature_name": feat_name,
                "feature_value": val_float,
                "shap_value_log_odds": round(shap_float, 4),
                "direction": direction,
                "abs_magnitude": abs(shap_float),
            })

        # Sort by absolute SHAP magnitude descending
        attributions.sort(key=lambda x: x["abs_magnitude"], reverse=True)
        for rank, attr in enumerate(attributions, start=1):
            attr["rank"] = rank
            del attr["abs_magnitude"]

        return {
            "task_type": self.task_type,
            "model_name": self.model_name,
            "model_version": self.model_version,
            "model_estimated_risk_probability": round(proba, 4),
            "estimated_risk_tier": tier,
            "decision_threshold": self.decision_threshold,
            "explanation_space": "log_odds",
            "base_value_log_odds": round(base_val_scalar, 4),
            "total_log_odds": round(total_log_odds, 4),
            "feature_attributions": attributions,
            "safety_disclaimer": self.DISCLAIMER,
        }

    def compute_global_explanation(
        self,
        val_df: pd.DataFrame,
        max_samples: Optional[int] = 500,
    ) -> Dict[str, Any]:
        """Computes population-level mean absolute SHAP importance on validation data.

        Important: val_df must be validation cohort data. Never supply holdout test data.
        """
        # Validate columns
        missing = [f for f in self.feature_names if f not in val_df.columns]
        if missing:
            raise ValueError(f"Validation DataFrame missing required clinical features: {missing}")

        sample_df = val_df[self.feature_names].copy()
        if max_samples is not None and len(sample_df) > max_samples:
            sample_df = sample_df.iloc[:max_samples]

        X_scaled = self.preprocessor.transform(sample_df)
        explanation = self.explainer(X_scaled)
        raw_shap = explanation.values

        if len(raw_shap.shape) == 3:
            shap_mat = raw_shap[:, :, 1]
        else:
            shap_mat = raw_shap

        mean_abs_shap = np.mean(np.abs(shap_mat), axis=0)

        importance_list = []
        for j, feat_name in enumerate(self.feature_names):
            importance_list.append({
                "feature_name": feat_name,
                "mean_abs_shap_log_odds": round(float(mean_abs_shap[j]), 4),
            })

        # Sort by mean absolute SHAP descending
        importance_list.sort(key=lambda x: x["mean_abs_shap_log_odds"], reverse=True)
        for rank, item in enumerate(importance_list, start=1):
            item["rank"] = rank

        return {
            "task_type": self.task_type,
            "model_name": self.model_name,
            "cohort_size": len(sample_df),
            "explanation_space": "mean_absolute_log_odds",
            "global_feature_importance": importance_list,
            "safety_disclaimer": self.DISCLAIMER,
        }

    def save_global_explanation(
        self,
        save_dir: Union[str, Path],
        val_df: pd.DataFrame,
        max_samples: Optional[int] = 500,
    ) -> Path:
        """Computes and persists global_explanation.json inside the model artifact directory."""
        out_dir = Path(save_dir)
        out_dir.mkdir(parents=True, exist_ok=True)

        global_expl = self.compute_global_explanation(val_df, max_samples=max_samples)
        out_file = out_dir / "global_explanation.json"

        with open(out_file, "w", encoding="utf-8") as f:
            json.dump(global_expl, f, indent=2)

        return out_file
