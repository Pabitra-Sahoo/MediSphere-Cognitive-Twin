import { axiosClient } from './axiosClient';
import type { LoginRequest, LoginResponse, RegisterRequest, User } from '../types/auth';

/**
 * Authentication API services connecting to backend `/api/auth` endpoints.
 */
export const authApi = {
  /**
   * Authenticates credentials and returns a JWT token with role and SMART scopes.
   */
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await axiosClient.post<LoginResponse>('/auth/login', credentials);
    return response.data;
  },

  /**
   * Fetches profile and scopes of currently authenticated user using JWT.
   */
  async getMe(): Promise<User> {
    const response = await axiosClient.get<User>('/auth/me');
    return response.data;
  },

  /**
   * Registers a new user account (Admin only).
   */
  async register(data: RegisterRequest): Promise<User> {
    const response = await axiosClient.post<User>('/auth/register', data);
    return response.data;
  },

  /**
   * Verifies backend system status and health check.
   */
  async getStatus(): Promise<{ status: string; service: string; milestone: string; timestamp: string }> {
    const response = await axiosClient.get('/status');
    return response.data;
  },
};
