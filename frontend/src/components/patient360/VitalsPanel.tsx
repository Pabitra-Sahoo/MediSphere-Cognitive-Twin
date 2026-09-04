import React, { useState, useEffect, useCallback } from 'react';
import type { TwinVitals } from '../../types/twin';
import type { VitalsRecord } from '../../types/vitals';
import { vitalsApi } from '../../api/vitalsApi';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { LoadingState } from '../common/LoadingState';
import { ErrorState } from '../common/ErrorState';
import { EmptyState } from '../common/EmptyState';

export interface VitalsPanelProps {
  patientId: string;
  twinVitals?: TwinVitals;
  showHistory?: boolean;
}

export const VitalsPanel: React.FC<VitalsPanelProps> = ({
  patientId,
  twinVitals,
  showHistory = false,
}) => {
  const [history, setHistory] = useState<VitalsRecord[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [latestVitals, setLatestVitals] = useState<VitalsRecord | null>(null);

  const fetchVitalsData = useCallback(async () => {
    if (!patientId) return;
    setIsLoading(true);
    setError(null);
    try {
      if (showHistory) {
        const pageRes = await vitalsApi.getVitalsHistory(patientId, 0, 10);
        setHistory(pageRes.content);
      }
      const latest = await vitalsApi.getLatestVitals(patientId);
      setLatestVitals(latest);
    } catch (err) {
      setError(err);
    } finally {
      setIsLoading(false);
    }
  }, [patientId, showHistory]);

  useEffect(() => {
    let ignore = false;
    vitalsApi.getLatestVitals(patientId)
      .then((latest) => {
        if (!ignore) {
          setLatestVitals(latest);
          setError(null);
        }
      })
      .catch((err) => {
        if (!ignore) {
          setError(err);
        }
      });

    if (showHistory) {
      vitalsApi.getVitalsHistory(patientId, 0, 10)
        .then((pageRes) => {
          if (!ignore) {
            setHistory(pageRes.content);
          }
        })
        .catch(() => {});
    }

    return () => {
      ignore = true;
    };
  }, [patientId, showHistory]);

  if (error) {
    return (
      <Card title="Vital Signs Stream">
        <ErrorState error={error} onRetry={fetchVitalsData} />
      </Card>
    );
  }

  // Use either standalone latestVitals or twinVitals from health twin
  const hr = latestVitals?.heartRate ?? twinVitals?.heartRate;
  const sbp = latestVitals?.systolicBP ?? twinVitals?.systolicBP;
  const dbp = latestVitals?.diastolicBP ?? twinVitals?.diastolicBP;
  const spo2 = latestVitals?.oxygenSaturation ?? twinVitals?.oxygenSaturation;
  const temp = latestVitals?.temperature ?? twinVitals?.temperature;
  const rr = latestVitals?.respiratoryRate ?? twinVitals?.respiratoryRate;
  const recordedTime = latestVitals?.recordedAt ?? twinVitals?.timestamp;

  return (
    <Card
      title="Vital Signs"
      subtitle={
        recordedTime
          ? `Latest recorded at ${new Date(recordedTime).toLocaleString()}`
          : 'Latest real-time sensor observations'
      }
      action={
        <Badge variant="primary" size="sm">
          Kafka Stream / Active
        </Badge>
      }
    >
      <div className="vitals-metrics-grid">
        <div className="vital-card">
          <div className="vital-card-header">
            <span className="vital-name">Heart Rate</span>
            <span className="vital-icon">💓</span>
          </div>
          <div className="vital-value-row">
            <span className="vital-value">{hr !== undefined ? hr : '—'}</span>
            <span className="vital-unit">bpm</span>
          </div>
          <div className="vital-baseline-ref">Reference: 60 - 100 bpm</div>
        </div>

        <div className="vital-card">
          <div className="vital-card-header">
            <span className="vital-name">Blood Pressure</span>
            <span className="vital-icon">🩸</span>
          </div>
          <div className="vital-value-row">
            <span className="vital-value">
              {sbp !== undefined && dbp !== undefined ? `${sbp}/${dbp}` : '—'}
            </span>
            <span className="vital-unit">mmHg</span>
          </div>
          <div className="vital-baseline-ref">Reference: 90/60 - 120/80</div>
        </div>

        <div className="vital-card">
          <div className="vital-card-header">
            <span className="vital-name">Oxygen Saturation</span>
            <span className="vital-icon">🫁</span>
          </div>
          <div className="vital-value-row">
            <span className="vital-value">{spo2 !== undefined ? `${spo2}` : '—'}</span>
            <span className="vital-unit">%</span>
          </div>
          <div className="vital-baseline-ref">Reference: 95 - 100%</div>
        </div>

        <div className="vital-card">
          <div className="vital-card-header">
            <span className="vital-name">Temperature</span>
            <span className="vital-icon">🌡️</span>
          </div>
          <div className="vital-value-row">
            <span className="vital-value">{temp !== undefined ? temp : '—'}</span>
            <span className="vital-unit">°C</span>
          </div>
          <div className="vital-baseline-ref">Reference: 36.1 - 37.2 °C</div>
        </div>

        <div className="vital-card">
          <div className="vital-card-header">
            <span className="vital-name">Respiratory Rate</span>
            <span className="vital-icon">🌬️</span>
          </div>
          <div className="vital-value-row">
            <span className="vital-value">{rr !== undefined ? rr : '—'}</span>
            <span className="vital-unit">/min</span>
          </div>
          <div className="vital-baseline-ref">Reference: 12 - 20 /min</div>
        </div>
      </div>

      {showHistory && (
        <div className="vitals-history-table-container">
          <h4 className="section-subheading">Recent Vitals Records</h4>
          {isLoading ? (
            <LoadingState message="Loading historical readings..." compact />
          ) : history.length === 0 ? (
            <EmptyState
              icon="💓"
              title="No Historical Vitals"
              description="No vital sign packets found for this patient."
            />
          ) : (
            <div className="clinical-table-wrapper">
              <table className="clinical-table">
                <thead>
                  <tr>
                    <th>Timestamp</th>
                    <th>Source</th>
                    <th>HR</th>
                    <th>BP</th>
                    <th>SpO2</th>
                    <th>Temp</th>
                    <th>RR</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map((item) => (
                    <tr key={item.id}>
                      <td>{new Date(item.recordedAt).toLocaleString()}</td>
                      <td>
                        <Badge variant="neutral" size="sm">
                          {item.source}
                        </Badge>
                      </td>
                      <td>{item.heartRate ?? '—'}</td>
                      <td>
                        {item.systolicBP && item.diastolicBP
                          ? `${item.systolicBP}/${item.diastolicBP}`
                          : '—'}
                      </td>
                      <td>{item.oxygenSaturation ? `${item.oxygenSaturation}%` : '—'}</td>
                      <td>{item.temperature ? `${item.temperature} °C` : '—'}</td>
                      <td>{item.respiratoryRate ?? '—'}</td>
                      <td>
                        <Badge variant={item.valid ? 'success' : 'warning'} size="sm">
                          {item.valid ? 'Valid' : 'Invalid'}
                        </Badge>
                      </td>
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
