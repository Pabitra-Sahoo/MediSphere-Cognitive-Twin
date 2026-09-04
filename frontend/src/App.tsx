import React, { useState } from 'react';
import { AuthProvider } from './auth/AuthContext';
import { useAuth } from './auth/useAuth';
import { LoginPage } from './auth/LoginPage';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { authApi } from './api/authApi';
import './styles/index.css';

/**
 * Inner component that renders either LoginPage or Authenticated Dashboard.
 */
const MainContent: React.FC = () => {
  const { user, token, isAuthenticated, logout, isLoading } = useAuth();
  const [testResult, setTestResult] = useState<{
    endpoint: string;
    status: number | string;
    data: unknown;
    error?: boolean;
  } | null>(null);
  const [testLoading, setTestLoading] = useState(false);
  const [showAdminSection, setShowAdminSection] = useState(false);

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

  const handleTestMe = async () => {
    setTestLoading(true);
    try {
      const data = await authApi.getMe();
      setTestResult({
        endpoint: 'GET /api/auth/me (Authenticated)',
        status: 200,
        data,
      });
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status: number; data: unknown } };
      setTestResult({
        endpoint: 'GET /api/auth/me',
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

  const handleTestRegister = async () => {
    setTestLoading(true);
    const testUsername = `user_${Date.now().toString().slice(-4)}`;
    try {
      const newUser = await authApi.register({
        username: testUsername,
        email: `${testUsername}@medisphere.local`,
        password: 'Password@123',
        role: 'PATIENT',
        linkedPatientId: 'pat-test',
      });
      setTestResult({
        endpoint: 'POST /api/auth/register (ADMIN Only RBAC Test)',
        status: 201,
        data: newUser,
      });
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status: number; data: unknown } };
      setTestResult({
        endpoint: 'POST /api/auth/register (ADMIN Only RBAC Test)',
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
            <span className="navbar-version">Cognitive Twin • Phase 2 Auth</span>
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
              <label>Email</label>
              <span>{user.email}</span>
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

          {token && (
            <div className="token-preview">
              <label>Bearer Token (Signed HMAC-SHA256)</label>
              <div className="token-box">
                <code>
                  {token.substring(0, 36)}...{token.substring(token.length - 20)}
                </code>
              </div>
            </div>
          )}
        </section>

        {/* RBAC Verification Controls */}
        <section className="dashboard-card rbac-test-card">
          <div className="card-header">
            <h2>RBAC & Security Verification</h2>
            <p className="card-description">
              Verify backend authorization and role enforcement directly against the Spring Boot API.
            </p>
          </div>

          <div className="test-actions-bar">
            <button
              onClick={handleTestMe}
              className="btn btn-secondary"
              disabled={testLoading}
            >
              Verify Session (GET /api/auth/me)
            </button>

            <button
              onClick={handleTestStatus}
              className="btn btn-secondary"
              disabled={testLoading}
            >
              Check Public Health (GET /api/status)
            </button>

            <button
              onClick={handleTestRegister}
              className={`btn ${user.role === 'ADMIN' ? 'btn-success' : 'btn-warning'}`}
              disabled={testLoading}
            >
              Test Register User (ADMIN Only)
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
                  manage platform users and configuration.
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
