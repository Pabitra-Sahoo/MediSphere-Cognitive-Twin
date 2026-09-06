import React, { useState, useEffect, useCallback, useMemo } from 'react';
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

  // Server-side Pagination State (20 items per page)
  const [page, setPage] = useState(0);
  const [pageSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Client-side Safe Filters (applied on currently loaded server page)
  const [filterOutcome, setFilterOutcome] = useState<string>('ALL');
  const [filterAction, setFilterAction] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');

  const fetchAuditLogs = useCallback(
    async (targetPage = page) => {
      if (!user || user.role === 'PROVIDER') return;
      setIsLoading(true);
      setError(null);
      try {
        if (viewScope === 'system' && user.role === 'ADMIN') {
          const res = await auditApi.getAllAuditLogs(targetPage, pageSize);
          setLogs(res.content);
          setTotalPages(res.totalPages);
          setTotalElements(res.totalElements);
        } else {
          const res = await auditApi.getPatientAuditLogs(patientId, targetPage, pageSize);
          setLogs(res.content);
          setTotalPages(res.totalPages);
          setTotalElements(res.totalElements);
        }
      } catch (err) {
        setError(err);
      } finally {
        setIsLoading(false);
      }
    },
    [user, viewScope, patientId, page, pageSize]
  );

  // Reset page when patientId or viewScope changes
  useEffect(() => {
    if (!user || user.role === 'PROVIDER') return;
    let ignore = false;
    queueMicrotask(() => {
      if (!ignore) {
        setPage(0);
        setIsLoading(true);
        setError(null);
      }
    });

    const req =
      viewScope === 'system' && user.role === 'ADMIN'
        ? auditApi.getAllAuditLogs(0, pageSize)
        : auditApi.getPatientAuditLogs(patientId, 0, pageSize);

    req
      .then((res) => {
        if (!ignore) {
          setLogs(res.content);
          setTotalPages(res.totalPages);
          setTotalElements(res.totalElements);
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
  }, [user, viewScope, patientId, pageSize]);

  // Page change handler
  const handlePageChange = (newPage: number) => {
    if (newPage < 0 || newPage >= totalPages || newPage === page) return;
    setPage(newPage);
    fetchAuditLogs(newPage);
  };

  // Safe client-side filtering on current page records
  const filteredLogs = useMemo(() => {
    return logs.filter((log) => {
      // Outcome filter
      if (filterOutcome !== 'ALL' && log.outcome !== filterOutcome) {
        return false;
      }
      // Action filter
      if (filterAction !== 'ALL' && log.action !== filterAction) {
        return false;
      }
      // Search query (username, details, resource, IP)
      if (searchQuery.trim()) {
        const query = searchQuery.toLowerCase();
        const matchesUser = log.username?.toLowerCase().includes(query);
        const matchesDetails = log.details?.toLowerCase().includes(query);
        const matchesResource = log.resourceType?.toLowerCase().includes(query) || log.resourceId?.toLowerCase().includes(query);
        const matchesIp = log.ipAddress?.toLowerCase().includes(query);
        const matchesAction = log.action?.toLowerCase().includes(query);
        if (!matchesUser && !matchesDetails && !matchesResource && !matchesIp && !matchesAction) {
          return false;
        }
      }
      return true;
    });
  }, [logs, filterOutcome, filterAction, searchQuery]);

  // Extract unique actions present in loaded logs for filter dropdown
  const availableActions = useMemo(() => {
    const actionSet = new Set<string>();
    logs.forEach((l) => {
      if (l.action) actionSet.add(l.action);
    });
    return Array.from(actionSet).sort();
  }, [logs]);

  // Provider Notice: Providers are strictly restricted from querying audit logs per HIPAA access control
  if (user?.role === 'PROVIDER') {
    return (
      <Card
        title="HIPAA Audit Activity"
        subtitle="Security & Compliance Event Trail"
        action={<Badge variant="warning">Access Restricted</Badge>}
      >
        <div className="audit-restricted-notice">
          <div className="restricted-icon">🔒</div>
          <h4 className="restricted-title">Audit Log Access Restricted for Healthcare Providers</h4>
          <p className="restricted-desc">
            Under HIPAA access control and MediSphere authorization rules, compliance audit logs are
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
          onRetry={() => fetchAuditLogs(page)}
        />
      </Card>
    );
  }

  const deniedCount = logs.filter((l) => l.outcome === 'DENIED').length;
  const isFiltered = filterOutcome !== 'ALL' || filterAction !== 'ALL' || searchQuery.trim() !== '';

  return (
    <Card
      title="HIPAA-Style Compliance Audit Trail"
      subtitle={
        viewScope === 'system'
          ? `System-wide compliance & authorization events (Admin View) — Page ${page + 1} of ${Math.max(1, totalPages)}`
          : `Append-only audit trail for patient ${patientId} — Page ${page + 1} of ${Math.max(1, totalPages)}`
      }
      action={
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          {deniedCount > 0 && (
            <Badge variant="danger" size="sm">
              {deniedCount} Access Denial{deniedCount > 1 ? 's' : ''} on Page
            </Badge>
          )}
          {user?.role === 'ADMIN' && (
            <div className="audit-scope-toggle">
              <button
                type="button"
                className={`btn btn-xs ${viewScope === 'patient' ? 'btn-primary' : 'btn-secondary'}`}
                onClick={() => {
                  setViewScope('patient');
                  setPage(0);
                }}
                disabled={isLoading}
              >
                Patient Logs
              </button>
              <button
                type="button"
                className={`btn btn-xs ${viewScope === 'system' ? 'btn-primary' : 'btn-secondary'}`}
                onClick={() => {
                  setViewScope('system');
                  setPage(0);
                }}
                disabled={isLoading}
              >
                System Logs
              </button>
            </div>
          )}
          <button
            type="button"
            onClick={() => fetchAuditLogs(page)}
            className="btn btn-secondary btn-xs"
            disabled={isLoading}
          >
            Refresh
          </button>
        </div>
      }
    >
      {/* Search & Safe Client-Side Filter Controls */}
      <div className="audit-filter-bar">
        <div className="audit-filter-controls">
          <div className="audit-filter-item">
            <label className="filter-label">Filter Outcome:</label>
            <select
              className="audit-filter-select"
              value={filterOutcome}
              onChange={(e) => setFilterOutcome(e.target.value)}
            >
              <option value="ALL">All Outcomes</option>
              <option value="SUCCESS">SUCCESS</option>
              <option value="DENIED">DENIED (Security Flags)</option>
              <option value="FAILURE">FAILURE</option>
            </select>
          </div>

          <div className="audit-filter-item">
            <label className="filter-label">Filter Action:</label>
            <select
              className="audit-filter-select"
              value={filterAction}
              onChange={(e) => setFilterAction(e.target.value)}
            >
              <option value="ALL">All Actions ({availableActions.length})</option>
              {availableActions.map((act) => (
                <option key={act} value={act}>
                  {act}
                </option>
              ))}
            </select>
          </div>

          <div className="audit-filter-item audit-search-item">
            <label htmlFor="audit-search-input" className="filter-label">Search Current Page:</label>
            <input
              id="audit-search-input"
              type="text"
              className="audit-search-input"
              placeholder="Search user, action, details..."
              aria-label="Search current audit page by user, action, or details"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
        </div>

        {isFiltered && (
          <div className="audit-filter-reset">
            <span className="filter-status-text">
              Showing <strong>{filteredLogs.length}</strong> of <strong>{logs.length}</strong> loaded events
            </span>
            <button
              type="button"
              className="btn btn-outline btn-xs"
              onClick={() => {
                setFilterOutcome('ALL');
                setFilterAction('ALL');
                setSearchQuery('');
              }}
            >
              Reset Filters
            </button>
          </div>
        )}
      </div>

      {isLoading ? (
        <LoadingState message="Loading compliance audit records from MongoDB repository..." compact />
      ) : filteredLogs.length === 0 ? (
        <EmptyState
          icon="📜"
          title={isFiltered ? 'No Matching Audit Records' : 'No Audit Records Found'}
          description={
            isFiltered
              ? 'No audit events on this page match the selected filter criteria. Try resetting the filters.'
              : 'No security or clinical access events have been recorded for this patient yet.'
          }
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
              {filteredLogs.map((log) => {
                const isDenied = log.outcome === 'DENIED' || log.action === 'ACCESS_DENIED';
                return (
                  <tr key={log.id} className={isDenied ? 'audit-row-denied' : ''}>
                    <td>{new Date(log.timestamp).toLocaleString()}</td>
                    <td>
                      <Badge
                        variant={
                          log.action === 'ACCESS_DENIED'
                            ? 'danger'
                            : log.action === 'CONSENT_REVOKED'
                            ? 'warning'
                            : log.action === 'CONSENT_GRANTED'
                            ? 'success'
                            : 'neutral'
                        }
                        size="sm"
                      >
                        {log.action}
                      </Badge>
                    </td>
                    <td>
                      <Badge
                        variant={
                          log.outcome === 'DENIED'
                            ? 'danger'
                            : log.outcome === 'SUCCESS'
                            ? 'success'
                            : 'warning'
                        }
                        size="sm"
                      >
                        {log.outcome}
                      </Badge>
                    </td>
                    <td>
                      <strong>{log.username || 'System'}</strong>
                      {log.userRole && <div className="code-subtle">{log.userRole}</div>}
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

      {/* Server Pagination Navigation Bar */}
      {totalPages > 1 && (
        <div className="audit-pagination-bar">
          <div className="pagination-info">
            <span>
              Showing Page <strong>{page + 1}</strong> of <strong>{totalPages}</strong> ({totalElements} total events recorded)
            </span>
          </div>
          <div className="pagination-actions">
            <button
              type="button"
              className="btn btn-secondary btn-xs"
              onClick={() => handlePageChange(page - 1)}
              disabled={page === 0 || isLoading}
              aria-label="Go to previous audit page"
            >
              ← Previous Page
            </button>
            <span className="pagination-page-indicator" aria-label={`Current page ${page + 1} of ${totalPages}`}>
              {page + 1} / {totalPages}
            </span>
            <button
              type="button"
              className="btn btn-secondary btn-xs"
              onClick={() => handlePageChange(page + 1)}
              disabled={page >= totalPages - 1 || isLoading}
              aria-label="Go to next audit page"
            >
              Next Page →
            </button>
          </div>
        </div>
      )}
    </Card>
  );
};
