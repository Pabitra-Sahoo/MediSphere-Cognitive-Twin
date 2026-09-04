import axios from 'axios';

/**
 * TOKEN STORAGE ARCHITECTURE NOTE:
 * For this student/demo SPA implementation, the JWT Bearer token is persisted
 * in localStorage under 'medisphere_token'.
 *
 * CRITICAL SECURITY INVARIANT: Passwords and password hashes are NEVER stored
 * in browser storage.
 *
 * NOTE: Storing tokens in localStorage is an intentional development tradeoff
 * for demo convenience. In a production healthcare deployment, tokens should
 * be managed via secure HTTP-Only, SameSite cookies or in-memory state with
 * refresh token rotation to mitigate XSS exposure.
 */
export const TOKEN_STORAGE_KEY = 'medisphere_token';
export const USER_STORAGE_KEY = 'medisphere_user';

export const axiosClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

// Request Interceptor: Injects Authorization Bearer JWT if present
axiosClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Catches 401 Unauthorized responses
axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Clear expired or invalid session state
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem(USER_STORAGE_KEY);
      // Notify application of session expiration
      window.dispatchEvent(new Event('medisphere_auth_expired'));
    }
    return Promise.reject(error);
  }
);
