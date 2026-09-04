import React from 'react';
import type { TwinCompleteness } from '../../types/twin';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';

export interface CompletenessCardProps {
  completeness?: TwinCompleteness;
  isLoading?: boolean;
}

export const CompletenessCard: React.FC<CompletenessCardProps> = ({
  completeness,
  isLoading = false,
}) => {
  if (isLoading || !completeness) {
    return (
      <Card title="Twin Completeness" subtitle="Logical field population status">
        <div className="completeness-skeleton">
          <div className="clinical-spinner" />
          <span>Calculating twin completeness...</span>
        </div>
      </Card>
    );
  }

  const { percentage, populatedFields, totalFields, missingFields } = completeness;
  const isTargetMet = percentage > 95;

  return (
    <Card
      title="Digital Twin Completeness"
      subtitle="Evaluation against 20 logical clinical fields"
      action={
        <Badge variant={isTargetMet ? 'success' : 'warning'}>
          {isTargetMet ? 'Criterion Met (>95%)' : 'Below Target (≤95%)'}
        </Badge>
      }
    >
      <div className="completeness-panel-content">
        <div className="completeness-metric-row">
          <div className="completeness-number-display">
            <span className={`completeness-large-percent ${isTargetMet ? 'text-pass' : 'text-warn'}`}>
              {percentage}%
            </span>
            <span className="completeness-ratio">
              {populatedFields} / {totalFields} fields populated
            </span>
          </div>

          <div className="completeness-rule-notice">
            <div className="rule-title">Target Requirement</div>
            <div className="rule-description">
              Strictly <strong>&gt;95%</strong> logical field completion required for digital twin validity.
            </div>
          </div>
        </div>

        <div className="completeness-progress-track">
          <div
            className={`completeness-progress-fill ${isTargetMet ? 'fill-pass' : 'fill-warn'}`}
            style={{ width: `${Math.min(100, percentage)}%` }}
          />
        </div>

        {missingFields && missingFields.length > 0 ? (
          <div className="missing-fields-section">
            <span className="missing-fields-title">Missing Logical Fields ({missingFields.length}):</span>
            <div className="missing-fields-tags">
              {missingFields.map((field) => (
                <span key={field} className="missing-field-tag">
                  {field}
                </span>
              ))}
            </div>
          </div>
        ) : (
          <div className="all-fields-complete-notice">
            <span className="complete-check-icon">✓</span>
            <span>All {totalFields} required logical fields are fully populated (100% complete).</span>
          </div>
        )}
      </div>
    </Card>
  );
};
