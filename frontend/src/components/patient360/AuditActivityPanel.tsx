import React, { useState, useEffect, useCallback } from 'react';
import type { AuditLogDTO } from '../../types/audit';
import { auditApi } from '../../api/auditApi';
import { useAuth } from '../../auth/useAuth';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { LoadingState } from '../common/LoadingState';
import { ErrorState } from '../common/ErrorState';
import { EmptyState } from '../common/EmptyState';

export interface AuditActivityPanelProps {
  patientId: string;
}

export const AuditActivityPanel: React.FC<AuditActivityPanelProps> = ({ patientId }) => {
  const { user } = useAuth();
  const [logs, setLogs] = useState<AuditLogDTO[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [viewScope, setViewScope] = useState<'patient' | 'system'>('patient');

  const fetchAuditLogs = useCallback(async () => {
    if (!user) return;
    setIsLoading(true);
    setError(null);
    try {
      if (viewScope === 'system' && user.role === 'ADMIN') {
        const res = await auditApi.getAllAuditLogs(0, 25);
        setLogs(res.content);
      } else {
        const res = await auditApi.getPatientAuditLogs(patientId, 0, 25);
        setLogs(res.content);
      }
    } catch (err) {
      setError(err);
    } finally {
      setIsLoading(false);
    }
  }, [user, viewScope, patientId]);

  useEffect(() => {
    if (!user || user.role === 'PROVIDER') return;
    let ignore = false;
    const req = (viewScope === 'system' && user.role === 'ADMIN')
      ? auditApi.getAllAuditLogs(0, 25)
      : auditApi.getPatientAuditLogs(patientId, 0, 25);

    req
      .then((res) => {
        if (!ignore) {
          setLogs(res.content);
          setError(null);
        }
      })
      .catch((err) => {
        if (!ignore) {
          setError(err);
        }
      });

    return () => {
      ignore = true;
    };
  }, [user, viewScope, patientId]);

  // Provider notice: Providers are not permitted to inspect audit logs under HIPAA access control
  if (user?.role === 'PROVIDER') {
    return (
      <Card
        title="HIPAA Audit Activity"
        subtitle="Security & Compliance Event Trail"
        action={<Badge variant="warning">Access Restricted</Badge>}
      >
        <div className="audit-restricted-notice">
          <div className="restricted-icon">🔒</div>
          <h4 className="restricted-title">Audit Log Access Restricted for Providers</h4>
          <p className="restricted-desc">
            Under HIPAA access control and project authorization rules, clinical audit logs are
            accessible only by the <strong>Patient</strong> (for their own health record) and{' '}
            <strong>Platform Administrators</strong>.
          </p>
          <div className="restricted-badge-row">
            <span className="code-subtle">Authorized Roles: ADMIN, PATIENT (self)</span>
          </div>
        </div>
      </Card>
    );
  }

  if (error) {
    return (
      <Card title="HIPAA Audit Trail">
        <ErrorState
          error={error}
          title="Audit Log Query Denied (403 Forbidden)"
          message="You do not have authorization to view audit logs for this patient."
          onRetry={fetchAuditLogs}
        />
      </Card>
    );
  }

  return (
    <Card
      title="HIPAA-Style Audit Activity"
      subtitle={
        viewScope === 'system'
          ? 'System-wide compliance & authorization trail (Admin Global)'
          : `Append-only audit trail for patient ${patientId}`
      }
      action={
        user?.role === 'ADMIN' && (
          <div className="audit-scope-toggle">
            <button
              type="button"
              className={`btn btn-xs ${viewScope === 'patient' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setViewScope('patient')}
            >
              Patient Logs
            </button>
            <button
              type="button"
              className={`btn btn-xs ${viewScope === 'system' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setViewScope('system')}
            >
              System Logs
            </button>
          </div>
        )
      }
    >
      {isLoading ? (
        <LoadingState message="Loading compliance audit records..." compact />
      ) : logs.length === 0 ? (
        <EmptyState
          icon="📜"
          title="No Audit Records Found"
          description="No security or clinical access events recorded yet."
        />
      ) : (
        <div className="clinical-table-wrapper">
          <table className="clinical-table">
            <thead>
              <tr>
                <th>Timestamp</th>
                <th>Action</th>
                <th>Outcome</th>
                <th>Actor</th>
                <th>Role</th>
                <th>Resource</th>
                <th>Details</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id}>
                  <td>{new Date(log.timestamp).toLocaleString()}</td>
                  <td>
                    <Badge variant="neutral" size="sm">
                      {log.action}
                    </Badge>
                  </td>
                  <td>
                    <Badge
                      variant={log.outcome === 'SUCCESS' ? 'success' : 'danger'}
                      size="sm"
                    >
                      {log.outcome}
                    </Badge>
                  </td>
                  <td>
                    <strong>{log.username}</strong>
                  </td>
                  <td>{log.userRole || '—'}</td>
                  <td>
                    {log.resourceType ? `${log.resourceType}` : '—'}{' '}
                    {log.resourceId && <code className="code-subtle">({log.resourceId})</code>}
                  </td>
                  <td>{log.details || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
};
