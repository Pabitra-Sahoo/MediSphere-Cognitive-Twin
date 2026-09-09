"""MediSphere Diabetes Complications Baseline Model

Implements clinical risk models for the authentic UCI Diabetes 130-US Hospitals cohort:
- Contract: 11 encounter diagnostic & therapeutic features (age, gender, time_in_hospital,
  num_lab_procedures, num_procedures, num_medications, number_diagnoses, max_glu_serum,
  A1Cresult, insulin, diabetesMed).
- Excluded: systolicBP, diastolicBP, heartRate, oxygenSaturation (never fabricated).
- Target: has_complication (binary indicator of secondary microvascular/macrovascular end-organ damage).
- Supported algorithms: Regularized Logistic Regression (L2) and HistGradientBoostingClassifier.
"""

from typing import Any, Dict, List, Optional
from sklearn.ensemble import HistGradientBoostingClassifier
from sklearn.linear_model import LogisticRegression

from config import (
    DEFAULT_RISK_TIERS,
    DIABETES_FEATURES,
    DIABETES_TARGET,
    RANDOM_SEED,
)
from models.base_model import BaseClinicalModel


class DiabetesComplicationsBaselineModel(BaseClinicalModel):
    """Diabetes secondary complication risk baseline model."""

    SUPPORTED_ALGORITHMS = ["LOGISTIC_REGRESSION", "HIST_GRADIENT_BOOSTING"]

    def __init__(
        self,
        algorithm: str = "LOGISTIC_REGRESSION",
        decision_threshold: float = 0.50,
        risk_tiers: Optional[Dict[str, float]] = None,
        hyperparameters: Optional[Dict[str, Any]] = None,
        random_seed: int = RANDOM_SEED,
    ):
        algo = algorithm.upper()
        if algo not in self.SUPPORTED_ALGORITHMS:
            raise ValueError(f"Unsupported algorithm '{algorithm}'. Choose from {self.SUPPORTED_ALGORITHMS}.")

        model_name = f"diabetes_complications_{algo.lower()}"
        super().__init__(
            model_name=model_name,
            task_type="DIABETES",
            algorithm=algo,
            feature_names=DIABETES_FEATURES,
            target_name=DIABETES_TARGET,
            decision_threshold=decision_threshold,
            risk_tiers=risk_tiers or DEFAULT_RISK_TIERS,
            random_seed=random_seed,
        )
        self.hyperparameters = dict(hyperparameters or {})

    def _create_estimator(self) -> Any:
        """Instantiates the scikit-learn estimator with reproducible seed and hyperparameters."""
        if self.algorithm == "LOGISTIC_REGRESSION":
            params = {
                "penalty": "l2",
                "C": 1.0,
                "class_weight": "balanced",
                "solver": "lbfgs",
                "max_iter": 1000,
                "random_state": self.random_seed,
            }
            params.update(self.hyperparameters)
            return LogisticRegression(**params)

        elif self.algorithm == "HIST_GRADIENT_BOOSTING":
            params = {
                "class_weight": "balanced",
                "max_iter": 100,
                "learning_rate": 0.05,
                "max_leaf_nodes": 31,
                "min_samples_leaf": 30,
                "random_state": self.random_seed,
            }
            params.update(self.hyperparameters)
            return HistGradientBoostingClassifier(**params)

        raise ValueError(f"Unrecognized algorithm '{self.algorithm}'")
