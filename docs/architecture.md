# MediSphere Cognitive Twin — Architecture (Milestone 1)

## 1. System Overview

MediSphere Cognitive Twin M1 is a monolithic-backend + SPA-frontend system
that ingests healthcare data via FHIR R4, streams wearable vitals through
Kafka, persists Digital Health Twins in MongoDB, and presents a Patient 360
dashboard in the browser.

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Browser (React)                             │
│  Login ─► Patient List ─► Patient 360  (Demographics, Vitals,      │
│           Labs, FHIR, Digital Twin 3D, Consent, Audit Activity)     │
└────────────────────────────┬────────────────────────────────────────┘
                             │  REST / JSON
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   Spring Boot Backend (Java 21)                     │
│                                                                     │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│  │ Auth /   │ │ FHIR     │ │ Patient  │ │ Vitals   │ │ Consent  │ │
│  │ RBAC     │ │ Ingestion│ │ HealthTwin│ │ Stream  │ │ Mgmt     │ │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘ │
│  ┌──────────┐ ┌──────────┐                                         │
│  │ Audit    │ │ LabResult│                                          │
│  │ Logging  │ │ Mgmt     │                                          │
│  └──────────┘ └──────────┘                                         │
└──────┬──────────────────────────┬──────────────────────────────────┘
       │                          │
       ▼                          ▼
  ┌──────────┐            ┌──────────────┐
  │ MongoDB  │            │ Apache Kafka │
  │  (twins, │            │ (vitals      │
  │  patients│            │  streaming)  │
  │  audits) │            │              │
  └──────────┘            └──────────────┘
