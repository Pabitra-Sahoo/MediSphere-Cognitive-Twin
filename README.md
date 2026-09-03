# MediSphere Cognitive Twin

AI-based healthcare management platform — Digital Health Twin foundation.

## Current Status: Milestone 1 — FHIR Integration & Twin Foundation

**Phase 1 (Scaffolding) complete.** Project foundation is in place.

---

## Technology Stack

| Layer            | Technology                        |
|-----------------|-----------------------------------|
| Frontend         | React 18 + TypeScript + Vite      |
| Backend          | Java 21 + Spring Boot 3.3         |
| Database         | MongoDB 7                         |
| Messaging        | Apache Kafka 3.9 (KRaft mode)     |
| Infrastructure   | Docker + Docker Compose           |
| Build            | Maven (backend), npm (frontend)   |

## Repository Structure

```
MediSphere Cognitive Twin/
├── docs/                   # Architecture, API spec, decisions, requirements
├── sample-fhir/            # FHIR R4 sample resources (added in Phase 4)
├── kafka-simulator/        # Vitals event producer script (added in Phase 5)
├── backend/                # Java 21 + Spring Boot
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/medisphere/
│       │   ├── MediSphereApplication.java
│       │   ├── config/         # Configuration classes
│       │   └── common/         # Shared exceptions, DTOs
│       └── main/resources/
│           ├── application.yml
│           ├── application-dev.yml
│           └── application-docker.yml
├── frontend/               # React + TypeScript + Vite
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
├── docker-compose.yml      # MongoDB + Kafka (KRaft)
├── .env.example            # Environment template
├── .gitignore
└── README.md               # This file
```

## Prerequisites

- **Java 21** (JDK)
- **Maven 3.9+**
- **Node.js 20+** and **npm 10+**
- **Docker** and **Docker Compose v2**

## Quick Start

### 1. Start Infrastructure

```bash
docker compose up -d
```

This starts:
- **MongoDB** on port `27017`
- **Kafka** (KRaft, single broker) on port `9092`

### 2. Start Backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Backend runs on **http://localhost:8080**

### 3. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on **http://localhost:5173**

## Ports

| Service    | Port  |
|-----------|-------|
| Frontend   | 5173  |
| Backend    | 8080  |
| MongoDB    | 27017 |
| Kafka      | 9092  |

## Environment Configuration

Copy `.env.example` to `.env` and adjust values as needed.

The backend uses Spring profiles:
- `dev` — local development (default MongoDB/Kafka on localhost)
- `docker` — Docker Compose networking (services referenced by container name)

## Implementation Status

- [x] Phase 1: Project Scaffolding & Infrastructure
- [ ] Phase 2: Authentication & RBAC
- [ ] Phase 3: Patient & HealthTwin Core
- [ ] Phase 4: FHIR Integration
- [ ] Phase 5: Kafka Vitals Streaming & Lab Results
- [ ] Phase 6: Consent & Audit
- [ ] Phase 7: Frontend Patient 360 & 3D Twin

## License

Student project — not for production use.
