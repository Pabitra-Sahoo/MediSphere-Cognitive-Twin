"""MediSphere Phase 12 Federated Learning / FedAvg Simulation Runner

Orchestrates decentralized training across independent client silos (hospital_alpha, clinic_beta, regional_gamma):
- Cardiovascular Risk: Authentic Framingham 8-feature contract (TenYearCHD target).
- Diabetes Complications: Authentic UCI 11-feature contract (has_complication target).

Discipline:
- 70% decentralized training, 15% central validation, 15% isolated holdout test.
- Simulated federated training with client-local raw-data handling.
- Preprocessing coordinates inherited as a frozen transform from Phase 11 (never refit on pooled client data in Phase 12).
- 5 rounds of weighted Federated Averaging (W_global = Σ (n_k / N) * W_k).
- Side-by-side comparison against Phase 11 centralized baselines.
- Persists federated model bundles under ml-service/models/saved/.
"""

import json
import os
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Tuple

import joblib
import pandas as pd

# Ensure root directory resolution
BASE_DIR = Path(__file__).resolve().parent.parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from config import (
    CARDIOVASCULAR_FEATURES,
    CARDIOVASCULAR_TARGET,
    DEFAULT_RISK_TIERS,
    DIABETES_FEATURES,
    DIABETES_TARGET,
    RANDOM_SEED,
    SAVED_MODELS_DIR,
)
from data.dataset_loader import DatasetLoader
from data.partitioner import DataPartitioner
from data.preprocessor import ClinicalPreprocessor
from federated.client import FederatedClient
from federated.coordinator import FedAvgCoordinator


def load_centralized_baseline_metrics(task: str) -> Dict[str, Any]:
    """Loads Phase 11 centralized baseline metrics for direct comparative analysis."""
    lr_meta_path = SAVED_MODELS_DIR / f"{task}_logistic_regression" / "metadata.json"
    hgb_meta_path = SAVED_MODELS_DIR / f"{task}_hist_gradient_boosting" / "metadata.json"

    comparison: Dict[str, Any] = {}

    if lr_meta_path.exists():
        with open(lr_meta_path, "r", encoding="utf-8") as f:
            meta = json.load(f)
            comparison["centralized_logistic_regression"] = meta.get("metrics", {}).get("holdout_test", {})

    if hgb_meta_path.exists():
        with open(hgb_meta_path, "r", encoding="utf-8") as f:
            meta = json.load(f)
            comparison["centralized_hist_gradient_boosting"] = meta.get("metrics", {}).get("holdout_test", {})

    return comparison


