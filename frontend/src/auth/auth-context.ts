import { createContext } from 'react';
import type { Role } from '@/api';

export interface AuthContextValue {
  token: string | null;
  role: Role | null;
  isAuthenticated: boolean;
  /** Persist a freshly issued session (token + role). */
  login: (token: string, role: Role) => void;
  /** Revoke the session server-side (best effort) and clear local state. */
  logout: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
