import React, { useState } from 'react';
import { useAuth } from './useAuth';

export const LoginPage: React.FC = () => {
  const { login, error, clearError, isLoading } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError(null);
    clearError();

    if (!username.trim() || !password.trim()) {
      setLocalError('Please enter both username and password.');
      return;
    }

    try {
      await login({ username: username.trim(), password: password.trim() });
    } catch {
      // Error handled by AuthContext
    }
  };

  const handleQuickFill = (u: string, p: string) => {
    setUsername(u);
    setPassword(p);
    setLocalError(null);
    clearError();
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-brand">
          <div className="brand-logo-icon">⚕️</div>
          <h1 className="brand-title">MediSphere</h1>
          <p className="brand-subtitle">Cognitive Twin Platform</p>
        </div>

        <div className="login-intro">
          <h2>Sign In</h2>
          <p>Secure, role-based access with SMART-on-FHIR scopes</p>
        </div>

        {(error || localError) && (
          <div className="alert-banner alert-error" role="alert">
            <span className="alert-icon">⚠️</span>
            <span>{localError || error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="e.g. dr_smith or admin"
              disabled={isLoading}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
              disabled={isLoading}
              required
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-block"
            disabled={isLoading}
          >
            {isLoading ? 'Signing In...' : 'Sign In'}
          </button>
        </form>

        <div className="demo-credentials-section">
          <div className="demo-header">
            <span className="demo-tag">DEMO ACCOUNTS</span>
            <span className="demo-sub">Click any account to quick-fill credentials</span>
          </div>

          <div className="demo-cards-grid">
            <button
              type="button"
              className="demo-card-btn"
              onClick={() => handleQuickFill('admin', 'Admin@123')}
            >
              <div className="demo-role-badge badge-admin">ADMIN</div>
              <div className="demo-user-name">admin</div>
              <div className="demo-scopes-preview">
                <code>system/*.read</code> <code>user/*.write</code>
              </div>
            </button>

            <button
              type="button"
              className="demo-card-btn"
              onClick={() => handleQuickFill('dr_smith', 'Provider@123')}
            >
              <div className="demo-role-badge badge-provider">PROVIDER</div>
              <div className="demo-user-name">dr_smith</div>
              <div className="demo-scopes-preview">
                <code>patient/*.read</code> <code>patient/*.write</code>
              </div>
            </button>

            <button
              type="button"
              className="demo-card-btn"
              onClick={() => handleQuickFill('john_doe', 'Patient@123')}
            >
              <div className="demo-role-badge badge-patient">PATIENT</div>
              <div className="demo-user-name">john_doe</div>
              <div className="demo-scopes-preview">
                <code>patient/*.read</code>
              </div>
            </button>
          </div>
        </div>

        <div className="login-security-footer">
          <span className="security-shield">🔒</span>
          <span>Stateless JWT Authentication • BCrypt Hashing • SMART-on-FHIR Scopes</span>
        </div>
      </div>
    </div>
  );
};
