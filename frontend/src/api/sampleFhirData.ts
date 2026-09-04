/**
 * Sample FHIR R4 JSON payloads matching sample-fhir/ directory files
 * for interactive 1-click demo ingestion in the frontend.
 */

export const SAMPLE_PATIENT_BUNDLE = JSON.stringify({
  resourceType: "Bundle",
  id: "bundle-patient-jane-roe",
  type: "collection",
  entry: [
    {
      resource: {
        resourceType: "Patient",
        id: "pat-002",
        identifier: [{ system: "http://medisphere.com/mrn", value: "MRN-10002" }],
        active: true,
        name: [{ use: "official", family: "Roe", given: ["Jane"] }],
        gender: "female",
        birthDate: "1988-07-14",
        telecom: [
          { system: "phone", value: "+1-555-0102" },
          { system: "email", value: "jane.roe@medisphere.demo" }
        ],
        address: [{ use: "home", line: ["456 Elm Avenue"], city: "Metropolis", state: "NY", postalCode: "10002", country: "USA" }],
        contact: [{
          name: { family: "Roe", given: ["Richard"] },
          relationship: [{ text: "Spouse" }],
          telecom: [{ system: "phone", value: "+1-555-0199" }]
        }]
      }
    },
    {
      resource: {
        resourceType: "Observation",
        id: "obs-jane-hr",
        status: "final",
        category: [{ coding: [{ system: "http://terminology.hl7.org/CodeSystem/observation-category", code: "vital-signs" }] }],
        code: { coding: [{ system: "http://loinc.org", code: "8867-4", display: "Heart rate" }] },
        subject: { reference: "Patient/pat-002" },
        effectiveDateTime: "2026-09-04T09:30:00Z",
        valueQuantity: { value: 68.0, unit: "beats/minute", code: "/min" }
      }
    },
    {
      resource: {
        resourceType: "Observation",
        id: "obs-jane-glu",
        status: "final",
        category: [{ coding: [{ system: "http://terminology.hl7.org/CodeSystem/observation-category", code: "laboratory" }] }],
        code: { coding: [{ system: "http://loinc.org", code: "2345-7", display: "Glucose" }] },
        subject: { reference: "Patient/pat-002" },
        effectiveDateTime: "2026-09-04T09:30:00Z",
        valueQuantity: { value: 92.0, unit: "mg/dL", code: "mg/dL" },
        referenceRange: [{ low: { value: 70.0, unit: "mg/dL" }, high: { value: 99.0, unit: "mg/dL" } }]
      }
    }
  ]
}, null, 2);

export const SAMPLE_VITALS_BUNDLE = JSON.stringify({
  resourceType: "Bundle",
  id: "bundle-vitals-john-doe",
  type: "collection",
  entry: [
    {
      resource: {
        resourceType: "Observation",
        id: "obs-john-hr",
        status: "final",
        category: [{ coding: [{ system: "http://terminology.hl7.org/CodeSystem/observation-category", code: "vital-signs" }] }],
        code: { coding: [{ system: "http://loinc.org", code: "8867-4", display: "Heart rate" }] },
        subject: { reference: "Patient/pat-001" },
        effectiveDateTime: "2026-09-04T10:15:00Z",
        valueQuantity: { value: 74.0, unit: "beats/minute", code: "/min" }
      }
    },
    {
      resource: {
        resourceType: "Observation",
        id: "obs-john-bp",
        status: "final",
        category: [{ coding: [{ system: "http://terminology.hl7.org/CodeSystem/observation-category", code: "vital-signs" }] }],
        code: { coding: [{ system: "http://loinc.org", code: "85354-9", display: "Blood pressure" }] },
        subject: { reference: "Patient/pat-001" },
        effectiveDateTime: "2026-09-04T10:15:00Z",
        component: [
          {
            code: { coding: [{ system: "http://loinc.org", code: "8480-6", display: "Systolic blood pressure" }] },
            valueQuantity: { value: 122.0, unit: "mmHg", code: "mm[Hg]" }
          },
          {
            code: { coding: [{ system: "http://loinc.org", code: "8462-4", display: "Diastolic blood pressure" }] },
            valueQuantity: { value: 82.0, unit: "mmHg", code: "mm[Hg]" }
          }
        ]
      }
    },
    {
      resource: {
        resourceType: "Observation",
        id: "obs-john-spo2",
        status: "final",
        category: [{ coding: [{ system: "http://terminology.hl7.org/CodeSystem/observation-category", code: "vital-signs" }] }],
        code: { coding: [{ system: "http://loinc.org", code: "2708-6", display: "Oxygen saturation" }] },
        subject: { reference: "Patient/pat-001" },
        effectiveDateTime: "2026-09-04T10:15:00Z",
        valueQuantity: { value: 98.5, unit: "%", code: "%" }
      }
    },
    {
      resource: {
        resourceType: "Observation",
        id: "obs-john-temp",
        status: "final",
        category: [{ coding: [{ system: "http://terminology.hl7.org/CodeSystem/observation-category", code: "vital-signs" }] }],
        code: { coding: [{ system: "http://loinc.org", code: "8310-5", display: "Body temperature" }] },
        subject: { reference: "Patient/pat-001" },
        effectiveDateTime: "2026-09-04T10:15:00Z",
        valueQuantity: { value: 36.8, unit: "Cel", code: "Cel" }
      }
    },
    {
      resource: {
        resourceType: "Observation",
        id: "obs-john-rr",
        status: "final",
        category: [{ coding: [{ system: "http://terminology.hl7.org/CodeSystem/observation-category", code: "vital-signs" }] }],
        code: { coding: [{ system: "http://loinc.org", code: "9279-1", display: "Respiratory rate" }] },
        subject: { reference: "Patient/pat-001" },
        effectiveDateTime: "2026-09-04T10:15:00Z",
        valueQuantity: { value: 16.0, unit: "breaths/minute", code: "/min" }
      }
    },
    {
      resource: {
        resourceType: "Observation",
        id: "obs-john-height",
        status: "final",
        category: [{ coding: [{ system: "http://terminology.hl7.org/CodeSystem/observation-category", code: "vital-signs" }] }],
        code: { coding: [{ system: "http://loinc.org", code: "8302-2", display: "Body height" }] },
        subject: { reference: "Patient/pat-001" },
        effectiveDateTime: "2026-09-04T10:15:00Z",
        valueQuantity: { value: 178.0, unit: "cm", code: "cm" }
      }
    },
    {
      resource: {
        resourceType: "Observation",
        id: "obs-john-weight",
        status: "final",
        category: [{ coding: [{ system: "http://terminology.hl7.org/CodeSystem/observation-category", code: "vital-signs" }] }],
        code: { coding: [{ system: "http://loinc.org", code: "29463-7", display: "Body weight" }] },
        subject: { reference: "Patient/pat-001" },
        effectiveDateTime: "2026-09-04T10:15:00Z",
        valueQuantity: { value: 77.5, unit: "kg", code: "kg" }
      }
    }
  ]
}, null, 2);

