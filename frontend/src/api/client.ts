import { ApiError } from './errors';
import { getToken, notifyUnauthorized } from './session';

export const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080';

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  body?: unknown;
  /** Set false for public endpoints so we never send a stale header. */
  auth?: boolean;
  query?: Record<string, string | number | boolean | undefined>;
  signal?: AbortSignal;
}

function buildUrl(path: string, query?: RequestOptions['query']): string {
  const url = new URL(path, API_BASE.endsWith('/') ? API_BASE : `${API_BASE}/`);
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined) {
        url.searchParams.set(key, String(value));
      }
    }
  }
  return url.toString();
}

async function parseError(response: Response): Promise<ApiError> {
  let payload: Record<string, unknown> = {};
  try {
    const data = await response.json();
    if (data && typeof data === 'object') {
      payload = data as Record<string, unknown>;
    }
  } catch {
    // Non-JSON error body (e.g. gateway error) — fall back to status only.
  }
  return new ApiError(response.status, {
    code: typeof payload.code === 'string' ? payload.code : undefined,
    message: typeof payload.message === 'string' ? payload.message : undefined,
    details:
      payload.details && typeof payload.details === 'object'
        ? (payload.details as Record<string, unknown>)
        : null,
  });
}

/**
 * Single fetch wrapper for the whole app:
 *  - attaches `Authorization: Bearer <token>` automatically (unless auth:false),
 *  - parses JSON responses (204 -> undefined),
 *  - converts ErrorPayload bodies into typed {@link ApiError}s,
 *  - detects 401 to discard the token and trigger a login redirect.
 */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, auth = true, query, signal } = options;

  const headers: Record<string, string> = { Accept: 'application/json' };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (auth) {
    const token = getToken();
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
  }

  const response = await fetch(buildUrl(path, query), {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
  });

  if (response.status === 401) {
    const error = await parseError(response);
    notifyUnauthorized();
    throw error;
  }

  if (!response.ok) {
    throw await parseError(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  if (!text) {
    return undefined as T;
  }
  return JSON.parse(text) as T;
}
