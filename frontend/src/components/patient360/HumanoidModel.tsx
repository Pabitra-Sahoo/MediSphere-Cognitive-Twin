import React, { useRef } from 'react';
import type * as THREE from 'three';
import { useFrame } from '@react-three/fiber';

export interface HumanoidModelProps {
  heartRate?: number;
  temperature?: number;
}

/**
 * Geometric Humanoid Anatomical Representation for MediSphere Digital Twin.
 * Neutral visualization constructed with Three.js primitives.
 * Complies with strict non-diagnostic visualization rules.
 */
export const HumanoidModel: React.FC<HumanoidModelProps> = ({ heartRate }) => {
  const groupRef = useRef<THREE.Group>(null);
  const coreRef = useRef<THREE.Mesh>(null);

  // Subtle natural idle breath/pulse animation (lightweight, non-diagnostic)
  useFrame((state) => {
    if (groupRef.current) {
      // Gentle breathing motion on Y axis
      groupRef.current.position.y = Math.sin(state.clock.elapsedTime * 1.2) * 0.02;
    }
    if (coreRef.current) {
      // Gentle core illumination pulse linked to real heart rate frequency if available
      const bpm = heartRate && heartRate > 30 && heartRate < 220 ? heartRate : 72;
      const freq = (bpm / 60) * Math.PI;
      const scale = 1 + Math.sin(state.clock.elapsedTime * freq) * 0.15;
      coreRef.current.scale.set(scale, scale, scale);
    }
  });

  const bodyColor = '#38bdf8'; // Theme accent primary (cyan)
  const jointColor = '#818cf8'; // Theme accent secondary (indigo)
  const baseColor = '#162032';

  return (
    <group ref={groupRef} position={[0, -0.6, 0]}>
      {/* --- Head & Neck --- */}
      {/* Head */}
      <mesh position={[0, 1.85, 0]}>
        <sphereGeometry args={[0.18, 32, 32]} />
        <meshStandardMaterial
          color={bodyColor}
          roughness={0.25}
          metalness={0.2}
          transparent
          opacity={0.88}
        />
      </mesh>
      {/* Face visor highlight */}
      <mesh position={[0, 1.86, 0.12]}>
        <boxGeometry args={[0.16, 0.07, 0.08]} />
        <meshStandardMaterial color="#e0f2fe" roughness={0.1} metalness={0.8} />
      </mesh>
      {/* Neck */}
      <mesh position={[0, 1.62, 0]}>
        <cylinderGeometry args={[0.07, 0.08, 0.12, 16]} />
        <meshStandardMaterial color={jointColor} roughness={0.4} metalness={0.3} />
      </mesh>

      {/* --- Torso --- */}
      {/* Upper Torso / Thorax */}
      <mesh position={[0, 1.38, 0]}>
        <boxGeometry args={[0.42, 0.34, 0.22]} />
        <meshStandardMaterial
          color={bodyColor}
          roughness={0.25}
          metalness={0.2}
          transparent
          opacity={0.85}
        />
      </mesh>

      {/* Core Telemetry Node (Subtle physiological beacon) */}
      <mesh ref={coreRef} position={[0.04, 1.38, 0.12]}>
        <sphereGeometry args={[0.035, 16, 16]} />
        <meshStandardMaterial
          color="#f43f5e"
          emissive="#f43f5e"
          emissiveIntensity={0.6}
          roughness={0.2}
        />
      </mesh>

      {/* Mid Torso / Abdomen */}
      <mesh position={[0, 1.12, 0]}>
        <boxGeometry args={[0.36, 0.22, 0.19]} />
        <meshStandardMaterial
          color={bodyColor}
          roughness={0.25}
          metalness={0.2}
          transparent
          opacity={0.85}
        />
      </mesh>

      {/* Pelvis / Hips */}
      <mesh position={[0, 0.92, 0]}>
        <boxGeometry args={[0.38, 0.18, 0.21]} />
        <meshStandardMaterial color={jointColor} roughness={0.3} metalness={0.3} />
      </mesh>

      {/* --- Left Arm --- */}
      {/* Left Shoulder */}
      <mesh position={[-0.26, 1.48, 0]}>
        <sphereGeometry args={[0.075, 16, 16]} />
        <meshStandardMaterial color={jointColor} roughness={0.3} />
      </mesh>
      {/* Left Upper Arm */}
      <mesh position={[-0.32, 1.25, 0]}>
        <cylinderGeometry args={[0.06, 0.055, 0.36, 16]} />
        <meshStandardMaterial color={bodyColor} roughness={0.25} transparent opacity={0.85} />
      </mesh>
      {/* Left Elbow */}
      <mesh position={[-0.32, 1.03, 0]}>
        <sphereGeometry args={[0.055, 16, 16]} />
        <meshStandardMaterial color={jointColor} roughness={0.3} />
      </mesh>
      {/* Left Forearm */}
      <mesh position={[-0.32, 0.83, 0]}>
        <cylinderGeometry args={[0.05, 0.045, 0.32, 16]} />
        <meshStandardMaterial color={bodyColor} roughness={0.25} transparent opacity={0.85} />
      </mesh>
      {/* Left Hand */}
      <mesh position={[-0.32, 0.62, 0]}>
        <boxGeometry args={[0.065, 0.1, 0.04]} />
        <meshStandardMaterial color={jointColor} roughness={0.3} />
      </mesh>

      {/* --- Right Arm --- */}
      {/* Right Shoulder */}
      <mesh position={[0.26, 1.48, 0]}>
        <sphereGeometry args={[0.075, 16, 16]} />
        <meshStandardMaterial color={jointColor} roughness={0.3} />
      </mesh>
      {/* Right Upper Arm */}
      <mesh position={[0.32, 1.25, 0]}>
        <cylinderGeometry args={[0.06, 0.055, 0.36, 16]} />
        <meshStandardMaterial color={bodyColor} roughness={0.25} transparent opacity={0.85} />
      </mesh>
      {/* Right Elbow */}
      <mesh position={[0.32, 1.03, 0]}>
        <sphereGeometry args={[0.055, 16, 16]} />
        <meshStandardMaterial color={jointColor} roughness={0.3} />
      </mesh>
      {/* Right Forearm */}
      <mesh position={[0.32, 0.83, 0]}>
        <cylinderGeometry args={[0.05, 0.045, 0.32, 16]} />
        <meshStandardMaterial color={bodyColor} roughness={0.25} transparent opacity={0.85} />
      </mesh>
      {/* Right Hand */}
      <mesh position={[0.32, 0.62, 0]}>
        <boxGeometry args={[0.065, 0.1, 0.04]} />
        <meshStandardMaterial color={jointColor} roughness={0.3} />
      </mesh>

      {/* --- Left Leg --- */}
      {/* Left Hip Joint */}
      <mesh position={[-0.13, 0.82, 0]}>
        <sphereGeometry args={[0.075, 16, 16]} />
        <meshStandardMaterial color={jointColor} roughness={0.3} />
      </mesh>
      {/* Left Thigh */}
      <mesh position={[-0.13, 0.56, 0]}>
        <cylinderGeometry args={[0.075, 0.065, 0.42, 16]} />
        <meshStandardMaterial color={bodyColor} roughness={0.25} transparent opacity={0.85} />
      </mesh>
      {/* Left Knee */}
      <mesh position={[-0.13, 0.31, 0]}>
        <sphereGeometry args={[0.065, 16, 16]} />
        <meshStandardMaterial color={jointColor} roughness={0.3} />
      </mesh>
      {/* Left Shin / Calf */}
      <mesh position={[-0.13, 0.07, 0]}>
        <cylinderGeometry args={[0.06, 0.05, 0.40, 16]} />
        <meshStandardMaterial color={bodyColor} roughness={0.25} transparent opacity={0.85} />
      </mesh>
      {/* Left Foot */}
      <mesh position={[-0.13, -0.16, 0.04]}>
        <boxGeometry args={[0.08, 0.06, 0.18]} />
        <meshStandardMaterial color={jointColor} roughness={0.3} />
      </mesh>

      {/* --- Right Leg --- */}
      {/* Right Hip Joint */}
      <mesh position={[0.13, 0.82, 0]}>
        <sphereGeometry args={[0.075, 16, 16]} />
        <meshStandardMaterial color={jointColor} roughness={0.3} />
      </mesh>
      {/* Right Thigh */}
      <mesh position={[0.13, 0.56, 0]}>
        <cylinderGeometry args={[0.075, 0.065, 0.42, 16]} />
        <meshStandardMaterial color={bodyColor} roughness={0.25} transparent opacity={0.85} />
      </mesh>
      {/* Right Knee */}
      <mesh position={[0.13, 0.31, 0]}>
        <sphereGeometry args={[0.065, 16, 16]} />
        <meshStandardMaterial color={jointColor} roughness={0.3} />
      </mesh>
      {/* Right Shin / Calf */}
      <mesh position={[0.13, 0.07, 0]}>
        <cylinderGeometry args={[0.06, 0.05, 0.40, 16]} />
        <meshStandardMaterial color={bodyColor} roughness={0.25} transparent opacity={0.85} />
      </mesh>
      {/* Right Foot */}
      <mesh position={[0.13, -0.16, 0.04]}>
        <boxGeometry args={[0.08, 0.06, 0.18]} />
        <meshStandardMaterial color={jointColor} roughness={0.3} />
      </mesh>

      {/* --- Spatial Pedestal Base / Plinth --- */}
      <mesh position={[0, -0.21, 0]}>
        <cylinderGeometry args={[0.55, 0.58, 0.04, 32]} />
        <meshStandardMaterial color={baseColor} roughness={0.6} metalness={0.5} />
      </mesh>
      {/* Holographic Glowing Ring on Platform */}
      <mesh position={[0, -0.185, 0]} rotation={[-Math.PI / 2, 0, 0]}>
        <ringGeometry args={[0.50, 0.53, 32]} />
        <meshBasicMaterial color={bodyColor} transparent opacity={0.8} />
      </mesh>
    </group>
  );
};
