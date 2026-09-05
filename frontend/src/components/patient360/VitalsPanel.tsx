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
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [latestVitals, setLatestVitals] = useState<VitalsRecord | null>(null);

  const fetchVitalsData = useCallback(async () => {
    if (!patientId) return;
    setIsLoading(true);
    setError(null);
    try {
      if (showHistory) {
        const pageRes = await vitalsApi.getVitalsHistory(patientId, 0, 20);
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
    queueMicrotask(() => {
      if (!ignore) {
        setIsLoading(true);
        setError(null);
      }
    });

    const promises: Promise<unknown>[] = [
      vitalsApi.getLatestVitals(patientId).then((latest) => {
        if (!ignore) setLatestVitals(latest);
      }),
    ];

    if (showHistory) {
      promises.push(
        vitalsApi.getVitalsHistory(patientId, 0, 20).then((pageRes) => {
          if (!ignore) setHistory(pageRes.content);
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
  }, [patientId, showHistory]);

  if (error) {
    return (
      <Card title="Vital Signs Telemetry">
        <ErrorState
          error={error}
          title="Vitals Telemetry Access Denied"
          message="Unable to retrieve vital sign telemetry. Access requires patient ownership, provider assignment with active consent, or administrator privileges."
          onRetry={fetchVitalsData}
        />
      </Card>
    );
  }

  // Values from either latestVitals endpoint or TwinVitals
  const hr = latestVitals?.heartRate ?? twinVitals?.heartRate;
  const sbp = latestVitals?.systolicBP ?? twinVitals?.systolicBP;
  const dbp = latestVitals?.diastolicBP ?? twinVitals?.diastolicBP;
  const spo2 = latestVitals?.oxygenSaturation ?? twinVitals?.oxygenSaturation;
  const temp = latestVitals?.temperature ?? twinVitals?.temperature;
  const rr = latestVitals?.respiratoryRate ?? twinVitals?.respiratoryRate;
  const recordedTime = latestVitals?.recordedAt ?? twinVitals?.timestamp;
  const source = latestVitals?.source;
  const deviceId = latestVitals?.deviceId;
  const isValid = latestVitals?.valid;
  const validationErrors = latestVitals?.validationErrors || [];
  const validationWarnings = latestVitals?.validationWarnings || [];

  const hasAnyLatestReading =
    hr !== undefined ||
    sbp !== undefined ||
    dbp !== undefined ||
    spo2 !== undefined ||
    temp !== undefined ||
    rr !== undefined;

  return (
    <Card
      title="Vital Signs Telemetry"
      subtitle={
        recordedTime
          ? `Latest observation recorded: ${new Date(recordedTime).toLocaleString()}`
          : 'Real-time physiological sensor telemetry'
      }
      action={
        <div className="vitals-header-badges">
          {source && (
            <Badge variant="primary" size="sm">
              Source: {source}
            </Badge>
          )}
          {isValid !== undefined && (
            <Badge variant={isValid ? 'success' : 'warning'} size="sm">
              {isValid ? 'Quality Valid' : 'Quality Flag'}
            </Badge>
          )}
        </div>
      }
    >
      {isLoading ? (
        <LoadingState message="Connecting to vitals telemetry stream..." compact />
      ) : !hasAnyLatestReading ? (
        <EmptyState
          icon="💓"
          title="No Vital Signs Recorded"
          description="No telemetry readings or vital sign observations have been recorded for this patient yet."
        />
      ) : (
        <>
          {/* Real Telemetry Metadata Strip */}
          <div className="vitals-meta-strip">
            <div className="vitals-meta-item">
              <span className="meta-label">Observation Source:</span>
              <span className="meta-value">{source || 'Not available'}</span>
            </div>
            <div className="vitals-meta-item">
              <span className="meta-label">Sensor / Device ID:</span>
              <span className="meta-value">{deviceId ? <code>{deviceId}</code> : 'Not available'}</span>
            </div>
            <div className="vitals-meta-item">
              <span className="meta-label">Telemetry Event:</span>
              <span className="meta-value">
                {latestVitals?.eventId ? <code>{latestVitals.eventId}</code> : 'Direct Sync'}
              </span>
            </div>
            <div className="vitals-meta-item">
              <span className="meta-label">Data Quality Status:</span>
              <span className="meta-value">
                {isValid === undefined
                  ? 'Not available'
                  : isValid
                  ? 'Validated within expected ranges'
                  : 'Boundary check flag'}
              </span>
            </div>
          </div>

          {/* Data Quality Notice if validation issues exist */}
          {validationErrors.length > 0 && (
            <div className="vitals-quality-notice">
              <span className="notice-icon">⚠️</span>
              <div className="notice-content">
                <strong>Data Quality Boundary Flags:</strong>
                <ul>
                  {validationErrors.map((err, idx) => (
                    <li key={idx}>{err}</li>
                  ))}
                </ul>
              </div>
            </div>
          )}

          {validationWarnings.length > 0 && (
            <div className="vitals-warning-notice">
              <span className="notice-icon">ℹ️</span>
              <div className="notice-content">
                <strong>Data Quality Advisory:</strong>
                <ul>
                  {validationWarnings.map((warn, idx) => (
                    <li key={idx}>{warn}</li>
                  ))}
                </ul>
              </div>
            </div>
          )}

          {/* Primary 5 Vital Metrics Cards */}
          <div className="vitals-metrics-grid">
            {/* Heart Rate */}
            <div className="vital-card">
              <div className="vital-card-header">
                <span className="vital-name">Heart Rate</span>
                <span className="vital-icon">💓</span>
              </div>
              <div className="vital-value-row">
                <span className="vital-value">{hr !== undefined ? hr : 'Not available'}</span>
                {hr !== undefined && <span className="vital-unit">bpm</span>}
              </div>
            </div>

            {/* Blood Pressure */}
            <div className="vital-card">
              <div className="vital-card-header">
                <span className="vital-name">Blood Pressure</span>
                <span className="vital-icon">🩸</span>
              </div>
              <div className="vital-value-row">
                <span className="vital-value">
                  {sbp !== undefined && dbp !== undefined
                    ? `${sbp}/${dbp}`
                    : sbp !== undefined
                    ? `${sbp}/—`
                    : 'Not available'}
                </span>
                {sbp !== undefined && <span className="vital-unit">mmHg</span>}
              </div>
            </div>

            {/* Oxygen Saturation */}
            <div className="vital-card">
              <div className="vital-card-header">
                <span className="vital-name">Oxygen Saturation</span>
                <span className="vital-icon">🫁</span>
              </div>
              <div className="vital-value-row">
                <span className="vital-value">{spo2 !== undefined ? spo2 : 'Not available'}</span>
                {spo2 !== undefined && <span className="vital-unit">%</span>}
              </div>
            </div>

            {/* Body Temperature */}
            <div className="vital-card">
              <div className="vital-card-header">
                <span className="vital-name">Temperature</span>
                <span className="vital-icon">🌡️</span>
              </div>
              <div className="vital-value-row">
                <span className="vital-value">{temp !== undefined ? temp : 'Not available'}</span>
                {temp !== undefined && <span className="vital-unit">°C</span>}
              </div>
            </div>

            {/* Respiratory Rate */}
            <div className="vital-card">
              <div className="vital-card-header">
                <span className="vital-name">Respiratory Rate</span>
                <span className="vital-icon">🌬️</span>
              </div>
              <div className="vital-value-row">
                <span className="vital-value">{rr !== undefined ? rr : 'Not available'}</span>
                {rr !== undefined && <span className="vital-unit">/min</span>}
              </div>
            </div>
          </div>
        </>
      )}

      {/* Historical Telemetry Table */}
      {showHistory && (
        <div className="vitals-history-table-container">
          <div className="table-header-row">
            <h4 className="section-subheading">Historical Vitals Observations ({history.length})</h4>
            <button
              type="button"
              onClick={fetchVitalsData}
              className="btn btn-secondary btn-xs"
              disabled={isLoading}
            >
              Refresh History
            </button>
          </div>

          {isLoading && history.length === 0 ? (
            <LoadingState message="Loading historical readings..." compact />
          ) : history.length === 0 ? (
            <EmptyState
              icon="💓"
              title="No Historical Vitals Found"
              description="No historical telemetry packets have been recorded for this patient."
            />
          ) : (
            <div className="clinical-table-wrapper">
              <table className="clinical-table">
                <thead>
                  <tr>
                    <th>Observation Time</th>
                    <th>Source</th>
                    <th>Device ID</th>
                    <th>Heart Rate</th>
                    <th>Blood Pressure</th>
                    <th>SpO2</th>
                    <th>Temperature</th>
                    <th>Resp. Rate</th>
                    <th>Data Quality</th>
                  </tr>
                </thead>
                <tbody>
                  {history.map((record) => (
                    <tr key={record.id}>
                      <td>{new Date(record.recordedAt).toLocaleString()}</td>
                      <td>
                        <Badge variant="neutral" size="sm">
                          {record.source || 'Not available'}
                        </Badge>
                      </td>
                      <td>{record.deviceId ? <code>{record.deviceId}</code> : '—'}</td>
                      <td>{record.heartRate !== undefined ? `${record.heartRate} bpm` : 'Not available'}</td>
                      <td>
                        {record.systolicBP !== undefined && record.diastolicBP !== undefined
                          ? `${record.systolicBP}/${record.diastolicBP} mmHg`
                          : 'Not available'}
                      </td>
                      <td>
                        {record.oxygenSaturation !== undefined
                          ? `${record.oxygenSaturation}%`
                          : 'Not available'}
                      </td>
                      <td>
                        {record.temperature !== undefined ? `${record.temperature} °C` : 'Not available'}
                      </td>
                      <td>
                        {record.respiratoryRate !== undefined
                          ? `${record.respiratoryRate} /min`
                          : 'Not available'}
                      </td>
                      <td>
                        <Badge variant={record.valid ? 'success' : 'warning'} size="sm">
                          {record.valid ? 'Valid' : 'Flagged'}
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
