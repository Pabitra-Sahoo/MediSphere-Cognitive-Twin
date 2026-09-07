import React, { useState } from 'react';
import { Activity, AlertCircle, Shield, Stethoscope, User } from 'lucide-react';
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
      {/* Ambient Cognitive Twin Background Graphics (Clear, calm, high-tech clinical depth) */}
      <div className="login-bg-decorations" aria-hidden="true">
        <div className="login-ambient-orb orb-primary" />
        <div className="login-ambient-orb orb-secondary" />
        <div className="login-ambient-orb orb-tertiary" />
        <svg
          className="login-bg-svg"
          viewBox="0 0 1200 800"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
        >
          {/* Distinct concentric twin calibration rings */}
          <circle cx="600" cy="400" r="180" stroke="rgba(59, 130, 246, 0.22)" strokeWidth="1.5" strokeDasharray="6 8" />
          <circle cx="600" cy="400" r="300" stroke="rgba(59, 130, 246, 0.16)" strokeWidth="1.5" />
          <circle cx="600" cy="400" r="420" stroke="rgba(59, 130, 246, 0.12)" strokeWidth="1.5" strokeDasharray="5 7" />
          <circle cx="600" cy="400" r="540" stroke="rgba(59, 130, 246, 0.08)" strokeWidth="1" />

          {/* Coordinate axis crosshairs */}
          <line x1="120" y1="400" x2="1080" y2="400" stroke="rgba(59, 130, 246, 0.15)" strokeWidth="1.2" strokeDasharray="6 6" />
          <line x1="600" y1="40" x2="600" y2="760" stroke="rgba(59, 130, 246, 0.15)" strokeWidth="1.2" strokeDasharray="6 6" />

          {/* Precision telemetry coordinate cross markers */}
          <path d="M 280 220 L 300 220 M 290 210 L 290 230" stroke="rgba(59, 130, 246, 0.28)" strokeWidth="1.5" strokeLinecap="round" />
          <path d="M 900 220 L 920 220 M 910 210 L 910 230" stroke="rgba(59, 130, 246, 0.28)" strokeWidth="1.5" strokeLinecap="round" />
          <path d="M 280 580 L 300 580 M 290 570 L 290 590" stroke="rgba(59, 130, 246, 0.28)" strokeWidth="1.5" strokeLinecap="round" />
          <path d="M 900 580 L 920 580 M 910 570 L 910 590" stroke="rgba(59, 130, 246, 0.28)" strokeWidth="1.5" strokeLinecap="round" />

          {/* Clear vital telemetry ECG wave */}
          <path
            d="M 80 400 L 380 400 L 410 365 L 435 435 L 465 340 L 500 450 L 525 400 L 675 400 L 700 375 L 720 425 L 740 390 L 760 400 L 1120 400"
            stroke="url(#pulseGrad)"
            strokeWidth="2.2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />

          <defs>
            <linearGradient id="pulseGrad" x1="80" y1="400" x2="1120" y2="400" gradientUnits="userSpaceOnUse">
              <stop offset="0%" stopColor="#3B82F6" stopOpacity="0" />
              <stop offset="25%" stopColor="#3B82F6" stopOpacity="0.7" />
              <stop offset="50%" stopColor="#22A06B" stopOpacity="0.95" />
              <stop offset="75%" stopColor="#3B82F6" stopOpacity="0.7" />
              <stop offset="100%" stopColor="#3B82F6" stopOpacity="0" />
            </linearGradient>
          </defs>
        </svg>
      </div>

      <div className="login-card">
        <div className="login-brand">
          <div className="login-brand-icon" aria-hidden="true">
            <Activity size={26} strokeWidth={2} />
          </div>
          <h1 className="brand-title">MediSphere</h1>
          <p className="brand-subtitle">Cognitive Twin Platform</p>
        </div>

        {(error || localError) && (
          <div className="alert-banner alert-error" role="alert">
            <AlertCircle size={16} aria-hidden="true" />
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
            {isLoading ? 'Signing In…' : 'Sign In'}
          </button>
        </form>

        {/* Demo Accounts — Clean single horizontal row of small pills */}
        <div className="demo-accounts-row">
          <span className="demo-label">Demo:</span>
          <button
            type="button"
            className="demo-pill-btn badge-admin"
            onClick={() => handleQuickFill('admin', 'Admin@123')}
            title="Auto-fill Admin credentials"
          >
            <Shield size={11} />
            <span>Admin</span>
          </button>
          <button
            type="button"
            className="demo-pill-btn badge-provider"
            onClick={() => handleQuickFill('dr_smith', 'Provider@123')}
            title="Auto-fill Provider credentials"
          >
            <Stethoscope size={11} />
            <span>Provider</span>
          </button>
          <button
            type="button"
            className="demo-pill-btn badge-patient"
            onClick={() => handleQuickFill('john_doe', 'Patient@123')}
            title="Auto-fill Patient credentials"
          >
            <User size={11} />
            <span>Patient</span>
          </button>
        </div>
      </div>
    </div>
  );
};
