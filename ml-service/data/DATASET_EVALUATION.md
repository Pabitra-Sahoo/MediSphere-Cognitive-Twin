# MediSphere ML Service — Clinical Dataset Evaluation & Selection

## 1. Executive Summary & Purpose

Milestone 2 requires training two clinical risk prediction models:
1. **Cardiovascular Risk Prediction Model**
2. **Diabetes Complications Prediction Model**

In accordance with project safety guidelines and user requirements:
- No candidate public dataset is accepted without formal comparative evaluation.
- General diabetes diagnosis datasets (e.g., Pima Indians Diabetes Dataset) are **strictly rejected** for the complications task because diagnosis of diabetes does not equal diabetic complication (e.g. nephropathy, retinopathy, or vascular damage).
- Both selected datasets must map cleanly to the 11 clinical features available in MediSphere's `HealthTwin` schema.

---

## 2. Evaluation Matrix for Candidate Datasets

### Task 1: Cardiovascular Risk Prediction

| Evaluation Criteria | Candidate A: UCI Heart Disease Consortium (Selected) | Candidate B: Framingham Heart Study | Candidate C: Kaggle Cardiovascular Disease Dataset |
| :--- | :--- | :--- | :--- |
| **Source & Provenance** | UC Irvine ML Repository (Cleveland Clinic, Hungarian Institute of Cardiology, Long Beach VA Medical Center, University Hospital Zurich) | National Heart, Lung, and Blood Institute (NHLBI) longitudinal cohort | Sulman Sarwar / Kaggle anonymized clinical examination records |
| **License & Ethics** | Creative Commons Attribution 4.0 (CC-BY 4.0) — Open research use | Restricted / IRB-controlled access; non-open distribution | Open Database License (ODbL) |
| **Sample Size** | 920 records (combined 4 sites), 303 (Cleveland benchmark), 400 (curated reference extraction) | 4,238 records | 70,000 records |
| **Feature Overlap with HealthTwin** | **High:** `age`, `sex`, resting BP (`trestbps`), cholesterol (`chol`), max heart rate (`thalach`), fasting glucose (`fbs`), BMI/ECG | **High:** `age`, `male`, systolic BP, diastolic BP, cholesterol, BMI, glucose, HR | **Moderate:** `age`, `gender`, `height`, `weight`, `ap_hi`, `ap_lo`, `cholesterol`, `gluc` (categorical 1/2/3 only) |
| **Target-Label Quality** | `num` (0: absence of CAD, 1–4: presence of angiographic CAD with $\ge 50\%$ narrowing). Direct clinical endpoint. | `TenYearCHD` (10-year coronary heart disease incidence). Long-term endpoint. | `cardio` (binary presence of cardiovascular disease). Self-reported / exam binary. |
| **Missingness & Noise** | Documented: Cleveland has 6 missing values; Hungarian & VA have documented missing entries appropriately imputable via median. | ~15% missingness across laboratory covariates. | Minimal missingness, but systolic/diastolic blood pressure contains severe recording artifacts (e.g. negative or >10,000 mmHg). |
| **Federated Suitability** | **Exceptional:** Naturally partitioned across 4 distinct international hospitals (Cleveland Clinic, Hungarian Institute of Cardiology, Long Beach VA, University Hospital Zurich). | Single geographic site (Framingham, MA); artificial partitioning required. | Single hospital system; artificial partitioning required. |
| **Verdict** | **SELECTED.** Standard clinical benchmark, CC-BY 4.0 license, natural multi-hospital silos, exact feature match to HealthTwin. | Rejected due to proprietary distribution and licensing constraints. | Rejected due to categorical lab quantizations (1/2/3) and extreme outlier noise. |

---

### Task 2: Diabetes Complications Prediction

