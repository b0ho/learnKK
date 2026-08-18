import type { Role } from './types';

/**
 * Session persistence + cross-module coordination for the API client.
 *
 * The token/role live in sessionStorage (cleared when the tab closes) per the
 * U2 assumption. The client reads the token here to attach the auth header, and
 * calls the registered "unauthorized" handler when the server rejects a token
 * (HTTP 401) so the app can drop state and route to login.
 */
const TOKEN_KEY = 'learnkk.token';
const ROLE_KEY = 'learnkk.role';

export function getToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function getStoredRole(): Role | null {
  return sessionStorage.getItem(ROLE_KEY) as Role | null;
}

export function setSession(token: string, role: Role): void {
  sessionStorage.setItem(TOKEN_KEY, token);
  sessionStorage.setItem(ROLE_KEY, role);
}

export function clearSession(): void {
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(ROLE_KEY);
}

type UnauthorizedHandler = () => void;
let unauthorizedHandler: UnauthorizedHandler | null = null;

/** Register the app-level reaction to a 401 (token discard + redirect). */
export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  unauthorizedHandler = handler;
}

export function notifyUnauthorized(): void {
  clearSession();
  unauthorizedHandler?.();
}
