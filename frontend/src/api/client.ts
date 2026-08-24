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

/** A downloaded file: its binary payload plus the server-suggested filename. */
export interface DownloadedFile {
  blob: Blob;
  fileName: string;
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

  // FormData bodies (file uploads) are sent as-is so the browser sets the
  // multipart boundary; JSON bodies are serialized with an explicit content type.
  const isFormData = typeof FormData !== 'undefined' && body instanceof FormData;

  const headers: Record<string, string> = { Accept: 'application/json' };
  if (body !== undefined && !isFormData) {
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
    body: body === undefined ? undefined : isFormData ? (body as FormData) : JSON.stringify(body),
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

/** Extract the filename from a Content-Disposition header (RFC 5987 filename* or filename). */
function parseFileName(disposition: string | null, fallback: string): string {
  if (!disposition) {
    return fallback;
  }
  const utf8 = /filename\*=UTF-8''([^;]+)/i.exec(disposition);
  if (utf8?.[1]) {
    try {
      return decodeURIComponent(utf8[1]);
    } catch {
      // fall through to the plain filename
    }
  }
  const plain = /filename="?([^";]+)"?/i.exec(disposition);
  return plain?.[1] ?? fallback;
}

/**
 * Download a binary resource (e.g. a post attachment) as a {@link DownloadedFile}. Shares the
 * client's auth-header attachment and 401 handling, but returns the raw blob and the server's
 * suggested filename instead of parsing JSON.
 */
export async function downloadFile(
  path: string,
  fallbackName = 'download',
): Promise<DownloadedFile> {
  const headers: Record<string, string> = {};
  const token = getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(buildUrl(path), { method: 'GET', headers });

  if (response.status === 401) {
    const error = await parseError(response);
    notifyUnauthorized();
    throw error;
  }
  if (!response.ok) {
    throw await parseError(response);
  }

  const blob = await response.blob();
  const fileName = parseFileName(response.headers.get('Content-Disposition'), fallbackName);
  return { blob, fileName };
}
