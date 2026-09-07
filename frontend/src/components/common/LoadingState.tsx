import React from 'react';
import { Loader2 } from 'lucide-react';

export interface LoadingStateProps {
  message?: string;
  compact?: boolean;
}

export const LoadingState: React.FC<LoadingStateProps> = ({
  message = 'Loading clinical data…',
  compact = false,
}) => {
  return (
    <div className={`clinical-loading-container ${compact ? 'compact' : ''}`}>
      <Loader2
        size={compact ? 18 : 28}
        strokeWidth={1.75}
        className="clinical-spinner-icon"
        aria-hidden="true"
        style={{ animation: 'spin 800ms linear infinite' }}
      />
      <span className="clinical-loading-text">{message}</span>
    </div>
  );
};
