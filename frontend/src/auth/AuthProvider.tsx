import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import {
  authApi,
  clearSession,
  getStoredRole,
  getToken,
  setSession,
  setUnauthorizedHandler,
  type Role,
} from '@/api';
import { AuthContext, type AuthContextValue } from './auth-context';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => getToken());
  const [role, setRole] = useState<Role | null>(() => getStoredRole());

  const login = useCallback((nextToken: string, nextRole: Role) => {
    setSession(nextToken, nextRole);
    setToken(nextToken);
    setRole(nextRole);
  }, []);

  const clearLocal = useCallback(() => {
    clearSession();
    setToken(null);
    setRole(null);
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // Even if the server call fails, drop the local session.
    } finally {
      clearLocal();
    }
  }, [clearLocal]);

  // When the client detects a 401, clear local state so guards redirect to login.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      setToken(null);
      setRole(null);
    });
    return () => setUnauthorizedHandler(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ token, role, isAuthenticated: Boolean(token), login, logout }),
    [token, role, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
