import React, { useState, useEffect, useCallback } from 'react';
import type { TwinLabs } from '../../types/twin';
import type { LabResultRecord } from '../../types/lab';
import { labApi } from '../../api/labApi';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { LoadingState } from '../common/LoadingState';
import { ErrorState } from '../common/ErrorState';
import { EmptyState } from '../common/EmptyState';

export interface LabsPanelProps {
  patientId: string;
  twinLabs?: TwinLabs;
  showHistory?: boolean;
}

export const LabsPanel: React.FC<LabsPanelProps> = ({
  patientId,
  twinLabs,
  showHistory = false,
}) => {
  const [labs, setLabs] = useState<LabResultRecord[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);

  const fetchLabs = useCallback(async () => {
    if (!patientId) return;
    setIsLoading(true);
    setError(null);
    try {
      const pageRes = await labApi.getLabs(patientId, 0, 20);
      setLabs(pageRes.content);
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
      }
    });

    labApi.getLabs(patientId, 0, 20)
      .then((pageRes) => {
        if (!ignore) {
          setLabs(pageRes.content);
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

  if (error) {
    return (
      <Card title="Laboratory Test Results">
        <ErrorState
          error={error}
          title="Laboratory Results Access Denied"
          message="Unable to access patient laboratory records. Access requires patient ownership, provider assignment with active consent, or administrator privileges."
          onRetry={fetchLabs}
        />
      </Card>
    );
  }

  // Find any actual reference range from the discrete lab records if available
  const findLabRefRange = (testCodeQuery: string): string => {
    const matched = labs.find(
      (l) => l.testCode?.toLowerCase() === testCodeQuery.toLowerCase() ||
             l.testName?.toLowerCase().includes(testCodeQuery.toLowerCase())
    );
    if (matched?.referenceRange && (matched.referenceRange.low !== undefined || matched.referenceRange.high !== undefined)) {
      return `${matched.referenceRange.low ?? '—'} - ${matched.referenceRange.high ?? '—'} ${matched.unit}`;
    }
    return 'Not provided by laboratory';
  };

  const glucose = twinLabs?.glucose;
  const cholesterol = twinLabs?.cholesterol;
  const hemoglobin = twinLabs?.hemoglobin;
  const creatinine = twinLabs?.creatinine;

  const hasAnyLatestLab =
    glucose !== undefined ||
    cholesterol !== undefined ||
    hemoglobin !== undefined ||
    creatinine !== undefined;

  return (
    <Card
      title="Laboratory Panels"
      subtitle={
        twinLabs?.timestamp
          ? `Last updated: ${new Date(twinLabs.timestamp).toLocaleString()}`
          : 'Diagnostic biochemistry panels and historical lab orders'
      }
      action={
        <Badge variant="info" size="sm">
          Clinical Lab Stream
        </Badge>
      }
    >
      {/* Latest Diagnostic Summary Cards */}
      <div className="labs-metrics-grid">
        {/* Glucose */}
        <div className="lab-metric-card">
          <div className="lab-card-header">
            <span className="lab-name">Fasting Glucose</span>
            <span className="lab-icon">🍬</span>
          </div>
          <div className="lab-value-row">
            <span className="lab-value">{glucose !== undefined ? glucose : 'Not available'}</span>
            {glucose !== undefined && <span className="lab-unit">mg/dL</span>}
          </div>
          <div className="lab-ref-range">Reference: {findLabRefRange('glucose')}</div>
        </div>

        {/* Total Cholesterol */}
        <div className="lab-metric-card">
          <div className="lab-card-header">
            <span className="lab-name">Total Cholesterol</span>
            <span className="lab-icon">🧪</span>
          </div>
          <div className="lab-value-row">
            <span className="lab-value">{cholesterol !== undefined ? cholesterol : 'Not available'}</span>
            {cholesterol !== undefined && <span className="lab-unit">mg/dL</span>}
          </div>
          <div className="lab-ref-range">Reference: {findLabRefRange('cholesterol')}</div>
        </div>

        {/* Hemoglobin */}
        <div className="lab-metric-card">
          <div className="lab-card-header">
            <span className="lab-name">Hemoglobin</span>
            <span className="lab-icon">🩸</span>
          </div>
          <div className="lab-value-row">
            <span className="lab-value">{hemoglobin !== undefined ? hemoglobin : 'Not available'}</span>
            {hemoglobin !== undefined && <span className="lab-unit">g/dL</span>}
          </div>
          <div className="lab-ref-range">Reference: {findLabRefRange('hemoglobin')}</div>
        </div>

        {/* Serum Creatinine */}
        <div className="lab-metric-card">
          <div className="lab-card-header">
            <span className="lab-name">Serum Creatinine</span>
            <span className="lab-icon">💧</span>
          </div>
          <div className="lab-value-row">
            <span className="lab-value">{creatinine !== undefined ? creatinine : 'Not available'}</span>
            {creatinine !== undefined && <span className="lab-unit">mg/dL</span>}
          </div>
          <div className="lab-ref-range">Reference: {findLabRefRange('creatinine')}</div>
        </div>
      </div>

      {!hasAnyLatestLab && labs.length === 0 && !isLoading && (
        <EmptyState
          icon="🧪"
          title="No Laboratory Results Found"
          description="No diagnostic laboratory tests or biochemical observations have been reported for this patient."
        />
      )}

      {/* Historical Laboratory Tests Table */}
      {showHistory && (
        <div className="labs-history-table-container">
          <div className="table-header-row">
            <h4 className="section-subheading">Discrete Laboratory Test Records ({labs.length})</h4>
            <button
              type="button"
              onClick={fetchLabs}
              className="btn btn-secondary btn-xs"
              disabled={isLoading}
            >
              Refresh Labs
            </button>
          </div>

          {isLoading ? (
            <LoadingState message="Loading laboratory reports..." compact />
          ) : labs.length === 0 ? (
            <EmptyState
              icon="🧪"
              title="No Discrete Lab Test Records"
              description="No historical laboratory orders or test records exist in the laboratory store for this patient."
            />
          ) : (
            <div className="clinical-table-wrapper">
              <table className="clinical-table">
                <thead>
                  <tr>
                    <th>Observation Date</th>
                    <th>Test Name</th>
                    <th>LOINC / Code</th>
                    <th>Result Value</th>
                    <th>Reference Range</th>
                    <th>Status</th>
                    <th>Source</th>
                  </tr>
                </thead>
                <tbody>
                  {labs.map((lab) => {
                    const hasRange =
                      lab.referenceRange &&
                      (lab.referenceRange.low !== undefined || lab.referenceRange.high !== undefined);
                    const rangeDisplay = hasRange
                      ? `${lab.referenceRange?.low ?? '—'} - ${lab.referenceRange?.high ?? '—'} ${lab.unit}`
                      : 'Not available';

                    return (
                      <tr key={lab.id}>
                        <td>{lab.performedAt ? new Date(lab.performedAt).toLocaleString() : 'Not available'}</td>
                        <td>
                          <strong>{lab.testName || 'Not available'}</strong>
                        </td>
                        <td>
                          {lab.testCode ? <code>{lab.testCode}</code> : 'Not available'}
                        </td>
                        <td>
                          {lab.value !== undefined ? (
                            <span>
                              <strong>{lab.value}</strong> {lab.unit || ''}
                            </span>
                          ) : (
                            'Not available'
                          )}
                        </td>
                        <td>{rangeDisplay}</td>
                        <td>
                          <Badge variant="neutral" size="sm">
                            {lab.status || 'FINAL'}
                          </Badge>
                        </td>
                        <td>{lab.source || 'FHIR'}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </Card>
  );
};
