import React from 'react';

export interface LoadingStateProps {
  message?: string;
  compact?: boolean;
}

export const LoadingState: React.FC<LoadingStateProps> = ({
  message = 'Loading clinical data...',
  compact = false,
}) => {
  return (
    <div className={`clinical-loading-container ${compact ? 'compact' : ''}`}>
      <div className="clinical-spinner" />
      <span className="clinical-loading-text">{message}</span>
    </div>
  );
};
