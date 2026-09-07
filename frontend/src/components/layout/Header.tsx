import React from 'react';
import { Activity, User, LogOut, FlaskConical, Menu, X } from 'lucide-react';
import { useAuth } from '../../auth/useAuth';
import { Badge } from '../common/Badge';
import type { Patient } from '../../types/patient';

export interface HeaderProps {
  selectedPatient: Patient | null;
  onOpenDiagnostics?: () => void;
  isMobileNavOpen?: boolean;
  onToggleMobileNav?: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  selectedPatient,
  onOpenDiagnostics,
  isMobileNavOpen = false,
  onToggleMobileNav,
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
      <div className="shell-header-brand-group">
        {onToggleMobileNav && (
          <button
            type="button"
            className="mobile-nav-toggle-btn"
            onClick={onToggleMobileNav}
            aria-label={isMobileNavOpen ? 'Close navigation menu' : 'Open navigation menu'}
            aria-expanded={isMobileNavOpen}
            aria-controls="mobile-navigation-drawer"
          >
            {isMobileNavOpen ? (
              <X size={20} strokeWidth={2} />
            ) : (
              <Menu size={20} strokeWidth={2} />
            )}
          </button>
        )}

        <div className="shell-header-brand">
          <div className="brand-badge-icon" aria-hidden="true">
            <Activity size={20} strokeWidth={1.75} />
          </div>
          <div className="brand-text-block">
            <span className="brand-name">MediSphere</span>
            <span className="brand-tagline">Cognitive Twin · Patient 360</span>
          </div>
        </div>
      </div>

      {selectedPatient && (
        <div className="shell-header-patient-banner" aria-label={`Active patient: ${selectedPatient.firstName} ${selectedPatient.lastName}`}>
          <span className="patient-banner-label">Patient</span>
          <span className="patient-banner-name">
            {selectedPatient.firstName} {selectedPatient.lastName}
          </span>
          <span className="patient-banner-mrn">MRN: {selectedPatient.mrn}</span>
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
            <FlaskConical size={12} />
            Diagnostics
          </button>
        )}

        <div className="user-profile-chip" aria-label={`Logged in as ${user?.username || 'User'}, role: ${user?.role || 'GUEST'}`}>
          <span className="user-avatar-glyph" aria-hidden="true">
            <User size={14} strokeWidth={1.75} />
          </span>
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
          <LogOut size={14} />
          Sign Out
        </button>
      </div>
    </header>
  );
};
