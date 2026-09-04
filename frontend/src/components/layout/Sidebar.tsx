import React from 'react';
import { useAuth } from '../../auth/useAuth';

export type NavTab =
  | 'overview'
  | 'patient360'
  | 'vitals'
  | 'labs'
  | 'fhir'
  | 'consent'
  | 'audit';

export interface SidebarProps {
  activeTab: NavTab;
  onTabChange: (tab: NavTab) => void;
  selectedPatientId?: string;
}

interface NavItem {
  id: NavTab;
  label: string;
  icon: string;
  badge?: string;
  allowedRoles?: string[];
}

export const Sidebar: React.FC<SidebarProps> = ({
  activeTab,
  onTabChange,
  selectedPatientId,
}) => {
  const { user } = useAuth();
  const role = user?.role || 'GUEST';

  const navItems: NavItem[] = [
    { id: 'overview', label: 'Overview', icon: '📊' },
    { id: 'patient360', label: 'Patient 360', icon: '👤' },
    { id: 'vitals', label: 'Vitals Stream', icon: '💓' },
    { id: 'labs', label: 'Lab Results', icon: '🧪' },
    { id: 'fhir', label: 'FHIR Resources', icon: '🔥' },
    { id: 'consent', label: 'Consent Directives', icon: '🛡️' },
    {
      id: 'audit',
      label: 'Audit Activity',
      icon: '📜',
      badge: role === 'PROVIDER' ? 'Restricted' : undefined,
    },
  ];

  return (
    <aside className="shell-sidebar">
      <div className="sidebar-section-title">CLINICAL NAVIGATION</div>
      <nav className="sidebar-nav">
        {navItems.map((item) => {
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              type="button"
              className={`sidebar-nav-item ${isActive ? 'active' : ''}`}
              onClick={() => onTabChange(item.id)}
            >
              <span className="nav-item-icon">{item.icon}</span>
              <span className="nav-item-label">{item.label}</span>
              {item.badge && <span className="nav-item-badge">{item.badge}</span>}
            </button>
          );
        })}
      </nav>

      <div className="sidebar-footer-info">
        <div className="sidebar-info-row">
          <span className="info-label">Current Role</span>
          <span className="info-value">{role}</span>
        </div>
        {selectedPatientId && (
          <div className="sidebar-info-row">
            <span className="info-label">Active Patient</span>
            <span className="info-value">{selectedPatientId}</span>
          </div>
        )}
      </div>
    </aside>
  );
};
