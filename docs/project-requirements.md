# MediSphere Cognitive Twin
## Project Requirements and Source of Truth

## 1. Project Overview

MediSphere Cognitive Twin is an AI-based healthcare management platform.

The complete system collects patient information from hospital records,
laboratory reports, healthcare interoperability systems, and wearable
devices. It creates a Digital Health Twin for each patient.

The long-term system uses AI and Federated Learning to predict future
health risks, monitors patients in real time, creates alerts, and supports
personalized care plans.

Simple project flow:

Collect Data
→ Create Digital Twin
→ Predict Risk
→ Monitor Patient
→ Alert Doctor
→ Create Care Plan

Possible future predictions include:
- Cardiovascular problems
- Diabetes complications
- Hospital readmission risk

A Digital Health Twin is a digital representation of a patient's health
condition and may contain:
- Patient details
- Vitals
- Laboratory results
- Health risks
- Alerts
- Care plans

Patient privacy is a core project goal. Sensitive healthcare data should
remain protected, and future Federated Learning should allow model
training without moving patient data outside the hospital.

---

## 2. Overall Project Architecture

The overall project is divided into 4 milestones.

The intended system includes:

Frontend:
- Patient/Provider web application
- Interactive patient dashboard
- Digital Twin visualization

Core backend:
- Java-based backend
- REST APIs
- Authentication and authorization
- Healthcare data processing
- Patient Digital Twin management

Data:
- MongoDB for patient Digital Twins
- Healthcare/FHIR data

Messaging:
- Apache Kafka for real-time vital-sign streaming

Healthcare interoperability:
- FHIR APIs / FHIR R4
- SMART on FHIR authentication

Future AI/ML:
- Python
- TensorFlow Federated
- Risk prediction
- SHAP explainability

Infrastructure:
- Docker
- Future deployment infrastructure as needed

IMPORTANT:
Technology versions and frameworks may be changed when a simpler,
more reliable implementation satisfies the same functional
requirements.

Functional requirements are more important than copying the exact
technology versions from the source specification.

---

# 3. Milestones

## Milestone 1: FHIR Integration & Twin Foundation

Weeks 1-2 in the original project plan.

Main requirements:
- FHIR R4 API integration
- MongoDB patient twin store
- SMART on FHIR authentication
- Kafka vitals streaming
- Patient 360 UI
- Consent management

Validation requirements:
- FHIR resource validation
- HIPAA-style audit logging
- Patient consent verification
- Digital Twin data completeness greater than 95%
- Vitals range validation
- RBAC by provider/patient/role

## Milestone 2: Federated Learning & Risk Models

Future milestone.

Requirements include:
- TensorFlow Federated
- Cardiovascular risk prediction
- Diabetes complication prediction
- SHAP explainability
- Model versioning
- Federated model training
- Target model accuracy mentioned in the specification

DO NOT IMPLEMENT THIS DURING MILESTONE 1.

## Milestone 3: Continuous Monitoring & Alerts

Future milestone.

Requirements include:
- Real-time wearable monitoring
- Kafka stream anomaly detection
- Real-time alert engine
- Clinical rule engine
- Mobile notifications

DO NOT IMPLEMENT THIS DURING MILESTONE 1.

## Milestone 4: Careplan & Intervention

Future milestone.

Requirements include:
- AI-generated personalized care plans
- Clinical guideline engine
- Adherence tracking
- Outcome measurement
- Provider collaboration
- Provider approval workflow

DO NOT IMPLEMENT THIS DURING MILESTONE 1.

---

# 4. Core Domain Entities

The project class/domain model includes:

- Patient
- HealthTwin
- Vitals
- LabResult
- RiskPrediction
- Careplan
- Alert
- Provider
- FHIRResource
- FLModel

For Milestone 1, prioritize:

- Patient
- HealthTwin
- Vitals
- LabResult
- Provider
- FHIRResource
- Consent
- AuditLog

Future entities:
- RiskPrediction
- Careplan
- Alert
- FLModel

Do not implement future entities unless they are genuinely required
for an M1 dependency.

---

# 5. Expected Overall Data Flow

Healthcare sources:

Wearables + Hospital Records + Lab Results
→ FHIR API
→ Kafka where real-time streaming is required
→ MongoDB Digital Twin storage
→ Future AI/ML processing
→ Doctor/Provider Dashboard
→ Preventive intervention

For Milestone 1, concentrate on:

FHIR/EHR data
→ FHIR validation
→ Java backend
→ Patient/HealthTwin mapping
→ MongoDB

and:

Wearable vital data
→ Kafka
→ Java backend consumer
→ Vitals validation
→ MongoDB / HealthTwin
→ Patient 360 UI

---

# 6. Security and Privacy Concepts

The overall project emphasizes:

- Patient consent
- Role-based access control
- Authentication
- Audit trails
- Encryption in transit and at rest
- Protected healthcare data
- Privacy-preserving future Federated Learning

The M1 implementation should provide a practical student-project
implementation of these concepts.

Do not claim that the student project is officially HIPAA-certified
or production HIPAA compliant.

---

# 7. M1 Patient 360

The M1 expected output is a Patient 360 dashboard.

The dashboard should make the patient's Digital Twin understandable
to a provider and should expose relevant information such as:

