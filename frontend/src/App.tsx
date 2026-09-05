import React, { useState, useEffect, useCallback } from 'react';
import { AuthProvider } from './auth/AuthContext';
import { useAuth } from './auth/useAuth';
import { LoginPage } from './auth/LoginPage';
import { patientApi } from './api/patientApi';
import type { Patient, PatientSummary } from './types/patient';
import type { HealthTwin } from './types/twin';
import { Header } from './components/layout/Header';
import { Sidebar, type NavTab } from './components/layout/Sidebar';
import { PatientSelector } from './components/patient360/PatientSelector';
import { PatientSummary as PatientSummaryView } from './components/patient360/PatientSummary';
import { CompletenessCard } from './components/patient360/CompletenessCard';
import { DigitalTwinViewer } from './components/patient360/DigitalTwinViewer';
import { VitalsPanel } from './components/patient360/VitalsPanel';
import { LabsPanel } from './components/patient360/LabsPanel';
import { FhirPanel } from './components/patient360/FhirPanel';
import { ConsentPanel } from './components/patient360/ConsentPanel';
import { AuditActivityPanel } from './components/patient360/AuditActivityPanel';
import { DiagnosticsModal } from './components/patient360/DiagnosticsModal';
import { LoadingState } from './components/common/LoadingState';
import { ErrorState } from './components/common/ErrorState';
import { EmptyState } from './components/common/EmptyState';
import { Card } from './components/common/Card';
import './styles/index.css';

