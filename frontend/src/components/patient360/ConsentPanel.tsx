import React, { useState, useEffect, useCallback } from 'react';
import type { ConsentDTO, ConsentVerifyResponse } from '../../types/consent';
import { consentApi } from '../../api/consentApi';
import { useAuth } from '../../auth/useAuth';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { LoadingState } from '../common/LoadingState';
import { ErrorState } from '../common/ErrorState';
import { EmptyState } from '../common/EmptyState';

export interface ConsentPanelProps {
  patientId: string;
  patientName?: string;
  onConsentMutated?: () => void;
  onNavigateToAudit?: () => void;
}

export const ConsentPanel: React.FC<ConsentPanelProps> = ({
  patientId,
  patientName,
  onConsentMutated,
  onNavigateToAudit,
}) => {
  const { user } = useAuth();
  const [consents, setConsents] = useState<ConsentDTO[]>([]);
  const [verifyStatus, setVerifyStatus] = useState<ConsentVerifyResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  // Destructive Revocation Confirmation Modal State
  const [consentToRevoke, setConsentToRevoke] = useState<ConsentDTO | null>(null);

  // Grant Directive Form State
  const [showGrantForm, setShowGrantForm] = useState(false);
  const [grantedTo, setGrantedTo] = useState('prov-001');
  const [scope, setScope] = useState('patient/*.read');
  const [reason, setReason] = useState('Clinical consultation and digital twin monitoring');

  const fetchConsents = useCallback(async () => {
    if (!patientId) return;
    setIsLoading(true);
    setError(null);
    try {
      const data = await consentApi.getConsents(patientId);
      setConsents(data);

      if (user?.role === 'PROVIDER') {
        const verifyRes = await consentApi.verifyConsent(patientId);
        setVerifyStatus(verifyRes);
      } else if (user?.role === 'ADMIN') {
        // Admin checks verification for standard provider prov-001 or first consent grantee
        const targetProvider = data.length > 0 ? data[0].grantedTo : 'prov-001';
        const verifyRes = await consentApi.verifyConsent(patientId, targetProvider);
        setVerifyStatus(verifyRes);
      }
    } catch (err) {
      setError(err);
    } finally {
      setIsLoading(false);
    }
  }, [patientId, user]);

  useEffect(() => {
    let ignore = false;
    queueMicrotask(() => {
      if (!ignore) {
        setIsLoading(true);
        setError(null);
        setActionMessage(null);
        setActionError(null);
        setConsentToRevoke(null);
      }
    });

    const promises: Promise<unknown>[] = [
      consentApi.getConsents(patientId).then((data) => {
        if (!ignore) setConsents(data);
      }),
    ];

    if (user?.role === 'PROVIDER') {
      promises.push(
        consentApi.verifyConsent(patientId).then((verifyRes) => {
          if (!ignore) setVerifyStatus(verifyRes);
        })
      );
    } else if (user?.role === 'ADMIN') {
      promises.push(
        consentApi.verifyConsent(patientId, 'prov-001').then((verifyRes) => {
          if (!ignore) setVerifyStatus(verifyRes);
        })
      );
    }

    Promise.all(promises)
      .catch((err) => {
        if (!ignore) setError(err);
      })
      .finally(() => {
        if (!ignore) setIsLoading(false);
      });

    return () => {
      ignore = true;
    };
  }, [patientId, user]);

  const handleExecuteRevoke = async () => {
    if (!consentToRevoke) return;
    setActionLoading(true);
    setActionMessage(null);
    setActionError(null);

    const targetId = consentToRevoke.id;
    const targetName = consentToRevoke.grantedToName || consentToRevoke.grantedTo;

    try {
      const revoked = await consentApi.revokeConsent(patientId, targetId);
      setActionMessage(
        `Consent directive for ${revoked.grantedToName || revoked.grantedTo || targetName} has been REVOKED.`
      );
      setConsentToRevoke(null);
      await fetchConsents();
      if (onConsentMutated) {
        onConsentMutated();
      }
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setActionError(
        axiosErr.response?.data?.message || 'Failed to revoke consent directive on the backend.'
      );
    } finally {
      setActionLoading(false);
    }
  };

  const handleGrant = async (e: React.FormEvent) => {
    e.preventDefault();
    setActionLoading(true);
    setActionMessage(null);
    setActionError(null);
    try {
      const created = await consentApi.grantConsent(patientId, {
        grantedTo,
        scope,
        reason,
      });
      setActionMessage(
        `Consent directive GRANTED to ${created.grantedToName || created.grantedTo} successfully.`
      );
      setShowGrantForm(false);
      await fetchConsents();
      if (onConsentMutated) {
        onConsentMutated();
      }
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setActionError(
        axiosErr.response?.data?.message || 'Failed to authorize consent directive on the backend.'
      );
    } finally {
      setActionLoading(false);
    }
  };

  // Authoritative Role Alignment:
  // Only PATIENT (for their own record) or ADMIN can manage/grant/revoke consent.
  // PROVIDER role cannot manage consents.
  const canManageConsent = user?.role === 'ADMIN' || user?.role === 'PATIENT';

  if (error) {
    return (
      <Card title="Patient Consent Directives">
        <ErrorState
          error={error}
          title="Consent Directives Access Denied (403 Forbidden)"
          message="You do not have authorization to view or manage consent directives for this patient."
          onRetry={fetchConsents}
        />
      </Card>
    );
  }

  const activeGrantedCount = consents.filter((c) => c.status === 'GRANTED').length;
  const revokedCount = consents.filter((c) => c.status === 'REVOKED').length;

  return (
    <div className="consent-panel-container">
      {/* Provider Verification Card */}
      {(user?.role === 'PROVIDER' || user?.role === 'ADMIN') && (
        <Card
          title="Provider Consent Verification"
          subtitle={`Authoritative access policy determination for ${patientName || patientId}`}
          action={
            verifyStatus ? (
              <Badge
                variant={verifyStatus.hasConsent ? 'success' : 'danger'}
                size="md"
              >
                {verifyStatus.hasConsent ? 'ACTIVE / AUTHORIZED' : 'NO ACTIVE CONSENT'}
              </Badge>
            ) : (
              <Badge variant="neutral" size="md">
                NOT AVAILABLE
              </Badge>
            )
          }
        >
          {verifyStatus ? (
            <div className="consent-verify-details">
              <div className="verify-detail-row">
                <span className="verify-label">Authorization State:</span>
                <span className="verify-value">
                  {verifyStatus.hasConsent ? (
                    <strong className="text-pass">Authorized — Active Consent Directive Present</strong>
                  ) : (
                    <strong className="text-warn">Not Authorized — No Active Consent Found</strong>
                  )}
                </span>
              </div>
              <div className="verify-detail-row">
                <span className="verify-label">Provider Context:</span>
                <span className="verify-value">
                  <code>
                    {user.role === 'PROVIDER'
                      ? user.linkedProviderId || 'prov-001'
                      : 'prov-001 (Admin Inspection)'}
                  </code>
                </span>
              </div>
              {verifyStatus.consentId && (
                <div className="verify-detail-row">
                  <span className="verify-label">Active Directive ID:</span>
                  <span className="verify-value">
                    <code>{verifyStatus.consentId}</code>
                  </span>
                </div>
              )}
              <div className="verify-detail-row">
                <span className="verify-label">SMART Scope:</span>
                <span className="verify-value">
                  <code>{verifyStatus.scope || 'None'}</code>
                </span>
              </div>
              <div className="verify-detail-row">
                <span className="verify-label">Directive Expiry:</span>
                <span className="verify-value">
                  {verifyStatus.expiresAt
                    ? new Date(verifyStatus.expiresAt).toLocaleString()
                    : 'No expiration set'}
                </span>
              </div>
            </div>
          ) : (
            <div className="verify-detail-row">
              <span className="verify-label">Verification:</span>
              <span className="verify-value text-muted">No provider verification query available.</span>
            </div>
          )}
        </Card>
      )}

      {/* Directives Table Card */}
      <Card
        title={`Patient Consent Directives (${consents.length})`}
        subtitle={`Legal authorization directives registered for patient ${patientId}`}
        action={
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
            {activeGrantedCount > 0 && (
              <Badge variant="success" size="sm">
                {activeGrantedCount} GRANTED
              </Badge>
            )}
            {revokedCount > 0 && (
              <Badge variant="neutral" size="sm">
                {revokedCount} REVOKED
              </Badge>
            )}
            <button
              type="button"
              onClick={fetchConsents}
              className="btn btn-secondary btn-xs"
              disabled={isLoading || actionLoading}
            >
              Refresh
            </button>
            {canManageConsent && (
              <button
                type="button"
                onClick={() => {
                  setShowGrantForm((prev) => !prev);
                  setActionMessage(null);
                  setActionError(null);
                }}
                className="btn btn-primary btn-xs"
              >
                {showGrantForm ? 'Cancel Form' : '+ Grant New Consent'}
              </button>
            )}
          </div>
        }
      >
        {actionMessage && (
          <div className="alert-banner alert-success" style={{ marginBottom: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <span>✓</span> {actionMessage}
            </div>
            {onNavigateToAudit && (
              <button
                type="button"
                className="btn btn-secondary btn-xs"
                onClick={onNavigateToAudit}
                style={{ marginLeft: '1rem' }}
              >
                View in Audit Trail →
              </button>
            )}
          </div>
        )}

        {actionError && (
          <div className="alert-banner alert-danger" style={{ marginBottom: '1rem' }}>
            <span>⚠️</span> {actionError}
          </div>
        )}

        {/* Grant Directive Form */}
        {showGrantForm && canManageConsent && (
          <form onSubmit={handleGrant} className="consent-grant-form">
            <h4 className="form-heading">Grant Provider Consent Directive</h4>
            <div className="form-group-row">
              <div className="form-field">
                <label>Provider ID / Grantee</label>
                <input
                  type="text"
                  className="clinical-input"
                  value={grantedTo}
                  onChange={(e) => setGrantedTo(e.target.value)}
                  placeholder="prov-001"
                  required
                />
              </div>
              <div className="form-field">
                <label>SMART Scope</label>
                <input
                  type="text"
                  className="clinical-input"
                  value={scope}
                  onChange={(e) => setScope(e.target.value)}
                  placeholder="patient/*.read"
                  required
                />
              </div>
            </div>
            <div className="form-field">
              <label>Reason / Purpose of Use</label>
              <input
                type="text"
                className="clinical-input"
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="Reason for granting access"
                required
              />
            </div>
            <div className="form-actions">
              <button
                type="submit"
                className="btn btn-primary btn-sm"
                disabled={actionLoading}
              >
                {actionLoading ? 'Authorizing...' : 'Save Directive'}
              </button>
              <button
                type="button"
                onClick={() => setShowGrantForm(false)}
                className="btn btn-secondary btn-sm"
              >
                Cancel
              </button>
            </div>
          </form>
        )}

        {isLoading ? (
          <LoadingState message="Loading patient consent directives..." compact />
        ) : consents.length === 0 ? (
          <EmptyState
            icon="🛡️"
            title="No Consent Directives Available"
            description="No active or historical data sharing directives have been registered for this patient record."
          />
        ) : (
          <div className="clinical-table-wrapper">
            <table className="clinical-table">
              <thead>
                <tr>
                  <th>Grantee Provider</th>
                  <th>SMART Scope</th>
                  <th>Directive Status</th>
                  <th>Granted At</th>
                  <th>Expires At</th>
                  <th>Revoked At</th>
                  <th>Purpose / Reason</th>
                  {canManageConsent && <th>Actions</th>}
                </tr>
              </thead>
              <tbody>
                {consents.map((consent) => (
                  <tr key={consent.id}>
                    <td>
                      <strong>{consent.grantedToName || 'Not available'}</strong>
                      <div className="code-subtle">{consent.grantedTo || '—'}</div>
                    </td>
                    <td>
                      <code>{consent.scope || 'Not available'}</code>
                    </td>
                    <td>
                      <Badge
                        variant={
                          consent.status === 'GRANTED'
                            ? 'success'
                            : consent.status === 'REVOKED'
                            ? 'danger'
                            : consent.status === 'PENDING'
                            ? 'warning'
                            : 'neutral'
                        }
                        size="sm"
                      >
                        {consent.status}
                      </Badge>
                    </td>
                    <td>
                      {consent.grantedAt
                        ? new Date(consent.grantedAt).toLocaleDateString()
                        : 'Not available'}
                    </td>
                    <td>
                      {consent.expiresAt
                        ? new Date(consent.expiresAt).toLocaleDateString()
                        : 'No expiry'}
                    </td>
                    <td>
                      {consent.revokedAt
                        ? new Date(consent.revokedAt).toLocaleDateString()
                        : '—'}
                    </td>
                    <td>{consent.reason || 'Not available'}</td>
                    {canManageConsent && (
                      <td>
                        {consent.status === 'GRANTED' ? (
                          <button
                            type="button"
                            onClick={() => {
                              setActionMessage(null);
                              setActionError(null);
                              setConsentToRevoke(consent);
                            }}
                            className="btn btn-outline-danger btn-xs"
                            disabled={actionLoading}
                          >
                            Revoke Directive
                          </button>
                        ) : (
                          <span className="text-muted text-xs">Revoked</span>
                        )}
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* Explicit Revocation Confirmation Modal */}
      {consentToRevoke && (
        <div className="modal-backdrop" onClick={() => setConsentToRevoke(null)}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '560px' }}>
            <div className="modal-header">
              <div className="modal-title-group">
                <span className="modal-icon text-warn">⚠️</span>
                <h3 className="modal-title">Confirm Consent Revocation</h3>
              </div>
              <button
                type="button"
                onClick={() => setConsentToRevoke(null)}
                className="modal-close-btn"
                disabled={actionLoading}
              >
                ✕
              </button>
            </div>

            <div className="modal-body">
              <p style={{ fontSize: 'var(--font-size-sm)', marginBottom: '1rem', color: 'var(--color-text-primary)' }}>
                Are you sure you want to revoke this consent directive? Once revoked, the provider will be immediately denied access to this patient&apos;s digital twin and clinical data.
              </p>

              <div
                style={{
                  backgroundColor: 'var(--color-bg-secondary)',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-md)',
                  padding: 'var(--space-md)',
                  marginBottom: '1rem',
                  fontSize: 'var(--font-size-sm)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '0.4rem',
                }}
              >
                <div>
                  <span className="meta-label">Grantee Provider: </span>
                  <strong>{consentToRevoke.grantedToName}</strong> (<code>{consentToRevoke.grantedTo}</code>)
                </div>
                <div>
                  <span className="meta-label">SMART Scope: </span>
                  <code>{consentToRevoke.scope}</code>
                </div>
                <div>
                  <span className="meta-label">Granted On: </span>
                  <span>{new Date(consentToRevoke.grantedAt).toLocaleString()}</span>
                </div>
                <div>
                  <span className="meta-label">Purpose / Reason: </span>
                  <span>{consentToRevoke.reason || 'Not specified'}</span>
                </div>
              </div>

              <div className="vitals-warning-notice" style={{ margin: 0 }}>
                <span className="notice-icon">ℹ️</span>
                <div className="notice-content">
                  <strong>Compliance Note:</strong> This revocation event is immediately appended to the HIPAA-style audit trail as a <code>CONSENT_REVOKED</code> action.
                </div>
              </div>
            </div>

            <div className="modal-footer" style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
              <button
                type="button"
                onClick={() => setConsentToRevoke(null)}
                className="btn btn-secondary btn-sm"
                disabled={actionLoading}
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleExecuteRevoke}
                className="btn btn-warning btn-sm"
                disabled={actionLoading}
                style={{ backgroundColor: 'var(--color-accent-danger)', borderColor: 'var(--color-accent-danger)', color: '#fff' }}
              >
                {actionLoading ? 'Revoking Directive...' : 'Yes, Revoke Access'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
