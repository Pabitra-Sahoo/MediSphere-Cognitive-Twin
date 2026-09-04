# MediSphere Cognitive Twin

AI-based healthcare management platform — Digital Health Twin foundation.

## Current Status: Milestone 1 — FHIR Integration & Twin Foundation

**Phases 1 through 6 are fully implemented, tested, and verified.**
- Phase 1: Project Scaffolding & Infrastructure
- Phase 2: Authentication & RBAC (SMART-on-FHIR scope foundation)
- Phase 3: Patient & HealthTwin Core (20-field completeness strictly >95%)
- Phase 4: FHIR R4 Integration & Validation (HAPI FHIR R4)
- Phase 5: Kafka Vitals Streaming & Idempotency Pipeline
- Phase 6: Consent Management & HIPAA-Style Audit Logging
- Phase 7: Frontend Patient 360 & 3D Twin (Next phase)

---

## Technology Stack

| Layer            | Technology                        |
|-----------------|-----------------------------------|
| Frontend         | React 18 + TypeScript + Vite      |
| Backend          | Java 21 + Spring Boot 3.5         |
| Database         | MongoDB 7                         |
| Messaging        | Apache Kafka 3.9 (KRaft mode)     |
| Healthcare       | HAPI FHIR R4 7.4.0                |
| Infrastructure   | Docker + Docker Compose           |
| Build            | Maven (backend), npm (frontend)   |

---

## M1 Demo Credentials

Deterministic demo accounts seeded on application startup:

| Username | Password | Role | Linked Entity | Permissions / Scope |
|---|---|---|---|---|
| `admin` | `Admin@123` | `ADMIN` | — | Global access, user registration, FHIR ingest, vitals simulation, system audit logs |
| `dr_smith` | `Provider@123` | `PROVIDER` | `prov-001` | Assigned to `pat-001`. Accesses clinical data when active consent exists |
| `john_doe` | `Patient@123` | `PATIENT` | `pat-001` | Own record access, grant/revoke consent for `pat-001` |

### Seeded Demo Patients

- **`pat-001`** (`MRN-10001` — John Doe): 100% completeness (20/20 fields), assigned to `prov-001`, active 1-year consent seeded.
- **`pat-002`** (`MRN-10002` — Jane Roe): 100% completeness (20/20 fields), assigned to `prov-001`, no active consent (access denial demo).
- **`pat-003`** (`MRN-10003` — Robert Chen): 100% completeness (20/20 fields), unassigned to `prov-001` (provider boundary RBAC demo).

---

## Quick Start / Local Setup

### 1. Prerequisites
- **Java 21** (JDK)
- **Maven 3.9+** (or included `mvnw` wrapper)
- **Node.js 20+** and **npm 10+**
- **Docker** and **Docker Compose v2**

### 2. Start Infrastructure
```bash
docker compose up -d
```
Starts:
- **MongoDB 7.0** on `localhost:27017`
- **Apache Kafka 3.9 (KRaft)** on `localhost:9092`

### 3. Start Backend
```bash
cd backend
.\mvnw.cmd spring-boot:run
```
*(On Linux/macOS: `./mvnw spring-boot:run`)*
Backend runs on **http://localhost:8080**

### 4. Start Frontend
```bash
cd frontend
npm install
npm run dev
```
Frontend runs on **http://localhost:5173**

### 5. Run Standalone Kafka Simulator (Optional)
```bash
cd kafka-simulator
npm install
node simulator.js --mode=single --patientId=pat-001
```

---

## Important M1 Demo Flow

Follow this sequence to demonstrate all Milestone 1 capabilities:

1. **Authentication**:
   - `POST http://localhost:8080/api/auth/login` with `{"username": "dr_smith", "password": "Provider@123"}`.
   - Response returns stateless JWT with role `PROVIDER` and SMART scopes (`patient/*.read`, `user/*.write`).

2. **Digital HealthTwin & Completeness (>95%)**:
   - `GET http://localhost:8080/api/patients/pat-001/twin` with Dr. Smith's token.
   - HTTP 200 OK returns twin with **100.0% completeness** (20 of 20 logical fields populated, strictly exceeding the >95% requirement).

3. **HIPAA-Style Audit Logging (Success)**:
   - `GET http://localhost:8080/api/patients/pat-001/audit-logs` (as `admin` or `john_doe`).
   - Confirms `VIEW_TWIN` audit record with `outcome: SUCCESS`, `performedBy: dr_smith`, and timestamp.

4. **Consent Revocation**:
   - `PUT http://localhost:8080/api/patients/pat-001/consents/{consentId}/revoke` as `john_doe` (`Patient@123`).
   - Consent status changes to `REVOKED`.

5. **Consent Enforcement & Access Denial Audit**:
   - Attempt `GET http://localhost:8080/api/patients/pat-001/twin` as `dr_smith`.
   - Returns **HTTP 403 Forbidden** (`no active consent found for provider prov-001`).
   - Audit log captures `ACCESS_DENIED` with `outcome: DENIED` and sanitized details.

6. **Provider Boundary Enforcement (RBAC)**:
   - Attempt `GET http://localhost:8080/api/patients/pat-003/twin` as `dr_smith`.
   - Returns **HTTP 403 Forbidden** (Dr. Smith is not assigned to `pat-003`).

7. **FHIR R4 Ingestion & Consent Mapping**:
   - `POST http://localhost:8080/api/fhir/ingest` as `admin` with body from `sample-fhir/consent-resource.json`.
   - Validates resource, persists raw FHIR resource, maps to domain Consent with status `GRANTED` for `prov-001`, and records `FHIR_SYNC` audit event.

8. **Kafka Vitals Streaming & Twin Update**:
   - `POST http://localhost:8080/api/vitals/simulate` as `admin` with simulated vitals payload for `pat-001`.
   - Published to Kafka topic `vitals.ingest`, consumed by Spring Boot, validated for data quality, persisted, and updates `HealthTwin.latestVitals`.
   - Completeness score remains 100.0% (>95%), and Dr. Smith can access the twin again.

---

## Ports Summary

| Service    | Port  | Notes |
|-----------|-------|---|
| Frontend   | 5173  | Vite development server |
| Backend    | 8080  | Spring Boot REST API |
| MongoDB    | 27017 | Database storage |
| Kafka      | 9092  | KRaft broker port (external) |

---

## Implementation Status

- [x] Phase 1: Project Scaffolding & Infrastructure
- [x] Phase 2: Authentication & RBAC (SMART scopes)
- [x] Phase 3: Patient & HealthTwin Core (20-field completeness >95%)
- [x] Phase 4: FHIR R4 Integration & Validation
- [x] Phase 5: Kafka Vitals Streaming & Idempotency Pipeline
- [x] Phase 6: Consent Management & HIPAA-Style Audit Logging
- [ ] Phase 7: Frontend Patient 360 & 3D Twin

---

## License & Disclaimer

Student project developed for Infosys Springboard Internship 7.0 — not for production clinical use.
