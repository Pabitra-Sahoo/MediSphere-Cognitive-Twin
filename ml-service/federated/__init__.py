"""MediSphere Federated Learning Simulation Package

Implements decentralized clinical risk modeling using standard Federated Averaging (FedAvg):
- FederatedClient: Simulates independent healthcare facilities with private local records.
- FedAvgCoordinator: Orchestrates parameter broadcast, weighted averaging, and global validation.
"""

from federated.client import FederatedClient
from federated.coordinator import FedAvgCoordinator

__all__ = ["FederatedClient", "FedAvgCoordinator"]
