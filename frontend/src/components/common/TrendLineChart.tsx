import React, { useState, useId } from 'react';
import { TrendingUp, AlertTriangle } from 'lucide-react';

export interface TrendDataPoint {
  /** ISO timestamp string of observation */
  timestamp: string;
  /** Primary numeric measurement value */
  value: number;
  /** Optional secondary measurement value (e.g. Diastolic BP) */
  secondaryValue?: number;
  /** Whether the measurement passed clinical data-quality boundary validation */
  isValid?: boolean;
  /** Any validation errors associated with this point */
  validationErrors?: string[];
  /** Sensor, wearable or source identifier */
  deviceId?: string;
  /** Source channel */
  source?: string;
  /** Optional custom display label */
  label?: string;
}

export interface TrendReferenceRange {
  low?: number;
  high?: number;
  label?: string;
}

export interface TrendLineChartProps {
  /** Array of chronological observations (sorted ASC by timestamp) */
  data: TrendDataPoint[];
  /** Human-readable title of the measurement series */
  title: string;
  /** Unit of measurement (e.g. bpm, mmHg, %, °C, mg/dL) */
  unit: string;
  /** Label for primary series (defaults to title) */
  primaryLabel?: string;
  /** Label for secondary series (e.g. Diastolic) */
  secondaryLabel?: string;
  /** Primary line color (CSS color or theme var) */
  primaryColor?: string;
  /** Secondary line color (CSS color or theme var) */
  secondaryColor?: string;
  /** Chart height in pixels (default 220) */
  height?: number;
  /** Optional reference bounds supplied by authoritative source */
  referenceRange?: TrendReferenceRange;
  /** Optional accessible description */
  description?: string;
  /** Optional compact mode for dashboard tiles */
  compact?: boolean;
}

/**
 * Zero-dependency, purely observational SVG time-series trendline chart.
 * Renders longitudinal clinical telemetry without interpolating missing values
 * or making diagnostic interpretations.
 */
