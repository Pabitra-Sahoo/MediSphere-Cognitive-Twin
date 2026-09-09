"""MediSphere Global Explanation Generator

Generates and persists global_explanation.json for all 6 clinical risk models:
1. cardiovascular_logistic_regression (Phase 11 Centralized Baseline)
2. cardiovascular_hist_gradient_boosting (Phase 11 Centralized Benchmark)
3. cardiovascular_federated_logistic_regression (Phase 12 Federated Consensus)
4. diabetes_complications_logistic_regression (Phase 11 Centralized Baseline)
5. diabetes_complications_hist_gradient_boosting (Phase 11 Centralized Benchmark)
6. diabetes_complications_federated_logistic_regression (Phase 12 Federated Consensus)

Strict Discipline:
- Computed exclusively on the central validation cohort.
- Holdout test split is strictly isolated and never accessed for explanations.
- Private federated client partitions are never accessed.
"""

import os
import sys
import time
from pathlib import Path
from typing import Dict, Any

# Ensure root directory resolution
BASE_DIR = Path(__file__).resolve().parent.parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from config import (
    CARDIOVASCULAR_TARGET,
    DIABETES_TARGET,
    RANDOM_SEED,
    SAVED_MODELS_DIR,
)
from data.dataset_loader import DatasetLoader
from data.partitioner import DataPartitioner
from explainability.clinical_explainer import ClinicalExplainer


def generate_all_global_explanations() -> Dict[str, Any]:
    """Generates global_explanation.json artifacts for all 6 models."""
    print("=" * 70)
    print("MEDISPHERE PHASE 13: GENERATING SHAP GLOBAL EXPLANATIONS")
    print(f"Random Seed: {RANDOM_SEED}")
    print("=" * 70)

    partitioner = DataPartitioner(random_seed=RANDOM_SEED)

    # 1. Load authentic datasets & obtain validation cohorts
    print("\nLoading authentic datasets & extracting validation cohorts...")
    cvd_raw = DatasetLoader.load_cardiovascular_dataset()
    _, cvd_val_df, _ = partitioner.split_train_val_test(cvd_raw, target_col=CARDIOVASCULAR_TARGET)

    diab_raw = DatasetLoader.load_diabetes_complications_dataset()
    _, diab_val_df, _ = partitioner.split_train_val_test(diab_raw, target_col=DIABETES_TARGET)

    print(f"  Cardiovascular validation cohort: {len(cvd_val_df)} records")
    print(f"  Diabetes validation cohort: {len(diab_val_df)} records")

    models_to_explain = [
        ("cardiovascular_logistic_regression", cvd_val_df, "LinearExplainer"),
        ("cardiovascular_hist_gradient_boosting", cvd_val_df, "TreeExplainer"),
        ("cardiovascular_federated_logistic_regression", cvd_val_df, "LinearExplainer (FedAvg Global)"),
        ("diabetes_complications_logistic_regression", diab_val_df, "LinearExplainer"),
        ("diabetes_complications_hist_gradient_boosting", diab_val_df, "TreeExplainer"),
        ("diabetes_complications_federated_logistic_regression", diab_val_df, "LinearExplainer (FedAvg Global)"),
    ]

    results = {}

    for model_name, val_df, explainer_desc in models_to_explain:
        model_dir = SAVED_MODELS_DIR / model_name
        if not model_dir.exists():
            print(f"\n[SKIP] Directory not found: {model_dir}")
            continue

        print(f"\n--- Generating global explanation for: {model_name} [{explainer_desc}] ---")
        t0 = time.perf_counter()

        explainer = ClinicalExplainer.load(model_dir)
        out_path = explainer.save_global_explanation(
            save_dir=model_dir,
            val_df=val_df,
            max_samples=500,
        )
        duration = time.perf_counter() - t0

        global_res = explainer.compute_global_explanation(val_df, max_samples=500)
        results[model_name] = global_res
        top3 = global_res["global_feature_importance"][:3]
        top3_str = ", ".join(f"{item['feature_name']}: {item['mean_abs_shap_log_odds']}" for item in top3)

        print(f"  Saved to: {out_file if 'out_file' in locals() else out_path}")
        print(f"  Completed in {duration:.2f}s (Cohort size: {global_res['cohort_size']})")
        print(f"  Top 3 risk drivers: {top3_str}")

    print("\n" + "=" * 70)
    print("PHASE 13 GLOBAL EXPLANATIONS GENERATION COMPLETE (6/6)")
    print("=" * 70)
    return results


if __name__ == "__main__":
    generate_all_global_explanations()
