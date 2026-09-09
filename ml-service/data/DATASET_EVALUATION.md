# MediSphere ML Service — Clinical Dataset Provenance, Evaluation & Contracts

## 1. Executive Summary & Provenance Statement

In accordance with strict clinical AI governance and project data-integrity rules:
- **Zero synthetic rows** are utilized for production machine learning model training or baseline evaluation.
- **Zero synthetic target labels** are generated mathematically.
- **Zero clinical measurements** (`oxygenSaturation`, `creatinine`, `hemoglobin`, blood pressure, etc.) are fabricated to artificially pad feature vectors.
- Models consume **source-native clinical features** via explicit model-specific contracts.
- The previously committed synthetic reference files (`cardiovascular_reference.csv` and `diabetes_complications_reference.csv`) have been **relocated to `ml-service/data/fixtures/synthetic_pipeline_test_data/`** and are strictly designated as synthetic unit-test fixtures. They are excluded from the model training path.

---

## 2. Authentic Benchmark Specifications

### 2.1 Task 1: Cardiovascular Risk Prediction Model
* **Benchmark Cohort:** Framingham Heart Study Teaching / Public Research Cohort
* **Provenance & Source:** Publicly accessible teaching/research dataset source curated from the National Heart, Lung, and Blood Institute (NHLBI) longitudinal study; source and attribution documented via Duke University Department of Statistical Science ([`matackett/sta210/framingham.csv`](https://raw.githubusercontent.com/matackett/sta210/master/data/framingham.csv)).
* **Licensing & Usage Terms:** Publicly accessible teaching and research dataset source. Dedicated to open academic statistical research and education. Raw data files are kept local in `data/raw/` and excluded from Git commits.
* **Citation & Attribution:**
  > Framingham Heart Study, National Heart, Lung, and Blood Institute (NHLBI) and Boston University. Teaching dataset extraction distributed for academic biostatistical and machine learning instruction.
* **Exact Record Count:** **4,240 authentic patient records**
* **Target Label Definition:**
  * Column: `TenYearCHD`
  * Clinical Meaning: 10-year prospective incidence of coronary heart disease (myocardial infarction, angina pectoris, or coronary insufficiency).
  * Empirical Ground Truth:
    * Negative (`TenYearCHD = 0`): **3,596 patients** (84.81%)
    * Positive (`TenYearCHD = 1`): **644 patients** (15.19%)
* **Feature Scope:** Consumes exactly 8 native features. The 3 HealthTwin features not collected in this historical protocol (`oxygenSaturation`, `creatinine`, `hemoglobin`) are marked `UNAVAILABLE` and are **strictly excluded from the model sub-vector**.

---

### 2.2 Task 2: Diabetes Complications Prediction Model
* **Benchmark Cohort:** UCI Diabetes 130-US Hospitals (1999–2008) Inpatient Encounters
* **Provenance & Source:** Official UC Irvine Machine Learning Repository (Dataset ID: 296, donated by Strack et al., 2014; [`archive.ics.uci.edu`](https://archive.ics.uci.edu/static/public/296/diabetes+130-us+hospitals+for+years+1999-2008.zip)).
* **Licensing & Usage Terms:** Creative Commons Attribution 4.0 International (CC BY 4.0). Permitted for open research, evaluation, and educational use with appropriate attribution. Raw data files are kept local in `data/raw/` and excluded from Git commits.
* **Citation & Attribution:**
  > Beata Strack, Jonathan P. DeShazo, Chris Gennings, Juan L. Olmo, Sebastian Ventura, Krzysztof J. Cios, and John N. Clore, "Impact of HbA1c Measurement on Hospital Readmission Rates: Analysis of 70,000 Clinical Database Patient Records," BioMed Research International, vol. 2014, Article ID 781670, 2014.
* **Exact Record Count:** **101,766 authentic clinical inpatient encounter records** across 130 US medical centers.
* **Target Label Definition & Extraction:**
  * Column: `has_complication`
  * Clinical Meaning: Diagnosis of secondary diabetic microvascular or macrovascular organ complications documented in hospital discharge billing records (`diag_1`, `diag_2`, `diag_3`).
  * Extraction Rule:
    * **Diabetic Nephropathy:** ICD-9 `250.4x` (diabetes with renal manifestations), `585.x` (chronic kidney disease).
    * **Diabetic Retinopathy:** ICD-9 `250.5x` (diabetes with ophthalmic manifestations), `362.0x` (diabetic retinopathy).
    * **Diabetic Neuropathy:** ICD-9 `250.6x` (diabetes with neurological manifestations), `357.2` (diabetic polyneuropathy).
    * **Diabetic Peripheral Vascular Disease:** ICD-9 `250.7x` (diabetes with peripheral circulatory disorders), `443.81`/`443.9` (peripheral angiopathy).
  * Empirical Ground Truth:
    * Negative (`has_complication = 0`): **91,521 encounters** (89.93%)
    * Positive (`has_complication = 1`): **10,245 encounters** (10.07%)
* **Feature Scope:** Consumes 11 authentic encounter features. Vital signs (continuous blood pressure, continuous heart rate, SpO2) and continuous blood chemistries are unavailable in hospital encounter billing records and are **strictly not fabricated**.

---

## 3. Comprehensive Feature Mapping & Transparency Audit

The table below documents every feature across both authentic benchmarks against the MediSphere HealthTwin schema:

| Target Model | Source Dataset | Source Column | Model Feature | Classification | Transformation / Encoding | Missing-Value Handling |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Cardiovascular** | Framingham | `age` | `age` | **SOURCE-NATIVE** | Direct continuous years (32–70) | 0 missing |
| **Cardiovascular** | Framingham | `male` | `gender` | **SOURCE-NATIVE** | Binary float (1.0 = Male, 0.0 = Female) | 0 missing |
| **Cardiovascular** | Framingham | `BMI` | `bmi` | **SOURCE-NATIVE** | Continuous body mass index in $\text{kg/m}^2$ | 19 missing (0.45%); training-split median |
| **Cardiovascular** | Framingham | `sysBP` | `systolicBP` | **SOURCE-NATIVE** | Continuous resting systolic BP in mmHg | 0 missing |
| **Cardiovascular** | Framingham | `diaBP` | `diastolicBP` | **SOURCE-NATIVE** | Continuous resting diastolic BP in mmHg | 0 missing |
| **Cardiovascular** | Framingham | `heartRate` | `heartRate` | **SOURCE-NATIVE** | Continuous resting heart rate in bpm | 1 missing (0.02%); training-split median |
| **Cardiovascular** | Framingham | `glucose` | `glucose` | **SOURCE-NATIVE** | Continuous blood glucose in mg/dL | 388 missing (9.15%); training-split median |
| **Cardiovascular** | Framingham | `totChol` | `cholesterol` | **SOURCE-NATIVE** | Continuous total serum cholesterol in mg/dL | 50 missing (1.18%); training-split median |
| **Cardiovascular** | Framingham | *(None)* | `oxygenSaturation` | **UNAVAILABLE** | **Excluded from sub-vector** | Never fabricated; omitted from model contract |
| **Cardiovascular** | Framingham | *(None)* | `creatinine` | **UNAVAILABLE** | **Excluded from sub-vector** | Never fabricated; omitted from model contract |
| **Cardiovascular** | Framingham | *(None)* | `hemoglobin` | **UNAVAILABLE** | **Excluded from sub-vector** | Never fabricated; omitted from model contract |
| **Cardiovascular Target** | Framingham | `TenYearCHD` | `TenYearCHD` | **SOURCE-NATIVE** | Binary empirical target (1 = CHD event in 10 yrs) | 0 missing (644 pos / 3596 neg) |
| **Diabetes** | UCI 130-US | `age` | `age` | **DERIVED/ENCODED** | Decile midpoint (e.g. `[50-60)` $\rightarrow 55.0$) | Imputed to 55.0 if missing |
| **Diabetes** | UCI 130-US | `gender` | `gender` | **SOURCE-NATIVE** | Binary float (1.0 = Male, 0.0 = Female) | 3 missing; training-split median |
| **Diabetes** | UCI 130-US | `time_in_hospital` | `time_in_hospital` | **SOURCE-NATIVE** | Integer inpatient duration (1–14 days) | 0 missing |
| **Diabetes** | UCI 130-US | `num_lab_procedures` | `num_lab_procedures` | **SOURCE-NATIVE** | Integer count of lab diagnostic tests (1–132) | 0 missing |
| **Diabetes** | UCI 130-US | `num_procedures` | `num_procedures` | **SOURCE-NATIVE** | Integer count of interventional procedures (0–6) | 0 missing |
| **Diabetes** | UCI 130-US | `num_medications` | `num_medications` | **SOURCE-NATIVE** | Integer count of administered medications (1–81) | 0 missing |
| **Diabetes** | UCI 130-US | `number_diagnoses` | `number_diagnoses` | **SOURCE-NATIVE** | Integer count of recorded diagnoses (1–16) | 0 missing |
| **Diabetes** | UCI 130-US | `max_glu_serum` | `max_glu_serum` | **DERIVED/ENCODED** | Ordinal: None=0.0, Norm=1.0, >200=2.0, >300=3.0 | Mapped to 0.0 (unmeasured) if null |
| **Diabetes** | UCI 130-US | `A1Cresult` | `A1Cresult` | **DERIVED/ENCODED** | Ordinal: None=0.0, Norm=1.0, >7=2.0, >8=3.0 | Mapped to 0.0 (unmeasured) if null |
| **Diabetes** | UCI 130-US | `insulin` | `insulin` | **DERIVED/ENCODED** | Ordinal: No=0.0, Steady=1.0, Up=2.0, Down=3.0 | Mapped to 0.0 if missing |
| **Diabetes** | UCI 130-US | `diabetesMed` | `diabetesMed` | **DERIVED/ENCODED** | Binary: Yes=1.0, No=0.0 | Mapped to 0.0 if missing |
| **Diabetes** | UCI 130-US | *(None)* | `systolicBP` | **UNAVAILABLE** | **Excluded from encounter contract** | Never fabricated; omitted from model contract |
| **Diabetes** | UCI 130-US | *(None)* | `diastolicBP` | **UNAVAILABLE** | **Excluded from encounter contract** | Never fabricated; omitted from model contract |
| **Diabetes** | UCI 130-US | *(None)* | `heartRate` | **UNAVAILABLE** | **Excluded from encounter contract** | Never fabricated; omitted from model contract |
| **Diabetes** | UCI 130-US | *(None)* | `oxygenSaturation` | **UNAVAILABLE** | **Excluded from encounter contract** | Never fabricated; omitted from model contract |
| **Diabetes Target** | UCI 130-US | `diag_1/2/3` | `has_complication` | **DERIVED/ENCODED** | Binary indicator of ICD-9 complication code | 0 missing (10,245 pos / 91,521 neg) |

---

## 4. Synthetic Pipeline Test Fixtures

* **Location:** `ml-service/data/fixtures/synthetic_pipeline_test_data/`
* **Status:**
  > **Synthetic test fixture only. Not derived from UCI or Framingham patient records.**
  > **Do not use them for Phase 11 model training.**
* **Files:**
  * `cardiovascular_reference.csv` (400 synthetic rows)
  * `diabetes_complications_reference.csv` (500 synthetic rows)
  * `README.md` (explicit isolation disclaimer)
* **Usage:** Strictly reserved for unit tests verifying schema validators, JSON endpoint serialization, and mock interfaces.

---

## 5. Raw Data Storage & Ingestion Pipeline

* **Local Storage Directory:** `ml-service/data/raw/` (excluded from Git via root `.gitignore`).
* **Ingestion Script:** `ml-service/data/download_datasets.py`
  * Fetches authentic Framingham dataset (SHA-256: `2c0e57dc0361b420becf1facec0a054af06c420eae0ae2faf0fdc8591fadb018`).
  * Fetches authentic UCI Diabetes 130-US Hospitals dataset (SHA-256: `0689e7ec031237dc63031b938805c48377748761a3b26acab621567afa24df97`).
  * Verifies file headers and raises immediate errors on network or content corruption.
  * Never replaces missing files with synthetic data.
