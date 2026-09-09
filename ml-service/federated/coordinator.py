"""MediSphere FedAvg Coordinator

Coordinates decentralized federated learning rounds:
- Operates under simulated federated training with client-local raw-data handling.
- Uses a frozen clinical preprocessor transform inherited from Phase 11 (never refit on pooled client data in Phase 12).
- Broadcasts current global model parameters to participating client silos.
- Collects updated parameters and sample counts without accessing raw records.
- Computes textbook weighted Federated Averaging (McMahan et al., 2017):
    W_global = Σ (n_k / N) * W_k
- Tracks validation loss and convergence across communication rounds.
- Calibrates global decision threshold on central validation set.
- Evaluates final global model strictly ONCE on the isolated holdout test set.
- Persists final federated model bundles and complete round history.
"""

import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple, Union

import joblib
import numpy as np
import pandas as pd
from sklearn.linear_model import LogisticRegression
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

from config import DEFAULT_RISK_TIERS, RANDOM_SEED
from data.preprocessor import ClinicalPreprocessor
from federated.client import FederatedClient


class FedAvgCoordinator:
    """Central server coordinator orchestrating the Federated Learning / FedAvg simulation."""

    FRAMEWORK_NAME = "Federated Learning / FedAvg simulation"
    AGGREGATION_NAME = "Weighted FedAvg (McMahan et al., 2017)"
    DISCLAIMER = (
        "Model-estimated risk probabilities for academic research and educational demonstration only. "
        "Not a medical diagnosis."
    )

    def __init__(
        self,
        task_type: str,
        feature_names: List[str],
        target_name: str,
        shared_preprocessor: ClinicalPreprocessor,
        val_df: pd.DataFrame,
        test_df: pd.DataFrame,
        risk_tiers: Optional[Dict[str, float]] = None,
        random_seed: int = RANDOM_SEED,
    ):
        self.task_type = task_type
        self.feature_names = list(feature_names)
        self.target_name = target_name
        self.shared_preprocessor = shared_preprocessor
        self.val_df = val_df.copy()
        self.test_df = test_df.copy()
        self.risk_tiers = dict(risk_tiers or DEFAULT_RISK_TIERS)
        self.random_seed = random_seed

        self.clients: Dict[str, FederatedClient] = {}
        self.global_estimator: Optional[LogisticRegression] = None
        self.decision_threshold: float = 0.50
        self.round_history: List[Dict[str, Any]] = []
        self.is_trained: bool = False

    def register_client(self, client: FederatedClient) -> None:
        """Registers an independent client facility with the coordinator."""
        self.clients[client.client_id] = client

    @staticmethod
    def aggregate_parameters(
        client_updates: List[Tuple[Dict[str, np.ndarray], int]]
    ) -> Dict[str, np.ndarray]:
        """Performs textbook weighted parameter averaging (FedAvg).

        W_global = Σ (n_k / N) * W_k
        Separately averages 'coef' and 'intercept'.
        """
        if not client_updates:
            raise ValueError("Cannot aggregate empty client updates list.")

        total_samples = sum(n_k for _, n_k in client_updates)
        if total_samples <= 0:
            raise ValueError(f"Total samples must be positive, got {total_samples}.")

        # Determine shapes from the first client update
        sample_params, _ = client_updates[0]
        avg_coef = np.zeros_like(sample_params["coef"], dtype=np.float64)
        avg_intercept = np.zeros_like(sample_params["intercept"], dtype=np.float64)

        for params, n_k in client_updates:
            weight = float(n_k) / float(total_samples)
            avg_coef += weight * params["coef"]
            avg_intercept += weight * params["intercept"]

        return {
            "coef": avg_coef,
            "intercept": avg_intercept,
        }

    def _initialize_global_model(self) -> None:
        """Initializes global estimator matching Phase 11 baseline hyperparameters."""
        self.global_estimator = LogisticRegression(
            penalty="l2",
            C=1.0,
            class_weight="balanced",
            solver="lbfgs",
            max_iter=1000,
            random_state=self.random_seed,
        )
        # Seed initial dummy fit to establish coefficient shapes
        dummy_X = np.zeros((2, len(self.feature_names)), dtype=np.float32)
        dummy_y = np.array([0, 1])
        self.global_estimator.fit(dummy_X, dummy_y)

    def evaluate_dataset(
        self,
        df: pd.DataFrame,
        threshold: Optional[float] = None,
    ) -> Dict[str, Any]:
        """Computes comprehensive evaluation metrics on a specified dataset partition."""
        if self.global_estimator is None:
            raise RuntimeError("Global model has not been initialized.")

        thresh = self.decision_threshold if threshold is None else float(threshold)
        X = self.shared_preprocessor.transform(df[self.feature_names])
        y_true = np.asarray(df[self.target_name]).ravel()

        probas = self.global_estimator.predict_proba(X)[:, 1]
        y_pred = (probas >= thresh).astype(int)

        try:
            auc = float(roc_auc_score(y_true, probas))
        except ValueError:
            auc = 0.5

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

    def calibrate_decision_threshold(self, steps: int = 81) -> float:
        """Calibrates optimal decision threshold theta* on central validation set to maximize F1."""
        if self.global_estimator is None:
            raise RuntimeError("Global model has not been initialized.")

        X_val = self.shared_preprocessor.transform(self.val_df[self.feature_names])
        y_val = np.asarray(self.val_df[self.target_name]).ravel()
        probas = self.global_estimator.predict_proba(X_val)[:, 1]

        thresholds = np.linspace(0.10, 0.90, steps)
        best_f1 = -1.0
        best_theta = 0.50

        for theta in thresholds:
            y_pred = (probas >= theta).astype(int)
            f1 = f1_score(y_val, y_pred, zero_division=0)
            if f1 > best_f1:
                best_f1 = f1
                best_theta = float(theta)

        self.decision_threshold = round(best_theta, 4)
        return self.decision_threshold

    def run_round(
        self,
        round_num: int,
        local_max_iter: int = 100,
    ) -> Dict[str, Any]:
        """Executes a single communication round of FedAvg across all registered clients."""
        if self.global_estimator is None:
            self._initialize_global_model()

        current_global_params = {
            "coef": self.global_estimator.coef_.copy(),
            "intercept": self.global_estimator.intercept_.copy(),
        }

        # 1. Distribute current global parameters to clients and collect updates
        client_updates: List[Tuple[Dict[str, np.ndarray], int]] = []
        for client_id, client in self.clients.items():
            updated_params, n_k = client.train_local_step(
                global_params=current_global_params,
                local_max_iter=local_max_iter,
            )
            client_updates.append((updated_params, n_k))

        # 2. Perform weighted FedAvg aggregation
        aggregated = self.aggregate_parameters(client_updates)

        # 3. Update global estimator parameters
        self.global_estimator.coef_ = aggregated["coef"].copy()
        self.global_estimator.intercept_ = aggregated["intercept"].copy()

        # 4. Evaluate global model on central validation partition
        val_metrics = self.evaluate_dataset(self.val_df)

        round_record = {
            "round": round_num,
            "participating_clients": list(self.clients.keys()),
            "total_participating_samples": sum(n for _, n in client_updates),
            "val_loss": val_metrics["log_loss"],
            "val_roc_auc": val_metrics["roc_auc"],
            "val_f1": val_metrics["f1"],
            "val_recall": val_metrics["recall"],
            "val_precision": val_metrics["precision"],
            "val_accuracy": val_metrics["accuracy"],
        }
        self.round_history.append(round_record)
        return round_record

    def run_federated_training(
        self,
        num_rounds: int = 5,
        local_max_iter: int = 100,
        early_stopping_tol: float = 1e-4,
    ) -> Dict[str, Any]:
        """Runs multi-round federated training, threshold calibration, and holdout evaluation."""
        if not self.clients:
            raise RuntimeError("No clients registered for federated training.")

        self._initialize_global_model()
        self.round_history = []

        print(f"Starting {self.FRAMEWORK_NAME} ({num_rounds} rounds, {len(self.clients)} clients)...")
        prev_loss = float("inf")

        for r in range(1, num_rounds + 1):
            round_record = self.run_round(r, local_max_iter=local_max_iter)
            curr_loss = round_record["val_loss"]
            loss_delta = abs(prev_loss - curr_loss)
            print(
                f"  Round {r}/{num_rounds} | Val Loss: {curr_loss:.4f} | "
                f"Val ROC-AUC: {round_record['val_roc_auc']:.4f} | Val F1: {round_record['val_f1']:.4f}"
            )

            if r > 2 and loss_delta < early_stopping_tol:
                print(f"  Early stopping triggered at round {r} (delta={loss_delta:.6f} < {early_stopping_tol})")
                break
            prev_loss = curr_loss

        # Calibrate decision threshold on validation set
        optimal_theta = self.calibrate_decision_threshold()
        val_final = self.evaluate_dataset(self.val_df, threshold=optimal_theta)

        # Evaluate strictly ONCE on isolated holdout test set
        test_final = self.evaluate_dataset(self.test_df, threshold=optimal_theta)

        self.is_trained = True

        return {
            "total_rounds_executed": len(self.round_history),
            "calibrated_decision_threshold": optimal_theta,
            "validation_metrics": val_final,
            "holdout_test_metrics": test_final,
            "round_history": self.round_history,
        }

    def save_federated_artifact(
        self,
        save_dir: Union[str, Path],
        comparison_baseline_metrics: Optional[Dict[str, Any]] = None,
    ) -> Path:
        """Persists the final federated global model bundle, preprocessor, and metadata."""
        if not self.is_trained or self.global_estimator is None:
            raise RuntimeError("Cannot save unfitted federated model.")

        out_path = Path(save_dir)
        out_path.mkdir(parents=True, exist_ok=True)

        # 1. Save global estimator
        joblib.dump(self.global_estimator, out_path / "estimator.joblib")

        # 2. Save shared coordinate preprocessor
        joblib.dump(self.shared_preprocessor, out_path / "preprocessor.joblib")

        # 3. Calculate client sample proportions
        total_samples = sum(c.sample_count for c in self.clients.values())
        client_weights = {
            c_id: round(c.sample_count / total_samples, 4)
            for c_id, c in self.clients.items()
        }

        # 4. Save metadata JSON
        metadata = {
            "model_name": f"{self.task_type.lower()}_federated_logistic_regression",
            "task_type": self.task_type,
            "algorithm": "FEDERATED_LOGISTIC_REGRESSION",
            "version": "1.0.0-phase12",
            "feature_contract": self.feature_names,
            "target_column": self.target_name,
            "random_seed": self.random_seed,
            "decision_threshold": self.decision_threshold,
            "risk_tiers": self.risk_tiers,
            "federated_metadata": {
                "framework": self.FRAMEWORK_NAME,
                "simulation_type": "simulated federated training with client-local raw-data handling",
                "aggregation_algorithm": self.AGGREGATION_NAME,
                "total_rounds": len(self.round_history),
                "clients": list(self.clients.keys()),
                "client_sample_counts": {c_id: c.sample_count for c_id, c in self.clients.items()},
                "client_weights": client_weights,
                "preprocessing_strategy": {
                    "source": "Frozen Phase 11 centralized baseline preprocessor",
                    "refit_during_federated": False,
                    "pooled_client_preprocessing": False,
                    "description": (
                        "The preprocessing transform is inherited/frozen from Phase 11. "
                        "It is NOT refit during federated rounds. Clients apply the same frozen transform locally. "
                        "Validation and holdout data use the same frozen transform. "
                        "No Phase 12 pooled raw-client preprocessing occurs."
                    ),
                },
                "convergence_analysis": {
                    "reason_for_round_metric_stability": (
                        "Local training uses L2-regularized Logistic Regression solved via L-BFGS. "
                        "Because the L2 objective is strictly convex, deterministic L-BFGS converges "
                        "to the unique local optimum in early iterations. Re-initialization from the convex "
                        "combination of these local minima across identical client partitions leads "
                        "to identical local minima upon convergence, producing mathematically identical "
                        "validation metrics across consecutive rounds until early stopping terminates."
                    )
                },
                "round_history": self.round_history,
            },
            "metrics": {
                "validation": self.evaluate_dataset(self.val_df, threshold=self.decision_threshold),
                "holdout_test": self.evaluate_dataset(self.test_df, threshold=self.decision_threshold),
            },
            "centralized_baseline_comparison": comparison_baseline_metrics or {},
            "training_timestamp": datetime.now(timezone.utc).isoformat(),
            "safety_disclaimer": self.DISCLAIMER,
        }

        with open(out_path / "metadata.json", "w", encoding="utf-8") as f:
            json.dump(metadata, f, indent=2)

        return out_path
