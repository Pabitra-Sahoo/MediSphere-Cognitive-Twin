# MediSphere Cognitive Twin — Architectural Decisions (Milestone 1)

## Decision Log

This document records key architectural and technology decisions for M1,
including rationale, alternatives considered, and tradeoffs.

---

## ADR-001: Monolithic Backend (Single Spring Boot Application)

**Status:** Accepted

**Context:**  
The project requirements mention multiple services (FHIR, vitals, consent, etc.).
M1 could implement these as separate microservices or as modules within a single application.

**Decision:**  
Use a single Spring Boot application with feature-based package organization.

**Rationale:**
- M1 has a single development team (student project)
- No independent scaling requirements in M1
- Eliminates inter-service communication complexity (no service discovery, no distributed tracing)
- Simpler Docker Compose setup
- Simpler debugging and testing
- Feature packages provide clear module boundaries internally

**Alternatives rejected:**
- Microservices architecture: unnecessary complexity for M1 scope
- Modular monolith with Spring Modulith: adds framework overhead with no M1 benefit

**Future compatibility:**  
The feature-based package structure makes extracting modules into separate services
straightforward if needed in later milestones.

---

## ADR-002: SMART-on-FHIR-Compatible Authentication Foundation (Not Full OAuth2)

**Status:** Accepted

**Context:**  
The requirements mention SMART on FHIR authentication and OAuth2. A full SMART-on-FHIR
implementation requires an OAuth2 Authorization Server with SMART App Launch Framework.
This adds significant infrastructure (e.g., Keycloak, Spring Authorization Server).

**Decision:**  
Implement a **SMART-on-FHIR-compatible authentication foundation** using JWT and
Spring Security. JWT tokens include SMART-on-FHIR-style scope claims (e.g.,
`patient/*.read`, `user/*.write`), but authentication uses a simplified internal
login flow rather than a full OAuth2 Authorization Server.

This is explicitly a **foundation** — not a claim of full SMART-on-FHIR compliance.

**What M1 includes:**
- JWT-based stateless authentication
- SMART-style scope claims in the JWT payload
- Role-based access control (PROVIDER, PATIENT, ADMIN)
- Token structure compatible with future migration to a real SMART-on-FHIR server

**What M1 does NOT include:**
- A full OAuth2 Authorization Server
- OAuth2 authorization code flow or PKCE
- SMART App Launch Framework
- External identity provider federation

**Rationale:**
- JWT provides stateless authentication suitable for the SPA ↔ API architecture
- SMART-style scopes demonstrate the concept without full OAuth2 infrastructure
- Avoids adding Keycloak or another server to Docker Compose
- Simpler to explain during project evaluation
- The foundation can be upgraded to full SMART-on-FHIR in a future milestone

**Alternatives rejected:**
- Keycloak + Spring OAuth2 Resource Server: too much infrastructure for M1
- Session-based authentication: not suitable for SPA architecture
- Spring Authorization Server: production overhead, unnecessary for demo

**Future compatibility:**  
JWT can be replaced with tokens from a real OAuth2/SMART-on-FHIR Authorization Server
by changing only the token validation configuration in `SecurityConfig`.

---

## ADR-003: HAPI FHIR for Validation and Parsing

**Status:** Accepted

**Context:**  
FHIR R4 is a complex standard. The project could parse FHIR JSON manually or use
a dedicated FHIR library.

**Decision:**  
Use the HAPI FHIR library for FHIR R4 parsing, validation, and resource handling.

**Rationale:**
- HAPI FHIR is the standard Java FHIR library
- Provides built-in structural validation against FHIR R4 profiles
- Handles resource parsing/serialization correctly
- Well-documented with strong community support
- Eliminates custom FHIR parsing code

**Alternatives rejected:**
- Manual JSON parsing: error-prone, would not provide real FHIR validation
- Other FHIR libraries: HAPI is the de facto standard for Java

---

## ADR-004: React Three Fiber for 3D Digital Twin

**Status:** Accepted

**Context:**  
The specification requires a 3D Digital Twin visualization showing a human body.
Multiple 3D rendering approaches are available for React applications.

**Decision:**  
Use React Three Fiber (R3F) with Three.js for the 3D body visualization.

**Rationale:**
- R3F integrates natively with React's component model and state management
- Declarative scene graph composition matches React patterns
- Three.js provides sufficient 3D capability without game-engine overhead
- Active ecosystem with @react-three/drei for common helpers
- Simplest path to a rotatable 3D body model within a React app

**M1 scope limitation:**  
- Simple low-poly humanoid body (static or procedural geometry)
- Mouse-controlled rotation
- Completeness percentage overlay
- No physics, skeletal animation, or complex materials
- Foundation for M2+ risk heatmap overlays

