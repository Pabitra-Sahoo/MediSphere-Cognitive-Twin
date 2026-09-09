# MediSphere ML Service — Phase 10 Foundation

AI/ML and Federated Learning service foundation for the MediSphere Cognitive Twin platform.

> [!IMPORTANT]
> **Clinical Safety & Academic Disclaimer:**
> MediSphere ML service schemas, models, and outputs are developed exclusively for academic research, education, and algorithmic demonstration. They do **not** constitute a medical device, diagnosis, or clinical recommendation.

---

## 1. Overview & Phase 10 Scope

Phase 10 delivers the decoupled Python service foundation for Milestone 2:
- Containerized Linux-compatible environment (`python:3.11-slim`) with host virtualenv fallback.
- Standardized 11-feature clinical schema matching MediSphere's `HealthTwin`.
- Comparative dataset evaluation matrix and reference dataset extraction.
- Leakage-free preprocessing pipeline (median imputation and standard scaling).
- Deterministic data partitioner with 70/15/15 centralized splits and 3-client decentralized silos.
- FastAPI service with `/health` and `/api/ml/schema` endpoints.
- Fully automated test suite (`pytest`).

---

## 2. Canonical 11-Feature Clinical Schema

| Feature | Physiological Range | Standard Unit | Source Sub-Document |
| :--- | :--- | :--- | :--- |
| `age` | 0 – 125 | years | `demographics.age` |
| `gender` | 0 – 1 | encoded (1: Male, 0: Female) | `demographics.gender` |
| `bmi` | 10.0 – 75.0 | kg/m² | `demographics.bmi` |
| `systolicBP` | 60.0 – 250.0 | mmHg | `latestVitals.systolicBP` |
| `diastolicBP` | 30.0 – 150.0 | mmHg | `latestVitals.diastolicBP` |
| `heartRate` | 30.0 – 220.0 | bpm | `latestVitals.heartRate` |
| `oxygenSaturation` | 65.0 – 100.0 | % | `latestVitals.oxygenSaturation` |
| `glucose` | 30.0 – 600.0 | mg/dL | `latestLabs.glucose` |
| `cholesterol` | 50.0 – 600.0 | mg/dL | `latestLabs.cholesterol` |
| `creatinine` | 0.1 – 20.0 | mg/dL | `latestLabs.creatinine` |
| `hemoglobin` | 3.0 – 25.0 | g/dL | `latestLabs.hemoglobin` |

---

## 3. Dataset Evaluation & Selection Summary

Detailed comparative evaluations across candidate datasets are documented in [data/DATASET_EVALUATION.md](file:///c:/Users/sahoo/OneDrive/Desktop/MediSphere%20Cognitive%20Twin/ml-service/data/DATASET_EVALUATION.md).

* **Cardiovascular Risk Task:**
  * Selected: **Framingham Heart Study Teaching / Public Benchmark Dataset** (4,240 authentic records).
  * Target: `TenYearCHD = 1` indicates 10-year prospective incidence of coronary heart disease (644 positive events, 15.19% prevalence).
  * Model feature contract: 8 source-native continuous features (`age`, `gender`, `bmi`, `systolicBP`, `diastolicBP`, `heartRate`, `glucose`, `cholesterol`). Unavailable HealthTwin features (`oxygenSaturation`, `creatinine`, `hemoglobin`) are excluded from the model sub-vector and never fabricated.
* **Diabetes Complications Task:**
  * Selected: **UCI Diabetes 130-US Hospitals (1999–2008)** (101,766 authentic inpatient clinical encounters).
  * Target: `has_complication = 1` extracted from discharge ICD-9 diagnoses matching secondary diabetic microvascular/macrovascular damage (nephropathy, retinopathy, neuropathy, peripheral vascular disease; 10,245 positive encounters, 10.07% prevalence).
  * Model feature contract: 11 encounter features (`age`, `gender`, `time_in_hospital`, `num_lab_procedures`, `num_procedures`, `num_medications`, `number_diagnoses`, `max_glu_serum`, `A1Cresult`, `insulin`, `diabetesMed`).
* **Raw Data Download:**
  ```bash
  python data/download_datasets.py
  ```
  Downloads raw authentic files into `data/raw/` (ignored by Git).


---

## 4. Deterministic Partitioning & Privacy Rules

All data generation and partitioning enforce `RANDOM_SEED = 42`:

1. **Centralized Splits:**
   * **Holdout Test Set (15%):** Isolated immediately. Never accessible to federated clients.
   * **Validation Set (15%):** Used for global model validation across rounds.
   * **Training Set (70%):** Available for federated client distribution.
2. **Federated Client Silos:**
   * `hospital_alpha`: 40% of training data
   * `clinic_beta`: 35% of training data
   * `regional_gamma`: 25% of training data
   * Strictly non-overlapping: each training record belongs to exactly one client.
3. **Privacy Boundary:**
   * Raw patient data exists **client-side only**.
   * Feature vectors strictly exclude patient names, MRNs, phone numbers, or addresses.

---

## 5. Local Setup & Execution

### Option A: Running via Docker (Preferred)
```bash
# From repository root
docker compose up -d ml-service
```
Service runs at `http://localhost:8000`.

### Option B: Local Python Virtual Environment (Fallback)
```bash
# Windows
cd ml-service
scripts\setup_venv.bat

# Linux / macOS
cd ml-service
chmod +x scripts/setup_venv.sh
./scripts/setup_venv.sh
```

### Running Tests
```bash
cd ml-service
pytest -v
```

### Endpoints
* **Healthcheck:** `GET http://localhost:8000/health`
* **Schema Specification:** `GET http://localhost:8000/api/ml/schema`
* **Dataset Info:** `GET http://localhost:8000/api/ml/datasets`
* **Interactive Docs:** `GET http://localhost:8000/docs`
