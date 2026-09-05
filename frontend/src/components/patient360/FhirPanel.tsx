import React, { useState, useEffect, useCallback } from 'react';
import type { TwinFhirSyncStatus } from '../../types/twin';
import type { FhirResourceSummary, FhirIngestionResult, FhirResourceDetail } from '../../types/fhir';
import { fhirApi } from '../../api/fhirApi';
import { useAuth } from '../../auth/useAuth';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { LoadingState } from '../common/LoadingState';
import { ErrorState } from '../common/ErrorState';
import { EmptyState } from '../common/EmptyState';
import {
  SAMPLE_PATIENT_BUNDLE,
  SAMPLE_VITALS_BUNDLE,
  SAMPLE_LABS_BUNDLE,
  SAMPLE_DIAGNOSTIC_REPORT,
  SAMPLE_CONSENT,
  SAMPLE_INVALID_RESOURCE,
} from '../../api/sampleFhirData';

export interface FhirPanelProps {
  patientId: string;
  syncStatus?: TwinFhirSyncStatus;
  onIngestionSuccess?: () => void;
}

export const FhirPanel: React.FC<FhirPanelProps> = ({
  patientId,
  syncStatus,
  onIngestionSuccess,
}) => {
  const { user } = useAuth();
  const [resources, setResources] = useState<FhirResourceSummary[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [isIngesting, setIsIngesting] = useState(false);
  const [lastOutcome, setLastOutcome] = useState<FhirIngestionResult | null>(null);

  // Raw JSON inspect state
  const [inspectingId, setInspectingId] = useState<string | null>(null);
  const [inspectDetail, setInspectDetail] = useState<FhirResourceDetail | null>(null);
  const [inspectLoading, setInspectLoading] = useState(false);
  const [inspectError, setInspectError] = useState<unknown>(null);

  const fetchFhirResources = useCallback(async () => {
    if (!patientId) return;
    setIsLoading(true);
    setError(null);
    try {
      const res = await fhirApi.getPatientFhirResources(patientId);
      setResources(res.content);
    } catch (err) {
      setError(err);
    } finally {
      setIsLoading(false);
    }
  }, [patientId]);

  useEffect(() => {
    let ignore = false;
    queueMicrotask(() => {
      if (!ignore) {
        setIsLoading(true);
        setError(null);
        setInspectingId(null);
        setInspectDetail(null);
      }
    });

    fhirApi.getPatientFhirResources(patientId)
      .then((res) => {
        if (!ignore) {
          setResources(res.content);
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
  }, [patientId]);

  const handleIngest = async (_label: string, payload: string) => {
    setIsIngesting(true);
    try {
      const result = await fhirApi.ingest(payload);
      setLastOutcome(result);
      await fetchFhirResources();
      if (onIngestionSuccess) {
        onIngestionSuccess();
      }
    } catch (err) {
      setError(err);
    } finally {
      setIsIngesting(false);
    }
  };

  const handleInspectResource = async (resourceId: string) => {
    setInspectingId(resourceId);
    setInspectLoading(true);
    setInspectDetail(null);
    setInspectError(null);

    try {
      const detail = await fhirApi.getResourceById(resourceId);
      setInspectDetail(detail);
    } catch (err) {
      setInspectError(err);
    } finally {
      setInspectLoading(false);
    }
  };

  if (error) {
    return (
      <Card title="FHIR R4 Resources">
        <ErrorState
          error={error}
          title="FHIR Resource Access Denied"
          message="Unable to access patient FHIR resources. Access is protected by patient ownership, provider assignment with active consent, or administrator privileges."
          onRetry={fetchFhirResources}
        />
      </Card>
    );
  }

  return (
    <div className="fhir-panel-container">
      {/* Synchronization Summary */}
      <Card
        title="FHIR R4 Synchronization Status"
        subtitle="HAPI FHIR R4 parser, structural validator, and domain bridge status"
        action={
          <Badge
            variant={syncStatus?.syncStatus === 'SYNCED' ? 'success' : 'neutral'}
          >
            {syncStatus?.syncStatus || 'SYNCED'}
          </Badge>
        }
      >
        <div className="fhir-sync-grid">
          <div className="fhir-sync-item">
            <span className="sync-item-label">Synchronized Resources</span>
            <span className="sync-item-value">{syncStatus?.resourceCount ?? resources.length}</span>
          </div>
          <div className="fhir-sync-item">
            <span className="sync-item-label">Sync State</span>
            <span className="sync-item-value highlight">{syncStatus?.syncStatus || 'SYNCED'}</span>
          </div>
          <div className="fhir-sync-item">
            <span className="sync-item-label">Last Sync Timestamp</span>
            <span className="sync-item-value">
              {syncStatus?.lastSyncTime
                ? new Date(syncStatus.lastSyncTime).toLocaleString()
                : 'Initial Seed Synchronized'}
            </span>
          </div>
          <div className="fhir-sync-item">
            <span className="sync-item-label">Target Patient</span>
            <span className="sync-item-value"><code>{patientId}</code></span>
          </div>
        </div>
      </Card>

      {/* Admin Demo Ingestion Actions */}
      {user?.role === 'ADMIN' && (
        <Card
          title="FHIR R4 Ingestion Pipeline (Admin Demo)"
          subtitle="Publish standard FHIR R4 JSON payloads to test structural validation & twin update"
        >
          <div className="fhir-ingest-button-group">
            <button
              type="button"
              onClick={() => handleIngest('Patient Bundle', SAMPLE_PATIENT_BUNDLE)}
              className="btn btn-primary btn-sm"
              disabled={isIngesting}
            >
              Ingest Patient Bundle (Jane Roe pat-002)
            </button>
            <button
              type="button"
              onClick={() => handleIngest('Vitals Bundle', SAMPLE_VITALS_BUNDLE)}
              className="btn btn-secondary btn-sm"
              disabled={isIngesting}
            >
              Ingest Vitals Bundle (John Doe pat-001)
            </button>
            <button
              type="button"
              onClick={() => handleIngest('Labs Bundle', SAMPLE_LABS_BUNDLE)}
              className="btn btn-secondary btn-sm"
              disabled={isIngesting}
            >
              Ingest Labs Bundle (John Doe pat-001)
            </button>
            <button
              type="button"
              onClick={() => handleIngest('Diagnostic Report', SAMPLE_DIAGNOSTIC_REPORT)}
              className="btn btn-secondary btn-sm"
              disabled={isIngesting}
            >
              Ingest Diagnostic Report
            </button>
            <button
              type="button"
              onClick={() => handleIngest('Consent', SAMPLE_CONSENT)}
              className="btn btn-secondary btn-sm"
              disabled={isIngesting}
            >
              Ingest Consent Resource
            </button>
            <button
              type="button"
              onClick={() => handleIngest('Invalid Schema', SAMPLE_INVALID_RESOURCE)}
              className="btn btn-warning btn-sm"
              disabled={isIngesting}
            >
              Ingest Invalid (Test Rejection)
            </button>
          </div>

          {lastOutcome && (
            <div className="fhir-ingest-outcome-box">
              <span className="outcome-title">Last Ingestion Outcome:</span>
              <span>Processed: <strong>{lastOutcome.processedResources}</strong></span>
              <span className="text-pass">Valid: {lastOutcome.validResources}</span>
              {lastOutcome.invalidResources > 0 && (
                <span className="text-warn">Invalid: {lastOutcome.invalidResources}</span>
              )}
            </div>
          )}
        </Card>
      )}

      {/* Synced Resources Table */}
      <Card
        title={`Synchronized FHIR Resources (${resources.length})`}
        subtitle={`Clinical entities mapped into FHIR R4 domain collection for patient ${patientId}`}
        action={
          <button
            type="button"
            onClick={fetchFhirResources}
            className="btn btn-secondary btn-xs"
            disabled={isLoading}
          >
            {isLoading ? 'Refreshing...' : 'Refresh Resources'}
          </button>
        }
      >
        {isLoading && resources.length === 0 ? (
          <LoadingState message="Fetching FHIR resources from MongoDB repository..." compact />
        ) : resources.length === 0 ? (
          <EmptyState
            icon="🔥"
            title="No Synced FHIR Resources Found"
            description="No discrete FHIR R4 resources have been ingested or mapped for this patient record."
          />
        ) : (
          <div className="clinical-table-wrapper">
            <table className="clinical-table">
              <thead>
                <tr>
                  <th>Resource Type</th>
                  <th>FHIR Resource ID</th>
                  <th>Patient Ref</th>
                  <th>Validation Status</th>
                  <th>Processed Timestamp</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {resources.map((res) => (
                  <tr key={res.id}>
                    <td>
                      <Badge variant="primary" size="sm">
                        {res.resourceType}
                      </Badge>
                    </td>
                    <td>
                      <code>{res.resourceId}</code>
                    </td>
                    <td>
                      <span className="code-subtle">Patient/{patientId}</span>
                    </td>
                    <td>
                      <Badge
                        variant={res.validationStatus === 'VALID' ? 'success' : 'danger'}
                        size="sm"
                      >
                        {res.validationStatus}
                      </Badge>
                    </td>
                    <td>{new Date(res.processedAt).toLocaleString()}</td>
                    <td>
                      <button
                        type="button"
                        onClick={() => handleInspectResource(res.resourceId)}
                        className="btn btn-secondary btn-xs"
                      >
                        Inspect Raw JSON
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* Raw FHIR JSON Inspection Modal */}
      {inspectingId && (
        <div className="modal-backdrop" onClick={() => setInspectingId(null)}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title-group">
                <span className="modal-icon">🔥</span>
                <h3 className="modal-title">FHIR R4 Resource: <code>{inspectingId}</code></h3>
              </div>
              <button
                type="button"
                onClick={() => setInspectingId(null)}
                className="modal-close-btn"
              >
                ✕
              </button>
            </div>

            <div className="modal-body">
              {inspectLoading ? (
                <LoadingState message="Retrieving raw FHIR JSON payload..." />
              ) : inspectError ? (
                <ErrorState
                  error={inspectError}
                  title="Raw FHIR Inspection Unavailable"
                  message="Raw resource payload inspection is restricted to Clinicians and Administrators per FHIR security policy."
                />
              ) : inspectDetail ? (
                <div className="fhir-detail-container">
                  <div className="fhir-detail-meta-grid">
                    <div>
                      <span className="detail-label">Resource Type:</span>
                      <strong> {inspectDetail.resourceType}</strong>
                    </div>
                    <div>
                      <span className="detail-label">FHIR Version:</span>
                      <strong> {inspectDetail.version || 'R4'}</strong>
                    </div>
                    <div>
                      <span className="detail-label">Validation Status:</span>
                      <Badge
                        variant={inspectDetail.validationStatus === 'VALID' ? 'success' : 'danger'}
                        size="sm"
                      >
                        {inspectDetail.validationStatus}
                      </Badge>
                    </div>
                    <div>
                      <span className="detail-label">Processed At:</span>
                      <span> {new Date(inspectDetail.processedAt).toLocaleString()}</span>
                    </div>
                  </div>

                  {inspectDetail.validationErrors && inspectDetail.validationErrors.length > 0 && (
                    <div className="fhir-validation-errors-box" style={{ margin: '0.75rem 0' }}>
                      <strong className="text-warn">Validation Errors:</strong>
                      <ul>
                        {inspectDetail.validationErrors.map((err, idx) => (
                          <li key={idx}>{err}</li>
                        ))}
                      </ul>
                    </div>
                  )}

                  <div style={{ marginTop: '0.75rem' }}>
                    <span className="detail-label">Raw FHIR JSON Payload:</span>
                    <pre className="result-json" style={{ maxHeight: '350px', overflowY: 'auto', marginTop: '0.25rem' }}>
                      {(() => {
                        try {
                          return JSON.stringify(JSON.parse(inspectDetail.rawJson), null, 2);
                        } catch {
                          return inspectDetail.rawJson;
                        }
                      })()}
                    </pre>
                  </div>
                </div>
              ) : null}
            </div>

            <div className="modal-footer">
              <button
                type="button"
                onClick={() => setInspectingId(null)}
                className="btn btn-secondary btn-sm"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
