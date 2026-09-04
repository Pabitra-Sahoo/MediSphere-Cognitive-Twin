/**
 * MediSphere Wearable Vitals Kafka Simulator
 *
 * Emulates smart wearable telemetry by publishing realistic vital sign packets
 * to the Kafka topic 'vitals.ingest' partitioned by patientId.
 *
 * Usage:
 *   node simulator.js [--mode=single|invalid|duplicate|unknown|stream] [--patientId=pat-001] [--broker=localhost:9092]
 */

const { Kafka } = require('kafkajs');
const crypto = require('crypto');

// Command-line arguments parser
const args = process.argv.slice(2).reduce((acc, arg) => {
  const [key, val] = arg.replace(/^--/, '').split('=');
  acc[key] = val !== undefined ? val : true;
  return acc;
}, {});

const BROKER = args.broker || process.env.KAFKA_BROKER || 'localhost:9092';
const TOPIC = args.topic || 'vitals.ingest';
const PATIENT_ID = args.patientId || 'pat-001';
const MODE = args.mode || 'single';
const COUNT = parseInt(args.count || '5', 10);
const INTERVAL = parseInt(args.interval || '2000', 10);

const kafka = new Kafka({
  clientId: 'wearable-simulator',
  brokers: [BROKER],
});

const producer = kafka.producer();

function generateRealisticVitals(patientId, eventId = null) {
  // Realistic slight variance for telemetry
  const hr = Math.round(68 + (Math.random() * 14 - 7));
  const sbp = Math.round(118 + (Math.random() * 10 - 5));
  const dbp = Math.round(78 + (Math.random() * 8 - 4));
  const spo2 = Math.round((98 + (Math.random() * 2 - 1)) * 10) / 10;
  const temp = Math.round((36.7 + (Math.random() * 0.6 - 0.3)) * 10) / 10;
  const rr = Math.round(15 + (Math.random() * 4 - 2));

  return {
    eventId: eventId || crypto.randomUUID(),
    patientId: patientId,
    deviceId: 'WEARABLE-SIM-' + patientId,
    heartRate: hr,
    systolicBP: sbp,
    diastolicBP: dbp,
    oxygenSaturation: Math.min(100.0, spo2),
    temperature: temp,
    respiratoryRate: rr,
    source: 'WEARABLE',
    recordedAt: new Date().toISOString(),
  };
}

function generateInvalidVitals(patientId) {
  return {
    eventId: crypto.randomUUID(),
    patientId: patientId,
    deviceId: 'WEARABLE-MALFUNCTION-' + patientId,
    heartRate: 290.0, // Exceeds data-quality boundary [30 - 220]
    systolicBP: 70.0, // Below diastolic (violates relational check)
    diastolicBP: 95.0,
    oxygenSaturation: 62.0, // Below plausibility boundary [70 - 100]
    temperature: 44.5, // Exceeds plausibility boundary [32 - 42]
    respiratoryRate: 85.0, // Exceeds boundary [5 - 60]
    source: 'WEARABLE',
    recordedAt: new Date().toISOString(),
  };
}

async function sendEvent(event) {
  const messagePayload = JSON.stringify(event);
  const result = await producer.send({
    topic: TOPIC,
    messages: [
      {
        key: event.patientId,
        value: messagePayload,
      },
    ],
  });

  console.log(`[Simulator] Sent event ${event.eventId} (patient=${event.patientId}, HR=${event.heartRate}, BP=${event.systolicBP}/${event.diastolicBP}, SpO2=${event.oxygenSaturation}%) -> partition ${result[0].partition} offset ${result[0].baseOffset}`);
  return result;
}

async function run() {
  console.log(`========================================`);
  console.log(` MediSphere Kafka Wearable Simulator`);
  console.log(`========================================`);
  console.log(`Broker:     ${BROKER}`);
  console.log(`Topic:      ${TOPIC}`);
  console.log(`Patient:    ${PATIENT_ID}`);
  console.log(`Mode:       ${MODE}`);
  console.log(`----------------------------------------`);

  await producer.connect();
  console.log(`Connected to Kafka broker at ${BROKER}`);

  try {
    if (MODE === 'single') {
      const event = generateRealisticVitals(PATIENT_ID);
      await sendEvent(event);
      console.log(`[Simulator] Single event test completed successfully.`);

    } else if (MODE === 'invalid') {
      const event = generateInvalidVitals(PATIENT_ID);
      console.log(`[Simulator] Sending invalid/out-of-boundary vitals to test boundary validation...`);
      await sendEvent(event);
      console.log(`[Simulator] Invalid event test sent. Verify rejection in logs/API.`);

    } else if (MODE === 'duplicate') {
      const event = generateRealisticVitals(PATIENT_ID);
      console.log(`[Simulator] 1/2: Sending original event ${event.eventId}...`);
      await sendEvent(event);

      console.log(`[Simulator] 2/2: Sending duplicate event with same eventId ${event.eventId}...`);
      await sendEvent(event);
      console.log(`[Simulator] Duplicate test sent. Verify idempotency in consumer logs.`);

    } else if (MODE === 'unknown') {
      const event = generateRealisticVitals('pat-nonexistent-999');
      console.log(`[Simulator] Sending event for non-existent patient 'pat-nonexistent-999'...`);
      await sendEvent(event);
      console.log(`[Simulator] Unknown patient event sent.`);

    } else if (MODE === 'stream') {
      console.log(`[Simulator] Streaming ${COUNT} events every ${INTERVAL}ms...`);
      for (let i = 1; i <= COUNT; i++) {
        const event = generateRealisticVitals(PATIENT_ID);
        await sendEvent(event);
        if (i < COUNT) {
          await new Promise((resolve) => setTimeout(resolve, INTERVAL));
        }
      }
      console.log(`[Simulator] Streaming completed.`);
    } else {
      console.error(`Unknown mode: ${MODE}. Available modes: single, invalid, duplicate, unknown, stream`);
    }
  } finally {
    await producer.disconnect();
    console.log(`Disconnected from Kafka broker.`);
  }
}

run().catch((err) => {
  console.error(`[Simulator Error]:`, err);
  process.exit(1);
});