- Patient identity/demographics
- Vitals
- Laboratory information
- FHIR resources
- Digital Twin information
- Consent state
- Relevant recent/audit activity

The source specification depicts a Digital Twin with a 3D body and
risk heatmap.

For M1, the 3D visualization should be implemented only to the level
that is practical and demonstrable. It must not delay the core M1
requirements.

---

# 8. FHIR

FHIR means Fast Healthcare Interoperability Resources.

FHIR is used to exchange healthcare information between systems.

Example:

Hospital System
→ FHIR
→ MediSphere

M1 must support FHIR R4 integration and validation.

Use an appropriate FHIR library rather than manually recreating the
FHIR standard.

---

# 9. Kafka

Apache Kafka is the real-time event-streaming layer.

Example:

Wearable
→ Kafka
→ MediSphere backend
→ Vital validation
→ Digital Twin update

The project describes wearable information such as:
- Heart rate
- Blood pressure
- Oxygen saturation

M1 must demonstrate the streaming path and vitals validation.

---

# 10. Consent

Patient consent must be represented and checked before protected
healthcare data is accessed where appropriate.

M1 must support:
- Consent status
- Consent verification
- Consent management
- Consent-aware protected access
- Audit trail for relevant actions

---

# 11. RBAC

M1 must provide role-based access control involving at least the
concepts of:

- Provider
- Patient
- Appropriate privileged/system role where needed

A provider may access appropriate patient information.
A patient should not automatically have access to other patients'
healthcare information.

Access decisions must be enforced on backend endpoints, not only
hidden in the frontend.

---

# 12. Audit Logging

Sensitive operations should be auditable.

Examples:
- Patient record viewed
- Digital Twin accessed
- Consent verified
- Consent changed
- FHIR resource synchronized
- Protected operation denied

Audit information should include enough context to explain who performed
an action, what action occurred, which patient/resource was involved
where appropriate, when it occurred, and whether the action succeeded.

---

# 13. Validation

M1 validation includes:

FHIR resource validation
Vitals range validation
Consent verification
RBAC enforcement
Digital Twin completeness
Audit logging

Digital Twin completeness must exceed 95% for the validation scenario.

The implementation should define explicitly which required fields count
toward completeness and how the percentage is calculated.

---

# 14. Technology Direction for Our Implementation

Current preferred direction:

Frontend:
- React
- TypeScript
- Vite

UI:
- Tailwind CSS
- Appropriate component library if useful

Digital Twin visualization:
- React Three Fiber / Three.js if practical
- Simpler alternative allowed if it provides the required M1 result

Backend:
- Java
- Spring Boot

Database:
- MongoDB

Messaging:
- Apache Kafka

Healthcare:
- FHIR R4
- HAPI FHIR where useful

Security:
- Spring Security
- Appropriate OAuth2 / SMART-on-FHIR implementation strategy

Infrastructure:
- Docker
- Docker Compose for local development

Testing:
- Backend unit/integration testing
- Frontend testing where useful

IMPORTANT:
Do not add technologies merely because they appear in the original
specification.

Choose the simplest reliable technology that satisfies the requirement,
is free or suitable for a student project, is maintainable, and is easy
to deploy and explain.

---

# 15. Future AI Architecture

The future AI layer should be separate from the Java core backend.

Expected future direction:

Java Spring Boot
→ Python AI service
→ TensorFlow / TensorFlow Federated
→ Risk prediction
→ SHAP explainability
→ Java backend / frontend

This is NOT an M1 implementation requirement.

M1 should merely avoid architectural decisions that would make the
future AI service difficult to add.

---

# 16. Quality Requirements

The project should prioritize:

- Clean code
- Clear separation of concerns
- Feature-oriented organization
- DTOs
- Strong validation
- Centralized exception handling
- Meaningful logging
- Environment-based configuration
- No hardcoded secrets
- No unnecessary duplication
- Testable services
- API documentation
- Clear README
- Reproducible local setup
- Docker support

Do not create fake functionality where a required backend feature is
presented as if it were real.

Mock/sample data may be used for demonstration where appropriate, but
it must be clearly distinguishable from actual system functionality.

---

# 17. M1 Definition of Done

M1 is complete only when the system can demonstrate:

1. User authentication
2. Provider/patient role handling
3. FHIR R4 resource ingestion
4. FHIR validation
5. Patient data persistence
6. Digital Twin creation
7. Digital Twin completeness calculation
8. Kafka vital event production
9. Kafka vital event consumption
10. Vital-range validation
11. MongoDB Digital Twin update from vitals
12. Patient 360 dashboard
13. Consent management
14. Consent verification before protected access
15. Audit logging
16. Backend RBAC enforcement
17. Tests for important M1 functionality
18. Dockerized local infrastructure
19. Clear documentation
20. A reproducible demo flow

---

# 18. Explicitly Excluded from M1

Do not implement these as M1 functionality:

- TensorFlow Federated training
- Cardiovascular risk prediction
- Diabetes risk prediction
- SHAP explanations
- Model versioning
- AI anomaly detection
- Alert engine
- Mobile notifications
- AI-generated care plans
- Clinical guideline engine
- Adherence tracking
- Outcome measurement

These belong to later milestones.