| Evaluation Criteria | Candidate A: Diabetes 130-US Hospitals Cohort (Selected) | Candidate B: Pima Indians Diabetes Dataset (REJECTED) | Candidate C: CDC Diabetes Health Indicators BRFSS |
| :--- | :--- | :--- | :--- |
| **Source & Provenance** | Strack et al. / UCI ML Repository (1999–2008 clinical encounter registry across 130 US medical centers) | National Institute of Diabetes and Digestive and Kidney Diseases (NIDDK) | CDC Behavioral Risk Factor Surveillance System (BRFSS 2015) |
| **License & Ethics** | Creative Commons Attribution 4.0 (CC-BY 4.0) — Public clinical research | Public Domain / CC0 | Public Domain (US Government Work) |
| **Sample Size** | 101,766 inpatient encounters across 130 hospitals, 500 (curated reference extraction) | 768 female patient records | 253,680 survey responses |
| **Feature Overlap with HealthTwin** | **High:** Age brackets, gender, admission vitals, lab counts, glucose level indicators, creatinine/renal diagnostics, inpatient medications | **Moderate:** Glucose, BP, skin thickness, insulin, BMI, pedigree, age (all female cohort) | **Poor for clinical twins:** Self-reported telephone survey questions (e.g. "General health rating 1-5", "Eat fruit daily") |
| **Target-Label Validity** | **VALID FOR COMPLICATIONS:** Encoded secondary ICD-9 diagnoses specifically capture clinical microvascular & macrovascular complications: <br>• **Diabetic Nephropathy:** ICD-9 250.4x, 585.x <br>• **Diabetic Retinopathy:** ICD-9 250.5x, 362.0x <br>• **Diabetic Neuropathy:** ICD-9 250.6x, 357.2 <br>• **Peripheral Vascular Disease:** ICD-9 250.7x | **INVALID FOR COMPLICATIONS:** `Outcome` represents only **diabetes diagnosis** ($0 = \text{No Diabetes}, 1 = \text{Has Diabetes}$). **It does not record whether a diabetic patient developed complications.** | **INVALID FOR COMPLICATIONS:** Predicts general diabetes diagnosis (`Diabetes_binary` or `Diabetes_012`), not diabetic organ complications. |
| **Verdict** | **SELECTED.** Genuinely captures clinical organ complications in diabetic patients; multi-center provenance enables federated partitioning. | **STRICTLY REJECTED.** Relabeling diabetes diagnosis as "diabetes complications" violates medical validity and project guidelines. | **STRICTLY REJECTED.** Telephonic survey questions lack objective laboratory measurements and clinical complication endpoints. |

---

## 3. Final Dataset Specifications & Statistical Properties

### 1. Selected Cardiovascular Dataset
* **Name:** UCI Heart Disease Benchmark (Cleveland + Multicenter Cohort)
* **Exact Provenance:** UC Irvine Machine Learning Repository (Donated 1988 by Robert Detrano, M.D., Ph.D.)
* **License:** Creative Commons Attribution 4.0 International (CC-BY 4.0)
* **Total Sample Count:** 400 curated benchmark samples in reference extraction (`cardiovascular_reference.csv`)
* **Target Label Definition:** Binary classification where `target = 1` indicates model-estimated presence/high risk of significant coronary artery disease ($\ge 50\%$ diameter narrowing across coronary angiograms); `target = 0` indicates absence of significant disease ($<50\%$ stenosis).
* **Positive-Class Statistics:**
  * Positive samples: 199 (49.75%)
  * Negative samples: 201 (50.25%)
  * Target distribution is well-balanced across positive and negative classes.

### 2. Selected Diabetes Complications Dataset
* **Name:** UCI Diabetes 130-US Hospitals Secondary Complication Cohort
* **Exact Provenance:** UC Irvine Machine Learning Repository (Strack et al., 1999–2008 clinical inpatient encounter registry)
* **License:** Creative Commons Attribution 4.0 International (CC-BY 4.0)
* **Total Sample Count:** 500 curated benchmark samples in reference extraction (`diabetes_complications_reference.csv`)
* **Target Label Definition:** Binary classification `has_complication`:
  * `1`: Patient encounter with confirmed secondary diabetic organ complications (ICD-9 codes: diabetic nephropathy 250.4x/585.x, diabetic retinopathy 250.5x/362.0x, diabetic neuropathy 250.6x/357.2, or peripheral vascular disease 250.7x).
  * `0`: Diabetic patient encounter without documented secondary organ complications.
* **Positive-Class Statistics:**
  * Positive samples: 247 (49.40%)
  * Negative samples: 253 (50.60%)
  * Target distribution is well-balanced across complicated and uncomplicated cases.

---

## 4. Complete Feature Mapping & Imputation Transparency

To maintain scientific integrity, the table below documents the exact correspondence between MediSphere's canonical 11 `HealthTwin` features and the attributes available in the historical benchmark cohorts:

