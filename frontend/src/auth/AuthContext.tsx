import React, { useState, useEffect, useCallback, type ReactNode } from 'react';
import type { User, LoginRequest } from '../types/auth';
import { authApi } from '../api/authApi';
import { TOKEN_STORAGE_KEY, USER_STORAGE_KEY } from '../api/axiosClient';
import { AuthContext } from './AuthContextDefinition';
export type { AuthContextType } from './AuthContextDefinition';

interface AuthProviderProps {
  children: ReactNode;
}

/**
 * AuthProvider component managing global authentication state.
 *
 * ARCHITECTURAL NOTES & SECURITY TRADE-OFFS:
 * 1. For this student/demo SPA, the JWT token is stored in localStorage.
 * 2. PASSWORDS AND PASSWORD HASHES ARE NEVER PERSISTED IN STORAGE.
 * 3. In a production healthcare system, tokens should be managed via
 *    HttpOnly SameSite cookies or memory-only storage with refresh tokens
 *    to mitigate XSS vulnerabilities.
 */
export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const savedUser = localStorage.getItem(USER_STORAGE_KEY);
    if (savedUser) {
      try {
        return JSON.parse(savedUser) as User;
      } catch {
        return null;
      }
    }
    return null;
  });

  const [token, setToken] = useState<string | null>(() => {
    return localStorage.getItem(TOKEN_STORAGE_KEY);
  });

  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => {
    return !!localStorage.getItem(TOKEN_STORAGE_KEY);
  });

  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
    setToken(null);
    setUser(null);
    setIsAuthenticated(false);
    setError(null);
  }, []);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  // Validate existing token with backend on initial load
  useEffect(() => {
    const initAuth = async () => {
      const storedToken = localStorage.getItem(TOKEN_STORAGE_KEY);
      if (storedToken) {
        try {
          const profile = await authApi.getMe();
          setUser(profile);
          setIsAuthenticated(true);
          localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(profile));
        } catch {
          // Token invalid or expired
          logout();
        }
      } else {
        logout();
      }
      setIsLoading(false);
    };

    initAuth();

    // Listen for session expiration events emitted by axiosClient interceptor
    const handleExpired = () => {
      logout();
      setError('Your session has expired. Please sign in again.');
    };

    window.addEventListener('medisphere_auth_expired', handleExpired);
    return () => {
      window.removeEventListener('medisphere_auth_expired', handleExpired);
    };
  }, [logout]);

  const login = async (credentials: LoginRequest) => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await authApi.login(credentials);
      localStorage.setItem(TOKEN_STORAGE_KEY, response.token);
      localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(response.user));
      setToken(response.token);
      setUser(response.user);
      setIsAuthenticated(true);
    } catch (err: unknown) {
      logout();
      let errorMsg = 'Failed to sign in. Please check your credentials.';
      if (typeof err === 'object' && err !== null && 'response' in err) {
        const axiosErr = err as { response?: { data?: { message?: string } } };
        if (axiosErr.response?.data?.message) {
          errorMsg = axiosErr.response.data.message;
        }
      }
      setError(errorMsg);
      throw new Error(errorMsg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated,
        isLoading,
        error,
        login,
        logout,
        clearError,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
