import React from 'react';
import { useAuth } from '../../auth/useAuth';
import { Badge } from '../common/Badge';
import type { Patient } from '../../types/patient';

export interface HeaderProps {
  selectedPatient: Patient | null;
  onOpenDiagnostics?: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  selectedPatient,
  onOpenDiagnostics,
}) => {
  const { user, logout } = useAuth();

  const getRoleBadgeVariant = (role?: string) => {
    switch (role) {
      case 'ADMIN':
        return 'admin';
      case 'PROVIDER':
        return 'provider';
      case 'PATIENT':
        return 'patient';
      default:
        return 'neutral';
    }
  };

  return (
    <header className="shell-header">
      <div className="shell-header-brand">
        <div className="brand-badge-icon">⚕️</div>
        <div className="brand-text-block">
          <span className="brand-name">MediSphere</span>
          <span className="brand-tagline">Cognitive Twin • Patient 360</span>
        </div>
      </div>

      {selectedPatient && (
        <div className="shell-header-patient-banner">
          <span className="patient-banner-label">Active Context:</span>
          <span className="patient-banner-name">
            {selectedPatient.firstName} {selectedPatient.lastName}
          </span>
          <span className="patient-banner-mrn">MRN: {selectedPatient.mrn}</span>
          <span className="patient-banner-id">ID: {selectedPatient.id}</span>
        </div>
      )}

      <div className="shell-header-actions">
        {onOpenDiagnostics && (
          <button
            type="button"
            onClick={onOpenDiagnostics}
            className="btn btn-outline btn-xs"
            title="Open M1 RBAC & API verification diagnostics"
            aria-label="Open system diagnostics modal"
          >
            Diagnostics
          </button>
        )}

        <div className="user-profile-chip" aria-label={`Logged in as ${user?.username || 'User'}, role: ${user?.role || 'GUEST'}`}>
          <span className="user-avatar-glyph" aria-hidden="true">👤</span>
          <div className="user-info-text">
            <span className="user-username">{user?.username || 'User'}</span>
            <Badge variant={getRoleBadgeVariant(user?.role)} size="sm">
              {user?.role || 'GUEST'}
            </Badge>
          </div>
        </div>

        <button
          type="button"
          onClick={logout}
          className="btn btn-outline-danger btn-sm"
          title="Sign out of current session"
          aria-label="Sign out of current session"
        >
          Sign Out
        </button>
      </div>
    </header>
  );
};
