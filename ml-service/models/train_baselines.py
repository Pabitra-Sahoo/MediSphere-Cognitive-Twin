"""MediSphere Phase 11 Baseline Model Training & Evaluation Runner

Executes reproducible, deterministic training and evaluation of baseline clinical models:
1. Cardiovascular 10-Year Risk (Framingham authentic teaching cohort)
   - Logistic Regression (L2 / Ridge)
   - HistGradientBoostingClassifier
2. Diabetes Secondary Complications (UCI Diabetes 130-US Hospitals cohort)
   - Logistic Regression (L2 / Ridge)
   - HistGradientBoostingClassifier

Discipline:
- 70% Train, 15% Validation, 15% Holdout Test (stratified, RANDOM_SEED=42).
- Preprocessing parameters fit exclusively on training partition (zero leakage).
- Hyperparameters & decision threshold (theta*) selected on validation partition.
- Holdout test evaluated strictly ONCE for unbiased generalization measurement.
- Decision threshold stored separately from categorical display risk tiers.
- Serialized bundles saved under ml-service/models/saved/<model_name>/.
"""

import os
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Tuple

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
from models.cardiovascular_model import CardiovascularBaselineModel
from models.diabetes_model import DiabetesComplicationsBaselineModel


def format_duration(seconds: float) -> str:
    """Formats runtime seconds into readable string."""
    if seconds < 1.0:
        return f"{seconds * 1000:.1f}ms"
    return f"{seconds:.2f}s"


def get_dir_size_kb(directory: Path) -> float:
    """Computes total size of files in directory in kilobytes."""
    total_bytes = sum(f.stat().st_size for f in directory.glob("**/*") if f.is_file())
    return round(total_bytes / 1024.0, 2)


def train_and_evaluate_cardiovascular(
    df: pd.DataFrame,
    algorithm: str,
    hyperparameters: Dict[str, Any],
) -> Tuple[CardiovascularBaselineModel, Dict[str, Any]]:
    """Trains and validates a Cardiovascular baseline model."""
    print(f"\n--- Training Cardiovascular Risk [{algorithm}] ---")
    start_time = time.perf_counter()

    partitioner = DataPartitioner(random_seed=RANDOM_SEED)
    train_df, val_df, test_df = partitioner.split_train_val_test(df, target_col=CARDIOVASCULAR_TARGET)

    X_train = train_df[CARDIOVASCULAR_FEATURES]
    y_train = train_df[CARDIOVASCULAR_TARGET]
    X_val = val_df[CARDIOVASCULAR_FEATURES]
    y_val = val_df[CARDIOVASCULAR_TARGET]
    X_test = test_df[CARDIOVASCULAR_FEATURES]
    y_test = test_df[CARDIOVASCULAR_TARGET]

    # Initialize model
    model = CardiovascularBaselineModel(
        algorithm=algorithm,
        hyperparameters=hyperparameters,
        risk_tiers=DEFAULT_RISK_TIERS,
        random_seed=RANDOM_SEED,
    )

    # Fit strictly on train split
    model.fit(X_train, y_train)

    # Calibrate decision threshold on validation split (maximize F1)
    optimal_threshold = model.calibrate_decision_threshold(X_val, y_val, metric="f1")

    # Evaluate validation metrics at calibrated threshold
    val_metrics = model.evaluate(X_val, y_val, threshold=optimal_threshold)

    # Evaluate holdout test split ONCE
    test_metrics = model.evaluate(X_test, y_test, threshold=optimal_threshold)

    duration = time.perf_counter() - start_time

    # Persist artifact bundle
    save_dir = SAVED_MODELS_DIR / model.model_name
    extra_metadata = {
        "dataset_metadata": {
            "source": "Framingham Heart Study Teaching Cohort",
            "total_records": len(df),
            "train_records": len(train_df),
            "val_records": len(val_df),
            "test_records": len(test_df),
            "positive_prevalence": round(float(df[CARDIOVASCULAR_TARGET].mean()), 4),
        },
        "hyperparameters": hyperparameters,
        "metrics": {
            "validation": val_metrics,
            "holdout_test": test_metrics,
        },
        "training_duration_seconds": round(duration, 4),
    }
    model.save(save_dir, metadata_extra=extra_metadata)
    artifact_size_kb = get_dir_size_kb(save_dir)

    print(f"  Training Duration: {format_duration(duration)}")
    print(f"  Calibrated Decision Threshold (theta*): {optimal_threshold}")
    print(f"  Risk Tier Display Thresholds: LOW (< {DEFAULT_RISK_TIERS['low_max']}), "
          f"MODERATE ({DEFAULT_RISK_TIERS['low_max']} - {DEFAULT_RISK_TIERS['moderate_max']}), "
          f"HIGH (>= {DEFAULT_RISK_TIERS['moderate_max']})")
    print(f"  Validation ROC-AUC: {val_metrics['roc_auc']:.4f} | F1: {val_metrics['f1']:.4f} | "
          f"Rec: {val_metrics['recall']:.4f} | Prec: {val_metrics['precision']:.4f}")
    print(f"  Holdout Test ROC-AUC: {test_metrics['roc_auc']:.4f} | F1: {test_metrics['f1']:.4f} | "
          f"Rec: {test_metrics['recall']:.4f} | Prec: {test_metrics['precision']:.4f}")
    print(f"  Artifact Directory: {save_dir} ({artifact_size_kb} KB)")

    return model, extra_metadata