```

### Key M1 Principle

One Java Spring Boot application, one React SPA, one MongoDB instance,
one Kafka broker. No API gateway, no service mesh, no Kubernetes.
Infrastructure runs entirely via Docker Compose for local development.

---

## 2. Repository Structure

```
MediSphere Cognitive Twin/
│
├── docs/
│   ├── project-requirements.md     # Source of truth
│   ├── milestone-1.md              # M1 acceptance checklist
│   ├── architecture.md             # This file
│   ├── api-spec.md                 # REST API specification
│   └── decisions.md                # Architectural decisions
│
├── sample-fhir/                    # Real FHIR R4 sample resources for demo
│   ├── patient-bundle.json         # FHIR Patient resource
│   ├── vitals-observations.json    # FHIR Observation (vital signs)
│   ├── lab-observations.json       # FHIR Observation (lab results)
│   ├── diagnostic-report.json      # FHIR DiagnosticReport
│   ├── consent-resource.json       # FHIR Consent resource
│   └── invalid-resource.json       # Intentionally invalid (for validation demo)
│
├── backend/                        # Java 21 + Spring Boot
│   ├── pom.xml                     # Maven build
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/medisphere/
│   │   │   │   ├── MediSphereApplication.java
│   │   │   │   │
│   │   │   │   ├── config/
│   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   │   ├── KafkaConfig.java
│   │   │   │   │   ├── MongoConfig.java
│   │   │   │   │   ├── WebConfig.java
│   │   │   │   │   └── FhirConfig.java
│   │   │   │   │
│   │   │   │   ├── auth/
│   │   │   │   │   ├── controller/AuthController.java
│   │   │   │   │   ├── dto/LoginRequest.java
│   │   │   │   │   ├── dto/LoginResponse.java
│   │   │   │   │   ├── dto/RegisterRequest.java
│   │   │   │   │   ├── model/User.java
│   │   │   │   │   ├── model/Role.java          # enum: PROVIDER, PATIENT, ADMIN
│   │   │   │   │   ├── repository/UserRepository.java
│   │   │   │   │   ├── service/AuthService.java
│   │   │   │   │   ├── security/JwtTokenProvider.java
│   │   │   │   │   └── security/JwtAuthFilter.java
│   │   │   │   │
│   │   │   │   ├── patient/
│   │   │   │   │   ├── controller/PatientController.java
│   │   │   │   │   ├── dto/PatientDTO.java
│   │   │   │   │   ├── dto/PatientSummaryDTO.java
│   │   │   │   │   ├── model/Patient.java
│   │   │   │   │   ├── repository/PatientRepository.java
│   │   │   │   │   └── service/PatientService.java
│   │   │   │   │
│   │   │   │   ├── twin/
│   │   │   │   │   ├── controller/HealthTwinController.java
│   │   │   │   │   ├── dto/HealthTwinDTO.java
│   │   │   │   │   ├── dto/TwinCompletenessDTO.java
│   │   │   │   │   ├── model/HealthTwin.java
│   │   │   │   │   ├── repository/HealthTwinRepository.java
│   │   │   │   │   └── service/HealthTwinService.java
│   │   │   │   │
│   │   │   │   ├── vitals/
│   │   │   │   │   ├── controller/VitalsController.java
│   │   │   │   │   ├── dto/VitalsDTO.java
│   │   │   │   │   ├── dto/VitalsEventDTO.java
│   │   │   │   │   ├── model/Vitals.java
│   │   │   │   │   ├── repository/VitalsRepository.java
│   │   │   │   │   ├── service/VitalsService.java
│   │   │   │   │   ├── validation/VitalsValidator.java
│   │   │   │   │   ├── kafka/VitalsProducer.java
│   │   │   │   │   └── kafka/VitalsConsumer.java
│   │   │   │   │
│   │   │   │   ├── lab/
│   │   │   │   │   ├── controller/LabResultController.java
│   │   │   │   │   ├── dto/LabResultDTO.java
│   │   │   │   │   ├── model/LabResult.java
│   │   │   │   │   ├── repository/LabResultRepository.java
│   │   │   │   │   └── service/LabResultService.java
│   │   │   │   │
│   │   │   │   ├── fhir/
│   │   │   │   │   ├── controller/FhirController.java
│   │   │   │   │   ├── dto/FhirIngestionResult.java
│   │   │   │   │   ├── model/FhirResource.java
│   │   │   │   │   ├── repository/FhirResourceRepository.java
│   │   │   │   │   ├── service/FhirIngestionService.java
│   │   │   │   │   ├── service/FhirValidationService.java
│   │   │   │   │   └── mapper/FhirToTwinMapper.java
│   │   │   │   │
│   │   │   │   ├── consent/
│   │   │   │   │   ├── controller/ConsentController.java
│   │   │   │   │   ├── dto/ConsentDTO.java
│   │   │   │   │   ├── dto/ConsentUpdateRequest.java
│   │   │   │   │   ├── model/Consent.java
│   │   │   │   │   ├── model/ConsentStatus.java  # enum: GRANTED, REVOKED, PENDING
│   │   │   │   │   ├── repository/ConsentRepository.java
│   │   │   │   │   ├── service/ConsentService.java
│   │   │   │   │   └── interceptor/ConsentInterceptor.java
│   │   │   │   │
│   │   │   │   ├── audit/
│   │   │   │   │   ├── controller/AuditController.java
│   │   │   │   │   ├── dto/AuditLogDTO.java
│   │   │   │   │   ├── model/AuditLog.java
│   │   │   │   │   ├── model/AuditAction.java   # enum of auditable actions
│   │   │   │   │   ├── repository/AuditLogRepository.java
│   │   │   │   │   ├── service/AuditService.java
│   │   │   │   │   └── aspect/AuditAspect.java
│   │   │   │   │
│   │   │   │   └── common/
│   │   │   │       ├── exception/GlobalExceptionHandler.java
│   │   │   │       ├── exception/ResourceNotFoundException.java
│   │   │   │       ├── exception/ConsentDeniedException.java
│   │   │   │       ├── exception/FhirValidationException.java
│   │   │   │       ├── exception/VitalsValidationException.java
│   │   │   │       ├── dto/ApiError.java
│   │   │   │       └── dto/PagedResponse.java
│   │   │   │
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── application-dev.yml
│   │   │       └── application-docker.yml
│   │   │
│   │   └── test/java/com/medisphere/
│   │       ├── fhir/
│   │       │   ├── FhirIngestionServiceTest.java
│   │       │   └── FhirValidationServiceTest.java
│   │       ├── vitals/
│   │       │   ├── VitalsValidatorTest.java
│   │       │   └── VitalsConsumerTest.java
│   │       ├── twin/
│   │       │   └── HealthTwinServiceTest.java
│   │       ├── consent/
│   │       │   └── ConsentServiceTest.java
│   │       ├── audit/
│   │       │   └── AuditServiceTest.java
│   │       └── auth/
│   │           └── AuthServiceTest.java
│   │
│   └── Dockerfile
│
├── frontend/                       # React + TypeScript + Vite
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── index.html
│   ├── public/
│   ├── src/
│   │   ├── main.tsx
│   │   ├── App.tsx
│   │   ├── api/
│   │   │   ├── axiosClient.ts       # Axios instance w/ JWT interceptor
│   │   │   ├── authApi.ts
│   │   │   ├── patientApi.ts
│   │   │   ├── vitalsApi.ts
│   │   │   ├── labApi.ts
│   │   │   ├── fhirApi.ts
│   │   │   ├── consentApi.ts
│   │   │   └── auditApi.ts
│   │   │
│   │   ├── auth/
│   │   │   ├── LoginPage.tsx
│   │   │   ├── AuthContext.tsx
│   │   │   ├── ProtectedRoute.tsx
│   │   │   └── useAuth.ts
│   │   │
│   │   ├── patients/
│   │   │   ├── PatientListPage.tsx
│   │   │   └── PatientCard.tsx
│   │   │
│   │   ├── patient360/
│   │   │   ├── Patient360Page.tsx    # Main Patient 360 layout
│   │   │   ├── DemographicsPanel.tsx
│   │   │   ├── VitalsPanel.tsx
│   │   │   ├── LabsPanel.tsx
│   │   │   ├── FhirResourcesPanel.tsx
│   │   │   ├── ConsentPanel.tsx
│   │   │   ├── AuditActivityPanel.tsx
│   │   │   └── DigitalTwinPanel.tsx
│   │   │
│   │   ├── twin3d/
│   │   │   ├── DigitalTwinViewer.tsx  # React Three Fiber scene
│   │   │   ├── BodyModel.tsx          # Simple 3D body
│   │   │   └── CompletenessIndicator.tsx
│   │   │
│   │   ├── components/
│   │   │   ├── Navbar.tsx
│   │   │   ├── Sidebar.tsx
│   │   │   ├── LoadingSpinner.tsx
│   │   │   └── StatusBadge.tsx
│   │   │
│   │   ├── hooks/
│   │   │   └── usePolling.ts
│   │   │
│   │   ├── types/
│   │   │   ├── patient.ts
│   │   │   ├── vitals.ts
│   │   │   ├── lab.ts
│   │   │   ├── fhir.ts
│   │   │   ├── consent.ts
│   │   │   ├── audit.ts
│   │   │   ├── auth.ts
│   │   │   └── twin.ts
│   │   │
│   │   └── styles/
│   │       └── index.css
│   │
│   └── Dockerfile
│
├── kafka-simulator/                # Simple vitals event producer
│   └── simulate-vitals.sh          # Or a small Java/JS script
│
├── docker-compose.yml              # MongoDB + Kafka (KRaft) + Backend + Frontend
├── .env.example                    # Environment template (no secrets)
├── .gitignore
└── README.md
```

---

## 3. M1 Domain Model

### 3.1 Core Entities & Relationships

```
User ──────1──N──► AuditLog
 │
 │ (auth identity)
 ▼
