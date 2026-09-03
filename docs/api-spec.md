# MediSphere Cognitive Twin — REST API Specification (Milestone 1)

## Base URL

```
http://localhost:8080/api
```

All endpoints require `Authorization: Bearer <JWT>` unless noted otherwise.

---

## 1. Authentication

### POST `/api/auth/login`
**Auth:** None required  
**Request:**
```json
{
  "username": "string",
  "password": "string"
}
```
**Response 200:**
```json
{
  "token": "jwt-string",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "string",
    "username": "string",
    "email": "string",
    "role": "PROVIDER | PATIENT | ADMIN",
    "linkedPatientId": "string | null",
    "linkedProviderId": "string | null"
  }
}
```
**Response 401:** Invalid credentials  
**Audit:** `USER_LOGIN` / `USER_LOGIN_FAILED`

### POST `/api/auth/register`
**Auth:** ADMIN only  
**Request:**
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "role": "PROVIDER | PATIENT | ADMIN",
  "linkedPatientId": "string | null",
  "linkedProviderId": "string | null"
}
```
**Response 201:** Created user (no password in response)  
**Response 409:** Username/email already exists

### GET `/api/auth/me`
**Auth:** Any authenticated user  
**Response 200:** Current user details (same shape as login response user object)

---

## 2. Patients

### GET `/api/patients`
**Auth:** PROVIDER, ADMIN  
**Query params:** `page`, `size`, `search` (optional name search)  
**Response 200:**
```json
{
  "content": [
    {
      "id": "string",
      "mrn": "string",
      "firstName": "string",
      "lastName": "string",
      "dateOfBirth": "date",
      "gender": "string",
      "twinCompleteness": 96.0
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```
**Notes:** Providers see only their assigned patients. Admins see all.

### GET `/api/patients/{patientId}`
**Auth:** PROVIDER (assigned), PATIENT (own), ADMIN  
**Consent:** Required for PROVIDER  
**Response 200:**
```json
{
  "id": "string",
  "mrn": "string",
  "firstName": "string",
  "lastName": "string",
  "dateOfBirth": "date",
  "gender": "string",
  "email": "string",
  "phone": "string",
  "address": { ... },
  "emergencyContact": { ... },
  "insuranceInfo": { ... },
  "assignedProviderIds": ["string"]
}
```
**Response 403:** No consent or not assigned  
**Response 404:** Patient not found  
**Audit:** `VIEW_PATIENT`

### POST `/api/patients`
**Auth:** ADMIN  
**Request:** Patient object (no id)  
**Response 201:** Created patient

### PUT `/api/patients/{patientId}`
**Auth:** ADMIN  
**Request:** Updated patient fields  
**Response 200:** Updated patient

---

## 3. Health Twin

### GET `/api/patients/{patientId}/twin`
**Auth:** PROVIDER (assigned + consent), PATIENT (own), ADMIN  
**Consent:** Required for PROVIDER  
**Response 200:**
```json
{
  "id": "string",
  "patientId": "string",
  "demographics": {
    "age": 45,
    "gender": "Male",
    "bmi": 24.5,
    "height": 175,
    "weight": 75,
    "bloodType": "A+"
  },
  "latestVitals": {
    "heartRate": 72,
    "systolicBP": 120,
    "diastolicBP": 80,
    "oxygenSaturation": 98.0,
    "temperature": 36.6,
    "respiratoryRate": 16,
    "timestamp": "ISO-datetime"
  },
  "latestLabs": {
    "glucose": 98,
    "cholesterol": 185,
    "hemoglobin": 14.2,
    "creatinine": 0.9,
    "timestamp": "ISO-datetime"
  },
  "fhirSyncStatus": {
    "lastSyncTime": "ISO-datetime",
    "resourceCount": 8,
    "syncStatus": "SYNCED"
  },
  "completeness": {
    "percentage": 96.0,
    "missingFields": ["bloodType"],
    "totalFields": 20,
    "populatedFields": 19,
    "calculatedAt": "ISO-datetime"
  },
  "riskScores": {},
  "createdAt": "ISO-datetime",
  "updatedAt": "ISO-datetime"
}
```
**Audit:** `VIEW_TWIN`

### GET `/api/patients/{patientId}/twin/completeness`
**Auth:** PROVIDER (assigned), PATIENT (own), ADMIN  
**Response 200:**
```json
{
  "percentage": 96.0,
  "missingFields": ["bloodType"],
  "totalFields": 20,
  "populatedFields": 19,
  "calculatedAt": "ISO-datetime"
}
```

---

## 4. Vitals

### GET `/api/patients/{patientId}/vitals`
**Auth:** PROVIDER (assigned + consent), PATIENT (own), ADMIN  
**Consent:** Required for PROVIDER  
**Query params:** `page`, `size`, `from` (ISO date), `to` (ISO date)  
**Response 200:**
```json
{
  "content": [
    {
      "id": "string",
      "patientId": "string",
      "heartRate": 72,
      "systolicBP": 120,
      "diastolicBP": 80,
      "oxygenSaturation": 98.0,
      "temperature": 36.6,
      "respiratoryRate": 16,
      "source": "WEARABLE",
      "valid": true,
      "recordedAt": "ISO-datetime"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 50,
  "totalPages": 3
}
```
**Audit:** `VIEW_VITALS`

### GET `/api/patients/{patientId}/vitals/latest`
**Auth:** PROVIDER (assigned + consent), PATIENT (own), ADMIN  
**Consent:** Required for PROVIDER  
**Response 200:** Single latest vitals object  
**Audit:** `VIEW_VITALS`

### POST `/api/vitals/simulate`
**Auth:** ADMIN (for demo/testing purposes)  
**Description:** Sends a simulated vitals event to Kafka  
**Request:**
```json
{
  "patientId": "string",
  "heartRate": 72,
  "systolicBP": 120,
  "diastolicBP": 80,
  "oxygenSaturation": 98.0,
  "temperature": 36.6,
  "respiratoryRate": 16
}
```
**Response 202:** Event published to Kafka

---

## 5. Lab Results

### GET `/api/patients/{patientId}/labs`
**Auth:** PROVIDER (assigned + consent), PATIENT (own), ADMIN  
**Consent:** Required for PROVIDER  
**Query params:** `page`, `size`  
**Response 200:**
```json
{
  "content": [
    {
      "id": "string",
      "patientId": "string",
      "testName": "Glucose",
      "testCode": "GLU",
      "value": 98.0,
      "unit": "mg/dL",
      "referenceRange": { "low": 70, "high": 100 },
      "status": "FINAL",
      "performedAt": "ISO-datetime",
      "source": "FHIR"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 10,
  "totalPages": 1
}
```
**Audit:** `VIEW_LABS`

### POST `/api/patients/{patientId}/labs`
**Auth:** PROVIDER, ADMIN  
**Request:** Lab result object (no id)  
**Response 201:** Created lab result

---

## 6. FHIR Integration

### POST `/api/fhir/ingest`
**Auth:** ADMIN, system integration  
**Description:** Accepts a FHIR R4 Bundle or individual resource, validates it,
extracts data, and maps it into the patient twin.  
**Request:** FHIR R4 JSON (Bundle or single resource)  
**Content-Type:** `application/fhir+json`  
**Response 200:**
```json
{
  "processedResources": 5,
  "validResources": 4,
  "invalidResources": 1,
  "results": [
    {
      "resourceType": "Patient",
      "resourceId": "patient-123",
      "status": "VALID",
      "action": "MAPPED",
      "errors": []
    },
    {
      "resourceType": "Observation",
      "resourceId": "obs-456",
      "status": "INVALID",
      "action": "REJECTED",
      "errors": ["Missing required field: code"]
    }
  ]
}
```
**Response 400:** Completely invalid FHIR payload  
**Audit:** `FHIR_SYNC` / `FHIR_VALIDATION_FAIL`

### GET `/api/patients/{patientId}/fhir-resources`
**Auth:** PROVIDER (assigned + consent), PATIENT (own), ADMIN  
**Consent:** Required for PROVIDER  
**Query params:** `resourceType` (optional filter)  
**Response 200:**
```json
{
  "content": [
    {
      "id": "string",
      "resourceType": "Patient",
      "resourceId": "fhir-resource-id",
      "validationStatus": "VALID",
      "processedAt": "ISO-datetime"
    }
  ],
  "totalElements": 8
}
```

### GET `/api/fhir/resources/{resourceId}`
**Auth:** PROVIDER, ADMIN  
**Response 200:** Full FHIR resource JSON with validation metadata

---

## 7. Consent Management

### GET `/api/patients/{patientId}/consents`
**Auth:** PATIENT (own), PROVIDER (assigned), ADMIN  
**Response 200:**
```json
[
  {
    "id": "string",
    "patientId": "string",
    "grantedTo": "provider-user-id",
    "grantedToName": "Dr. Smith",
    "scope": "treatment",
    "status": "GRANTED",
    "grantedAt": "ISO-datetime",
    "expiresAt": "ISO-datetime | null"
  }
]
```

### POST `/api/patients/{patientId}/consents`
**Auth:** PATIENT (own), ADMIN  
**Description:** Grant consent to a provider  
**Request:**
```json
{
  "grantedTo": "provider-user-id",
  "scope": "treatment",
  "expiresAt": "ISO-datetime | null"
}
```
**Response 201:** Created consent  
**Audit:** `CONSENT_GRANTED`

### PUT `/api/patients/{patientId}/consents/{consentId}/revoke`
**Auth:** PATIENT (own), ADMIN  
**Response 200:** Updated consent with `status = REVOKED`  
**Audit:** `CONSENT_REVOKED`

### GET `/api/patients/{patientId}/consents/verify`
**Auth:** PROVIDER  
**Query params:** `providerId`  
**Response 200:**
```json
{
  "hasConsent": true,
  "consentId": "string",
  "scope": "treatment",
  "expiresAt": "ISO-datetime | null"
}
```
**Audit:** `CONSENT_VERIFIED`

---

## 8. Audit Logs

### GET `/api/patients/{patientId}/audit-logs`
**Auth:** ADMIN, PATIENT (own limited view)  
**Query params:** `page`, `size`, `action` (optional filter), `from`, `to`  
**Response 200:**
```json
{
  "content": [
    {
      "id": "string",
      "userId": "string",
      "username": "Dr. Smith",
      "userRole": "PROVIDER",
      "action": "VIEW_TWIN",
      "resourceType": "TWIN",
      "resourceId": "twin-id",
      "patientId": "patient-id",
      "details": "Viewed digital twin",
      "outcome": "SUCCESS",
      "timestamp": "ISO-datetime"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

### GET `/api/audit-logs`
**Auth:** ADMIN only  
**Description:** System-wide audit log view  
**Query params:** `page`, `size`, `action`, `userId`, `from`, `to`  
**Response 200:** Same paginated shape as patient-specific audit

---

## 9. Error Response Format

All errors follow a consistent shape:

```json
{
  "timestamp": "ISO-datetime",
  "status": 400,
  "error": "Bad Request",
  "message": "Human-readable error message",
  "path": "/api/fhir/ingest",
  "details": ["field-level error 1", "field-level error 2"]
}
```

### HTTP Status Codes Used

| Code | Meaning                                    |
|------|--------------------------------------------|
| 200  | Success                                    |
| 201  | Created                                    |
| 202  | Accepted (async, e.g., Kafka publish)      |
| 400  | Validation error / bad request             |
| 401  | Not authenticated                          |
| 403  | Not authorized / no consent                |
| 404  | Resource not found                         |
| 409  | Conflict (duplicate)                       |
| 422  | FHIR validation failure                    |
| 500  | Internal server error                      |

---

## 10. API Summary Table

| Method | Endpoint                                    | Auth Required | Consent | Audit |
|--------|---------------------------------------------|---------------|---------|-------|
| POST   | `/api/auth/login`                           | None          | —       | ✓     |
| POST   | `/api/auth/register`                        | ADMIN         | —       | —     |
| GET    | `/api/auth/me`                              | Any           | —       | —     |
| GET    | `/api/patients`                             | PROVIDER/ADMIN| —       | —     |
| GET    | `/api/patients/{id}`                        | Scoped        | ✓       | ✓     |
| POST   | `/api/patients`                             | ADMIN         | —       | —     |
| PUT    | `/api/patients/{id}`                        | ADMIN         | —       | —     |
| GET    | `/api/patients/{id}/twin`                   | Scoped        | ✓       | ✓     |
| GET    | `/api/patients/{id}/twin/completeness`      | Scoped        | —       | —     |
| GET    | `/api/patients/{id}/vitals`                 | Scoped        | ✓       | ✓     |
| GET    | `/api/patients/{id}/vitals/latest`          | Scoped        | ✓       | ✓     |
| POST   | `/api/vitals/simulate`                      | ADMIN         | —       | ✓     |
| GET    | `/api/patients/{id}/labs`                   | Scoped        | ✓       | ✓     |
| POST   | `/api/patients/{id}/labs`                   | PROVIDER/ADMIN| —       | —     |
| POST   | `/api/fhir/ingest`                          | ADMIN         | —       | ✓     |
| GET    | `/api/patients/{id}/fhir-resources`         | Scoped        | ✓       | —     |
| GET    | `/api/fhir/resources/{id}`                  | PROVIDER/ADMIN| —       | —     |
| GET    | `/api/patients/{id}/consents`               | Scoped        | —       | —     |
| POST   | `/api/patients/{id}/consents`               | PATIENT/ADMIN | —       | ✓     |
| PUT    | `/api/patients/{id}/consents/{cid}/revoke`  | PATIENT/ADMIN | —       | ✓     |
| GET    | `/api/patients/{id}/consents/verify`        | PROVIDER      | —       | ✓     |
| GET    | `/api/patients/{id}/audit-logs`             | ADMIN/PATIENT | —       | —     |
| GET    | `/api/audit-logs`                           | ADMIN         | —       | —     |

**"Scoped"** = PROVIDER (only assigned patients), PATIENT (own only), ADMIN (all)
