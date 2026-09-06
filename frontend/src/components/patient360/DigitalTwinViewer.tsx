import React, { useRef, useState, useCallback } from 'react';
import { Canvas } from '@react-three/fiber';
import { OrbitControls } from '@react-three/drei';
import type { OrbitControls as OrbitControlsType } from 'three-stdlib';
import type { TwinVitals, TwinLabs, TwinCompleteness } from '../../types/twin';
import { HumanoidModel } from './HumanoidModel';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';

import { LoadingState } from '../common/LoadingState';

export interface DigitalTwinViewerProps {
  patientName?: string;
  patientId?: string;
  vitals?: TwinVitals;
  labs?: TwinLabs;
  completeness?: TwinCompleteness;
  isLoading?: boolean;
}

/**
 * 3D Digital Twin Viewer for MediSphere Cognitive Twin (Phase 7D/7E).
 * Renders an interactive 3D humanoid anatomical representation alongside
 * authenticated real physiological twin telemetry.
 */
export const DigitalTwinViewer: React.FC<DigitalTwinViewerProps> = ({
  patientName,
  patientId,
  vitals,
  labs,
  completeness,
  isLoading = false,
}) => {
  const controlsRef = useRef<OrbitControlsType>(null);
  const [hasRenderError, setHasRenderError] = useState(false);

  // Reset Camera position to initial viewpoint
  const handleResetCamera = useCallback(() => {
    if (controlsRef.current) {
      controlsRef.current.reset();
    }
  }, []);

  const hr = vitals?.heartRate;
  const sbp = vitals?.systolicBP;
  const dbp = vitals?.diastolicBP;
  const spo2 = vitals?.oxygenSaturation;
  const temp = vitals?.temperature;
  const glu = labs?.glucose;

  const completenessPercent = completeness?.percentage ?? 100;
  const isTwinValidated = completenessPercent >= 95;

  return (
    <Card
      title="3D Digital Twin Viewer"
      subtitle={`Spatial physiological twin representation for ${patientName || patientId || 'selected patient'}`}
      action={
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          <Badge variant={isTwinValidated ? 'success' : 'warning'} size="sm">
            {completenessPercent}% Twin Validated
          </Badge>
          <button
            type="button"
            className="btn btn-secondary btn-xs"
            onClick={handleResetCamera}
            title="Reset 3D camera to default viewpoint"
            aria-label="Reset 3D camera to default viewpoint"
          >
            Reset Camera
          </button>
        </div>
      }
      className="twin-3d-card"
    >
      <div className="twin-3d-viewport-container">
        {/* Real-Time Telemetry Top Header Banner */}
        <div className="twin-viewport-header">
          <div className="twin-header-identity">
            <span className="twin-patient-label">Spatial Twin:</span>
            <strong className="twin-patient-name">{patientName || patientId}</strong>
            {patientId && <code className="code-subtle">({patientId})</code>}
          </div>
          <div className="twin-header-controls-hint" aria-hidden="true">
            <span>🖱️ Orbit: Left-drag • Zoom: Scroll • Pan: Right-drag</span>
          </div>
        </div>

        {/* 3D WebGL Canvas Viewport */}
        <div
          className="twin-canvas-wrapper"
          role="region"
          aria-label={`Interactive 3D anatomical humanoid model for patient ${patientName || patientId || 'selected'}. Current vitals: Heart rate ${hr !== undefined ? `${hr} bpm` : 'Not available'}, SpO2 ${spo2 !== undefined ? `${spo2}%` : 'Not available'}.`}
          tabIndex={0}
        >
          {/* Screen-reader descriptive summary */}
          <p className="twin-canvas-sr-only">
            Interactive Three.js 3D viewport displaying a neutral geometric humanoid anatomical model.
            Use mouse drag to orbit and rotate, mouse wheel to zoom in and out, and right-click drag to pan.
            Refer to the clinical data chips below for exact physiological measurements.
          </p>

          {/* Loading overlay during patient switching */}
          {isLoading && (
            <div className="twin-canvas-loading-overlay">
              <LoadingState message="Updating digital twin spatial model..." compact />
            </div>
          )}

          {hasRenderError ? (
            <div className="clinical-empty-state" style={{ padding: '3rem 1rem' }}>
              <span className="empty-state-icon" aria-hidden="true">⚠️</span>
              <h4 className="empty-state-title">3D WebGL Context Unavailable</h4>
              <p className="empty-state-desc">
                Your browser or device does not currently support WebGL rendering. Real physiological metrics remain accessible in the panels below.
              </p>
            </div>
          ) : (
            <Canvas
              camera={{ position: [0, 0.4, 3.1], fov: 45 }}
              onCreated={({ gl }) => {
                gl.setClearColor('#070c14', 1);
              }}
              onError={() => setHasRenderError(true)}
              style={{ width: '100%', height: '100%' }}
            >
              {/* Lighting Setup */}
              <ambientLight intensity={0.7} />
              <directionalLight position={[5, 8, 5]} intensity={1.2} />
              <directionalLight position={[-5, 3, -3]} intensity={0.6} color="#818cf8" />
              <pointLight position={[0, 2, 2]} intensity={0.8} color="#e0f2fe" />

              {/* Geometric Humanoid Model */}
              <HumanoidModel heartRate={hr} temperature={temp} />

              {/* Orbit and Zoom Controls */}
              <OrbitControls
                ref={controlsRef}
                enablePan={true}
                enableZoom={true}
                minDistance={1.4}
                maxDistance={5.5}
                minPolarAngle={Math.PI / 8}
                maxPolarAngle={Math.PI - Math.PI / 8}
                target={[0, 0.15, 0]}
              />
            </Canvas>
          )}

          {/* Spatial Grid Floor Overlay Effect */}
          <div className="viewport-grid-background" style={{ pointerEvents: 'none' }} aria-hidden="true" />
        </div>

        {/* Real Physiological Twin Layer Indicators */}
        <div className="twin-layer-indicators" style={{ marginTop: '0.75rem' }}>
          <div className="layer-chip">
            <span className="chip-icon">💓</span>
            <span className="chip-title">Cardiovascular</span>
            <span className="chip-data">
              {hr !== undefined ? `${hr} bpm` : 'Not available'}
            </span>
          </div>

          <div className="layer-chip">
            <span className="chip-icon">🩸</span>
            <span className="chip-title">Blood Pressure</span>
            <span className="chip-data">
              {sbp !== undefined && dbp !== undefined ? `${sbp}/${dbp} mmHg` : 'Not available'}
            </span>
          </div>

          <div className="layer-chip">
            <span className="chip-icon">🫁</span>
            <span className="chip-title">Respiratory</span>
            <span className="chip-data">
              {spo2 !== undefined ? `${spo2}% SpO2` : 'Not available'}
            </span>
          </div>

          <div className="layer-chip">
            <span className="chip-icon">🌡️</span>
            <span className="chip-title">Thermal</span>
            <span className="chip-data">
              {temp !== undefined ? `${temp} °C` : 'Not available'}
            </span>
          </div>

          <div className="layer-chip">
            <span className="chip-icon">🧪</span>
            <span className="chip-title">Biochemistry</span>
            <span className="chip-data">
              {glu !== undefined ? `${glu} mg/dL Glu` : 'Not available'}
            </span>
          </div>
        </div>

        {/* Compliance Notice: Visualization Boundary */}
        <div className="twin-visualization-notice">
          <span>* 3D visualization is spatial representation only; refer to clinical data panels for authoritative telemetry.</span>
          <span>Latest Observation: {vitals?.timestamp ? new Date(vitals.timestamp).toLocaleTimeString() : 'Not available'}</span>
        </div>
      </div>
    </Card>
  );
};