**Alternatives rejected:**
- Raw Three.js (imperative, harder React integration)
- Babylon.js (heavier, game-engine oriented)
- CSS 3D transforms (not real 3D, insufficient for body model)
- Pre-rendered 2D body image (doesn't satisfy "3D" requirement)

---

## ADR-005: MongoDB as Single Database

**Status:** Accepted

**Context:**  
The requirements specify MongoDB for Digital Twin storage. The question is whether
additional databases (e.g., PostgreSQL for relational data) are needed.

**Decision:**  
Use MongoDB as the sole database for all M1 collections (users, patients, twins,
vitals, labs, consents, audit logs, FHIR resources).

**Rationale:**
- Reduces infrastructure complexity (one database)
- MongoDB's flexible schema suits the Digital Twin's evolving structure
- Audit logs and vitals are naturally document-oriented
- Spring Data MongoDB provides repository abstractions
- Single database simplifies Docker Compose and development setup
- MongoDB indexing is sufficient for M1 query patterns

**Tradeoffs acknowledged:**
- Some data (users, consents) is inherently relational
- No foreign key enforcement — referential integrity must be maintained in application code
- MongoDB transactions are available but less ergonomic than RDBMS

---

## ADR-006: Vanilla CSS (No Tailwind CSS for M1)

**Status:** Accepted

**Context:**  
The project-requirements.md mentions Tailwind CSS as a UI option. The implementation
instructions specify vanilla CSS unless the user explicitly requests Tailwind.

**Decision:**  
Use vanilla CSS with a design-token-based approach for the M1 frontend.

**Rationale:**
- Follows the implementation guideline to use vanilla CSS by default
- No build-tool configuration for Tailwind needed
- CSS custom properties (variables) provide a lightweight design system
- Easier to understand and debug during evaluation
- Can migrate to Tailwind later if desired

---

## ADR-007: AOP + Explicit Calls for Audit Logging

**Status:** Accepted

**Context:**  
Audit logging must cover multiple scenarios: access, consent changes, FHIR syncs,
authorization failures. Options include pure AOP, explicit service calls, or event-based.

**Decision:**  
Use a hybrid approach:
1. Spring AOP `@Audited` annotation for standard view/access logging
2. Explicit `AuditService.log()` calls for complex scenarios (failures, conditional logic)

**Rationale:**
- AOP handles the majority of audit cases cleanly (view patient, view twin)
- Complex audit scenarios (FHIR validation failures, consent denials) need explicit control
- Simpler than an event-driven audit system for M1
- Audit entries written directly to MongoDB (no additional message queue)

**Alternatives rejected:**
- Pure AOP: insufficient control for failure scenarios
- Kafka-based audit event stream: unnecessary infrastructure for M1
- Servlet filter-based logging: too coarse-grained

---

## ADR-008: Consent as Interceptor Pattern

**Status:** Accepted

**Context:**  
Consent verification must occur before protected patient data access. This could be
implemented as a filter, interceptor, aspect, or explicit service call.

**Decision:**  
Implement consent verification as a Spring `HandlerInterceptor` that activates on
endpoints accessing patient-specific protected data.

**Rationale:**
- Interceptors integrate cleanly with Spring MVC
- Centralized consent logic avoids duplication across controllers
- Can be selectively applied via path matching
- Clear separation from RBAC (which is handled by Spring Security filters)

**Flow:**
```
Request → Security Filter (JWT + RBAC) → Consent Interceptor → Controller
```

---

## ADR-009: Kafka KRaft Mode (No Zookeeper)

**Status:** Accepted (Updated)

**Context:**  
Apache Kafka can run with traditional Zookeeper coordination or the newer KRaft
(Kafka Raft) mode. For M1, we need the simplest reliable single-broker Docker
Compose configuration.

**Decision:**  
Use Kafka in KRaft mode with the `apache/kafka` official Docker image. No Zookeeper.

**Rationale:**
- **Simpler Docker Compose:** One container instead of two (no Zookeeper service)
- **Lower resource usage:** Eliminates Zookeeper's JVM memory overhead
- **Officially supported:** KRaft is production-ready since Kafka 3.3+ and is the
  default mode in Kafka 4.0+. Zookeeper support is deprecated.
- **Simpler configuration:** Combined broker + controller in a single process for
  single-node development
- **Better documentation:** The official `apache/kafka` image is designed for KRaft

**KRaft single-broker configuration summary:**
```yaml
kafka:
  image: apache/kafka:latest
  environment:
    KAFKA_NODE_ID: 1
    KAFKA_PROCESS_ROLES: broker,controller
    KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
    KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

**Alternatives rejected:**
- Kafka + Zookeeper: adds a second container, Zookeeper is deprecated in Kafka 4.0+
- Redpanda: Kafka-compatible but introduces a different codebase, unnecessary for M1

---

## ADR-010: Vitals Simulator as Simple Script

**Status:** Accepted

**Context:**  
M1 needs to demonstrate Kafka vitals streaming from a wearable. Real wearables
are not available.

**Decision:**  
Provide both:
1. A `POST /api/vitals/simulate` endpoint for convenient REST-based simulation
2. A simple shell/JS script in `kafka-simulator/` for command-line Kafka producer testing

**Rationale:**
- REST endpoint allows easy demo without Kafka CLI tools
- Script demonstrates actual Kafka producer behavior
- Both approaches are clearly identified as simulation

---

## ADR-011: No API Gateway

**Status:** Accepted

**Context:**  
The instruction explicitly says not to add API gateways unless concretely needed.

**Decision:**  
No API gateway. The React frontend communicates directly with the Spring Boot
backend via REST.

**Rationale:**
- One backend service → no routing/aggregation needed
- Spring Security handles CORS, rate limiting not required for M1
- Avoids unnecessary infrastructure (Spring Cloud Gateway, Kong, etc.)

---

## ADR-012: Feature-Based Package Organization

**Status:** Accepted

**Context:**  
Java backend packages could be organized by layer (controllers/, services/,
repositories/) or by feature (patient/, vitals/, fhir/).

**Decision:**  
Organize by feature (domain module), with each feature containing its own
controller, service, repository, DTOs, and model classes.

**Rationale:**
- Higher cohesion within each feature package
- Easier to navigate and understand during evaluation
- Each feature is self-contained
- Aligns with the project's requirement for "feature-based organization"
- Simplifies future extraction if microservices are ever needed

---

## ADR-013: No Unnecessary M1 Technologies

**Status:** Accepted

**Technologies explicitly excluded from M1:**

| Technology          | Reason for Exclusion                          |
|--------------------|-----------------------------------------------|
| Kubernetes          | No deployment orchestration needed             |
| Service Mesh        | Single service, no inter-service traffic       |
| API Gateway         | Single backend, no routing needed              |
| Redis               | No caching requirement in M1                   |
| Elasticsearch       | MongoDB text search sufficient for M1          |
| GraphQL             | REST is simpler and sufficient                 |
| WebSockets          | Polling is sufficient for M1 dashboard updates |
| gRPC                | No inter-service communication in M1           |
| Keycloak            | JWT is simpler for M1 authentication           |
| TensorFlow          | M2+ only                                       |
| Python services     | M2+ only                                       |
| SHAP                | M2+ only                                       |
| Notification service| M3+ only                                       |

---

## ADR-014: Polling for Dashboard Updates (Not WebSockets)

**Status:** Accepted

**Context:**  
The Patient 360 dashboard should show updated vitals after Kafka events are processed.
This could use WebSockets/SSE for real-time push or simple polling.

**Decision:**  
Use frontend polling (every 10–30 seconds) to refresh vitals data.

**Rationale:**
- Vastly simpler than WebSocket infrastructure
- M1 does not require sub-second real-time updates
- Demonstrates the data flow: Kafka → backend → MongoDB → REST → frontend
- WebSockets can be added in M3 (Continuous Monitoring) if needed

---

## ADR-015: Seed Data for M1 Demo

**Status:** Accepted

**Decision:**  
Include a `DataSeeder` (Spring `CommandLineRunner`) that populates demo data on first startup:

- 2–3 sample patients with full demographics
- Corresponding HealthTwins
- Sample vitals history
- Sample lab results
- FHIR resources (pre-validated)
- Sample consents
- Sample provider and patient user accounts

**Rationale:**
- Enables immediate demo without manual data setup
- Demo flow can be followed out of the box
- Seed data is clearly marked as sample data, not production

---

## Risk Register

| # | Risk                                     | Impact | Mitigation                                 |
|---|------------------------------------------|--------|---------------------------------------------|
| 1 | HAPI FHIR library is large (~100MB+ JAR) | Medium | Accept the size; it's the standard library  |
| 2 | React Three Fiber 3D model complexity     | Medium | Use simplest procedural geometry; time-box effort; do not delay backend |
| 3 | MongoDB has no FK enforcement             | Low    | Application-level validation + tests         |
| 4 | JWT secret management in dev              | Low    | Use env variables, .env.example template     |
| 5 | Completeness >95% demo depends on data    | Low    | Seed data ensures populated twin fields      |
| 6 | FHIR validation may be slow               | Low    | Validate individual resources, not bundles   |
| 7 | Kafka KRaft single-node data loss on crash| Low    | Acceptable for dev; not production deployment|
