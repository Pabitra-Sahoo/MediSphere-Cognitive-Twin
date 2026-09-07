import React, { useState } from 'react';
import { ChevronDown, ChevronUp } from 'lucide-react';
import type { Patient } from '../../types/patient';
import type { HealthTwin } from '../../types/twin';
import { Badge } from '../common/Badge';
import { TwinFreshnessIndicator } from './TwinFreshnessIndicator';

export interface PatientSummaryProps {
  patient: Patient;
  twin: HealthTwin | null;
  compact?: boolean;
}

export const PatientSummary: React.FC<PatientSummaryProps> = ({ patient, twin, compact = false }) => {
  const [expanded, setExpanded] = useState(false);

  const birthYear = patient.dateOfBirth
    ? parseInt(patient.dateOfBirth.substring(0, 4), 10)
    : null;
  const age = birthYear && !isNaN(birthYear) ? 2026 - birthYear : null;

  const demographics = twin?.demographics;
  const completeness = twin?.completeness?.percentage;

  const initials =
    (patient.firstName?.charAt(0) || '') +
    (patient.lastName?.charAt(0) || '');

  return (
    <div className={`patient-summary-banner ${compact ? 'compact-mode' : ''}`}>
      {/* Compact always-visible row */}
      <div className="summary-primary-row">
        <div className="summary-identity">
          {!compact && (
            <div className="summary-avatar-badge" aria-hidden="true">
              {initials}
            </div>
          )}
          <div className="summary-text-block">
            <h2 className="summary-patient-name">
              {patient.firstName} {patient.lastName}
            </h2>
            <div className="summary-identifiers">
              <span className="identifier-tag">MRN-{patient.mrn}</span>
              <span className="identifier-tag">{patient.gender || 'Unknown'}</span>
              {age !== null && <span className="identifier-tag">{age}</span>}
              {!compact && patient.dateOfBirth && (
                <span className="identifier-tag dob-tag">DOB: {patient.dateOfBirth}</span>
              )}
            </div>
          </div>
        </div>

        <div className="summary-actions-group" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          {!compact && completeness !== undefined && (
            <div className="summary-completeness-box">
              <span className="completeness-box-label">Twin</span>
              <span className={`completeness-box-value ${completeness > 95 ? 'text-pass' : 'text-warn'}`}>
                {completeness}%
              </span>
              <Badge variant={completeness > 95 ? 'success' : 'warning'} size="sm">
                {completeness > 95 ? '>95%' : 'Incomplete'}
              </Badge>
            </div>
          )}

          <button
            type="button"
            className="compact-context-expand-btn"
            onClick={() => setExpanded((e) => !e)}
            aria-expanded={expanded}
            aria-controls="patient-demographics-expanded"
          >
            {expanded ? 'Less' : 'Details'}
            {expanded ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
          </button>
        </div>
      </div>

      {/* Expandable secondary demographics */}
      <div
        id="patient-demographics-expanded"
        className={`summary-demographics-expanded ${expanded ? 'open' : ''}`}
        aria-hidden={!expanded}
      >
        <div className="summary-details-grid">
          <div className="summary-detail-item">
            <span className="detail-label">Blood Type</span>
            <span className="detail-value">{demographics?.bloodType || '—'}</span>
          </div>
          <div className="summary-detail-item">
            <span className="detail-label">Height</span>
            <span className="detail-value">
              {demographics?.height ? `${demographics.height} cm` : '—'}
            </span>
          </div>
          <div className="summary-detail-item">
            <span className="detail-label">Weight</span>
            <span className="detail-value">
              {demographics?.weight ? `${demographics.weight} kg` : '—'}
            </span>
          </div>
          <div className="summary-detail-item">
            <span className="detail-label">BMI</span>
            <span className="detail-value">{demographics?.bmi ?? '—'}</span>
          </div>
          <div className="summary-detail-item">
            <span className="detail-label">Emergency Contact</span>
            <span className="detail-value">
              {patient.emergencyContact?.name
                ? `${patient.emergencyContact.name} (${patient.emergencyContact.relationship || 'Contact'})`
                : '—'}
            </span>
          </div>
          <div className="summary-detail-item">
            <span className="detail-label">Insurance</span>
            <span className="detail-value">
              {patient.insuranceInfo?.provider
                ? `${patient.insuranceInfo.provider} (#${patient.insuranceInfo.policyNumber || '—'})`
                : '—'}
            </span>
          </div>
          <div className="summary-detail-item">
            <span className="detail-label">Assigned Providers</span>
            <span className="detail-value">
              {patient.assignedProviderIds && patient.assignedProviderIds.length > 0
                ? patient.assignedProviderIds.join(', ')
                : 'None assigned'}
            </span>
          </div>
        </div>
      </div>

      {/* Health Twin Synchronization Freshness & Provenance */}
      {!compact && twin && (
        <div className="summary-freshness-wrapper">
          <TwinFreshnessIndicator
            updatedAt={twin.updatedAt}
            latestVitalsTime={twin.latestVitals?.timestamp}
            latestLabsTime={twin.latestLabs?.timestamp}
            fhirSyncStatus={twin.fhirSyncStatus}
            compact
          />
        </div>
      )}
    </div>
  );
};
