import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { request } from './client';
import { ApiError } from './errors';
import { setSession, getToken, setUnauthorizedHandler } from './session';

function mockFetch(response: Response) {
  const fetchMock = vi.fn().mockResolvedValue(response);
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

describe('API client', () => {
  beforeEach(() => {
    sessionStorage.clear();
    setUnauthorizedHandler(null);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    setUnauthorizedHandler(null);
  });

  it('attaches the bearer token when authenticated', async () => {
    setSession('tok-123', 'MENTEE');
    const fetchMock = mockFetch(
      new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );

    await request('/api/users/me/profile');

    const [, init] = fetchMock.mock.calls[0];
    expect(init.headers.Authorization).toBe('Bearer tok-123');
  });

  it('omits the auth header for public requests', async () => {
    setSession('tok-123', 'MENTEE');
    const fetchMock = mockFetch(new Response('[]', { status: 200 }));

    await request('/api/meetings', { auth: false });

    const [, init] = fetchMock.mock.calls[0];
    expect(init.headers.Authorization).toBeUndefined();
  });

  it('serializes JSON bodies and sets content-type', async () => {
    const fetchMock = mockFetch(new Response('{}', { status: 200 }));

    await request('/api/auth/login', { method: 'POST', body: { nickname: 'a' }, auth: false });

    const [, init] = fetchMock.mock.calls[0];
    expect(init.headers['Content-Type']).toBe('application/json');
    expect(init.body).toBe(JSON.stringify({ nickname: 'a' }));
  });

  it('returns undefined for 204 responses', async () => {
    mockFetch(new Response(null, { status: 204 }));
    await expect(request('/api/auth/logout', { method: 'POST' })).resolves.toBeUndefined();
  });

  it('throws a typed ApiError from an ErrorPayload body', async () => {
    mockFetch(
      new Response(JSON.stringify({ code: 'DUPLICATE_NICKNAME', message: '중복' }), {
        status: 409,
        headers: { 'Content-Type': 'application/json' },
      }),
    );

    await expect(request('/api/auth/signup', { method: 'POST', body: {}, auth: false }))
      .rejects.toMatchObject({ code: 'DUPLICATE_NICKNAME', status: 409 });
  });

  it('discards the token and notifies on 401', async () => {
    setSession('tok-123', 'MENTEE');
    const handler = vi.fn();
    setUnauthorizedHandler(handler);
    mockFetch(
      new Response(JSON.stringify({ code: 'AUTH_SESSION_EXPIRED', message: '만료' }), {
        status: 401,
        headers: { 'Content-Type': 'application/json' },
      }),
    );

    await expect(request('/api/users/me/profile')).rejects.toBeInstanceOf(ApiError);
    expect(getToken()).toBeNull();
    expect(handler).toHaveBeenCalledOnce();
  });

  it('handles a non-JSON error body gracefully', async () => {
    mockFetch(new Response('gateway down', { status: 502 }));
    await expect(request('/api/meetings', { auth: false })).rejects.toMatchObject({
      status: 502,
      code: 'UNKNOWN',
    });
  });
});
