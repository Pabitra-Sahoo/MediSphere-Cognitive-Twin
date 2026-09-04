import { useContext } from 'react';
import { AuthContext, type AuthContextType } from './AuthContextDefinition';

/**
 * Hook providing access to the current authentication state and actions.
 */
export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
