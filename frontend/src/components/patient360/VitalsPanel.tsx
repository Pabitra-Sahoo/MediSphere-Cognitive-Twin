import React, { useState, useEffect, useCallback, useMemo } from 'react';
import type { TwinVitals } from '../../types/twin';
import type { VitalsRecord } from '../../types/vitals';
import { vitalsApi } from '../../api/vitalsApi';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { LoadingState } from '../common/LoadingState';
import { ErrorState } from '../common/ErrorState';
import { EmptyState } from '../common/EmptyState';
import { TrendLineChart, type TrendDataPoint } from '../common/TrendLineChart';

export interface VitalsPanelProps {
  patientId: string;
  twinVitals?: TwinVitals;
  showHistory?: boolean;
}

export type VitalsMetricType = 'all' | 'hr' | 'bp' | 'spo2' | 'temp' | 'rr';

export const VitalsPanel: React.FC<VitalsPanelProps> = ({
  patientId,
  twinVitals,
  showHistory = false,
}) => {
  const [history, setHistory] = useState<VitalsRecord[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [latestVitals, setLatestVitals] = useState<VitalsRecord | null>(null);
  const [selectedMetric, setSelectedMetric] = useState<VitalsMetricType>('all');

  const fetchVitalsData = useCallback(async () => {
    if (!patientId) return;
    setIsLoading(true);
    setError(null);
    try {
      if (showHistory) {
        const pageRes = await vitalsApi.getVitalsHistory(patientId, 0, 50);
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
        setHistory([]);
        setLatestVitals(null);
      }
    });

    const promises: Promise<unknown>[] = [
      vitalsApi.getLatestVitals(patientId).then((latest) => {
        if (!ignore) setLatestVitals(latest);
      }),
    ];

    if (showHistory) {
      promises.push(
        vitalsApi.getVitalsHistory(patientId, 0, 50).then((pageRes) => {
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

  // Compute chronologically sorted historical observations (ASC)
  const sortedHistory = useMemo(() => {
    return [...history].sort(
      (a, b) => new Date(a.recordedAt).getTime() - new Date(b.recordedAt).getTime()
    );
  }, [history]);

  // Extract separate metric series without fabricating or interpolating missing points
  const hrData = useMemo<TrendDataPoint[]>(() => {
    return sortedHistory
      .filter((r) => r.heartRate !== undefined && r.heartRate !== null)
      .map((r) => ({
        timestamp: r.recordedAt,
        value: r.heartRate as number,
        isValid: r.valid,
        validationErrors: r.validationErrors,
        deviceId: r.deviceId,
        source: r.source,
      }));
  }, [sortedHistory]);

  const bpData = useMemo<TrendDataPoint[]>(() => {
    return sortedHistory
      .filter((r) => r.systolicBP !== undefined && r.systolicBP !== null)
      .map((r) => ({
        timestamp: r.recordedAt,
        value: r.systolicBP as number,
        secondaryValue: r.diastolicBP ?? undefined,
        isValid: r.valid,
        validationErrors: r.validationErrors,
        deviceId: r.deviceId,
        source: r.source,
      }));
  }, [sortedHistory]);

  const spo2Data = useMemo<TrendDataPoint[]>(() => {
    return sortedHistory
      .filter((r) => r.oxygenSaturation !== undefined && r.oxygenSaturation !== null)
      .map((r) => ({
        timestamp: r.recordedAt,
        value: r.oxygenSaturation as number,
        isValid: r.valid,
        validationErrors: r.validationErrors,
        deviceId: r.deviceId,
        source: r.source,
      }));
  }, [sortedHistory]);

  const tempData = useMemo<TrendDataPoint[]>(() => {
    return sortedHistory
      .filter((r) => r.temperature !== undefined && r.temperature !== null)
      .map((r) => ({
        timestamp: r.recordedAt,
        value: r.temperature as number,
        isValid: r.valid,
        validationErrors: r.validationErrors,
        deviceId: r.deviceId,
        source: r.source,
      }));
  }, [sortedHistory]);

  const rrData = useMemo<TrendDataPoint[]>(() => {
    return sortedHistory
      .filter((r) => r.respiratoryRate !== undefined && r.respiratoryRate !== null)
      .map((r) => ({
        timestamp: r.recordedAt,
        value: r.respiratoryRate as number,
        isValid: r.valid,
        validationErrors: r.validationErrors,
        deviceId: r.deviceId,
        source: r.source,
      }));
  }, [sortedHistory]);

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
  const receivedTime = latestVitals?.receivedAt;
  const source = latestVitals?.source;
  const deviceId = latestVitals?.deviceId;
  const isValid = latestVitals?.valid;
  const validationErrors = latestVitals?.validationErrors || [];
  const validationWarnings = latestVitals?.validationWarnings || [];

  // Ingestion Transit Latency in milliseconds (receivedAt - recordedAt)
  const streamingLatencyMs =
    recordedTime && receivedTime
      ? Math.max(0, new Date(receivedTime).getTime() - new Date(recordedTime).getTime())
      : null;

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
          {streamingLatencyMs !== null && (
            <span title="Kafka Ingestion Pipeline Transit Latency">
              <Badge variant="neutral" size="sm">
                Latency: {streamingLatencyMs}ms
              </Badge>
            </span>
          )}
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
      ) : !hasAnyLatestReading && history.length === 0 ? (
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
            {streamingLatencyMs !== null && (
              <div className="vitals-meta-item">
                <span className="meta-label">Stream Latency:</span>
                <span className="meta-value"><strong>{streamingLatencyMs} ms</strong></span>
              </div>
            )}
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

      {/* Historical Telemetry & Trends Section */}
      {showHistory && (
        <div className="vitals-trends-section">
          <div className="trends-section-header">
            <div className="trends-header-title-group">
              <h4 className="section-subheading">Longitudinal Telemetry Trends</h4>
              <span className="trends-subtitle">
                Authoritative time-series observations ({history.length} telemetry records)
              </span>
            </div>
            <button
              type="button"
              onClick={fetchVitalsData}
              className="btn btn-secondary btn-xs"
              disabled={isLoading}
            >
              Refresh Telemetry
            </button>
          </div>

          {/* Metric Selector Tabs */}
          <div className="trends-metric-tabs" role="tablist" aria-label="Select vital sign trend metric">
            <button
              type="button"
              role="tab"
              aria-selected={selectedMetric === 'all'}
              className={`metric-tab-btn ${selectedMetric === 'all' ? 'active' : ''}`}
              onClick={() => setSelectedMetric('all')}
            >
              All Trends
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={selectedMetric === 'hr'}
              className={`metric-tab-btn ${selectedMetric === 'hr' ? 'active' : ''}`}
              onClick={() => setSelectedMetric('hr')}
            >
              💓 Heart Rate ({hrData.length})
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={selectedMetric === 'bp'}
              className={`metric-tab-btn ${selectedMetric === 'bp' ? 'active' : ''}`}
              onClick={() => setSelectedMetric('bp')}
            >
              🩸 Blood Pressure ({bpData.length})
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={selectedMetric === 'spo2'}
              className={`metric-tab-btn ${selectedMetric === 'spo2' ? 'active' : ''}`}
              onClick={() => setSelectedMetric('spo2')}
            >
              🫁 SpO2 ({spo2Data.length})
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={selectedMetric === 'temp'}
              className={`metric-tab-btn ${selectedMetric === 'temp' ? 'active' : ''}`}
              onClick={() => setSelectedMetric('temp')}
            >
              🌡️ Temperature ({tempData.length})
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={selectedMetric === 'rr'}
              className={`metric-tab-btn ${selectedMetric === 'rr' ? 'active' : ''}`}
              onClick={() => setSelectedMetric('rr')}
            >
              🌬️ Resp. Rate ({rrData.length})
            </button>
          </div>

          {/* Trends Viewport */}
          {selectedMetric === 'all' ? (
            <div className="trends-overview-grid">
              {/* Heart Rate Compact Chart */}
              <div className="trend-tile">
                <div className="trend-tile-header">
                  <span className="tile-title">Heart Rate Timeline</span>
                  <span className="tile-unit">bpm</span>
                </div>
                <TrendLineChart
                  data={hrData}
                  title="Heart Rate"
                  unit="bpm"
                  primaryColor="var(--danger, #ef4444)"
                  height={190}
                  compact
                />
              </div>

              {/* Blood Pressure Compact Chart */}
              <div className="trend-tile">
                <div className="trend-tile-header">
                  <span className="tile-title">Blood Pressure Timeline (Systolic / Diastolic)</span>
                  <span className="tile-unit">mmHg</span>
                </div>
                <TrendLineChart
                  data={bpData}
                  title="Blood Pressure"
                  primaryLabel="Systolic"
                  secondaryLabel="Diastolic"
                  unit="mmHg"
                  primaryColor="var(--warning, #f59e0b)"
                  secondaryColor="var(--secondary, #3b82f6)"
                  height={190}
                  compact
                />
              </div>

              {/* Oxygen Saturation Compact Chart */}
              <div className="trend-tile">
                <div className="trend-tile-header">
                  <span className="tile-title">Oxygen Saturation Timeline</span>
                  <span className="tile-unit">%</span>
                </div>
                <TrendLineChart
                  data={spo2Data}
                  title="SpO2"
                  unit="%"
                  primaryColor="var(--info, #06b6d4)"
                  height={190}
                  compact
                />
              </div>

              {/* Temperature Compact Chart */}
              <div className="trend-tile">
                <div className="trend-tile-header">
                  <span className="tile-title">Body Temperature Timeline</span>
                  <span className="tile-unit">°C</span>
                </div>
                <TrendLineChart
                  data={tempData}
                  title="Temperature"
                  unit="°C"
                  primaryColor="var(--success, #10b981)"
                  height={190}
                  compact
                />
              </div>

              {/* Respiratory Rate Compact Chart */}
              <div className="trend-tile">
                <div className="trend-tile-header">
                  <span className="tile-title">Respiratory Rate Timeline</span>
                  <span className="tile-unit">/min</span>
                </div>
                <TrendLineChart
                  data={rrData}
                  title="Respiratory Rate"
                  unit="/min"
                  primaryColor="var(--primary, #6366f1)"
                  height={190}
                  compact
                />
              </div>
            </div>
          ) : (
            /* Single Detailed Trend Chart */
            <div className="trend-detailed-card">
              {selectedMetric === 'hr' && (
                <TrendLineChart
                  data={hrData}
                  title="Heart Rate"
                  unit="bpm"
                  primaryColor="var(--danger, #ef4444)"
                  height={260}
                />
              )}
              {selectedMetric === 'bp' && (
                <TrendLineChart
                  data={bpData}
                  title="Blood Pressure"
                  primaryLabel="Systolic"
                  secondaryLabel="Diastolic"
                  unit="mmHg"
                  primaryColor="var(--warning, #f59e0b)"
                  secondaryColor="var(--secondary, #3b82f6)"
                  height={260}
                />
              )}
              {selectedMetric === 'spo2' && (
                <TrendLineChart
                  data={spo2Data}
                  title="Oxygen Saturation (SpO2)"
                  unit="%"
                  primaryColor="var(--info, #06b6d4)"
                  height={260}
                />
              )}
              {selectedMetric === 'temp' && (
                <TrendLineChart
                  data={tempData}
                  title="Body Temperature"
                  unit="°C"
                  primaryColor="var(--success, #10b981)"
                  height={260}
                />
              )}
              {selectedMetric === 'rr' && (
                <TrendLineChart
                  data={rrData}
                  title="Respiratory Rate"
                  unit="/min"
                  primaryColor="var(--primary, #6366f1)"
                  height={260}
                />
              )}
            </div>
          )}

          {/* Historical Telemetry Table for Complete Auditability */}
          <div className="vitals-history-table-container">
            <h5 className="table-subheading">Discrete Telemetry Observations ({history.length})</h5>

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
                        <td>{record.deviceId ? <code>{record.deviceId}</code> : 'Not available'}</td>
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
        </div>
      )}
    </Card>
  );
};