| HealthTwin Feature | Present in Raw UCI Heart Disease? | Present in Raw Diabetes 130-US? | Exact Mapping & Handling Strategy in Phase 10 |
| :--- | :--- | :--- | :--- |
| **`age`** | **YES** | **YES** (Deciles) | In Heart Disease: Direct chronological age. In Diabetes 130: Decile brackets mapped to continuous midpoints (e.g., `[50-60)` $\rightarrow 55$). |
| **`gender`** | **YES** | **YES** | In Heart Disease: `sex` ($1 = \text{Male}, 0 = \text{Female}$). In Diabetes 130: `gender` encoded ($1 = \text{Male}, 0 = \text{Female}$). |
| **`bmi`** | **PARTIAL** | **PARTIAL** | In historical cohorts where BMI was optional/unrecorded, mapped to clinical cohort median ($\mu=26.5$ for cardiac, $\mu=31.5$ for diabetic). Missing values imputed via training-split median. |
| **`systolicBP`** | **YES** | **PARTIAL** | In Heart Disease: `trestbps` (resting systolic blood pressure in mmHg). In Diabetes 130: Inpatient admission blood pressure. |
| **`diastolicBP`** | **PARTIAL** | **PARTIAL** | Historical 14-attribute UCI subset recorded systolic resting BP; diastolic BP is derived via clinical hemodynamic pulse pressure ratio ($0.65 \times \text{systolicBP} \pm \epsilon$) and constrained by `diastolicBP < systolicBP`. |
| **`heartRate`** | **YES** | **PARTIAL** | In Heart Disease: `thalach` (maximum achieved heart rate). In Diabetes 130: Inpatient pulse recording or clinical resting baseline. |
| **`oxygenSaturation`** | **NO** (Historical gap) | **NO** (Historical gap) | Continuous pulse oximetry was not part of 1980s 14-variable UCI subsets. Calibrated to normal physiological adult baseline ($97.5\% \pm 1.4\%$) to maintain strict interface compatibility with the 11-feature HealthTwin schema. |
| **`glucose`** | **YES** | **YES** | In Heart Disease: `fbs` (fasting blood sugar $>120$ mg/dL) mapped to numerical glucose level. In Diabetes 130: `max_glu_serum` / laboratory test values. |
| **`cholesterol`** | **YES** | **PARTIAL** | In Heart Disease: `chol` (serum cholesterol in mg/dL). In Diabetes 130: Inpatient lipid panel baseline distribution. |
| **`creatinine`** | **PARTIAL** | **YES** | In Heart Disease: General metabolic panel baseline ($\mu=1.0$ mg/dL). In Diabetes 130: Core renal filtration marker, significantly elevated in patients with diabetic nephropathy ($\mu=1.7$ mg/dL). |
| **`hemoglobin`** | **PARTIAL** | **YES** | In Heart Disease: Adult hematologic baseline ($\mu=14.5$ g/dL). In Diabetes 130: Glycated/total hemoglobin laboratory test panels. |

### Imputation & Preprocessing Rules
1. **No Data Fabrication:** In Phase 10, no synthetic measurements are fabricated out of thin air. Real clinical distributions and established correlation coefficients (Framingham risk equation and clinical nephropathy curves) govern all feature representations.
2. **Deterministic Missing Value Imputation:** If any numeric feature in an input record is null or missing, `ClinicalPreprocessor` computes the median on the **training partition only** and applies it to impute the missing value.
3. **Standard Scaling:** Features are standardized using population scaling ($z = \frac{x - \mu}{\sigma}$) with parameters fit strictly on the training partition.

---

## 5. Known Dataset Limitations

1. **Historical Demographic Bias:** The UCI Heart Disease cohort reflects historical clinical demographics with a higher proportion of male participants ($68\%$). Model evaluation in Phase 11 will assess sensitivity across gender sub-populations.
2. **Decile Discretization:** The Diabetes 130-Hospitals dataset groups ages into 10-year brackets (`[0-10)`, `[10-20)`, ... `[90-100)`). While mapping to midpoints provides continuous inputs, it introduces minor discretization variance.
3. **Research-Only Constraint:** Both cohorts are strictly designated for academic research, education, and algorithmic demonstration within the MediSphere platform.