const MainShell: React.FC = () => {
  const { user, isAuthenticated, isLoading } = useAuth();

  // Navigation and Modal State
  const [activeTab, setActiveTab] = useState<NavTab>('overview');
  const [isDiagnosticsOpen, setIsDiagnosticsOpen] = useState(false);

  // Patient Listing & Selection State
  const [authorizedPatients, setAuthorizedPatients] = useState<PatientSummary[]>([]);
  const [patientsLoading, setPatientsLoading] = useState(false);
  const [selectedPatientId, setSelectedPatientId] = useState<string>('');

  // Selected Patient Record & Digital Twin State
  const [selectedPatient, setSelectedPatient] = useState<Patient | null>(null);
  const [selectedTwin, setSelectedTwin] = useState<HealthTwin | null>(null);
  const [recordLoading, setRecordLoading] = useState(false);
  const [recordError, setRecordError] = useState<unknown>(null);
  const [auditVersion, setAuditVersion] = useState(0);

  /**
   * Load authorized patients strictly from authoritative backend response:
   * GET /api/patients
   */
  const loadAuthorizedPatients = useCallback(async () => {
    if (!user) return;
    setPatientsLoading(true);
    try {
      if (user.role === 'ADMIN' || user.role === 'PROVIDER') {
        const paged = await patientApi.getPatients(0, 50);
        setAuthorizedPatients(paged.content);
        if (paged.content.length > 0) {
          setSelectedPatientId((prev) => {
            const exists = paged.content.some((p) => p.id === prev);
            return exists ? prev : paged.content[0].id;
          });
        }
      } else if (user.role === 'PATIENT' && user.linkedPatientId) {
        setSelectedPatientId(user.linkedPatientId);
      }
    } catch (err) {
      console.error('Failed to retrieve authorized patients:', err);
    } finally {
      setPatientsLoading(false);
    }
  }, [user]);

  /**
   * Fetch patient record and health twin for the currently selected patient ID.
   * If a 403 or error occurs, clear state immediately to prevent stale data leakage.
   */
  const loadPatientDetails = useCallback(async (patientId: string) => {
    if (!patientId) {
      setSelectedPatient(null);
      setSelectedTwin(null);
      return;
    }
    setRecordLoading(true);
    setRecordError(null);
    try {
      const [patientData, twinData] = await Promise.all([
        patientApi.getPatient(patientId),
        patientApi.getTwin(patientId),
      ]);
      setSelectedPatient(patientData);
      setSelectedTwin(twinData);
    } catch (err) {
      // Clear sensitive protected data on authorization error
      setSelectedPatient(null);
      setSelectedTwin(null);
      setRecordError(err);
    } finally {
      setRecordLoading(false);
    }
  }, []);

  // Initial load when user logs in
  useEffect(() => {
    if (!isAuthenticated || !user) return;
    let ignore = false;
    if (user.role === 'ADMIN' || user.role === 'PROVIDER') {
      patientApi.getPatients(0, 50)
        .then((paged) => {
          if (!ignore) {
            setAuthorizedPatients(paged.content);
            if (paged.content.length > 0) {
              setSelectedPatientId((prev) => {
                const exists = paged.content.some((p) => p.id === prev);
                return exists ? prev : paged.content[0].id;
              });
            }
          }
        })
        .catch((err) => {
          console.error('Failed to retrieve authorized patients:', err);
        });
    } else if (user.role === 'PATIENT' && user.linkedPatientId) {
      const pid = user.linkedPatientId;
      queueMicrotask(() => {
        if (!ignore) {
          setSelectedPatientId(pid);
        }
      });
    }
    return () => {
      ignore = true;
    };
  }, [isAuthenticated, user]);

  // Load details whenever selected patient ID changes
  useEffect(() => {
    if (!selectedPatientId) {
      queueMicrotask(() => {
        setSelectedPatient(null);
        setSelectedTwin(null);
      });
      return;
    }
    let ignore = false;
    queueMicrotask(() => {
      if (!ignore) {
        setRecordLoading(true);
        setRecordError(null);
      }
    });

    Promise.all([
      patientApi.getPatient(selectedPatientId),
      patientApi.getTwin(selectedPatientId),
    ])
      .then(([patientData, twinData]) => {
        if (!ignore) {
          setSelectedPatient(patientData);
          setSelectedTwin(twinData);
        }
      })
      .catch((err) => {
        if (!ignore) {
          setSelectedPatient(null);
          setSelectedTwin(null);
          setRecordError(err);
        }
      })
      .finally(() => {
        if (!ignore) {
          setRecordLoading(false);
        }
      });

    return () => {
      ignore = true;
    };
  }, [selectedPatientId]);

  // Full-screen loading while checking session
  if (isLoading) {
    return (
      <div className="login-container">
        <LoadingState message="Initializing MediSphere Cognitive Twin session..." />
      </div>
    );
  }

  // If not authenticated, render login page
  if (!isAuthenticated || !user) {
    return <LoginPage />;
  }

  const patientFullName = selectedPatient
    ? `${selectedPatient.firstName} ${selectedPatient.lastName}`
    : selectedPatientId;

  return (
    <div className="shell-wrapper">
      {/* Top Header */}
      <Header
        selectedPatient={selectedPatient}
        onOpenDiagnostics={() => setIsDiagnosticsOpen(true)}
      />

      <div className="shell-body">
        {/* Navigation Sidebar */}
        <Sidebar
          activeTab={activeTab}
          onTabChange={setActiveTab}
          selectedPatientId={selectedPatientId}
        />

        {/* Central Content Area */}
        <main className="shell-main-content">
          {/* Patient Selector */}
          <PatientSelector
            patients={authorizedPatients}
            selectedPatientId={selectedPatientId}
            onSelectPatient={setSelectedPatientId}
            isLoading={patientsLoading}
          />

          {/* Authorization or Loading States */}
          {recordError ? (
            <ErrorState
              error={recordError}
              title="Patient Record Access Denied (403 Forbidden)"
              message="You do not have active authorization or consent to view this patient's digital twin."
              onRetry={() => loadPatientDetails(selectedPatientId)}
            />
          ) : recordLoading ? (
            <Card>
              <LoadingState message="Retrieving real-time clinical twin and patient records..." />
            </Card>
          ) : !selectedPatient ? (
            <EmptyState
              icon="👤"
              title="No Patient Selected"
              description="Please select an authorized patient from the dropdown above to inspect their digital twin."
            />
          ) : (
            <>
              {/* Prominent Patient Summary Banner */}
              <PatientSummaryView patient={selectedPatient} twin={selectedTwin} />

              {/* View Tab Contents */}
              {activeTab === 'overview' && (
                <div className="overview-two-col-grid">
                  {/* Left Column: 3D Twin Viewport + Completeness Card */}
                  <div className="overview-left-col">
                    <DigitalTwinViewer
                      patientName={patientFullName}
                      patientId={selectedPatient.id}
                      vitals={selectedTwin?.latestVitals}
                      labs={selectedTwin?.latestLabs}
                      completeness={selectedTwin?.completeness}
                    />
                    <CompletenessCard completeness={selectedTwin?.completeness} />
                  </div>

                  {/* Right Column: Latest Vitals & Latest Labs Panels */}
                  <div className="overview-right-col">
                    <VitalsPanel
                      key={`vitals-ov-${selectedPatient.id}`}
                      patientId={selectedPatient.id}
                      twinVitals={selectedTwin?.latestVitals}
                      showHistory={false}
                    />
                    <LabsPanel
                      key={`labs-ov-${selectedPatient.id}`}
                      patientId={selectedPatient.id}
                      twinLabs={selectedTwin?.latestLabs}
                      showHistory={false}
                    />
                  </div>
                </div>
              )}

              {activeTab === 'patient360' && (
                <div className="patient-360-full-view">
                  <CompletenessCard completeness={selectedTwin?.completeness} />

                  <Card
                    title="Digital Twin Demographics (6 Logical Fields)"
                    subtitle="Biometric baseline parameters for digital twin modeling"
                  >
                    <div className="twin-metrics-grid">
                      <div className="twin-metric-card">
                        <h4>Height</h4>
                        <span>{selectedTwin?.demographics?.height ? `${selectedTwin.demographics.height} cm` : '—'}</span>
                      </div>
                      <div className="twin-metric-card">
                        <h4>Weight</h4>
                        <span>{selectedTwin?.demographics?.weight ? `${selectedTwin.demographics.weight} kg` : '—'}</span>
                      </div>
                      <div className="twin-metric-card">
                        <h4>BMI</h4>
                        <span>{selectedTwin?.demographics?.bmi ? selectedTwin.demographics.bmi : '—'}</span>
                      </div>
                      <div className="twin-metric-card">
                        <h4>Blood Type</h4>
                        <span>{selectedTwin?.demographics?.bloodType || '—'}</span>
                      </div>
                      <div className="twin-metric-card">
                        <h4>Age</h4>
                        <span>{selectedTwin?.demographics?.age !== undefined ? `${selectedTwin.demographics.age} yrs` : '—'}</span>
                      </div>
                      <div className="twin-metric-card">
                        <h4>Gender</h4>
                        <span>{selectedTwin?.demographics?.gender || '—'}</span>
                      </div>
                    </div>
                  </Card>

                  <div className="overview-two-col-grid">
                    <VitalsPanel
                      key={`vitals-p360-${selectedPatient.id}`}
                      patientId={selectedPatient.id}
                      twinVitals={selectedTwin?.latestVitals}
                      showHistory={false}
                    />
                    <LabsPanel
                      key={`labs-p360-${selectedPatient.id}`}
                      patientId={selectedPatient.id}
                      twinLabs={selectedTwin?.latestLabs}
                      showHistory={false}
                    />
                  </div>
                </div>
              )}

              {activeTab === 'vitals' && (
                <VitalsPanel
                  key={`vitals-tab-${selectedPatient.id}`}
                  patientId={selectedPatient.id}
                  twinVitals={selectedTwin?.latestVitals}
                  showHistory={true}
                />
              )}

              {activeTab === 'labs' && (
                <LabsPanel
                  key={`labs-tab-${selectedPatient.id}`}
                  patientId={selectedPatient.id}
                  twinLabs={selectedTwin?.latestLabs}
                  showHistory={true}
                />
              )}

              {activeTab === 'fhir' && (
                <FhirPanel
                  key={`fhir-tab-${selectedPatient.id}`}
                  patientId={selectedPatient.id}
                  syncStatus={selectedTwin?.fhirSyncStatus}
                  onIngestionSuccess={() => {
                    loadAuthorizedPatients();
                    loadPatientDetails(selectedPatient.id);
                  }}
                />
              )}

              {activeTab === 'consent' && (
                <ConsentPanel
                  key={`consent-tab-${selectedPatient.id}`}
                  patientId={selectedPatient.id}
                  patientName={patientFullName}
                  onConsentMutated={() => {
                    setAuditVersion((v) => v + 1);
                  }}
                  onNavigateToAudit={() => {
                    setActiveTab('audit');
                  }}
                />
              )}

              {activeTab === 'audit' && (
                <AuditActivityPanel
                  key={`audit-tab-${selectedPatient.id}-${auditVersion}`}
                  patientId={selectedPatient.id}
                />
              )}
            </>
          )}
        </main>
      </div>

      {/* Secondary System Diagnostics Modal (Preserving M1 RBAC Tests) */}
      <DiagnosticsModal
        isOpen={isDiagnosticsOpen}
        onClose={() => setIsDiagnosticsOpen(false)}
      />
    </div>
  );
};

export function App() {
  return (
    <AuthProvider>
      <MainShell />
    </AuthProvider>
  );
}

export default App;
