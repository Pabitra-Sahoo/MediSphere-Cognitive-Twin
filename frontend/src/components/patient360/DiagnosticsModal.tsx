import React, { useState, useEffect } from 'react';
import { useAuth } from '../../auth/useAuth';
import { patientApi } from '../../api/patientApi';
import { authApi } from '../../api/authApi';
import { ProtectedRoute } from '../../auth/ProtectedRoute';

export interface DiagnosticsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const DiagnosticsModal: React.FC<DiagnosticsModalProps> = ({
  isOpen,
  onClose,
}) => {
  const { user } = useAuth();
  const [testResult, setTestResult] = useState<{
    endpoint: string;
    status: number | string;
    data: unknown;
    error?: boolean;
  } | null>(null);
  const [testLoading, setTestLoading] = useState(false);
  const [showAdminPreview, setShowAdminPreview] = useState(false);

  // Keyboard Escape listener
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

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
        endpoint: 'GET /api/status (Public)',
        status: axiosErr.response?.status || 'Error',
        data: axiosErr.response?.data || String(err),
        error: true,
      });
    } finally {
      setTestLoading(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="diagnostics-modal-title"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <div className="modal-title-group">
            <span className="modal-icon" aria-hidden="true">⚙️</span>
            <h3 id="diagnostics-modal-title" className="modal-title">System & RBAC Diagnostics</h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="modal-close-btn"
            aria-label="Close diagnostics dialog"
          >
            ✕
          </button>
        </div>

        <div className="modal-body">
          <p className="modal-intro">
            Secondary verification utilities for M1 authorization boundaries and public endpoints.
          </p>

          {/* User Session Diagnostics */}
          <div className="diagnostics-subpanel">
            <h4 className="subpanel-title">Active Security Principal & Scopes</h4>
            <div className="session-grid compact">
              <div className="session-field">
                <label>User ID</label>
                <code>{user?.id || 'N/A'}</code>
              </div>
              <div className="session-field">
                <label>Username</label>
                <span>{user?.username}</span>
              </div>
              <div className="session-field">
                <label>Role</label>
                <span>{user?.role}</span>
              </div>
              <div className="session-field">
                <label>Linked Patient</label>
                <span>{user?.linkedPatientId || '—'}</span>
              </div>
              <div className="session-field">
                <label>Linked Provider</label>
                <span>{user?.linkedProviderId || '—'}</span>
              </div>
            </div>

            <div className="scopes-section compact">
              <label>Claimed SMART-on-FHIR Scopes:</label>
              <div className="scopes-list">
                {user?.scopes && user.scopes.length > 0 ? (
                  user.scopes.map((scope) => (
                    <span key={scope} className="scope-tag">
                      {scope}
                    </span>
                  ))
                ) : (
                  <span className="no-scopes">None</span>
                )}
              </div>
            </div>
          </div>

          {/* RBAC Quick Test Buttons */}
          <div className="diagnostics-subpanel">
            <h4 className="subpanel-title">Authorization Barrier Tests</h4>
            <div className="test-button-row">
              {user?.role === 'PROVIDER' && (
                <button
                  type="button"
                  onClick={() => handleTestUnassignedAccess('pat-003')}
                  className="btn btn-warning btn-sm"
                  disabled={testLoading}
                >
                  Test Unassigned Access (pat-003) → Expect 403
                </button>
              )}

              {user?.role === 'PATIENT' && (
                <button
                  type="button"
                  onClick={() => handleTestUnassignedAccess('pat-002')}
                  className="btn btn-warning btn-sm"
                  disabled={testLoading}
                >
                  Test Foreign Patient Access (pat-002) → Expect 403
                </button>
              )}

              <button
                type="button"
                onClick={handleTestStatus}
                className="btn btn-secondary btn-sm"
                disabled={testLoading}
              >
                Verify Public Health (GET /api/status)
              </button>

              <button
                type="button"
                onClick={() => setShowAdminPreview((prev) => !prev)}
                className="btn btn-outline btn-sm"
              >
                {showAdminPreview ? 'Hide' : 'Test'} ProtectedRoute
              </button>
            </div>
          </div>

          {showAdminPreview && (
            <div className="admin-protected-preview" style={{ marginTop: '0.75rem' }}>
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <div className="admin-only-content">
                  <span className="admin-check">✓</span>
                  <strong>Client-Side Guard Verified:</strong> Current user has the privileged
                  ADMIN role.
                </div>
              </ProtectedRoute>
            </div>
          )}

          {testResult && (
            <div
              className={`test-result-box ${testResult.error ? 'result-error' : 'result-success'}`}
              style={{ marginTop: '1rem' }}
            >
              <div className="result-header">
                <strong>{testResult.endpoint}</strong>
                <span className="result-code">HTTP {testResult.status}</span>
              </div>
              <pre className="result-json">{JSON.stringify(testResult.data, null, 2)}</pre>
            </div>
          )}
        </div>

        <div className="modal-footer">
          <button type="button" onClick={onClose} className="btn btn-secondary btn-sm">
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
