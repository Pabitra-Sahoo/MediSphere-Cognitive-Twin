import React from 'react';
import type { Patient } from '../../types/patient';
import type { HealthTwin } from '../../types/twin';
import { Badge } from '../common/Badge';
import { TwinFreshnessIndicator } from './TwinFreshnessIndicator';

export interface PatientSummaryProps {
  patient: Patient;
  twin: HealthTwin | null;
}

export const PatientSummary: React.FC<PatientSummaryProps> = ({ patient, twin }) => {
  const birthYear = patient.dateOfBirth
    ? parseInt(patient.dateOfBirth.substring(0, 4), 10)
    : null;
  const age = birthYear && !isNaN(birthYear) ? 2026 - birthYear : null;

  const demographics = twin?.demographics;
  const completeness = twin?.completeness?.percentage;

  return (
    <div className="patient-summary-banner">
      <div className="summary-primary-row">
        <div className="summary-identity">
          <div className="summary-avatar-badge">
            <span className="summary-avatar-initials">
              {patient.firstName?.charAt(0) || ''}
              {patient.lastName?.charAt(0) || ''}
            </span>
          </div>
          <div>
            <h2 className="summary-patient-name">
              {patient.firstName} {patient.lastName}
            </h2>
            <div className="summary-identifiers">
              <span className="identifier-tag">MRN: <strong>{patient.mrn}</strong></span>
              <span className="identifier-tag">ID: <code>{patient.id}</code></span>
              <span className="identifier-tag">
                DOB: {patient.dateOfBirth || 'Not available'} {age !== null ? `(${age} yrs)` : ''}
              </span>
              <span className="identifier-tag">Gender: {patient.gender || 'Not available'}</span>
            </div>
          </div>
        </div>

        {completeness !== undefined && (
          <div className="summary-completeness-box">
            <span className="completeness-box-label">Twin Completeness</span>
            <div className="completeness-box-value">
              <strong className={completeness > 95 ? 'text-pass' : 'text-warn'}>
                {completeness}%
              </strong>
            </div>
            <Badge variant={completeness > 95 ? 'success' : 'warning'} size="sm">
              {completeness > 95 ? 'M1 Valid (>95%)' : 'Incomplete'}
            </Badge>
          </div>
        )}
      </div>

      <div className="summary-details-grid">
        <div className="summary-detail-item">
          <span className="detail-label">Blood Type</span>
          <span className="detail-value">{demographics?.bloodType || 'Not available'}</span>
        </div>
        <div className="summary-detail-item">
          <span className="detail-label">Height</span>
          <span className="detail-value">
            {demographics?.height ? `${demographics.height} cm` : 'Not available'}
          </span>
        </div>
        <div className="summary-detail-item">
          <span className="detail-label">Weight</span>
          <span className="detail-value">
            {demographics?.weight ? `${demographics.weight} kg` : 'Not available'}
          </span>
        </div>
        <div className="summary-detail-item">
          <span className="detail-label">BMI</span>
          <span className="detail-value">{demographics?.bmi ? demographics.bmi : 'Not available'}</span>
        </div>
        <div className="summary-detail-item">
          <span className="detail-label">Emergency Contact</span>
          <span className="detail-value">
            {patient.emergencyContact?.name
              ? `${patient.emergencyContact.name} (${patient.emergencyContact.relationship || 'Contact'})`
              : 'Not available'}
          </span>
        </div>
        <div className="summary-detail-item">
          <span className="detail-label">Insurance</span>
          <span className="detail-value">
            {patient.insuranceInfo?.provider
              ? `${patient.insuranceInfo.provider} (#${patient.insuranceInfo.policyNumber || '—'})`
              : 'Not available'}
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

      {/* Health Twin Synchronization Freshness & Provenance */}
      {twin && (
        <div className="summary-freshness-wrapper">
          <TwinFreshnessIndicator
            updatedAt={twin.updatedAt}
            latestVitalsTime={twin.latestVitals?.timestamp}
            latestLabsTime={twin.latestLabs?.timestamp}
            fhirSyncStatus={twin.fhirSyncStatus}
          />
        </div>
      )}
    </div>
  );
};
