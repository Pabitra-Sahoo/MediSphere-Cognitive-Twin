import React from 'react';
import { AlertTriangle, ShieldOff } from 'lucide-react';

export interface ErrorStateProps {
  error?: unknown;
  title?: string;
  message?: string;
  onRetry?: () => void;
  status?: number;
}

export const ErrorState: React.FC<ErrorStateProps> = ({
  error,
  title,
  message,
  onRetry,
  status,
}) => {
  const isForbidden =
    status === 403 ||
    (error as { response?: { status: number } })?.response?.status === 403;

  const defaultTitle = isForbidden
    ? 'Access Denied (403 Forbidden)'
    : 'Unable to Load Data';

  const defaultMessage = isForbidden
    ? 'You do not have authorization to access this clinical record. Access is protected by patient ownership, provider assignment, and active consent directives.'
    : message ||
      (error instanceof Error ? error.message : 'An unexpected error occurred while communicating with the server.');

  return (
    <div className={`clinical-error-card ${isForbidden ? 'forbidden-card' : ''}`}>
      <div className="error-card-header">
        <span className="error-icon" aria-hidden="true">
          {isForbidden ? <ShieldOff size={18} strokeWidth={1.75} /> : <AlertTriangle size={18} strokeWidth={1.75} />}
        </span>
        <div>
          <h4 className="error-title">{title || defaultTitle}</h4>
          <p className="error-description">{defaultMessage}</p>
        </div>
      </div>
      {onRetry && (
        <div className="error-card-action">
          <button type="button" onClick={onRetry} className="btn btn-secondary btn-sm">
            Retry Request
          </button>
        </div>
      )}
    </div>
  );
};
