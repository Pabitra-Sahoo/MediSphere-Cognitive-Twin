# Synthetic Pipeline Test Fixtures

> [!WARNING]
> **Synthetic test fixture only. Not derived from UCI or Framingham patient records.**
> **Do not use them for Phase 11 model training.**

## Purpose
The CSV files in this directory:
- `cardiovascular_reference.csv`
- `diabetes_complications_reference.csv`

were deterministically generated in Phase 10 via parametric NumPy distributions (`generate_reference_datasets.py`) strictly as lightweight, non-sensitive fixtures for unit testing schema validation, preprocessor mechanics, and fast integration testing.

These files contain **no empirical clinical measurements** and **no real patient outcome labels**. They are excluded from all production ML model training and baseline evaluation paths. Production model training strictly uses authentic public benchmarks located in `ml-service/data/raw/`.
