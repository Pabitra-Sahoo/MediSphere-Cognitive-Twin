"""MediSphere Base Clinical Risk Model Abstraction

Provides abstract foundation for clinical baseline models:
- Enforces strict feature contracts and zero data leakage.
- Explicitly separates binary decision thresholds from categorical risk tiers.
- Provides standard evaluation across ROC-AUC, F1, Brier score, and confusion matrix.
- Prepares parameter extraction and injection hooks for Phase 12 Federated Learning (FedAvg).
- Provides deterministic serialization with joblib and JSON audit metadata.
"""

import json
import os
import sys
from abc import ABC, abstractmethod
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple, Union

import joblib
import numpy as np
import pandas as pd
from sklearn.metrics import (
    accuracy_score,
    brier_score_loss,
    confusion_matrix,
    f1_score,
    log_loss,
    precision_score,
    recall_score,
    roc_auc_score,
)

# Ensure parent path resolution
BASE_DIR = Path(__file__).resolve().parent.parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from config import DEFAULT_RISK_TIERS, RANDOM_SEED
from data.preprocessor import ClinicalPreprocessor


class BaseClinicalModel(ABC):
    """Abstract base class for MediSphere clinical risk models."""

    DISCLAIMER = (
        "Model-estimated risk probabilities for academic research and educational demonstration only. "
        "Not a medical diagnosis."
    )

    def __init__(
        self,
        model_name: str,
        task_type: str,
        algorithm: str,
        feature_names: List[str],
        target_name: str,
        decision_threshold: float = 0.50,
        risk_tiers: Optional[Dict[str, float]] = None,
        random_seed: int = RANDOM_SEED,
    ):
        self.model_name = model_name
        self.task_type = task_type
        self.algorithm = algorithm
        self.feature_names = list(feature_names)
        self.target_name = target_name
        self.random_seed = random_seed

        # Binary decision threshold (tuned on validation set to maximize F1 or similar metric)
        self.decision_threshold = float(decision_threshold)

        # Categorical risk-tier display thresholds (strictly separated from binary decision threshold)
        self.risk_tiers = dict(risk_tiers or DEFAULT_RISK_TIERS)

        self.estimator: Any = None
        self.preprocessor: Optional[ClinicalPreprocessor] = None
        self.is_fitted: bool = False
        self.metadata_: Dict[str, Any] = {}

    @abstractmethod
    def _create_estimator(self) -> Any:
        """Instantiates the underlying scikit-learn estimator with configured hyperparameters."""
        pass

    def fit(self, X_train: pd.DataFrame, y_train: Union[pd.Series, np.ndarray]) -> "BaseClinicalModel":
        """Fits preprocessor and estimator strictly on the training partition to prevent leakage."""
        self._validate_input_columns(X_train)

        # 1. Initialize and fit preprocessor on train data ONLY
        self.preprocessor = ClinicalPreprocessor(feature_names=self.feature_names)
        X_train_scaled = self.preprocessor.fit_transform(X_train)

        # 2. Instantiate and fit estimator
        self.estimator = self._create_estimator()
        y_train_arr = np.asarray(y_train).ravel()
        self.estimator.fit(X_train_scaled, y_train_arr)

        self.is_fitted = True
        return self

    def predict_proba(self, X: pd.DataFrame) -> np.ndarray:
        """Computes model-estimated risk probabilities in range [0.0, 1.0]."""
        if not self.is_fitted or self.preprocessor is None or self.estimator is None:
            raise RuntimeError(f"Model '{self.model_name}' must be fitted before predict_proba().")

        self._validate_input_columns(X)
        X_scaled = self.preprocessor.transform(X)

        if hasattr(self.estimator, "predict_proba"):
            # Return positive class probability (column index 1)
            probas = self.estimator.predict_proba(X_scaled)[:, 1]
        elif hasattr(self.estimator, "decision_function"):
            decision = self.estimator.decision_function(X_scaled)
            # Sigmoid link
            probas = 1.0 / (1.0 + np.exp(-decision))
        else:
            raise RuntimeError(f"Estimator {type(self.estimator)} does not support probability estimation.")

        return np.clip(probas.astype(np.float64), 0.0, 1.0)

    def predict(self, X: pd.DataFrame, threshold: Optional[float] = None) -> np.ndarray:
        """Predicts binary outcomes using the calibrated decision threshold."""
        thresh = self.decision_threshold if threshold is None else float(threshold)
        probas = self.predict_proba(X)
        return (probas >= thresh).astype(int)

    def predict_risk_tier(self, X: pd.DataFrame) -> List[str]:
        """Categorizes model-estimated risk probabilities into LOW, MODERATE, or HIGH tiers.

        Risk tiers are strictly clinical display boundaries and are distinct from
        the binary decision threshold.
        """
        probas = self.predict_proba(X)
        low_max = self.risk_tiers.get("low_max", 0.20)
        mod_max = self.risk_tiers.get("moderate_max", 0.50)

        tiers: List[str] = []
        for p in probas:
            if p < low_max:
                tiers.append("LOW")
            elif p < mod_max:
                tiers.append("MODERATE")
            else:
                tiers.append("HIGH")
        return tiers

    def calibrate_decision_threshold(
        self,
        X_val: pd.DataFrame,
        y_val: Union[pd.Series, np.ndarray],
        metric: str = "f1",
        steps: int = 81,
    ) -> float:
        """Sweeps decision thresholds on validation set to find the optimal threshold theta*."""
        probas = self.predict_proba(X_val)
        y_true = np.asarray(y_val).ravel()

        thresholds = np.linspace(0.10, 0.90, steps)
        best_score = -1.0
        best_theta = 0.50

        for theta in thresholds:
            y_pred = (probas >= theta).astype(int)
            if metric == "f1":
                score = f1_score(y_true, y_pred, zero_division=0)
            elif metric == "accuracy":
                score = accuracy_score(y_true, y_pred)
            elif metric == "balanced_accuracy":
                rec = recall_score(y_true, y_pred, zero_division=0)
                spec = recall_score(1 - y_true, 1 - y_pred, zero_division=0)
                score = 0.5 * (rec + spec)
            else:
                raise ValueError(f"Unsupported calibration metric: {metric}")

            if score > best_score:
                best_score = score
                best_theta = float(theta)

        self.decision_threshold = round(best_theta, 4)
        return self.decision_threshold

    def evaluate(
        self,
        X: pd.DataFrame,
        y: Union[pd.Series, np.ndarray],
        threshold: Optional[float] = None,
    ) -> Dict[str, Any]:
        """Computes comprehensive evaluation metrics over the designated partition."""
        probas = self.predict_proba(X)
        y_true = np.asarray(y).ravel()
        thresh = self.decision_threshold if threshold is None else float(threshold)
        y_pred = (probas >= thresh).astype(int)

        # Safe ROC-AUC calculation
        try:
            auc = float(roc_auc_score(y_true, probas))
        except ValueError:
            auc = 0.5

        # Safe Log-Loss calculation
        try:
            loss = float(log_loss(y_true, probas))
        except ValueError:
            loss = float("nan")

        brier = float(brier_score_loss(y_true, probas))
        acc = float(accuracy_score(y_true, y_pred))
        prec = float(precision_score(y_true, y_pred, zero_division=0))
        rec = float(recall_score(y_true, y_pred, zero_division=0))
        f1 = float(f1_score(y_true, y_pred, zero_division=0))

        cm = confusion_matrix(y_true, y_pred)
        if cm.shape == (2, 2):
            tn, fp, fn, tp = cm.ravel()
        else:
            tn, fp, fn, tp = 0, 0, 0, 0

        return {
            "roc_auc": round(auc, 4),
            "accuracy": round(acc, 4),
            "precision": round(prec, 4),
            "recall": round(rec, 4),
            "f1": round(f1, 4),
            "log_loss": round(loss, 4),
            "brier_score": round(brier, 4),
            "decision_threshold_used": round(thresh, 4),
            "confusion_matrix": {
                "tn": int(tn),
                "fp": int(fp),
                "fn": int(fn),
                "tp": int(tp),
            },
        }

    # =========================================================================
    # Phase 12 Forward Compatibility: Parameter Extraction & Injection Hooks
    # =========================================================================

    def get_parameters(self) -> Dict[str, np.ndarray]:
        """Extracts trainable model parameters for Phase 12 Federated Averaging (FedAvg)."""
        if not self.is_fitted or self.estimator is None:
            raise RuntimeError(f"Cannot extract parameters from unfitted model '{self.model_name}'.")

        if hasattr(self.estimator, "coef_") and hasattr(self.estimator, "intercept_"):
            return {
                "coef": self.estimator.coef_.copy(),
                "intercept": self.estimator.intercept_.copy(),
            }
        else:
            raise NotImplementedError(
                f"Estimator '{type(self.estimator).__name__}' does not support linear parameter averaging."
            )

    def set_parameters(self, params: Dict[str, np.ndarray]) -> None:
        """Injects aggregated global weights from Phase 12 Federated Averaging (FedAvg)."""
        if not self.is_fitted or self.estimator is None:
            raise RuntimeError(f"Cannot set parameters on unfitted model '{self.model_name}'.")

        if "coef" in params and "intercept" in params:
            self.estimator.coef_ = params["coef"].copy()
            self.estimator.intercept_ = params["intercept"].copy()
        else:
            raise ValueError("Parameters dict must contain 'coef' and 'intercept' keys.")

    # =========================================================================
    # Artifact Persistence & Serialization
    # =========================================================================

    def save(self, save_dir: Union[str, Path], metadata_extra: Optional[Dict[str, Any]] = None) -> Path:
        """Saves model estimator, preprocessor, and comprehensive audit metadata."""
        if not self.is_fitted or self.estimator is None or self.preprocessor is None:
            raise RuntimeError(f"Cannot save unfitted model '{self.model_name}'.")

        out_path = Path(save_dir)
        out_path.mkdir(parents=True, exist_ok=True)

        # 1. Save scikit-learn estimator
        estimator_file = out_path / "estimator.joblib"
        joblib.dump(self.estimator, estimator_file)

        # 2. Save fitted clinical preprocessor
        preprocessor_file = out_path / "preprocessor.joblib"
        joblib.dump(self.preprocessor, preprocessor_file)

        # 3. Construct and save audit metadata
        metadata = {
            "model_name": self.model_name,
            "task_type": self.task_type,
            "algorithm": self.algorithm,
            "version": "1.0.0-phase11",
            "feature_contract": self.feature_names,
            "target_column": self.target_name,
            "random_seed": self.random_seed,
            "decision_threshold": self.decision_threshold,
            "risk_tiers": self.risk_tiers,
            "estimator_class": type(self.estimator).__name__,
            "estimator_params": self.estimator.get_params(),
            "preprocessor_state": self.preprocessor.get_state(),
            "training_timestamp": datetime.now(timezone.utc).isoformat(),
            "safety_disclaimer": self.DISCLAIMER,
        }

        if metadata_extra:
            metadata.update(metadata_extra)

        self.metadata_ = metadata
        metadata_file = out_path / "metadata.json"
        with open(metadata_file, "w", encoding="utf-8") as f:
            json.dump(metadata, f, indent=2)

        return out_path

    @classmethod
    def load(cls, load_dir: Union[str, Path]) -> "BaseClinicalModel":
        """Loads a persisted model from saved directory, dynamically resolving concrete model subclass."""
        in_path = Path(load_dir)
        metadata_file = in_path / "metadata.json"
        estimator_file = in_path / "estimator.joblib"
        preprocessor_file = in_path / "preprocessor.joblib"

        if not (metadata_file.exists() and estimator_file.exists() and preprocessor_file.exists()):
            raise FileNotFoundError(f"Missing required artifact files in '{in_path}'.")

        with open(metadata_file, "r", encoding="utf-8") as f:
            metadata = json.load(f)

        # Resolve target concrete class if BaseClinicalModel.load is called directly
        if cls is BaseClinicalModel:
            task = metadata.get("task_type", "").upper()
            if task == "CARDIOVASCULAR":
                from models.cardiovascular_model import CardiovascularBaselineModel
                target_cls = CardiovascularBaselineModel
            elif task == "DIABETES":
                from models.diabetes_model import DiabetesComplicationsBaselineModel
                target_cls = DiabetesComplicationsBaselineModel
            else:
                raise ValueError(f"Unknown or unsupported task_type '{task}' in metadata.json")
        else:
            target_cls = cls

        instance = target_cls(
            algorithm=metadata["algorithm"],
            decision_threshold=metadata.get("decision_threshold", 0.50),
            risk_tiers=metadata.get("risk_tiers", DEFAULT_RISK_TIERS),
            random_seed=metadata.get("random_seed", RANDOM_SEED),
        )

        instance.estimator = joblib.load(estimator_file)
        instance.preprocessor = joblib.load(preprocessor_file)
        instance.is_fitted = True
        instance.metadata_ = metadata
        return instance

    def _validate_input_columns(self, df: pd.DataFrame) -> None:
        """Validates that input DataFrame adheres to the feature contract."""
        missing = [f for f in self.feature_names if f not in df.columns]
        if missing:
            raise ValueError(
                f"Input DataFrame does not satisfy feature contract for '{self.model_name}'. "
                f"Missing features: {missing}"
            )
