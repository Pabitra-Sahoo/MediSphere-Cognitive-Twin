import React from 'react';
import type { TwinVitals, TwinLabs } from '../../types/twin';
import { Card } from '../common/Card';
import { Badge } from '../common/Badge';
import { Activity, Wind, FlaskConical, Thermometer } from 'lucide-react';

export interface DigitalTwinPlaceholderProps {
  patientName?: string;
  vitals?: TwinVitals;
  labs?: TwinLabs;
}

export const DigitalTwinPlaceholder: React.FC<DigitalTwinPlaceholderProps> = ({
  patientName,
  vitals,
  labs,
}) => {
  return (
    <Card
      title="3D Digital Twin"
      subtitle={`Cognitive twin spatial representation for ${patientName || 'selected patient'}`}
      action={<Badge variant="info">3D Viewer in Phase 7D</Badge>}
      className="twin-3d-card"
    >
      <div className="twin-3d-viewport-placeholder">
        <div className="viewport-grid-background" />

        {/* Anatomical Hologram Wireframe Representation */}
        <div className="hologram-avatar-container">
          <div className="avatar-silhouette-ring">
            <svg
              className="avatar-silhouette-svg"
              viewBox="0 0 200 280"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              {/* Head */}
              <circle cx="100" cy="40" r="24" stroke="var(--color-accent-primary)" strokeWidth="2" strokeDasharray="3 3" />
              {/* Neck */}
              <line x1="100" y1="64" x2="100" y2="76" stroke="var(--color-accent-primary)" strokeWidth="2" />
              {/* Torso */}
              <path
                d="M70 80 L130 80 L120 170 L80 170 Z"
                stroke="var(--color-accent-primary)"
                strokeWidth="2"
                strokeDasharray="4 4"
                fill="rgba(56, 189, 248, 0.03)"
              />
              {/* Arms */}
              <path d="M70 80 L45 140 L35 190" stroke="var(--color-accent-primary)" strokeWidth="1.5" strokeOpacity="0.7" />
              <path d="M130 80 L155 140 L165 190" stroke="var(--color-accent-primary)" strokeWidth="1.5" strokeOpacity="0.7" />
              {/* Legs */}
              <path d="M85 170 L80 230 L75 270" stroke="var(--color-accent-primary)" strokeWidth="2" strokeOpacity="0.8" />
              <path d="M115 170 L120 230 L125 270" stroke="var(--color-accent-primary)" strokeWidth="2" strokeOpacity="0.8" />
              {/* Heart hotspot */}
              <circle cx="108" cy="105" r="5" fill="var(--color-accent-danger)" className="pulse-dot" />
            </svg>
          </div>

          <div className="hologram-status-overlay">
            <div className="twin-phase-pill">
              <span className="pill-dot">●</span> 3D Model WebGL Canvas Reserved (Phase 7D)
            </div>
          </div>
        </div>

        {/* Real Physiological Twin Layer Indicators */}
        <div className="twin-layer-indicators">
          <div className="layer-chip">
            <span className="chip-icon"><Activity size={13} strokeWidth={2} /></span>
            <span className="chip-title">Cardiovascular</span>
            <span className="chip-data">
              {vitals?.heartRate ? `${vitals.heartRate} bpm` : 'No reading'}
            </span>
          </div>

          <div className="layer-chip">
            <span className="chip-icon"><Wind size={13} strokeWidth={2} /></span>
            <span className="chip-title">Respiratory</span>
            <span className="chip-data">
              {vitals?.oxygenSaturation ? `${vitals.oxygenSaturation}% SpO2` : 'No reading'}
            </span>
          </div>

          <div className="layer-chip">
            <span className="chip-icon"><FlaskConical size={13} strokeWidth={2} /></span>
            <span className="chip-title">Metabolic / Renal</span>
            <span className="chip-data">
              {labs?.glucose ? `${labs.glucose} mg/dL Glu` : 'No reading'}
            </span>
          </div>

          <div className="layer-chip">
            <span className="chip-icon"><Thermometer size={13} strokeWidth={2} /></span>
            <span className="chip-title">Thermal</span>
            <span className="chip-data">
              {vitals?.temperature ? `${vitals.temperature} °C` : 'No reading'}
            </span>
          </div>
        </div>
      </div>
    </Card>
  );
};