export const TrendLineChart: React.FC<TrendLineChartProps> = ({
  data,
  title,
  unit,
  primaryLabel,
  secondaryLabel,
  primaryColor = '#3b82f6',
  secondaryColor = '#8b5cf6',
  height = 220,
  referenceRange,
  description,
  compact = false,
}) => {
  const chartId = useId();
  const [hoveredPoint, setHoveredPoint] = useState<{
    point: TrendDataPoint;
    x: number;
    y: number;
  } | null>(null);

  // Filter out points with undefined or NaN values
  const validPoints = data.filter((d) => d && typeof d.value === 'number' && !isNaN(d.value));
  const hasSecondary = validPoints.some((d) => typeof d.secondaryValue === 'number' && !isNaN(d.secondaryValue));

  if (validPoints.length === 0) {
    return (
      <div className="trend-chart-empty-state" style={{ minHeight: height }}>
        <span className="empty-icon" aria-hidden="true">
          <TrendingUp size={28} strokeWidth={1.5} />
        </span>
        <strong className="empty-title">No Historical Observations</strong>
        <p className="empty-text">No recorded telemetry points available for {title}.</p>
      </div>
    );
  }

  // Chart dimensions in SVG viewBox coordinate space
  const svgWidth = 600;
  const svgHeight = height;
  const padding = compact
    ? { top: 16, right: 16, bottom: 28, left: 38 }
    : { top: 24, right: 28, bottom: 38, left: 52 };

  const plotWidth = svgWidth - padding.left - padding.right;
  const plotHeight = svgHeight - padding.top - padding.bottom;

  // Compute value extent
  const primaryValues = validPoints.map((d) => d.value);
  const secondaryValues = hasSecondary
    ? validPoints.filter((d) => typeof d.secondaryValue === 'number').map((d) => d.secondaryValue as number)
    : [];

  const allValues = [...primaryValues, ...secondaryValues];
  if (referenceRange?.low !== undefined) allValues.push(referenceRange.low);
  if (referenceRange?.high !== undefined) allValues.push(referenceRange.high);

  let rawMin = Math.min(...allValues);
  let rawMax = Math.max(...allValues);

  // Prevent flat scale if min equals max
  if (rawMin === rawMax) {
    rawMin -= 1;
    rawMax += 1;
  }

  // 8% margin padding so points don't clip on edges
  const valueSpan = rawMax - rawMin;
  const margin = valueSpan * 0.08;
  const yMin = rawMin - margin;
  const yMax = rawMax + margin;
  const yRange = yMax - yMin;

  // Time extent
  const timestamps = validPoints.map((d) => new Date(d.timestamp).getTime());
  const minTime = Math.min(...timestamps);
  const maxTime = Math.max(...timestamps);
  const timeSpan = maxTime - minTime;

  // Coordinate mapping functions
  const getX = (idx: number, timestampStr: string) => {
    if (validPoints.length === 1) {
      return padding.left + plotWidth / 2;
    }
    if (timeSpan <= 0) {
      return padding.left + (idx / (validPoints.length - 1)) * plotWidth;
    }
    const t = new Date(timestampStr).getTime();
    return padding.left + ((t - minTime) / timeSpan) * plotWidth;
  };

  const getY = (val: number) => {
    return padding.top + plotHeight - ((val - yMin) / yRange) * plotHeight;
  };

  // Generate SVG Path for a series
  const buildPath = (getVal: (d: TrendDataPoint) => number | undefined) => {
    const coords: { x: number; y: number }[] = [];
    validPoints.forEach((d, idx) => {
      const val = getVal(d);
      if (val !== undefined && !isNaN(val)) {
        coords.push({ x: getX(idx, d.timestamp), y: getY(val) });
      }
    });

    if (coords.length === 0) return '';
    return coords.reduce((acc, pt, i) => `${acc} ${i === 0 ? 'M' : 'L'} ${pt.x.toFixed(1)} ${pt.y.toFixed(1)}`, '');
  };

  const primaryPath = buildPath((d) => d.value);
  const secondaryPath = hasSecondary ? buildPath((d) => d.secondaryValue) : '';

  // Area under the primary curve for depth
  const primaryAreaPath =
    validPoints.length > 1
      ? `${primaryPath} L ${getX(validPoints.length - 1, validPoints[validPoints.length - 1].timestamp).toFixed(1)} ${(
          padding.top + plotHeight
        ).toFixed(1)} L ${getX(0, validPoints[0].timestamp).toFixed(1)} ${(padding.top + plotHeight).toFixed(1)} Z`
      : '';

  // Reference Range Band
  let refTopY: number | null = null;
  let refBottomY: number | null = null;
  if (referenceRange?.high !== undefined && referenceRange?.low !== undefined) {
    refTopY = getY(referenceRange.high);
    refBottomY = getY(referenceRange.low);
  }

  // Y-axis tick gridlines (4 ticks)
  const yTicks = [0, 0.33, 0.66, 1].map((pct) => {
    const val = yMin + pct * yRange;
    const y = getY(val);
    return { val: Math.round(val * 10) / 10, y };
  });

  // X-axis time ticks
  const xTicks =
    validPoints.length === 1
      ? [{ x: padding.left + plotWidth / 2, label: new Date(validPoints[0].timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) }]
      : [0, 0.5, 1].map((pct) => {
          const t = minTime + pct * timeSpan;
          const x = padding.left + pct * plotWidth;
          const d = new Date(t);
          const label =
            timeSpan > 86400000
              ? `${d.toLocaleDateString([], { month: 'numeric', day: 'numeric' })} ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
              : d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
          return { x, label };
        });

  // Accessible summary text
  const firstDate = new Date(validPoints[0].timestamp).toLocaleString();
  const lastDate = new Date(validPoints[validPoints.length - 1].timestamp).toLocaleString();
  const accessibleSummary =
    description ||
    `Observation timeline for ${title}. Showing ${validPoints.length} point${validPoints.length === 1 ? '' : 's'} from ${firstDate} to ${lastDate}. Latest observed reading is ${validPoints[validPoints.length - 1].value} ${unit}.`;

  return (
    <div className={`trend-line-chart-container ${compact ? 'compact' : ''}`} style={{ position: 'relative' }}>
      {/* Legend strip if secondary series or reference band exists */}
      {(hasSecondary || referenceRange) && !compact && (
        <div className="trend-chart-legend">
          <div className="legend-item">
            <span className="legend-dot" style={{ backgroundColor: primaryColor }} />
            <span className="legend-label">{primaryLabel || title}</span>
          </div>
          {hasSecondary && (
            <div className="legend-item">
              <span className="legend-dot" style={{ backgroundColor: secondaryColor }} />
              <span className="legend-label">{secondaryLabel || 'Diastolic'}</span>
            </div>
          )}
          {referenceRange && (
            <div className="legend-item reference">
              <span className="legend-dash" />
              <span className="legend-label">
                Ref: {referenceRange.low ?? '—'} - {referenceRange.high ?? '—'} {unit}
              </span>
            </div>
          )}
        </div>
      )}

      {/* SVG Canvas */}
      <svg
        viewBox={`0 0 ${svgWidth} ${svgHeight}`}
        className="trend-svg"
        role="img"
        aria-label={accessibleSummary}
        preserveAspectRatio="xMidYMid meet"
      >
        <title>{title} Temporal Timeline</title>
        <desc>{accessibleSummary}</desc>

        <defs>
          <linearGradient id={`gradient-${chartId}`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={primaryColor} stopOpacity="0.25" />
            <stop offset="100%" stopColor={primaryColor} stopOpacity="0.0" />
          </linearGradient>
        </defs>

        {/* Reference Range Zone if provided */}
        {refTopY !== null && refBottomY !== null && (
          <rect
            x={padding.left}
            y={refTopY}
            width={plotWidth}
            height={Math.max(0, refBottomY - refTopY)}
            className="trend-reference-band"
          />
        )}

        {/* Y Gridlines and Labels */}
        {yTicks.map((tick, i) => (
          <g key={`ytick-${i}`} className="trend-grid-line-group">
            <line
              x1={padding.left}
              y1={tick.y}
              x2={padding.left + plotWidth}
              y2={tick.y}
              className="trend-grid-line"
            />
            <text
              x={padding.left - 8}
              y={tick.y + 4}
              className="trend-axis-label trend-y-label"
              textAnchor="end"
            >
              {tick.val}
            </text>
          </g>
        ))}

        {/* X Axis & Time Labels */}
        <line
          x1={padding.left}
          y1={padding.top + plotHeight}
          x2={padding.left + plotWidth}
          y2={padding.top + plotHeight}
          className="trend-axis-baseline"
        />
        {xTicks.map((tick, i) => (
          <text
            key={`xtick-${i}`}
            x={tick.x}
            y={padding.top + plotHeight + 18}
            className="trend-axis-label trend-x-label"
            textAnchor={i === 0 ? 'start' : i === xTicks.length - 1 ? 'end' : 'middle'}
          >
            {tick.label}
          </text>
        ))}

        {/* Gradient fill under primary line */}
        {primaryAreaPath && <path d={primaryAreaPath} fill={`url(#gradient-${chartId})`} />}

        {/* Secondary line (e.g. Diastolic BP) */}
        {secondaryPath && (
          <path
            d={secondaryPath}
            fill="none"
            stroke={secondaryColor}
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        )}

        {/* Primary line (e.g. Heart Rate, Systolic BP, etc.) */}
        {primaryPath && (
          <path
            d={primaryPath}
            fill="none"
            stroke={primaryColor}
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        )}

        {/* Single-point callout line */}
        {validPoints.length === 1 && (
          <line
            x1={padding.left + plotWidth / 2}
            y1={padding.top}
            x2={padding.left + plotWidth / 2}
            y2={padding.top + plotHeight}
            className="trend-single-point-guide"
          />
        )}

        {/* Secondary Data Points */}
        {hasSecondary &&
          validPoints.map((pt, i) => {
            if (pt.secondaryValue === undefined) return null;
            const cx = getX(i, pt.timestamp);
            const cy = getY(pt.secondaryValue);
            return (
              <circle
                key={`sec-pt-${i}`}
                cx={cx}
                cy={cy}
                r={hoveredPoint?.point === pt ? 5.5 : 3.5}
                fill={secondaryColor}
                stroke="var(--card-bg, #1e293b)"
                strokeWidth="2"
                className="trend-point"
              />
            );
          })}

        {/* Primary Data Points with interactive focus & hover */}
        {validPoints.map((pt, i) => {
          const cx = getX(i, pt.timestamp);
          const cy = getY(pt.value);
          const isFlagged = pt.isValid === false;
          const isHovered = hoveredPoint?.point === pt;

          return (
            <g key={`pt-${i}`}>
              <circle
                cx={cx}
                cy={cy}
                r={isHovered ? 6 : validPoints.length === 1 ? 5.5 : 4}
                fill={isFlagged ? 'var(--warning, #f59e0b)' : primaryColor}
                stroke="var(--card-bg, #1e293b)"
                strokeWidth={isHovered ? 2.5 : 2}
                className={`trend-point ${isFlagged ? 'point-flagged' : ''}`}
                tabIndex={0}
                role="button"
                aria-label={`${title}: ${pt.value} ${unit} observed at ${new Date(pt.timestamp).toLocaleString()}${
                  isFlagged ? ' (Data quality boundary flag)' : ''
                }`}
                onMouseEnter={() => setHoveredPoint({ point: pt, x: cx, y: cy })}
                onMouseLeave={() => setHoveredPoint(null)}
                onFocus={() => setHoveredPoint({ point: pt, x: cx, y: cy })}
                onBlur={() => setHoveredPoint(null)}
              />
              {isFlagged && (
                <text
                  x={cx}
                  y={cy - 8}
                  className="trend-flag-glyph"
                  textAnchor="middle"
                  aria-hidden="true"
                >
                  !
                </text>
              )}
            </g>
          );
        })}
      </svg>

      {/* Floating Hover Tooltip (HTML overlay clamped safely) */}
      {hoveredPoint && (
        <div
          className="trend-tooltip"
          style={{
            position: 'absolute',
            left: `${(hoveredPoint.x / svgWidth) * 100}%`,
            top: `${(hoveredPoint.y / svgHeight) * 100}%`,
            transform: `translate(${hoveredPoint.x > svgWidth * 0.7 ? '-100%' : hoveredPoint.x < svgWidth * 0.3 ? '0%' : '-50%'}, -115%)`,
            pointerEvents: 'none',
          }}
          role="tooltip"
        >
          <div className="tooltip-header">
            <span className="tooltip-time">
              {new Date(hoveredPoint.point.timestamp).toLocaleString()}
            </span>
            {hoveredPoint.point.isValid !== undefined && (
              <span className={`tooltip-badge ${hoveredPoint.point.isValid ? 'badge-valid' : 'badge-flagged'}`}>
                {hoveredPoint.point.isValid ? 'Valid Quality' : 'Quality Flag'}
              </span>
            )}
          </div>
          <div className="tooltip-body">
            <div className="tooltip-metric-row">
              <span className="tooltip-metric-name">{primaryLabel || title}:</span>
              <strong className="tooltip-metric-val">
                {hoveredPoint.point.value} {unit}
              </strong>
            </div>
            {hasSecondary && hoveredPoint.point.secondaryValue !== undefined && (
              <div className="tooltip-metric-row">
                <span className="tooltip-metric-name">{secondaryLabel || 'Diastolic'}:</span>
                <strong className="tooltip-metric-val">
                  {hoveredPoint.point.secondaryValue} {unit}
                </strong>
              </div>
            )}
            {hoveredPoint.point.source && (
              <div className="tooltip-meta-row">
                <span>Source: {hoveredPoint.point.source}</span>
                {hoveredPoint.point.deviceId && <span> • Device: {hoveredPoint.point.deviceId}</span>}
              </div>
            )}
            {hoveredPoint.point.validationErrors && hoveredPoint.point.validationErrors.length > 0 && (
              <div className="tooltip-validation-errors">
                {hoveredPoint.point.validationErrors.map((err, idx) => (
                  <div key={idx} className="tooltip-error-line">
                    <AlertTriangle size={11} style={{ display: 'inline', marginRight: 4, verticalAlign: 'middle' }} />
                    {err}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Single Point Observation Notice */}
      {validPoints.length === 1 && (
        <div className="trend-single-point-notice">
          <span>Single observation recorded: <strong>{validPoints[0].value} {unit}</strong> at {new Date(validPoints[0].timestamp).toLocaleString()}</span>
        </div>
      )}
    </div>
  );
};