def train_federated_task(
    task_type: str,
    df: pd.DataFrame,
    feature_names: List[str],
    target_name: str,
    num_rounds: int = 5,
) -> Tuple[FedAvgCoordinator, Dict[str, Any]]:
    """Runs complete federated training workflow for a given clinical task."""
    print(f"\n{'=' * 70}")
    print(f"FEDERATED LEARNING / FEDAVG: {task_type.upper()}")
    print(f"{'=' * 70}")
    start_time = time.perf_counter()
    task_key = "cardiovascular" if task_type == "CARDIOVASCULAR" else "diabetes_complications"

    # 1. Deterministic Central Split: 70% Train, 15% Val, 15% Test
    partitioner = DataPartitioner(random_seed=RANDOM_SEED)
    train_df, val_df, test_df = partitioner.split_train_val_test(df, target_col=target_name)

    # 2. Partition training data into 3 client silos (hospital_alpha, clinic_beta, regional_gamma)
    client_cohorts = partitioner.partition_federated_clients(train_df)
    del train_df  # Drop pooled training reference; no client raw datasets are ever pooled or concatenated

    # 3. Inherit frozen clinical preprocessor transform directly from Phase 11 baseline artifact
    phase11_model_dir = SAVED_MODELS_DIR / f"{task_key}_logistic_regression"
    preprocessor_path = phase11_model_dir / "preprocessor.joblib"
    if not preprocessor_path.exists():
        raise FileNotFoundError(
            f"Phase 11 frozen preprocessor artifact not found at '{preprocessor_path}'. "
            "Phase 12 federated simulation requires the frozen Phase 11 preprocessor."
        )
    frozen_preprocessor = joblib.load(preprocessor_path)

    # 4. Initialize Coordinator with frozen preprocessor and isolated central val/test sets
    coordinator = FedAvgCoordinator(
        task_type=task_type,
        feature_names=feature_names,
        target_name=target_name,
        shared_preprocessor=frozen_preprocessor,
        val_df=val_df,
        test_df=test_df,
        risk_tiers=DEFAULT_RISK_TIERS,
        random_seed=RANDOM_SEED,
    )

    # 5. Initialize and register client silos with their private local slices and frozen preprocessor
    for client_id, c_df in client_cohorts.items():
        client = FederatedClient(
            client_id=client_id,
            local_df=c_df,
            feature_names=feature_names,
            target_name=target_name,
            preprocessor=frozen_preprocessor,
            random_seed=RANDOM_SEED,
        )
        coordinator.register_client(client)
        dist = client.get_class_distribution()
        print(f"  Client [{client_id}]: {client.sample_count} samples (Neg: {dist['negative']}, Pos: {dist['positive']})")

    # 6. Execute multi-round FedAvg
    training_results = coordinator.run_federated_training(num_rounds=num_rounds)
    duration = time.perf_counter() - start_time

    # 7. Compare with Phase 11 Centralized Baseline
    task_key = "cardiovascular" if task_type == "CARDIOVASCULAR" else "diabetes_complications"
    baseline_comp = load_centralized_baseline_metrics(task_key)

    fed_test_metrics = training_results["holdout_test_metrics"]
    lr_base = baseline_comp.get("centralized_logistic_regression", {})
    delta_auc = fed_test_metrics["roc_auc"] - lr_base.get("roc_auc", 0.0)

    comparison_summary = {
        "centralized_baseline_holdout": lr_base,
        "federated_holdout": fed_test_metrics,
        "delta_roc_auc": round(delta_auc, 4),
        "non_linear_benchmark_holdout": baseline_comp.get("centralized_hist_gradient_boosting", {}),
    }

    # 8. Save Artifact Bundle
    save_dir = SAVED_MODELS_DIR / f"{task_key}_federated_logistic_regression"
    coordinator.save_federated_artifact(
        save_dir=save_dir,
        comparison_baseline_metrics=comparison_summary,
    )

    print(f"\n  Training Duration: {duration:.2f}s across {len(coordinator.round_history)} rounds")
    print(f"  Calibrated Decision Threshold (theta*): {training_results['calibrated_decision_threshold']}")
    print(f"  Holdout Test ROC-AUC (Federated): {fed_test_metrics['roc_auc']:.4f}")
    if lr_base:
        print(f"  Holdout Test ROC-AUC (Centralized Baseline): {lr_base.get('roc_auc', 0.0):.4f}")
        print(f"  Delta ROC-AUC (FedAvg vs. Centralized): {delta_auc:+.4f}")
    print(f"  Artifact Directory: {save_dir}")

    return coordinator, comparison_summary


def run_all_federated_training() -> Dict[str, Any]:
    """Executes end-to-end federated learning simulations across all clinical tasks."""
    print("=" * 70)
    print("MEDISPHERE PHASE 12: FEDERATED LEARNING / FEDAVG SIMULATION")
    print(f"Timestamp: {datetime.now(timezone.utc).isoformat()}")
    print(f"Random Seed: {RANDOM_SEED}")
    print("=" * 70)

    # 1. Load authentic datasets
    cvd_df = DatasetLoader.load_cardiovascular_dataset()
    diab_df = DatasetLoader.load_diabetes_complications_dataset()

    # 2. Train Cardiovascular Federated Model
    cvd_coord, cvd_comp = train_federated_task(
        task_type="CARDIOVASCULAR",
        df=cvd_df,
        feature_names=CARDIOVASCULAR_FEATURES,
        target_name=CARDIOVASCULAR_TARGET,
        num_rounds=5,
    )

    # 3. Train Diabetes Complications Federated Model
    diab_coord, diab_comp = train_federated_task(
        task_type="DIABETES",
        df=diab_df,
        feature_names=DIABETES_FEATURES,
        target_name=DIABETES_TARGET,
        num_rounds=5,
    )

    print(f"\n{'=' * 70}")
    print("PHASE 12 FEDERATED TRAINING COMPLETE")
    print("=" * 70)

    return {
        "cardiovascular": cvd_comp,
        "diabetes": diab_comp,
    }


if __name__ == "__main__":
    run_all_federated_training()
