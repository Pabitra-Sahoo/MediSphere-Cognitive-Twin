import React, { useEffect } from 'react';
import {
  LayoutDashboard,
  UserRound,
  Activity,
  FlaskConical,
  Database,
  ShieldCheck,
  ScrollText,
  X,
} from 'lucide-react';
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
  isOpen?: boolean;
  onClose?: () => void;
}

interface NavItem {
  id: NavTab;
  label: string;
  icon: React.ReactNode;
  badge?: string;
}

export const Sidebar: React.FC<SidebarProps> = ({
  activeTab,
  onTabChange,
  selectedPatientId,
  isOpen = false,
  onClose,
}) => {
  const { user } = useAuth();
  const role = user?.role || 'GUEST';

  // Handle Escape key to close mobile drawer when open
  useEffect(() => {
    if (!isOpen || !onClose) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  const navItems: NavItem[] = [
    { id: 'overview',   label: 'Overview',           icon: <LayoutDashboard size={16} strokeWidth={1.75} /> },
    { id: 'patient360', label: 'Patient 360',         icon: <UserRound size={16} strokeWidth={1.75} /> },
    { id: 'vitals',     label: 'Vitals Stream',       icon: <Activity size={16} strokeWidth={1.75} /> },
    { id: 'labs',       label: 'Lab Results',         icon: <FlaskConical size={16} strokeWidth={1.75} /> },
    { id: 'fhir',       label: 'FHIR Resources',      icon: <Database size={16} strokeWidth={1.75} /> },
    { id: 'consent',    label: 'Consent Directives',  icon: <ShieldCheck size={16} strokeWidth={1.75} /> },
    {
      id: 'audit',
      label: 'Audit Activity',
      icon: <ScrollText size={16} strokeWidth={1.75} />,
      badge: role === 'PROVIDER' ? 'Restricted' : undefined,
    },
  ];

  const handleNavClick = (id: NavTab) => {
    onTabChange(id);
    if (onClose) {
      onClose();
    }
  };

  return (
    <>
      {/* Mobile Drawer Backdrop */}
      {isOpen && (
        <div
          className="mobile-drawer-backdrop"
          onClick={onClose}
          aria-hidden="true"
        />
      )}

      <aside
        id="mobile-navigation-drawer"
        className={`shell-sidebar ${isOpen ? 'drawer-open' : ''}`}
        aria-label="Clinical navigation drawer"
      >
        {/* Mobile drawer header with close button */}
        <div className="sidebar-drawer-header">
          <div className="drawer-title-group">
            <Activity size={18} className="text-accent" strokeWidth={2} />
            <span className="drawer-title">Clinical Navigation</span>
          </div>
          {onClose && (
            <button
              type="button"
              className="drawer-close-btn"
              onClick={onClose}
              aria-label="Close navigation drawer"
            >
              <X size={18} strokeWidth={2} />
            </button>
          )}
        </div>

        <nav className="sidebar-nav" aria-label="Clinical navigation tabs">
          {navItems.map((item) => {
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                type="button"
                className={`sidebar-nav-item ${isActive ? 'active' : ''}`}
                onClick={() => handleNavClick(item.id)}
                aria-current={isActive ? 'page' : undefined}
              >
                <span className="nav-item-icon" aria-hidden="true">{item.icon}</span>
                <span className="nav-item-label">{item.label}</span>
                {item.badge && <span className="nav-item-badge">{item.badge}</span>}
              </button>
            );
          })}
        </nav>

        <div className="sidebar-footer-info">
          <div className="sidebar-info-row">
            <span className="info-label">Role</span>
            <span className="info-value">{role}</span>
          </div>
          {selectedPatientId && (
            <div className="sidebar-info-row">
              <span className="info-label">Patient</span>
              <span className="info-value" title={selectedPatientId}>{selectedPatientId}</span>
            </div>
          )}
        </div>
      </aside>
    </>
  );
};
