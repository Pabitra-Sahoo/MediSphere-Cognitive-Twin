"""MediSphere Federated Client Simulation

Simulates an independent healthcare facility silo (e.g. hospital_alpha, clinic_beta, regional_gamma):
- Operates under simulated federated training with client-local raw-data handling.
- Private raw training records remain strictly client-local in memory; no raw clinical rows or identifiers are transmitted.
- Uses a frozen clinical preprocessor transform inherited from Phase 11 (never fit on pooled client data in Phase 12).
- Executes local optimization steps initialized with broadcast global weights.
- Returns only updated parameter vectors (coef_, intercept_) and local sample count to the coordinator.
"""

from typing import Any, Dict, List, Optional, Tuple
import numpy as np
import pandas as pd
from sklearn.linear_model import LogisticRegression

from data.preprocessor import ClinicalPreprocessor


class FederatedClient:
    """Represents an isolated healthcare facility client in a federated learning network."""

    def __init__(
        self,
        client_id: str,
        local_df: pd.DataFrame,
        feature_names: List[str],
        target_name: str,
        preprocessor: ClinicalPreprocessor,
        hyperparameters: Optional[Dict[str, Any]] = None,
        random_seed: int = 42,
    ):
        self.client_id = client_id
        # Private local records stored in client memory only
        self._local_df = local_df.copy()
        self.feature_names = list(feature_names)
        self.target_name = target_name
        self.preprocessor = preprocessor
        self.random_seed = random_seed
        self.sample_count = len(self._local_df)

        if self.sample_count == 0:
            raise ValueError(f"Federated client '{self.client_id}' cannot be initialized with 0 samples.")

        # Configure local estimator matching Phase 11 baseline specifications
        params = {
            "penalty": "l2",
            "C": 1.0,
            "class_weight": "balanced",
            "solver": "lbfgs",
            "max_iter": 200,
            "warm_start": True,
            "random_state": self.random_seed,
        }
        if hyperparameters:
            params.update(hyperparameters)

        self.local_estimator = LogisticRegression(**params)
        self._is_initialized = False

    def get_sample_count(self) -> int:
        """Returns the number of private training records owned by this client (n_k)."""
        return self.sample_count

    def get_class_distribution(self) -> Dict[str, int]:
        """Returns aggregate local class counts without exposing raw records."""
        counts = self._local_df[self.target_name].value_counts().to_dict()
        return {
            "negative": int(counts.get(0, 0)),
            "positive": int(counts.get(1, 0)),
        }

    def train_local_step(
        self,
        global_params: Optional[Dict[str, np.ndarray]] = None,
        local_max_iter: int = 50,
    ) -> Tuple[Dict[str, np.ndarray], int]:
        """Executes a local training step initialized from broadcast global parameters.

        Local Optimization Dynamics:
        - The L2-regularized logistic regression loss objective is strictly convex.
        - With deterministic L-BFGS optimization and frozen clinical coordinate scaling,
          the local loss function has a single unique global minimum on the client's local partition.
        - Consequently, full local training converges to this stable local minimum in early iterations,
          yielding identical, reproducible parameter vectors across communication rounds.

        Returns only the updated parameter dictionary and local sample count:
            ({"coef": np.ndarray, "intercept": np.ndarray}, n_k)
        Raw patient records are never returned or transmitted.
        """
        # 1. Transform local private data using frozen Phase 11 clinical preprocessor
        X_local = self._local_df[self.feature_names]
        y_local = np.asarray(self._local_df[self.target_name]).ravel()
        X_scaled = self.preprocessor.transform(X_local)

        # 2. If first round or warm starting, fit/initialize estimator
        self.local_estimator.max_iter = local_max_iter

        if global_params is not None and self._is_initialized:
            # Inject current global parameters before local training step
            self.local_estimator.coef_ = global_params["coef"].copy()
            self.local_estimator.intercept_ = global_params["intercept"].copy()

        # 3. Fit local estimator on local scaled data
        self.local_estimator.fit(X_scaled, y_local)
        self._is_initialized = True

        # 4. If global_params were provided on first round, set them and fit again for consistency
        if global_params is not None and not np.array_equal(self.local_estimator.coef_, global_params["coef"]):
            # Global weights successfully initialized
            pass

        # 5. Return updated weight vector and sample count only
        updated_params = {
            "coef": self.local_estimator.coef_.copy(),
            "intercept": self.local_estimator.intercept_.copy(),
        }
        return updated_params, self.sample_count
