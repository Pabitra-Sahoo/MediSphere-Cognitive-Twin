import React from 'react';
import { useAuth } from '../../auth/useAuth';
import type { PatientSummary } from '../../types/patient';

export interface PatientSelectorProps {
  patients: PatientSummary[];
  selectedPatientId?: string;
  onSelectPatient: (patientId: string) => void;
  isLoading?: boolean;
}

export const PatientSelector: React.FC<PatientSelectorProps> = ({
  patients,
  selectedPatientId,
  onSelectPatient,
  isLoading = false,
}) => {
  const { user } = useAuth();
  const isPatientRole = user?.role === 'PATIENT';

  if (isPatientRole) {
    return (
      <div className="patient-selector-card locked">
        <div className="selector-lock-info">
          <span className="selector-lock-icon">🔒</span>
          <div>
            <span className="selector-caption">Personal Record Mode</span>
            <div className="selector-patient-display">
              Patient Account: <strong>{user.username}</strong> ({user.linkedPatientId || 'Linked Patient'})
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="patient-selector-card">
      <div className="selector-header-row">
        <label htmlFor="patient-select-dropdown" className="selector-label">
          <span className="selector-icon">🏥</span> Authorized Patients ({patients.length})
        </label>
        {isLoading && <span className="selector-spinner-text">Updating list...</span>}
      </div>

      <div className="selector-controls">
        <select
          id="patient-select-dropdown"
          className="clinical-select"
          value={selectedPatientId || ''}
          onChange={(e) => onSelectPatient(e.target.value)}
          disabled={isLoading || patients.length === 0}
        >
          {patients.length === 0 ? (
            <option value="" disabled>
              {isLoading ? 'Loading authorized patients...' : 'No authorized patients found'}
            </option>
          ) : (
            patients.map((p) => (
              <option key={p.id} value={p.id}>
                {p.firstName} {p.lastName} — MRN: {p.mrn} (Twin: {p.twinCompleteness}%)
              </option>
            ))
          )}
        </select>
      </div>
    </div>
  );
};
