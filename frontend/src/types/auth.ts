/**
 * Authentication and authorization domain types.
 */

export type Role = 'ADMIN' | 'PROVIDER' | 'PATIENT';

export interface User {
  id: string;
  username: string;
  email: string;
  role: Role;
  linkedPatientId: string | null;
  linkedProviderId: string | null;
  scopes: string[];
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  role: Role;
  linkedPatientId?: string | null;
  linkedProviderId?: string | null;
}

export interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}
