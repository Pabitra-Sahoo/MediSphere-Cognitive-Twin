import React, { type ReactNode } from 'react';
import { useAuth } from './useAuth';
import type { Role } from '../types/auth';

interface ProtectedRouteProps {
  children: ReactNode;
  allowedRoles?: Role[];
}

/**
 * Route protection component.
 *
 * <p>Enforces authentication status and optional role-level restrictions.
 * Displays informative feedback when unauthenticated or forbidden.</p>
 */
export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  allowedRoles,
}) => {
  const { user, isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="auth-loading-state">
        <div className="spinner"></div>
        <p>Verifying secure session...</p>
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return (
      <div className="auth-unauthorized-banner">
        <h3>Authentication Required</h3>
        <p>Please sign in to access this section of MediSphere Cognitive Twin.</p>
      </div>
    );
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return (
      <div className="auth-forbidden-card">
        <div className="forbidden-badge">403 Forbidden</div>
        <h3>Access Denied</h3>
        <p>
          Your current role (<strong>{user.role}</strong>) does not have permission
          to access this resource. Required role(s):{' '}
          {allowedRoles.map((r) => r).join(', ')}.
        </p>
      </div>
    );
  }

  return <>{children}</>;
};
