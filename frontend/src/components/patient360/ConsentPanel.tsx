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
}

export const ConsentPanel: React.FC<ConsentPanelProps> = ({
  patientId,
  patientName,
}) => {
  const { user } = useAuth();
  const [consents, setConsents] = useState<ConsentDTO[]>([]);
  const [verifyStatus, setVerifyStatus] = useState<ConsentVerifyResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  // Grant form state
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

      if (user?.role === 'PROVIDER' || user?.role === 'ADMIN') {
        const verifyRes = await consentApi.verifyConsent(patientId);
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
      }
    });

    const promises: Promise<unknown>[] = [
      consentApi.getConsents(patientId).then((data) => {
        if (!ignore) setConsents(data);
      }),
    ];

    if (user?.role === 'PROVIDER' || user?.role === 'ADMIN') {
      promises.push(
        consentApi.verifyConsent(patientId).then((verifyRes) => {
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

  const handleRevoke = async (consentId: string) => {
    setActionLoading(true);
    setActionMessage(null);
    setActionError(null);
    try {
      const revoked = await consentApi.revokeConsent(patientId, consentId);
      setActionMessage(`Consent directive for ${revoked.grantedToName || revoked.grantedTo} has been revoked.`);
      await fetchConsents();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setActionError(axiosErr.response?.data?.message || 'Failed to revoke consent directive.');
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
      setActionMessage(`Consent directive granted to ${created.grantedToName || created.grantedTo} successfully.`);
      setShowGrantForm(false);
      await fetchConsents();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } };
      setActionError(axiosErr.response?.data?.message || 'Failed to authorize consent directive.');
    } finally {
      setActionLoading(false);
    }
  };

  // Backend authorization alignment:
  // Only PATIENT (for their own record) or ADMIN can manage/grant/revoke consent.
  // PROVIDER role cannot manage consents.
  const canManageConsent = user?.role === 'ADMIN' || user?.role === 'PATIENT';

  if (error) {
    return (
      <Card title="Patient Consent Directives">
        <ErrorState
          error={error}
          title="Consent Directives Access Denied"
          message="Unable to access patient consent records. Access is restricted to the patient, assigned providers, or administrators."
          onRetry={fetchConsents}
        />
      </Card>
    );
  }

  return (
    <div className="consent-panel-container">
      {/* Provider Verification Card */}
      {(user?.role === 'PROVIDER' || user?.role === 'ADMIN') && verifyStatus && (
        <Card
          title="Provider Consent Verification"
          subtitle={`Verification against active consent directives for ${patientName || patientId}`}
          action={
            <Badge variant={verifyStatus.hasConsent ? 'success' : 'danger'}>
              {verifyStatus.hasConsent ? 'ACTIVE CONSENT VERIFIED' : 'NO ACTIVE CONSENT'}
            </Badge>
          }
        >
          <div className="consent-verify-details">
            <div className="verify-detail-row">
              <span className="verify-label">Provider ID:</span>
              <span className="verify-value"><code>{verifyStatus.providerId || 'Current Authenticated Provider'}</code></span>
            </div>
            <div className="verify-detail-row">
              <span className="verify-label">Authorization Policy:</span>
              <span className="verify-value">{verifyStatus.reason || 'Not available'}</span>
            </div>
            <div className="verify-detail-row">
              <span className="verify-label">Checked Timestamp:</span>
              <span className="verify-value">
                {verifyStatus.checkedAt ? new Date(verifyStatus.checkedAt).toLocaleString() : 'Not available'}
              </span>
            </div>
          </div>
        </Card>
      )}

      {/* Directives Table Card */}
      <Card
        title={`Patient Consent Directives (${consents.length})`}
        subtitle={`Legal authorization directives registered for patient ${patientId}`}
        action={
          <div style={{ display: 'flex', gap: '0.5rem' }}>
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
          <div className="alert-banner alert-success" style={{ marginBottom: '1rem' }}>
            <span>✓</span> {actionMessage}
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
            title="No Consent Directives Found"
            description="No active or historical consent records exist in the consent registry for this patient."
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
                  {canManageConsent && <th>Action</th>}
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
                          consent.status === 'ACTIVE'
                            ? 'success'
                            : consent.status === 'REVOKED'
                            ? 'danger'
                            : 'neutral'
                        }
                        size="sm"
                      >
                        {consent.status}
                      </Badge>
                    </td>
                    <td>{consent.grantedAt ? new Date(consent.grantedAt).toLocaleDateString() : 'Not available'}</td>
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
                        {consent.status === 'ACTIVE' ? (
                          <button
                            type="button"
                            onClick={() => handleRevoke(consent.id)}
                            className="btn btn-outline-danger btn-xs"
                            disabled={actionLoading}
                          >
                            Revoke
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
    </div>
  );
};
