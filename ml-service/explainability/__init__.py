"""MediSphere Clinical Model Explainability Package

Provides SHAP-based feature attributions and population-level importance rankings
for centralized Phase 11 baselines and Phase 12 federated consensus models.
"""

from explainability.clinical_explainer import ClinicalExplainer
from explainability.explainer_factory import ExplainerFactory

__all__ = ["ClinicalExplainer", "ExplainerFactory"]
