import React from 'react';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';

export interface TwinViewerSkeletonProps {
  patientName?: string;
  patientId?: string;
}

/**
 * Lightweight loading skeleton displayed while the 3D Digital Twin Viewer
 * bundle (Three.js / React-Three-Fiber) is loaded asynchronously.
 * Does not import Three.js or introduce synthetic medical data.
 */
export const TwinViewerSkeleton: React.FC<TwinViewerSkeletonProps> = ({
  patientName,
  patientId,
}) => {
  return (
    <Card
      title="3D Digital Twin Viewer"
      subtitle={`Spatial physiological twin representation for ${patientName || patientId || 'selected patient'}`}
      action={
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          <Badge variant="neutral" size="sm">
            Initializing WebGL...
          </Badge>
          <button
            type="button"
            className="btn btn-secondary btn-xs"
            disabled
            aria-disabled="true"
          >
            Reset Camera
          </button>
        </div>
      }
      className="twin-3d-card"
    >
      <div className="twin-3d-viewport-container" role="status" aria-live="polite" aria-busy="true">
        {/* Header strip placeholder */}
        <div className="twin-viewport-header">
          <div className="twin-header-identity">
            <span className="twin-patient-label">Spatial Twin:</span>
            <strong className="twin-patient-name">{patientName || patientId || 'Loading patient...'}</strong>
            {patientId && <code className="code-subtle">({patientId})</code>}
          </div>
          <div className="twin-header-controls-hint">
            <span>Loading spatial environment...</span>
          </div>
        </div>

        {/* Skeleton Canvas Viewport matching 420px height */}
        <div className="twin-canvas-container twin-skeleton-canvas">
          <div className="twin-skeleton-content">
            <div className="twin-skeleton-avatar-wireframe" aria-hidden="true">
              <div className="wireframe-head" />
              <div className="wireframe-torso" />
              <div className="wireframe-pulse-ring" />
            </div>
            <div className="twin-skeleton-text">
              <span className="twin-skeleton-title">Loading 3D Spatial Twin</span>
              <p className="twin-skeleton-desc">
                Streaming anatomical spatial viewport and WebGL shaders for {patientName || 'patient'}...
              </p>
            </div>
            <div className="twin-skeleton-progress-bar">
              <div className="twin-skeleton-progress-fill" />
            </div>
          </div>
        </div>
      </div>
    </Card>
  );
};