def train_and_evaluate_diabetes(
    df: pd.DataFrame,
    algorithm: str,
    hyperparameters: Dict[str, Any],
) -> Tuple[DiabetesComplicationsBaselineModel, Dict[str, Any]]:
    """Trains and validates a Diabetes Complications baseline model."""
    print(f"\n--- Training Diabetes Complications [{algorithm}] ---")
    start_time = time.perf_counter()

    partitioner = DataPartitioner(random_seed=RANDOM_SEED)
    train_df, val_df, test_df = partitioner.split_train_val_test(df, target_col=DIABETES_TARGET)

    X_train = train_df[DIABETES_FEATURES]
    y_train = train_df[DIABETES_TARGET]
    X_val = val_df[DIABETES_FEATURES]
    y_val = val_df[DIABETES_TARGET]
    X_test = test_df[DIABETES_FEATURES]
    y_test = test_df[DIABETES_TARGET]

    # Initialize model
    model = DiabetesComplicationsBaselineModel(
        algorithm=algorithm,
        hyperparameters=hyperparameters,
        risk_tiers=DEFAULT_RISK_TIERS,
        random_seed=RANDOM_SEED,
    )

    # Fit strictly on train split
    model.fit(X_train, y_train)

    # Calibrate decision threshold on validation split (maximize F1)
    optimal_threshold = model.calibrate_decision_threshold(X_val, y_val, metric="f1")

    # Evaluate validation metrics at calibrated threshold
    val_metrics = model.evaluate(X_val, y_val, threshold=optimal_threshold)

    # Evaluate holdout test split ONCE
    test_metrics = model.evaluate(X_test, y_test, threshold=optimal_threshold)

    duration = time.perf_counter() - start_time

    # Persist artifact bundle
    save_dir = SAVED_MODELS_DIR / model.model_name
    extra_metadata = {
        "dataset_metadata": {
            "source": "UCI Diabetes 130-US Hospitals (1999-2008)",
            "total_records": len(df),
            "train_records": len(train_df),
            "val_records": len(val_df),
            "test_records": len(test_df),
            "positive_prevalence": round(float(df[DIABETES_TARGET].mean()), 4),
        },
        "hyperparameters": hyperparameters,
        "metrics": {
            "validation": val_metrics,
            "holdout_test": test_metrics,
        },
        "training_duration_seconds": round(duration, 4),
    }
    model.save(save_dir, metadata_extra=extra_metadata)
    artifact_size_kb = get_dir_size_kb(save_dir)

    print(f"  Training Duration: {format_duration(duration)}")
    print(f"  Calibrated Decision Threshold (theta*): {optimal_threshold}")
    print(f"  Risk Tier Display Thresholds: LOW (< {DEFAULT_RISK_TIERS['low_max']}), "
          f"MODERATE ({DEFAULT_RISK_TIERS['low_max']} - {DEFAULT_RISK_TIERS['moderate_max']}), "
          f"HIGH (>= {DEFAULT_RISK_TIERS['moderate_max']})")
    print(f"  Validation ROC-AUC: {val_metrics['roc_auc']:.4f} | F1: {val_metrics['f1']:.4f} | "
          f"Rec: {val_metrics['recall']:.4f} | Prec: {val_metrics['precision']:.4f}")
    print(f"  Holdout Test ROC-AUC: {test_metrics['roc_auc']:.4f} | F1: {test_metrics['f1']:.4f} | "
          f"Rec: {test_metrics['recall']:.4f} | Prec: {test_metrics['precision']:.4f}")
    print(f"  Artifact Directory: {save_dir} ({artifact_size_kb} KB)")

    return model, extra_metadata