Patient ───1──1──► HealthTwin
 │                   │
 │ ◄──N──1──────────►│
 │                   │
 ├──1──N──► Vitals   │  (vitals update twin)
 ├──1──N──► LabResult│  (labs update twin)
 ├──1──N──► FhirResource
 └──1──N──► Consent

Provider ─────────── User (with PROVIDER role)
```

### 3.2 MongoDB Collections

#### `users`
```json
{
  "_id": "ObjectId",
  "username": "string",
  "email": "string",
  "passwordHash": "string",
  "role": "PROVIDER | PATIENT | ADMIN",
  "linkedPatientId": "string | null",
  "linkedProviderId": "string | null",
  "createdAt": "ISODate",
  "updatedAt": "ISODate",
  "active": "boolean"
}
```

#### `patients`
```json
{
  "_id": "ObjectId",
  "mrn": "string",
  "firstName": "string",
  "lastName": "string",
  "dateOfBirth": "ISODate",
  "gender": "string",
  "email": "string",
  "phone": "string",
  "address": {
    "street": "string",
    "city": "string",
    "state": "string",
    "zipCode": "string",
    "country": "string"
  },
  "emergencyContact": {
    "name": "string",
    "phone": "string",
    "relationship": "string"
  },
  "insuranceInfo": {
    "provider": "string",
    "policyNumber": "string"
  },
  "assignedProviderIds": ["string"],
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

#### `health_twins`
```json
{
  "_id": "ObjectId",
  "patientId": "string",
  "demographics": {
    "age": "number",
    "gender": "string",
    "bmi": "number | null",
    "height": "number | null",
    "weight": "number | null",
    "bloodType": "string | null"
  },
  "latestVitals": {
    "heartRate": "number | null",
    "systolicBP": "number | null",
    "diastolicBP": "number | null",
    "oxygenSaturation": "number | null",
    "temperature": "number | null",
    "respiratoryRate": "number | null",
    "timestamp": "ISODate | null"
  },
  "latestLabs": {
    "glucose": "number | null",
    "cholesterol": "number | null",
    "hemoglobin": "number | null",
    "creatinine": "number | null",
    "timestamp": "ISODate | null"
  },
  "fhirSyncStatus": {
    "lastSyncTime": "ISODate | null",
    "resourceCount": "number",
    "syncStatus": "SYNCED | PENDING | ERROR"
  },
  "completeness": {
    "percentage": "number",
    "missingFields": ["string"],
    "totalFields": "number",
    "populatedFields": "number",
    "calculatedAt": "ISODate"
  },
  "riskScores": {},
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

#### `vitals`
```json
{
  "_id": "ObjectId",
  "patientId": "string",
  "heartRate": "number",
  "systolicBP": "number",
  "diastolicBP": "number",
  "oxygenSaturation": "number",
  "temperature": "number",
  "respiratoryRate": "number",
  "source": "WEARABLE | MANUAL | FHIR",
  "valid": "boolean",
  "validationErrors": ["string"],
  "recordedAt": "ISODate",
  "receivedAt": "ISODate"
}
```

#### `lab_results`
```json
{
  "_id": "ObjectId",
  "patientId": "string",
  "testName": "string",
  "testCode": "string",
  "value": "number",
  "unit": "string",
  "referenceRange": { "low": "number", "high": "number" },
  "status": "FINAL | PRELIMINARY | CANCELLED",
  "performedAt": "ISODate",
  "reportedAt": "ISODate",
  "source": "FHIR | MANUAL"
}
```

#### `fhir_resources`
```json
{
  "_id": "ObjectId",
  "patientId": "string",
  "resourceType": "string",
  "resourceId": "string",
  "rawJson": "object",
  "version": "string",
  "validationStatus": "VALID | INVALID",
  "validationErrors": ["string"],
  "processedAt": "ISODate",
  "receivedAt": "ISODate"
}
```

#### `consents`
```json
{
  "_id": "ObjectId",
  "patientId": "string",
  "grantedTo": "string",
  "scope": "string",
  "status": "GRANTED | REVOKED | PENDING",
  "grantedAt": "ISODate | null",
  "revokedAt": "ISODate | null",
  "expiresAt": "ISODate | null",
  "reason": "string | null",
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

#### `audit_logs`
```json
{
  "_id": "ObjectId",
  "userId": "string",
  "username": "string",
  "userRole": "string",
  "action": "string",
  "resourceType": "string",
  "resourceId": "string",
  "patientId": "string | null",
  "details": "string | null",
  "outcome": "SUCCESS | FAILURE | DENIED",
  "ipAddress": "string",
  "timestamp": "ISODate"
}
```

---

## 4. Data Flows

### 4.1 FHIR R4 → HealthTwin Flow

**FHIR data source in M1:** Real sample FHIR R4 JSON files are provided in
`sample-fhir/`. These are posted to the ingestion endpoint via REST (e.g., curl,
Postman, or the frontend). No separate mock FHIR server is used.

```
sample-fhir/*.json  (real FHIR R4 resources)
        │
        ▼  (POST via curl / Postman / frontend)
POST /api/fhir/ingest   (FHIR Bundle or single resource)
        │
        ▼
FhirIngestionService
        │
        ├─► HAPI FHIR Validator (structural + profile validation)
        │       ├─ VALID ──► continue
        │       └─ INVALID ─► reject, log validation errors, store as INVALID
        │
        ├─► FhirResourceRepository.save()   (store raw FHIR resource)
        │
        ├─► FhirToTwinMapper
        │       ├─ Patient resource   ──► update Patient demographics
        │       ├─ Observation (vitals) ──► update HealthTwin.latestVitals
        │       ├─ Observation (labs)   ──► create LabResult + update HealthTwin.latestLabs
        │       └─ DiagnosticReport     ──► create LabResult
        │
        ├─► HealthTwinService.recalculateCompleteness()
        │
        └─► AuditService.log(FHIR_SYNC, ...)
```

**FHIR resource types handled in M1:**

| FHIR R4 Resource     | Maps To                               |
|----------------------|----------------------------------------|
| Patient              | Patient demographics                   |
| Observation (vital)  | Vitals → HealthTwin.latestVitals       |
| Observation (lab)    | LabResult → HealthTwin.latestLabs      |
| DiagnosticReport     | LabResult                              |
| Consent              | Consent                                |

### 4.2 Kafka Vitals Streaming Flow

```
Wearable Simulator (kafka-simulator/ or API endpoint)
        │
        ▼
Kafka Topic: "vitals.ingest"
        │
        ▼
VitalsConsumer (Spring Kafka @KafkaListener)
        │
        ├─► VitalsValidator
        │       ├─ Valid   ──► persist + update twin
        │       └─ Invalid ──► log rejection, store with valid=false
        │
        ├─► VitalsRepository.save()
        │
        ├─► HealthTwinService.updateLatestVitals()
        │
        └─► (M3+ alert engine hook point — not implemented in M1)
```

### 4.3 Protected Access Flow

```
User Request
     │
     ▼
JwtAuthFilter
     ├─ No token / invalid ──► 401 Unauthorized
     └─ Valid ──► SecurityContext populated
              │
              ▼
         RBAC Check (Spring Security @PreAuthorize)
              ├─ Unauthorized role ──► 403 Forbidden + audit log
              └─ Authorized ──► continue
                         │
                         ▼
                  ConsentInterceptor
                         ├─ No active consent for this provider+patient ──► 403 + audit
                         └─ Consent valid ──► continue
                                      │
                                      ▼
                              Service Layer
                                      │
                                      ├─► Return data
                                      └─► AuditService.log(VIEW_PATIENT, ...)
```

---

## 5. Kafka Topics & Events

### Topics

| Topic Name         | Purpose                              | Partitions | Retention |
|-------------------|--------------------------------------|------------|-----------|
| `vitals.ingest`   | Wearable vital-sign events           | 3          | 7 days    |
| `vitals.validated` | Successfully validated vitals (for future M3+ consumers) | 3 | 7 days |

### Vitals Event Schema

```json
{
  "eventId": "UUID",
  "patientId": "string",
  "deviceId": "string | null",
  "heartRate": 72,
  "systolicBP": 120,
  "diastolicBP": 80,
  "oxygenSaturation": 98.0,
  "temperature": 36.6,
  "respiratoryRate": 16,
  "source": "WEARABLE",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## 6. Vitals Validation Rules

The VitalsValidator applies clinically-informed range checks:

| Vital              | Unit    | Valid Range  | Critical Low | Critical High |
|--------------------|---------|-------------|-------------|---------------|
| Heart Rate         | bpm     | 30 – 220    | < 40        | > 180         |
| Systolic BP        | mmHg    | 60 – 250    | < 70        | > 200         |
| Diastolic BP       | mmHg    | 30 – 150    | < 40        | > 120         |
| O₂ Saturation      | %       | 70 – 100    | < 85        | —             |
| Temperature        | °C      | 32.0 – 42.0 | < 34.0      | > 40.0        |
| Respiratory Rate   | /min    | 5 – 60      | < 8         | > 40          |

Validation behavior:
- Values outside the valid range → rejected, `valid = false`, validation errors stored
- Values within range but in critical zone → accepted but flagged (M3+ alerting hook)
- Null/missing individual vital fields → accepted (partial reading), does NOT invalidate

---

## 7. Digital Twin Completeness Calculation

Completeness is calculated as: `(populatedFields / totalRequiredFields) × 100`

### Required Fields (20 total)

**Demographics (6):** firstName, lastName, dateOfBirth, gender, height, weight

**Latest Vitals (6):** heartRate, systolicBP, diastolicBP, oxygenSaturation, temperature, respiratoryRate

**Latest Labs (4):** glucose, cholesterol, hemoglobin, creatinine

**Metadata (4):** mrn, bloodType, emergencyContact, fhirSyncStatus

A field is "populated" when it is non-null and non-empty.

Example: 19 of 20 fields populated → 95% completeness ✓

Completeness is recalculated on every data-modifying operation (FHIR ingest, vitals update, lab addition).

---

## 8. Authentication & RBAC

### Authentication: SMART-on-FHIR-Compatible Foundation

M1 implements a **SMART-on-FHIR-compatible authentication foundation** — not a
full SMART-on-FHIR OAuth2 Authorization Server. The distinction is important:

| Aspect | M1 Implementation | Full SMART-on-FHIR |
|--------|-------------------|---------------------|
| Token format | JWT | JWT (typically via OAuth2) |
| Token issuer | Internal `JwtTokenProvider` | OAuth2 Authorization Server |
| Scopes | SMART-style scopes as JWT claims (e.g., `patient/*.read`, `user/*.write`) | SMART scopes via OAuth2 scope parameter |
| Auth flow | Username/password → JWT | OAuth2 Authorization Code + PKCE |
| Token validation | Local signature verification | Authorization server introspection or local verification |

**What M1 provides:**
- JWT-based stateless authentication via Spring Security
- Login endpoint issues JWT containing `userId`, `role`, `linkedPatientId` / `linkedProviderId`
- SMART-on-FHIR-style scope claims embedded in the JWT (e.g., `patient/*.read`)
- JWT validated on every request via `JwtAuthFilter`
- Token structure is compatible with future migration to a real SMART-on-FHIR Authorization Server

**What M1 does NOT provide:**
- A full OAuth2 Authorization Server
- OAuth2 authorization code flow or PKCE
- External identity provider federation
- SMART App Launch Framework

### Roles

| Role     | Access                                                     |
|----------|-------------------------------------------------------------|
| ADMIN    | Full system access, user management                        |
| PROVIDER | View assigned patients, manage consents they are granted    |
| PATIENT  | View own data only                                          |

### RBAC Enforcement
- `@PreAuthorize` annotations on controller methods
- Custom security expressions: `hasPatientAccess(patientId)`, `isAssignedProvider(patientId)`
- Patient users can only access endpoints for their own `linkedPatientId`
- Provider users can only access patients in their `assignedProviderIds` list
- RBAC enforced on backend; frontend shows/hides based on role but backend is authoritative

---

## 9. Consent Verification

### Flow
1. Before a provider accesses a patient's protected data (twin, vitals, labs),
   `ConsentInterceptor` checks for an active `Consent` where:
   - `patientId` matches the target patient
   - `grantedTo` matches the requesting user/provider
   - `status == GRANTED`
   - `expiresAt` is null or in the future
2. If no active consent → 403 + audit log entry with `outcome = DENIED`
3. If consent exists → proceed + audit log entry with `outcome = SUCCESS`

### Consent Management
- Patients (or admins) can grant/revoke consent
- Providers cannot grant themselves consent
- Consent changes are always audited

---

## 10. Audit Logging

### Approach: AOP-based + Explicit Service Calls

- `AuditAspect` (Spring AOP) intercepts annotated controller/service methods
  with `@Audited` custom annotation
- For complex scenarios (conditional logging, failure paths), services call
  `AuditService.log()` explicitly
- All audit entries are persisted to the `audit_logs` MongoDB collection

### Auditable Actions

| Action                | Trigger                                    |
|----------------------|---------------------------------------------|
| `USER_LOGIN`         | Successful authentication                   |
| `USER_LOGIN_FAILED`  | Failed authentication                       |
| `VIEW_PATIENT`       | Patient record accessed                     |
| `VIEW_TWIN`          | Digital Twin accessed                       |
| `VIEW_VITALS`        | Vitals data accessed                        |
| `VIEW_LABS`          | Lab results accessed                        |
| `FHIR_SYNC`          | FHIR resource ingested/synced               |
| `FHIR_VALIDATION_FAIL` | FHIR resource failed validation           |
| `CONSENT_GRANTED`    | Consent granted                             |
| `CONSENT_REVOKED`    | Consent revoked                             |
| `CONSENT_VERIFIED`   | Consent checked during access               |
| `ACCESS_DENIED`      | Authorization or consent check failed       |
| `VITALS_RECEIVED`    | Vitals event received from Kafka            |
| `VITALS_REJECTED`    | Vitals failed validation                    |

---

## 11. Patient 360 Dashboard

### Layout

```
┌──────────────────────────────────────────────────────────┐
│  Navbar: MediSphere Logo │ User Info │ Role │ Logout     │
├──────────────────────────────────────────────────────────┤
│ ┌──────────────┐ ┌─────────────────────────────────────┐ │
│ │ Demographics │ │ 3D Digital Twin Viewer               │ │
│ │ Name, DOB,   │ │ ┌─────────────────────────────────┐ │ │
│ │ MRN, Gender, │ │ │                                 │ │ │
│ │ Contact,     │ │ │   3D Body Model                 │ │ │
│ │ Emergency    │ │ │   + Completeness Indicator      │ │ │
│ │              │ │ │                                 │ │ │
│ └──────────────┘ │ └─────────────────────────────────┘ │ │
│                  │ Completeness: 96% ████████████░     │ │
│                  └─────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────┤
│ ┌─────────────┐ ┌─────────────┐ ┌──────────────────────┐│
│ │ Vitals      │ │ Labs        │ │ Consent Status       ││
│ │ HR: 72 bpm  │ │ Glucose:98  │ │ Dr. Smith: GRANTED   ││
│ │ BP: 120/80  │ │ Chol: 185   │ │ Dr. Lee:   REVOKED   ││
│ │ SpO2: 98%   │ │ HbA1c: 5.6  │ │ [Grant] [Revoke]     ││
│ │ Temp: 36.6  │ │ Creat: 0.9  │ │                      ││
│ └─────────────┘ └─────────────┘ └──────────────────────┘│
├──────────────────────────────────────────────────────────┤
│ ┌─────────────────────┐ ┌──────────────────────────────┐│
│ │ FHIR Resources      │ │ Recent Activity / Audit      ││
│ │ Patient (synced)    │ │ 10:30 Dr.Smith viewed twin   ││
│ │ Observation ×5      │ │ 10:28 Vitals received (Kafka)││
│ │ DiagnosticReport ×2 │ │ 10:25 Consent verified       ││
│ └─────────────────────┘ └──────────────────────────────┘│
└──────────────────────────────────────────────────────────┘
```

### 3D Digital Twin Visualization

**Technology:** React Three Fiber (R3F) / Three.js

**Justification:** R3F is the simplest production-quality solution for 3D in React.
It integrates directly into the React component tree with no separate rendering loop.
Alternatives evaluated:

| Option           | Verdict                                      |
|-----------------|-----------------------------------------------|
| React Three Fiber| ✅ Best React integration, declarative, active ecosystem |
| Raw Three.js     | ❌ Imperative, harder to integrate with React state |
| Babylon.js       | ❌ Heavier, game-engine oriented               |
| CSS 3D / SVG     | ❌ Not real 3D, insufficient for body model    |

**M1 Scope — Intentionally Minimal:**
- Simple 3D humanoid body using procedural geometry (capsules/spheres)
- Mouse/touch-controlled orbit rotation
- Completeness percentage indicator overlaid on or near the model
- Provides the structural foundation for M2+ risk heatmap overlays

**M1 Scope — Explicitly Excluded:**
- No GLB/GLTF model loading (procedural geometry only)
- No skeletal animation
- No physics simulation
- No complex materials, textures, or shaders
- No risk heatmap coloring (M2+)
- No organ-level detail
- 3D work must NOT delay core backend features

---

## 12. Future Python AI Service Integration

The M1 architecture explicitly supports future Python service addition:

```
                         ┌───────────────────────┐
                         │  Python AI Service    │  (M2+)
                         │  TensorFlow Federated │
                         │  SHAP                 │
                         └───────┬───────────────┘
                                 │ REST / gRPC
                                 ▼
┌──────────────────────────────────────────────────────┐
│              Spring Boot Backend                      │
│                                                       │
│  ai/                                                  │
│  ├── dto/RiskPredictionDTO.java                      │
│  ├── client/AiServiceClient.java  ◄── REST client    │
│  └── service/RiskPredictionService.java               │
│                                                       │
│  HealthTwin.riskScores: {}  ◄── empty map in M1      │
└──────────────────────────────────────────────────────┘
```

**M1 design decisions that enable this:**
1. `HealthTwin.riskScores` exists as an empty map — no schema change needed
2. Kafka `vitals.validated` topic available for Python consumers
3. MongoDB accessible from Python service via standard driver
4. No tightly coupled Java-only AI logic
5. Docker Compose can add a Python service container trivially

---

## 13. Infrastructure (Docker Compose)

```yaml
# Conceptual — actual docker-compose.yml created during implementation
services:
  mongodb:        # MongoDB 7
  kafka:          # Apache Kafka 3.x (KRaft mode — no Zookeeper needed)
  backend:        # Spring Boot JAR
  frontend:       # Nginx serving React build (or Vite dev server)
```

Kafka runs in KRaft mode (combined broker + controller in a single container),
eliminating the Zookeeper dependency entirely. This is the simplest reliable
configuration for a single-broker development setup.

All services run locally. No cloud dependencies for M1 development.

---

## 14. Technology Stack Summary

| Layer            | Technology                           | Version Guidance  |
|-----------------|--------------------------------------|-------------------|
| Frontend         | React + TypeScript + Vite            | React 18+, Vite 5+|
| 3D Visualization | React Three Fiber + Three.js         | Latest stable     |
| UI Styling       | CSS (vanilla) + component library    | —                 |
| Backend          | Java 21 + Spring Boot                | Spring Boot 3.2+  |
| FHIR             | HAPI FHIR                            | 7.x               |
| Security         | Spring Security + JWT                | —                 |
| Database         | MongoDB                              | 7.x               |
| Messaging        | Apache Kafka                         | 3.x               |
| Build            | Maven (backend), npm (frontend)      | —                 |
| Infrastructure   | Docker + Docker Compose              | —                 |
| Testing          | JUnit 5, Mockito, Vitest             | —                 |
