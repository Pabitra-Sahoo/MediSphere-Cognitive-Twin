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
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [viewScope, setViewScope] = useState<'patient' | 'system'>('patient');

  const fetchAuditLogs = useCallback(async () => {
    if (!user || user.role === 'PROVIDER') return;
    setIsLoading(true);
    setError(null);
    try {
      if (viewScope === 'system' && user.role === 'ADMIN') {
        const res = await auditApi.getAllAuditLogs(0, 50);
        setLogs(res.content);
      } else {
        const res = await auditApi.getPatientAuditLogs(patientId, 0, 50);
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
    queueMicrotask(() => {
      if (!ignore) {
        setIsLoading(true);
        setError(null);
      }
    });

    const req =
      viewScope === 'system' && user.role === 'ADMIN'
        ? auditApi.getAllAuditLogs(0, 50)
        : auditApi.getPatientAuditLogs(patientId, 0, 50);

    req
      .then((res) => {
        if (!ignore) {
          setLogs(res.content);
        }
      })
      .catch((err) => {
        if (!ignore) {
          setError(err);
        }
      })
      .finally(() => {
        if (!ignore) {
          setIsLoading(false);
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

  const deniedCount = logs.filter((l) => l.outcome === 'DENIED').length;

  return (
    <Card
      title="HIPAA-Style Audit Activity"
      subtitle={
        viewScope === 'system'
          ? 'System-wide compliance & authorization trail (Admin Global View)'
          : `Append-only audit trail recorded for patient ${patientId}`
      }
      action={
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          {deniedCount > 0 && (
            <Badge variant="danger" size="sm">
              {deniedCount} Access Denial{deniedCount > 1 ? 's' : ''}
            </Badge>
          )}
          {user?.role === 'ADMIN' && (
            <div className="audit-scope-toggle">
              <button
                type="button"
                className={`btn btn-xs ${viewScope === 'patient' ? 'btn-primary' : 'btn-secondary'}`}
                onClick={() => setViewScope('patient')}
                disabled={isLoading}
              >
                Patient Logs
              </button>
              <button
                type="button"
                className={`btn btn-xs ${viewScope === 'system' ? 'btn-primary' : 'btn-secondary'}`}
                onClick={() => setViewScope('system')}
                disabled={isLoading}
              >
                System Logs
              </button>
            </div>
          )}
          <button
            type="button"
            onClick={fetchAuditLogs}
            className="btn btn-secondary btn-xs"
            disabled={isLoading}
          >
            Refresh
          </button>
        </div>
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
                <th>Event Timestamp</th>
                <th>Action</th>
                <th>Outcome</th>
                <th>Actor / Role</th>
                <th>Target Patient</th>
                <th>Resource Ref</th>
                <th>Access & Denial Details</th>
                <th>IP Address</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => {
                const isDenied = log.outcome === 'DENIED';
                return (
                  <tr key={log.id} className={isDenied ? 'audit-row-denied' : ''}>
                    <td>{new Date(log.timestamp).toLocaleString()}</td>
                    <td>
                      <Badge variant="neutral" size="sm">
                        {log.action}
                      </Badge>
                    </td>
                    <td>
                      <Badge
                        variant={isDenied ? 'danger' : 'success'}
                        size="sm"
                      >
                        {log.outcome}
                      </Badge>
                    </td>
                    <td>
                      <strong>{log.username || 'System'}</strong>
                      {log.userRole && (
                        <div className="code-subtle">{log.userRole}</div>
                      )}
                    </td>
                    <td>
                      {log.patientId ? (
                        <code>Patient/{log.patientId}</code>
                      ) : (
                        <span className="text-muted text-xs">System / Global</span>
                      )}
                    </td>
                    <td>
                      {log.resourceType ? (
                        <span>
                          {log.resourceType}{' '}
                          {log.resourceId && <code className="code-subtle">({log.resourceId})</code>}
                        </span>
                      ) : (
                        '—'
                      )}
                    </td>
                    <td>
                      {isDenied ? (
                        <span className="text-warn" style={{ fontWeight: 600 }}>
                          🔒 {log.details || 'Access Denied by Security Policy'}
                        </span>
                      ) : (
                        <span>{log.details || '—'}</span>
                      )}
                    </td>
                    <td>
                      <span className="code-subtle">{log.ipAddress || '—'}</span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </Card>
  );
};
