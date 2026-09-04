import React, { useState, useEffect, useCallback } from 'react';
import { AuthProvider } from './auth/AuthContext';
import { useAuth } from './auth/useAuth';
import { LoginPage } from './auth/LoginPage';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { authApi } from './api/authApi';
import { patientApi } from './api/patientApi';
import type { PatientSummary, Patient } from './types/patient';
import type { HealthTwin } from './types/twin';
import './styles/index.css';

const MainContent: React.FC = () => {
  const { user, isAuthenticated, logout, isLoading } = useAuth();
  const [testResult, setTestResult] = useState<{
    endpoint: string;
    status: number | string;
    data: unknown;
    error?: boolean;
  } | null>(null);
  const [testLoading, setTestLoading] = useState(false);
  const [showAdminSection, setShowAdminSection] = useState(false);

  // Phase 3: Patients & Twin state
  const [patients, setPatients] = useState<PatientSummary[]>([]);
  const [patientsLoading, setPatientsLoading] = useState(false);
  const [selectedTwin, setSelectedTwin] = useState<HealthTwin | null>(null);
  const [selectedPatient, setSelectedPatient] = useState<Patient | null>(null);
  const [twinLoading, setTwinLoading] = useState(false);

  const loadPatients = useCallback(async () => {
    if (!user) return;
    setPatientsLoading(true);
    try {
      if (user.role === 'ADMIN' || user.role === 'PROVIDER') {
        const paged = await patientApi.getPatients(0, 10);
        setPatients(paged.content);
      } else if (user.role === 'PATIENT' && user.linkedPatientId) {
        // Patient role: load own patient record
        const p = await patientApi.getPatient(user.linkedPatientId);
        setSelectedPatient(p);
        const twin = await patientApi.getTwin(user.linkedPatientId);
        setSelectedTwin(twin);
      }
    } catch (err) {
      console.error('Failed to load patients/twin:', err);
    } finally {
      setPatientsLoading(false);
    }
  }, [user]);

  useEffect(() => {
    if (!isAuthenticated) return;
    let isMounted = true;

    patientApi.getPatients(0, 10)
      .then((paged) => {
        if (isMounted && (user?.role === 'ADMIN' || user?.role === 'PROVIDER')) {
          setPatients(paged.content);
        }
      })
      .catch(() => {
        // Handled silently or on manual refresh
      });

    if (user?.role === 'PATIENT' && user.linkedPatientId) {
      const pid = user.linkedPatientId;
      Promise.all([patientApi.getPatient(pid), patientApi.getTwin(pid)])
        .then(([p, twin]) => {
          if (isMounted) {
            setSelectedPatient(p);
            setSelectedTwin(twin);
          }
        })
        .catch(() => {});
    }

    return () => {
      isMounted = false;
    };
  }, [isAuthenticated, user]);

  if (isLoading) {
    return (
      <div className="app-loading-screen">
        <div className="spinner"></div>
        <p>Loading MediSphere...</p>
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return <LoginPage />;
  }

  const handleInspectTwin = async (patientId: string) => {
    setTwinLoading(true);
    try {
      const [patientData, twinData] = await Promise.all([
        patientApi.getPatient(patientId),
        patientApi.getTwin(patientId),
      ]);
      setSelectedPatient(patientData);
      setSelectedTwin(twinData);
      setTestResult({
        endpoint: `GET /api/patients/${patientId}/twin`,
        status: 200,
        data: twinData,
      });
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status: number; data: unknown } };
      setTestResult({
        endpoint: `GET /api/patients/${patientId}/twin`,
        status: axiosErr.response?.status || 'Error',
        data: axiosErr.response?.data || String(err),
        error: true,
      });
    } finally {
      setTwinLoading(false);
    }
  };

  const handleTestUnassignedAccess = async (targetPatientId: string) => {
    setTestLoading(true);
    try {
      const data = await patientApi.getPatient(targetPatientId);
      setTestResult({
        endpoint: `GET /api/patients/${targetPatientId} (RBAC Check)`,
        status: 200,
        data,
      });
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status: number; data: unknown } };
      setTestResult({
        endpoint: `GET /api/patients/${targetPatientId} (RBAC Check - Expected 403)`,
        status: axiosErr.response?.status || 'Error',
        data: axiosErr.response?.data || String(err),
        error: true,
      });
    } finally {
      setTestLoading(false);
    }
  };

  const handleTestStatus = async () => {
    setTestLoading(true);
    try {
      const data = await authApi.getStatus();
      setTestResult({
        endpoint: 'GET /api/status (Public)',
        status: 200,
        data,
      });
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status: number; data: unknown } };
      setTestResult({
        endpoint: 'GET /api/status',
        status: axiosErr.response?.status || 'Error',
        data: axiosErr.response?.data || String(err),
        error: true,
      });
    } finally {
      setTestLoading(false);
    }
  };

  return (
    <div className="authenticated-layout">
      <header className="main-navbar">
        <div className="navbar-brand">
          <span className="navbar-logo">⚕️</span>
          <div className="navbar-title-group">
            <span className="navbar-title">MediSphere</span>
            <span className="navbar-version">Cognitive Twin • Phase 3 Core</span>
          </div>
        </div>

        <div className="navbar-user-actions">
          <div className="user-profile-badge">
            <span className="user-avatar-icon">👤</span>
            <span className="user-display-name">{user.username}</span>
            <span className={`role-pill role-${user.role.toLowerCase()}`}>
              {user.role}
            </span>
          </div>
          <button onClick={logout} className="btn btn-outline-danger btn-sm">
            Sign Out
          </button>
        </div>
      </header>

      <main className="dashboard-container">
        {/* User Identity & SMART Scopes Card */}
        <section className="dashboard-card session-card">
          <div className="card-header">
            <h2>Active Authenticated Session</h2>
            <span className="status-indicator-live">● Stateless JWT Validated</span>
          </div>

          <div className="session-grid">
            <div className="session-field">
              <label>User ID</label>
              <code>{user.id || 'N/A'}</code>
            </div>

            <div className="session-field">
              <label>Username</label>
              <span>{user.username}</span>
            </div>

            <div className="session-field">
              <label>Assigned Role</label>
              <span className={`role-pill role-${user.role.toLowerCase()}`}>
                {user.role}
              </span>
            </div>

            <div className="session-field">
              <label>Linked Patient ID</label>
              <span>{user.linkedPatientId || '—'}</span>
            </div>

            <div className="session-field">
              <label>Linked Provider ID</label>
              <span>{user.linkedProviderId || '—'}</span>
            </div>
          </div>

          <div className="scopes-section">
            <label>SMART-on-FHIR Scopes (Claimed in JWT)</label>
            <div className="scopes-list">
              {user.scopes && user.scopes.length > 0 ? (
                user.scopes.map((scope) => (
                  <span key={scope} className="scope-tag">
                    {scope}
                  </span>
                ))
              ) : (
                <span className="no-scopes">No SMART scopes granted</span>
              )}
            </div>
          </div>
        </section>

        {/* Phase 3: Patient & Digital Health Twin Explorer */}
        <section className="dashboard-card patient-twin-card">
          <div className="card-header">
            <div>
              <h2>Patient & Digital Health Twin Explorer</h2>
              <p className="card-description">
                {user.role === 'ADMIN' && 'Showing all system patients (Admin Global View).'}
                {user.role === 'PROVIDER' && 'Showing patients assigned to Dr. Smith (Provider View).'}
                {user.role === 'PATIENT' && 'Showing your personal patient record and Digital Twin.'}
              </p>
            </div>
            <button onClick={loadPatients} className="btn btn-secondary btn-sm" disabled={patientsLoading}>
              {patientsLoading ? 'Refreshing...' : 'Refresh Data'}
            </button>
          </div>

          {/* Patient List (for Admin and Provider) */}
          {(user.role === 'ADMIN' || user.role === 'PROVIDER') && (
            <div className="patients-table-wrapper">
              <table className="patients-table">
                <thead>
                  <tr>
                    <th>MRN</th>
                    <th>Patient Name</th>
                    <th>Gender</th>
                    <th>Date of Birth</th>
                    <th>Twin Completeness</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {patients.length > 0 ? (
                    patients.map((p) => (
                      <tr key={p.id}>
                        <td><code>{p.mrn}</code></td>
                        <td><strong>{p.firstName} {p.lastName}</strong></td>
                        <td>{p.gender}</td>
                        <td>{p.dateOfBirth}</td>
                        <td>
                          <div className="completeness-bar-wrapper">
                            <div
                              className={`completeness-bar-fill ${p.twinCompleteness >= 95 ? 'fill-pass' : 'fill-warn'}`}
                              style={{ width: `${p.twinCompleteness}%` }}
                            ></div>
                            <span className="completeness-percent-text">{p.twinCompleteness}%</span>
                          </div>
                        </td>
                        <td>
                          <button
                            onClick={() => handleInspectTwin(p.id)}
                            className="btn btn-secondary btn-sm"
                            disabled={twinLoading}
                          >
                            Inspect Twin
                          </button>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={6} className="text-muted text-center">
                        {patientsLoading ? 'Loading patients...' : 'No assigned patients found.'}
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          )}

          {/* Selected Patient & Twin Details Panel */}
          {selectedTwin && selectedPatient && (
            <div className="twin-detail-panel">
              <div className="twin-panel-header">
                <h3>
                  Digital Health Twin: {selectedPatient.firstName} {selectedPatient.lastName}
                  <span className="badge-mrn">{selectedPatient.mrn}</span>
                </h3>
                <div className="twin-completeness-badge">
                  <span>Completeness: </span>
                  <strong className={selectedTwin.completeness.percentage >= 95 ? 'text-pass' : 'text-warn'}>
                    {selectedTwin.completeness.percentage}%
                  </strong>
                  <span className="fields-count">
                    ({selectedTwin.completeness.populatedFields}/20 logical fields populated)
                  </span>
                </div>
              </div>

              <div className="twin-metrics-grid">
                <div className="twin-metric-card">
                  <h4>Demographics (6 fields)</h4>
                  <ul>
                    <li>Height: <strong>{selectedTwin.demographics?.height || '—'} cm</strong></li>
                    <li>Weight: <strong>{selectedTwin.demographics?.weight || '—'} kg</strong></li>
                    <li>BMI: <strong>{selectedTwin.demographics?.bmi || '—'}</strong></li>
                    <li>Blood Type: <strong>{selectedTwin.demographics?.bloodType || '—'}</strong></li>
                  </ul>
                </div>

                <div className="twin-metric-card">
                  <h4>Latest Vitals (6 fields)</h4>
                  <ul>
                    <li>Heart Rate: <strong>{selectedTwin.latestVitals?.heartRate || '—'} bpm</strong></li>
                    <li>Blood Pressure: <strong>{selectedTwin.latestVitals?.systolicBP || '—'}/{selectedTwin.latestVitals?.diastolicBP || '—'} mmHg</strong></li>
                    <li>SpO2: <strong>{selectedTwin.latestVitals?.oxygenSaturation || '—'}%</strong></li>
                    <li>Temp: <strong>{selectedTwin.latestVitals?.temperature || '—'} °C</strong></li>
                    <li>Resp. Rate: <strong>{selectedTwin.latestVitals?.respiratoryRate || '—'} /min</strong></li>
                  </ul>
                </div>

                <div className="twin-metric-card">
                  <h4>Latest Labs (4 fields)</h4>
                  <ul>
                    <li>Glucose: <strong>{selectedTwin.latestLabs?.glucose || '—'} mg/dL</strong></li>
                    <li>Cholesterol: <strong>{selectedTwin.latestLabs?.cholesterol || '—'} mg/dL</strong></li>
                    <li>Hemoglobin: <strong>{selectedTwin.latestLabs?.hemoglobin || '—'} g/dL</strong></li>
                    <li>Creatinine: <strong>{selectedTwin.latestLabs?.creatinine || '—'} mg/dL</strong></li>
                  </ul>
                </div>

                <div className="twin-metric-card">
                  <h4>Metadata (4 fields)</h4>
                  <ul>
                    <li>MRN: <strong>{selectedPatient.mrn}</strong></li>
                    <li>Emergency Contact: <strong>{selectedPatient.emergencyContact?.name || '—'}</strong></li>
                    <li>FHIR Status: <strong className="status-synced">{selectedTwin.fhirSyncStatus?.syncStatus || '—'}</strong></li>
                    <li>Assigned Providers: <strong>{selectedPatient.assignedProviderIds?.join(', ') || 'None'}</strong></li>
                  </ul>
                </div>
              </div>
            </div>
          )}
        </section>

        {/* RBAC & Security Verification Controls */}
        <section className="dashboard-card rbac-test-card">
          <div className="card-header">
            <h2>RBAC & Security Verification</h2>
            <p className="card-description">
              Live authorization tests proving provider boundary enforcement and patient record isolation.
            </p>
          </div>

          <div className="test-actions-bar">
            {user.role === 'PROVIDER' && (
              <button
                onClick={() => handleTestUnassignedAccess('pat-003')}
                className="btn btn-warning"
                disabled={testLoading}
              >
                Test Access to Unassigned Patient (pat-003) → Expect 403
              </button>
            )}

            {user.role === 'PATIENT' && (
              <button
                onClick={() => handleTestUnassignedAccess('pat-002')}
                className="btn btn-warning"
                disabled={testLoading}
              >
                Test Access to Another Patient (pat-002) → Expect 403
              </button>
            )}

            <button
              onClick={handleTestStatus}
              className="btn btn-secondary"
              disabled={testLoading}
            >
              Verify Public Status (GET /api/status)
            </button>

            <button
              onClick={() => setShowAdminSection((prev) => !prev)}
              className="btn btn-outline"
            >
              {showAdminSection ? 'Hide' : 'Test'} ProtectedRoute Guard
            </button>
          </div>

          {testResult && (
            <div
              className={`test-result-box ${
                testResult.error ? 'result-error' : 'result-success'
              }`}
            >
              <div className="result-header">
                <strong>{testResult.endpoint}</strong>
                <span className="result-code">HTTP {testResult.status}</span>
              </div>
              <pre className="result-json">
                {JSON.stringify(testResult.data, null, 2)}
              </pre>
            </div>
          )}

          {showAdminSection && (
            <div className="admin-protected-preview">
              <h3>Client-Side ProtectedRoute Component Test:</h3>
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <div className="admin-only-content">
                  <span className="admin-check">✓</span>
                  <strong>Admin Privileged Area:</strong> You have the ADMIN role and can
                  manage platform patients, twins, and users.
                </div>
              </ProtectedRoute>
            </div>
          )}
        </section>
      </main>
    </div>
  );
};

export function App() {
  return (
    <AuthProvider>
      <MainContent />
    </AuthProvider>
  );
}

export default App;
