# MediSphere Kafka Wearable Vitals Simulator

Lightweight CLI simulator for testing the real-time Kafka vitals streaming pipeline in MediSphere Cognitive Twin (Milestone 1).

## Prerequisites

1. Node.js (v18+)
2. Apache Kafka running on `localhost:9092`
3. Dependencies installed:
   ```bash
   cd kafka-simulator
   npm install
   ```

## Supported Modes

| Mode | Command | Description |
|------|---------|-------------|
| `single` | `node simulator.js --mode=single` | Sends one realistic vital sign packet for `pat-001`. |
| `invalid` | `node simulator.js --mode=invalid` | Sends out-of-boundary vitals to test data-quality validation rejection. |
| `duplicate` | `node simulator.js --mode=duplicate` | Sends the exact same event twice to verify eventId idempotency. |
| `unknown` | `node simulator.js --mode=unknown` | Sends vitals for a non-existent patient ID to verify twin isolation. |
| `stream` | `node simulator.js --mode=stream --count=10 --interval=1000` | Emulates continuous wearable telemetry streaming. |

## Options

- `--patientId=<id>`: Patient ID (default: `pat-001`).
- `--broker=<host:port>`: Kafka broker bootstrap server (default: `localhost:9092`).
- `--topic=<topic>`: Target topic (default: `vitals.ingest`).
- `--count=<N>`: Number of packets for stream mode (default: `5`).
- `--interval=<ms>`: Interval between packets in stream mode (default: `2000`).

## PowerShell Helper

You can also use the included PowerShell helper script:
```powershell
.\send-vital.ps1 -Mode single -PatientId pat-001
.\send-vital.ps1 -Mode rest -PatientId pat-001
```