export const SAMPLE_LABS_BUNDLE = JSON.stringify({
  resourceType: "Bundle",
  id: "bundle-labs-john-doe",
  type: "collection",
  entry: [
    {
      resource: {
        resourceType: "Observation",
        id: "obs-john-glu",
        status: "final",
        category: [{ coding: [{ system: "http://terminology.hl7.org/CodeSystem/observation-category", code: "laboratory" }] }],
        code: { coding: [{ system: "http://loinc.org", code: "2345-7", display: "Glucose" }] },
        subject: { reference: "Patient/pat-001" },
        effectiveDateTime: "2026-09-04T08:00:00Z",
        valueQuantity: { value: 95.0, unit: "mg/dL", code: "mg/dL" },
        referenceRange: [{ low: { value: 70.0, unit: "mg/dL" }, high: { value: 99.0, unit: "mg/dL" } }]
      }
    },
    {
      resource: {
        resourceType: "Observation",
        id: "obs-john-chol",
        status: "final",
        category: [{ coding: [{ system: "http://terminology.hl7.org/CodeSystem/observation-category", code: "laboratory" }] }],
        code: { coding: [{ system: "http://loinc.org", code: "2093-3", display: "Cholesterol" }] },
        subject: { reference: "Patient/pat-001" },
        effectiveDateTime: "2026-09-04T08:00:00Z",
        valueQuantity: { value: 182.0, unit: "mg/dL", code: "mg/dL" },
        referenceRange: [{ high: { value: 200.0, unit: "mg/dL" } }]
      }
    },
    {
      resource: {
        resourceType: "Observation",
        id: "obs-john-hgb",
        status: "final",
        category: [{ coding: [{ system: "http://terminology.hl7.org/CodeSystem/observation-category", code: "laboratory" }] }],
        code: { coding: [{ system: "http://loinc.org", code: "718-7", display: "Hemoglobin" }] },
        subject: { reference: "Patient/pat-001" },
        effectiveDateTime: "2026-09-04T08:00:00Z",
        valueQuantity: { value: 14.5, unit: "g/dL", code: "g/dL" },
        referenceRange: [{ low: { value: 13.5, unit: "g/dL" }, high: { value: 17.5, unit: "g/dL" } }]
      }
    },
    {
      resource: {
        resourceType: "Observation",
        id: "obs-john-creat",
        status: "final",
        category: [{ coding: [{ system: "http://terminology.hl7.org/CodeSystem/observation-category", code: "laboratory" }] }],
        code: { coding: [{ system: "http://loinc.org", code: "2160-0", display: "Creatinine" }] },
        subject: { reference: "Patient/pat-001" },
        effectiveDateTime: "2026-09-04T08:00:00Z",
        valueQuantity: { value: 0.95, unit: "mg/dL", code: "mg/dL" },
        referenceRange: [{ low: { value: 0.7, unit: "mg/dL" }, high: { value: 1.3, unit: "mg/dL" } }]
      }
    }
  ]
}, null, 2);

export const SAMPLE_DIAGNOSTIC_REPORT = JSON.stringify({
  resourceType: "DiagnosticReport",
  id: "diag-rep-001",
  status: "final",
  category: [{ coding: [{ system: "http://terminology.hl7.org/CodeSystem/v2-0074", code: "CH" }] }],
  code: { coding: [{ system: "http://loinc.org", code: "24323-8", display: "Comprehensive metabolic panel" }] },
  subject: { reference: "Patient/pat-001" },
  effectiveDateTime: "2026-09-04T08:00:00Z",
  conclusion: "All metabolic markers and electrolyte panels within normal healthy limits."
}, null, 2);

export const SAMPLE_CONSENT = JSON.stringify({
  resourceType: "Consent",
  id: "consent-001",
  status: "active",
  scope: { coding: [{ system: "http://terminology.hl7.org/CodeSystem/consentscope", code: "patient-privacy" }] },
  patient: { reference: "Patient/pat-001" },
  dateTime: "2026-09-04T09:00:00Z",
  policy: [{ uri: "http://medisphere.com/policies/health-twin-data-sharing" }]
}, null, 2);

export const SAMPLE_INVALID_RESOURCE = JSON.stringify({
  resourceType: "Observation",
  id: "obs-invalid-missing-fields",
  subject: { reference: "Patient/pat-001" },
  valueQuantity: { value: 150.0, unit: "mg/dL" },
  note: [{ text: "Intentionally malformed FHIR observation missing mandatory status and code elements." }]
}, null, 2);
