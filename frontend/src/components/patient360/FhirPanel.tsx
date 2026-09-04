import React, { useState, useEffect, useCallback } from 'react';
import type { TwinFhirSyncStatus } from '../../types/twin';
import type { FhirResourceSummary, FhirIngestionResult } from '../../types/fhir';
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
    fhirApi.getPatientFhirResources(patientId)
      .then((res) => {
        if (!ignore) {
          setResources(res.content);
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

  return (
    <div className="fhir-panel-container">
      {/* Synchronization Summary */}
      <Card
        title="FHIR R4 Synchronization Status"
        subtitle="HAPI FHIR R4 parser and domain bridge status"
        action={
          <Badge
            variant={syncStatus?.syncStatus === 'SYNCED' ? 'success' : 'neutral'}
          >
            {syncStatus?.syncStatus || 'PENDING'}
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
        title={`Synced FHIR Resources (${resources.length})`}
        subtitle={`Clinical entities mapped to FHIR R4 for patient ${patientId}`}
        action={
          <button
            type="button"
            onClick={fetchFhirResources}
            className="btn btn-secondary btn-xs"
            disabled={isLoading}
          >
            {isLoading ? 'Refreshing...' : 'Refresh'}
          </button>
        }
      >
        {error ? (
          <ErrorState error={error} onRetry={fetchFhirResources} />
        ) : isLoading ? (
          <LoadingState message="Fetching FHIR resources..." compact />
        ) : resources.length === 0 ? (
          <EmptyState
            icon="🔥"
            title="No Synced FHIR Resources"
            description="No FHIR R4 resources have been ingested for this patient record yet."
          />
        ) : (
          <div className="clinical-table-wrapper">
            <table className="clinical-table">
              <thead>
                <tr>
                  <th>Resource Type</th>
                  <th>FHIR Resource ID</th>
                  <th>Validation Status</th>
                  <th>Processed At</th>
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
                      <Badge
                        variant={res.validationStatus === 'VALID' ? 'success' : 'danger'}
                        size="sm"
                      >
                        {res.validationStatus}
                      </Badge>
                    </td>
                    <td>{new Date(res.processedAt).toLocaleString()}</td>
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
