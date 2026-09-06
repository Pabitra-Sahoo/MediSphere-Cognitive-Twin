import React from 'react';
import type { TwinFhirSyncStatus } from '../../types/twin';

export interface TwinFreshnessIndicatorProps {
  /** Timestamp when HealthTwin document was last updated in MongoDB */
  updatedAt?: string;
  /** Timestamp when latest vitals observation was recorded */
  latestVitalsTime?: string;
  /** Timestamp when latest lab observation was recorded */
  latestLabsTime?: string;
  /** FHIR synchronization metadata */
  fhirSyncStatus?: TwinFhirSyncStatus;
  /** Streaming latency in milliseconds (receivedAt - recordedAt) if known */
  streamingLatencyMs?: number | null;
  /** Ingestion source type (e.g. WEARABLE, FHIR, SIMULATED) */
  source?: string;
  /** Compact presentation mode */
  compact?: boolean;
}

/**
 * Computes human-readable relative duration from an ISO timestamp string.
 * Strictly observational without clinical value judgment.
 */
function formatRelativeTime(isoString?: string, nowMs?: number): string {
  if (!isoString) return 'Not available';
  const time = new Date(isoString).getTime();
  if (isNaN(time)) return 'Not available';

  const currentMs = nowMs ?? time;
  const diffMs = currentMs - time;
  if (diffMs < 0) return 'Just now';

  const diffSec = Math.floor(diffMs / 1000);
  if (diffSec < 45) return 'Just now';
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin}m ago`;
  const diffHours = Math.floor(diffMin / 60);
  if (diffHours < 24) return `${diffHours}h ago`;
  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 30) return `${diffDays}d ago`;

  return new Date(isoString).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
}

/**
 * Reusable component displaying authoritative Health Twin synchronization
 * freshness and ingestion pipeline telemetry.
 * All statuses reflect technical sync status, never clinical status.
 */
export const TwinFreshnessIndicator: React.FC<TwinFreshnessIndicatorProps> = ({
  updatedAt,
  latestVitalsTime,
  latestLabsTime,
  fhirSyncStatus,
  streamingLatencyMs,
  source,
  compact = false,
}) => {
  const [nowMs] = React.useState(() => Date.now());
  const twinAge = formatRelativeTime(updatedAt, nowMs);
  const vitalsAge = formatRelativeTime(latestVitalsTime, nowMs);
  const labsAge = formatRelativeTime(latestLabsTime, nowMs);
  const fhirAge = formatRelativeTime(fhirSyncStatus?.lastSyncTime, nowMs);

  // Technical synchronization status based on latest twin update
  const isSyncCurrent = updatedAt
    ? nowMs - new Date(updatedAt).getTime() < 1000 * 60 * 15 // within 15 minutes
    : false;

  if (compact) {
    return (
      <div className="twin-freshness-chip" title={`Twin updated: ${twinAge} • Vitals: ${vitalsAge} • Labs: ${labsAge}`}>
        <span className={`sync-status-indicator ${isSyncCurrent ? 'status-current' : 'status-standby'}`} />
        <span className="freshness-label">Twin Sync:</span>
        <span className="freshness-value">{twinAge}</span>
        {streamingLatencyMs !== undefined && streamingLatencyMs !== null && (
          <span className="latency-badge" title="Telemetry ingestion latency (receivedAt - recordedAt)">
            ⚡ {streamingLatencyMs}ms
          </span>
        )}
      </div>
    );
  }

  return (
    <div className="twin-freshness-panel" role="region" aria-label="Digital Twin Data Freshness and Provenance">
      <div className="freshness-header-row">
        <div className="freshness-title-group">
          <span className={`sync-status-indicator ${isSyncCurrent ? 'status-current' : 'status-standby'}`} />
          <span className="freshness-title">Data Freshness & Provenance</span>
          <span className="freshness-technical-badge">
            {isSyncCurrent ? 'Sync Current' : 'Sync Standby'}
          </span>
        </div>
        {streamingLatencyMs !== undefined && streamingLatencyMs !== null && (
          <div className="freshness-latency-chip" title="Kafka stream ingestion transit latency (receivedAt - recordedAt)">
            <span className="latency-label">Pipeline Latency:</span>
            <strong className="latency-value">{streamingLatencyMs} ms</strong>
          </div>
        )}
      </div>

      <div className="freshness-metrics-grid">
        <div className="freshness-metric-item">
          <span className="metric-label">Twin State Modified</span>
          <span className="metric-value">{twinAge}</span>
          <span className="metric-timestamp">
            {updatedAt ? new Date(updatedAt).toLocaleTimeString() : '—'}
          </span>
        </div>

        <div className="freshness-metric-item">
          <span className="metric-label">Latest Vitals Telemetry</span>
          <span className="metric-value">{vitalsAge}</span>
          <span className="metric-timestamp">
            {latestVitalsTime ? new Date(latestVitalsTime).toLocaleTimeString() : '—'}
          </span>
        </div>

        <div className="freshness-metric-item">
          <span className="metric-label">Latest Diagnostic Labs</span>
          <span className="metric-value">{labsAge}</span>
          <span className="metric-timestamp">
            {latestLabsTime ? new Date(latestLabsTime).toLocaleDateString() : '—'}
          </span>
        </div>

        <div className="freshness-metric-item">
          <span className="metric-label">FHIR R4 Synchronization</span>
          <span className="metric-value">{fhirAge}</span>
          <span className="metric-timestamp">
            {fhirSyncStatus?.syncStatus || 'SYNCED'} ({fhirSyncStatus?.resourceCount ?? 0} res)
          </span>
        </div>

        {source && (
          <div className="freshness-metric-item">
            <span className="metric-label">Telemetry Ingest Source</span>
            <span className="metric-value">{source}</span>
            <span className="metric-timestamp">Kafka <code>vitals.ingest</code></span>
          </div>
        )}
      </div>
    </div>
  );
};