def run_all_baseline_training() -> Dict[str, Any]:
    """Runs end-to-end baseline model training across all tasks and candidate architectures."""
    print("=" * 70)
    print("MEDISPHERE PHASE 11: CLINICAL BASELINE MODEL TRAINING")
    print(f"Timestamp: {datetime.now(timezone.utc).isoformat()}")
    print(f"Random Seed: {RANDOM_SEED}")
    print("=" * 70)

    # 1. Load authentic datasets
    print("\nLoading authentic datasets...")
    cvd_df = DatasetLoader.load_cardiovascular_dataset()
    print(f"Cardiovascular records loaded: {len(cvd_df)} (Target prevalence: {cvd_df[CARDIOVASCULAR_TARGET].mean():.2%})")

    diab_df = DatasetLoader.load_diabetes_complications_dataset()
    print(f"Diabetes encounter records loaded: {len(diab_df)} (Target prevalence: {diab_df[DIABETES_TARGET].mean():.2%})")

    results: Dict[str, Any] = {}

    # 2. Train Cardiovascular Baselines
    # Model 1A: Logistic Regression
    _, cvd_lr_meta = train_and_evaluate_cardiovascular(
        cvd_df,
        algorithm="LOGISTIC_REGRESSION",
        hyperparameters={"C": 1.0, "penalty": "l2"},
    )
    results["cardiovascular_logistic_regression"] = cvd_lr_meta

    # Model 1B: HistGradientBoosting
    _, cvd_hgb_meta = train_and_evaluate_cardiovascular(
        cvd_df,
        algorithm="HIST_GRADIENT_BOOSTING",
        hyperparameters={"learning_rate": 0.05, "max_iter": 100, "min_samples_leaf": 20},
    )
    results["cardiovascular_hist_gradient_boosting"] = cvd_hgb_meta

    # 3. Train Diabetes Complications Baselines
    # Model 2A: Logistic Regression
    _, diab_lr_meta = train_and_evaluate_diabetes(
        diab_df,
        algorithm="LOGISTIC_REGRESSION",
        hyperparameters={"C": 1.0, "penalty": "l2"},
    )
    results["diabetes_logistic_regression"] = diab_lr_meta

    # Model 2B: HistGradientBoosting
    _, diab_hgb_meta = train_and_evaluate_diabetes(
        diab_df,
        algorithm="HIST_GRADIENT_BOOSTING",
        hyperparameters={"learning_rate": 0.05, "max_iter": 100, "min_samples_leaf": 30},
    )
    results["diabetes_hist_gradient_boosting"] = diab_hgb_meta

    print("\n" + "=" * 70)
    print("PHASE 11 BASELINE TRAINING COMPLETE")
    print("=" * 70)
    return results


if __name__ == "__main__":
    run_all_baseline_training()
