"""Tests for DataPartitioner and federated client partitioning"""

import pytest
import pandas as pd

from data.partitioner import DataPartitioner
from data.dataset_loader import DatasetLoader
from config import CLIENT_IDS, CLIENT_SPLIT_RATIOS, CARDIOVASCULAR_TARGET, DIABETES_TARGET


def test_train_val_test_ratios_and_stratification():
    """Verifies that centralized splits preserve 70/15/15 ratios and class balance."""
    df = DatasetLoader.load_cardiovascular_dataset()
    partitioner = DataPartitioner(random_seed=42)

    train_df, val_df, test_df = partitioner.split_train_val_test(df, target_col=CARDIOVASCULAR_TARGET)

    total_len = len(df)
    assert len(train_df) + len(val_df) + len(test_df) == total_len

    # Expected proportions ~70%, ~15%, ~15%
    assert abs(len(train_df) / total_len - 0.70) < 0.02
    assert abs(len(val_df) / total_len - 0.15) < 0.02
    assert abs(len(test_df) / total_len - 0.15) < 0.02

    # Verify stratified target proportions
    global_pos_rate = df[CARDIOVASCULAR_TARGET].mean()
    train_pos_rate = train_df[CARDIOVASCULAR_TARGET].mean()
    val_pos_rate = val_df[CARDIOVASCULAR_TARGET].mean()
    test_pos_rate = test_df[CARDIOVASCULAR_TARGET].mean()

    assert abs(train_pos_rate - global_pos_rate) < 0.05
    assert abs(val_pos_rate - global_pos_rate) < 0.05
    assert abs(test_pos_rate - global_pos_rate) < 0.05


def test_federated_client_partitioning_disjointness():
    """Verifies that federated client cohorts are non-empty, non-overlapping, and sum to train size."""
    df = DatasetLoader.load_cardiovascular_dataset()
    partitioner = DataPartitioner(random_seed=42)

    train_df, val_df, test_df = partitioner.split_train_val_test(df, target_col=CARDIOVASCULAR_TARGET)
    clients = partitioner.partition_federated_clients(train_df)

    assert set(clients.keys()) == set(CLIENT_IDS)

    # Check that client sum equals training count exactly
    client_total = sum(len(c_df) for c_df in clients.values())
    assert client_total == len(train_df)

    # Verification function checks non-emptiness and integrity
    verification = partitioner.verify_partitions(clients, val_df, test_df)
    assert verification["zeroTestLeakageVerified"] is True
    assert verification["nonOverlappingClientsVerified"] is True


def test_deterministic_reproducibility():
    """Verifies that two runs with RANDOM_SEED=42 produce identical partitions."""
    df = DatasetLoader.load_diabetes_complications_dataset()

    p1 = DataPartitioner(random_seed=42)
    train1, val1, test1 = p1.split_train_val_test(df, target_col="has_complication")
    clients1 = p1.partition_federated_clients(train1)

    p2 = DataPartitioner(random_seed=42)
    train2, val2, test2 = p2.split_train_val_test(df, target_col="has_complication")
    clients2 = p2.partition_federated_clients(train2)

    pd.testing.assert_frame_equal(train1, train2)
    pd.testing.assert_frame_equal(val1, val2)
    pd.testing.assert_frame_equal(test1, test2)
    for c_id in CLIENT_IDS:
        pd.testing.assert_frame_equal(clients1[c_id], clients2[c_id])
