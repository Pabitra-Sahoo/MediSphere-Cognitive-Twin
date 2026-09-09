"""MediSphere SHAP Explainer Factory

Dynamically instantiates architecture-specialized SHAP explainers:
- Regularized Logistic Regression -> shap.LinearExplainer (analytical closed-form in log-odds space)
- HistGradientBoostingClassifier -> shap.TreeExplainer (exact polynomial-time TreeSHAP)

Strictly avoids slow model-agnostic sampling approximations (KernelExplainer).
"""

from typing import Any, Optional
import numpy as np
from sklearn.ensemble import HistGradientBoostingClassifier
from sklearn.linear_model import LogisticRegression
import shap


class ExplainerFactory:
    """Factory creating specialized SHAP explainers for MediSphere clinical risk models."""

    @staticmethod
    def create_explainer(
        estimator: Any,
        background_data: Optional[np.ndarray] = None,
    ) -> shap.Explainer:
        """Instantiates the optimal SHAP explainer for the given scikit-learn estimator.

        Args:
            estimator: Trained scikit-learn model (LogisticRegression or HistGradientBoostingClassifier).
            background_data: Optional reference background array in scaled coordinate space.

        Returns:
            Configured SHAP explainer instance.

        Raises:
            ValueError: If estimator architecture is unsupported or incompatible with exact explainers.
        """
        if isinstance(estimator, LogisticRegression):
            # For linear models, LinearExplainer computes exact additive Shapley values in log-odds space
            if background_data is not None:
                return shap.LinearExplainer(estimator, background_data)
            else:
                # Default canonical background: training mean in standardized space is zero vector
                n_features = getattr(estimator, "n_features_in_", estimator.coef_.shape[1])
                zero_background = np.zeros((1, n_features), dtype=np.float64)
                return shap.LinearExplainer(estimator, zero_background)

        elif isinstance(estimator, HistGradientBoostingClassifier):
            # For GBDT ensembles, TreeExplainer evaluates exact tree-path conditional expectations
            if background_data is not None:
                return shap.TreeExplainer(estimator, background_data)
            else:
                return shap.TreeExplainer(estimator)

        else:
            raise ValueError(
                f"Unsupported estimator class '{type(estimator).__name__}'. "
                "Phase 13 requires exact explainers (shap.LinearExplainer for LogisticRegression "
                "or shap.TreeExplainer for HistGradientBoostingClassifier). KernelExplainer is prohibited."
            )
