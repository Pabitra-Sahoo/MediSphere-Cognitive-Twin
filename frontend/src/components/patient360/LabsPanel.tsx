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
    if (!patientId || !showHistory) return;
    setIsLoading(true);
    setError(null);
    try {
      const pageRes = await labApi.getLabs(patientId, 0, 10);
      setLabs(pageRes.content);
    } catch (err) {
      setError(err);
    } finally {
      setIsLoading(false);
    }
  }, [patientId, showHistory]);

  useEffect(() => {
    if (!patientId || !showHistory) return;
    let ignore = false;
    labApi.getLabs(patientId, 0, 10)
      .then((pageRes) => {
        if (!ignore) {
          setLabs(pageRes.content);
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
  }, [patientId, showHistory]);

  if (error) {
    return (
      <Card title="Laboratory Results">
        <ErrorState error={error} onRetry={fetchLabs} />
      </Card>
    );
  }

  const glucose = twinLabs?.glucose;
  const cholesterol = twinLabs?.cholesterol;
  const hemoglobin = twinLabs?.hemoglobin;
  const creatinine = twinLabs?.creatinine;

  return (
    <Card
      title="Laboratory Panels"
      subtitle={
        twinLabs?.timestamp
          ? `Last updated: ${new Date(twinLabs.timestamp).toLocaleString()}`
          : 'Latest diagnostic biochemistry values'
      }
      action={
        <Badge variant="info" size="sm">
          Diagnostic Lab
        </Badge>
      }
    >
      <div className="labs-metrics-grid">
        <div className="lab-metric-card">
          <div className="lab-card-header">
            <span className="lab-name">Fasting Glucose</span>
            <span className="lab-icon">🍬</span>
          </div>
          <div className="lab-value-row">
            <span className="lab-value">{glucose !== undefined ? glucose : '—'}</span>
            <span className="lab-unit">mg/dL</span>
          </div>
          <div className="lab-ref-range">Reference: 70 - 99 mg/dL</div>
        </div>

        <div className="lab-metric-card">
          <div className="lab-card-header">
            <span className="lab-name">Total Cholesterol</span>
            <span className="lab-icon">🧪</span>
          </div>
          <div className="lab-value-row">
            <span className="lab-value">{cholesterol !== undefined ? cholesterol : '—'}</span>
            <span className="lab-unit">mg/dL</span>
          </div>
          <div className="lab-ref-range">Reference: &lt; 200 mg/dL</div>
        </div>

        <div className="lab-metric-card">
          <div className="lab-card-header">
            <span className="lab-name">Hemoglobin</span>
            <span className="lab-icon">🩸</span>
          </div>
          <div className="lab-value-row">
            <span className="lab-value">{hemoglobin !== undefined ? hemoglobin : '—'}</span>
            <span className="lab-unit">g/dL</span>
          </div>
          <div className="lab-ref-range">Reference: 13.8 - 17.2 g/dL</div>
        </div>

        <div className="lab-metric-card">
          <div className="lab-card-header">
            <span className="lab-name">Serum Creatinine</span>
            <span className="lab-icon">💧</span>
          </div>
          <div className="lab-value-row">
            <span className="lab-value">{creatinine !== undefined ? creatinine : '—'}</span>
            <span className="lab-unit">mg/dL</span>
          </div>
          <div className="lab-ref-range">Reference: 0.7 - 1.3 mg/dL</div>
        </div>
      </div>

      {showHistory && (
        <div className="labs-history-table-container">
          <h4 className="section-subheading">Historical Laboratory Tests</h4>
          {isLoading ? (
            <LoadingState message="Loading laboratory reports..." compact />
          ) : labs.length === 0 ? (
            <EmptyState
              icon="🧪"
              title="No Historical Lab Reports"
              description="No discrete laboratory records stored for this patient."
            />
          ) : (
            <div className="clinical-table-wrapper">
              <table className="clinical-table">
                <thead>
                  <tr>
                    <th>Performed At</th>
                    <th>Test Name</th>
                    <th>Result Value</th>
                    <th>Reference Range</th>
                    <th>Status</th>
                    <th>Source</th>
                  </tr>
                </thead>
                <tbody>
                  {labs.map((lab) => (
                    <tr key={lab.id}>
                      <td>{new Date(lab.performedAt).toLocaleDateString()}</td>
                      <td>
                        <strong>{lab.testName}</strong>{' '}
                        <code className="code-subtle">{lab.testCode}</code>
                      </td>
                      <td>
                        <strong>{lab.value}</strong> {lab.unit}
                      </td>
                      <td>
                        {lab.referenceRange
                          ? `${lab.referenceRange.low ?? '—'} - ${lab.referenceRange.high ?? '—'} ${lab.unit}`
                          : '—'}
                      </td>
                      <td>
                        <Badge variant="neutral" size="sm">
                          {lab.status}
                        </Badge>
                      </td>
                      <td>{lab.source || 'FHIR'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </Card>
  );
};
