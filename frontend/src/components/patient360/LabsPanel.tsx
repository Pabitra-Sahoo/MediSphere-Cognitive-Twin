import React, { useState, useEffect, useCallback, useMemo } from 'react';
import type { TwinLabs } from '../../types/twin';
import type { LabResultRecord } from '../../types/lab';
import { labApi } from '../../api/labApi';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { LoadingState } from '../common/LoadingState';
import { ErrorState } from '../common/ErrorState';
import { EmptyState } from '../common/EmptyState';
import { TrendLineChart, type TrendDataPoint, type TrendReferenceRange } from '../common/TrendLineChart';

export interface LabsPanelProps {
  patientId: string;
  twinLabs?: TwinLabs;
  showHistory?: boolean;
}

export type LabAnalyteType = 'all' | 'glucose' | 'cholesterol' | 'hemoglobin' | 'creatinine';

export const LabsPanel: React.FC<LabsPanelProps> = ({
  patientId,
  twinLabs,
  showHistory = false,
}) => {
  const [labs, setLabs] = useState<LabResultRecord[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [selectedAnalyte, setSelectedAnalyte] = useState<LabAnalyteType>('all');

  const fetchLabs = useCallback(async () => {
    if (!patientId) return;
    setIsLoading(true);
    setError(null);
    try {
      const pageRes = await labApi.getLabs(patientId, 0, 50);
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
        setLabs([]);
      }
    });

    labApi.getLabs(patientId, 0, 50)
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

  // Helper to match analyte by LOINC code or clinical name
  const matchesAnalyte = (record: LabResultRecord, analyte: LabAnalyteType): boolean => {
    const code = record.testCode?.toLowerCase() || '';
    const name = record.testName?.toLowerCase() || '';

    switch (analyte) {
      case 'glucose':
        return code === '2345-7' || code === '2339-0' || name.includes('glucose');
      case 'cholesterol':
        return code === '2093-3' || name.includes('cholesterol');
      case 'hemoglobin':
        return code === '718-7' || name.includes('hemoglobin');
      case 'creatinine':
        return code === '2160-0' || name.includes('creatinine');
      default:
        return false;
    }
  };

  // Build sorted ASC time-series data for an analyte
  const getAnalyteSeries = useCallback((analyte: LabAnalyteType): { points: TrendDataPoint[]; unit: string; refRange?: TrendReferenceRange } => {
    const matched = labs
      .filter((l) => matchesAnalyte(l, analyte) && typeof l.value === 'number' && !isNaN(l.value))
      .sort((a, b) => new Date(a.performedAt).getTime() - new Date(b.performedAt).getTime());

    const points: TrendDataPoint[] = matched.map((l) => ({
      timestamp: l.performedAt,
      value: l.value,
      label: l.testName,
      source: l.source,
      deviceId: l.testCode,
      raw: l,
    }));

    const unit = matched[0]?.unit || (analyte === 'hemoglobin' ? 'g/dL' : 'mg/dL');

    // Extract actual reference range supplied by the lab record without fabricating
    const refRecord = matched.find((l) => l.referenceRange && (l.referenceRange.low !== undefined || l.referenceRange.high !== undefined));
    const refRange: TrendReferenceRange | undefined = refRecord?.referenceRange
      ? {
          low: refRecord.referenceRange.low,
          high: refRecord.referenceRange.high,
          label: `${refRecord.referenceRange.low ?? '—'} - ${refRecord.referenceRange.high ?? '—'} ${unit}`,
        }
      : undefined;

    return { points, unit, refRange };
  }, [labs]);

  const glucoseSeries = useMemo(() => getAnalyteSeries('glucose'), [getAnalyteSeries]);
  const cholesterolSeries = useMemo(() => getAnalyteSeries('cholesterol'), [getAnalyteSeries]);
  const hemoglobinSeries = useMemo(() => getAnalyteSeries('hemoglobin'), [getAnalyteSeries]);
  const creatinineSeries = useMemo(() => getAnalyteSeries('creatinine'), [getAnalyteSeries]);

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

      {/* Historical Laboratory Tests & Temporal Trends */}
      {showHistory && (
        <div className="labs-trends-section">
          <div className="trends-section-header">
            <div className="trends-header-title-group">
              <h4 className="section-subheading">Longitudinal Laboratory Analyte Trends</h4>
              <span className="trends-subtitle">
                Authoritative laboratory reports and time-series panels ({labs.length} records)
              </span>
            </div>
            <button
              type="button"
              onClick={fetchLabs}
              className="btn btn-secondary btn-xs"
              disabled={isLoading}
            >
              Refresh Labs
            </button>
          </div>

          {/* Analyte Selector Tabs */}
          <div className="trends-metric-tabs" role="tablist" aria-label="Select laboratory analyte trend">
            <button
              type="button"
              role="tab"
              aria-selected={selectedAnalyte === 'all'}
              className={`metric-tab-btn ${selectedAnalyte === 'all' ? 'active' : ''}`}
              onClick={() => setSelectedAnalyte('all')}
            >
              All Panels
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={selectedAnalyte === 'glucose'}
              className={`metric-tab-btn ${selectedAnalyte === 'glucose' ? 'active' : ''}`}
              onClick={() => setSelectedAnalyte('glucose')}
            >
              🍬 Fasting Glucose ({glucoseSeries.points.length})
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={selectedAnalyte === 'cholesterol'}
              className={`metric-tab-btn ${selectedAnalyte === 'cholesterol' ? 'active' : ''}`}
              onClick={() => setSelectedAnalyte('cholesterol')}
            >
              🧪 Cholesterol ({cholesterolSeries.points.length})
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={selectedAnalyte === 'hemoglobin'}
              className={`metric-tab-btn ${selectedAnalyte === 'hemoglobin' ? 'active' : ''}`}
              onClick={() => setSelectedAnalyte('hemoglobin')}
            >
              🩸 Hemoglobin ({hemoglobinSeries.points.length})
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={selectedAnalyte === 'creatinine'}
              className={`metric-tab-btn ${selectedAnalyte === 'creatinine' ? 'active' : ''}`}
              onClick={() => setSelectedAnalyte('creatinine')}
            >
              💧 Creatinine ({creatinineSeries.points.length})
            </button>
          </div>

          {/* Analyte Trends Viewport */}
          {selectedAnalyte === 'all' ? (
            <div className="trends-overview-grid">
              {/* Glucose Tile */}
              <div className="trend-tile">
                <div className="trend-tile-header">
                  <span className="tile-title">Fasting Glucose Timeline</span>
                  <span className="tile-unit">{glucoseSeries.unit}</span>
                </div>
                <TrendLineChart
                  data={glucoseSeries.points}
                  title="Fasting Glucose"
                  unit={glucoseSeries.unit}
                  primaryColor="var(--warning, #f59e0b)"
                  referenceRange={glucoseSeries.refRange}
                  height={190}
                  compact
                />
              </div>

              {/* Cholesterol Tile */}
              <div className="trend-tile">
                <div className="trend-tile-header">
                  <span className="tile-title">Total Cholesterol Timeline</span>
                  <span className="tile-unit">{cholesterolSeries.unit}</span>
                </div>
                <TrendLineChart
                  data={cholesterolSeries.points}
                  title="Total Cholesterol"
                  unit={cholesterolSeries.unit}
                  primaryColor="var(--info, #06b6d4)"
                  referenceRange={cholesterolSeries.refRange}
                  height={190}
                  compact
                />
              </div>

              {/* Hemoglobin Tile */}
              <div className="trend-tile">
                <div className="trend-tile-header">
                  <span className="tile-title">Hemoglobin Timeline</span>
                  <span className="tile-unit">{hemoglobinSeries.unit}</span>
                </div>
                <TrendLineChart
                  data={hemoglobinSeries.points}
                  title="Hemoglobin"
                  unit={hemoglobinSeries.unit}
                  primaryColor="var(--danger, #ef4444)"
                  referenceRange={hemoglobinSeries.refRange}
                  height={190}
                  compact
                />
              </div>

              {/* Creatinine Tile */}
              <div className="trend-tile">
                <div className="trend-tile-header">
                  <span className="tile-title">Serum Creatinine Timeline</span>
                  <span className="tile-unit">{creatinineSeries.unit}</span>
                </div>
                <TrendLineChart
                  data={creatinineSeries.points}
                  title="Serum Creatinine"
                  unit={creatinineSeries.unit}
                  primaryColor="var(--secondary, #3b82f6)"
                  referenceRange={creatinineSeries.refRange}
                  height={190}
                  compact
                />
              </div>
            </div>
          ) : (
            /* Detailed Single Analyte Trend */
            <div className="trend-detailed-card">
              {selectedAnalyte === 'glucose' && (
                <TrendLineChart
                  data={glucoseSeries.points}
                  title="Fasting Glucose"
                  unit={glucoseSeries.unit}
                  primaryColor="var(--warning, #f59e0b)"
                  referenceRange={glucoseSeries.refRange}
                  height={260}
                />
              )}
              {selectedAnalyte === 'cholesterol' && (
                <TrendLineChart
                  data={cholesterolSeries.points}
                  title="Total Cholesterol"
                  unit={cholesterolSeries.unit}
                  primaryColor="var(--info, #06b6d4)"
                  referenceRange={cholesterolSeries.refRange}
                  height={260}
                />
              )}
              {selectedAnalyte === 'hemoglobin' && (
                <TrendLineChart
                  data={hemoglobinSeries.points}
                  title="Hemoglobin"
                  unit={hemoglobinSeries.unit}
                  primaryColor="var(--danger, #ef4444)"
                  referenceRange={hemoglobinSeries.refRange}
                  height={260}
                />
              )}
              {selectedAnalyte === 'creatinine' && (
                <TrendLineChart
                  data={creatinineSeries.points}
                  title="Serum Creatinine"
                  unit={creatinineSeries.unit}
                  primaryColor="var(--secondary, #3b82f6)"
                  referenceRange={creatinineSeries.refRange}
                  height={260}
                />
              )}
            </div>
          )}

          {/* Discrete Laboratory Records Table */}
          <div className="labs-history-table-container">
            <h5 className="table-subheading">Discrete Laboratory Test Records ({labs.length})</h5>

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
                        : 'Not provided by laboratory';

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
        </div>
      )}
    </Card>
  );
};
