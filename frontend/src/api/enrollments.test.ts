import { afterEach, describe, expect, it, vi } from 'vitest';
import { enrollmentsApi } from './enrollments';
import { setSession, clearSession } from './session';

afterEach(() => {
  vi.unstubAllGlobals();
  clearSession();
});

function ok(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('enrollmentsApi', () => {
  it('apply POSTs to the meeting enrollments route with auth', async () => {
    setSession('me-tok', 'MENTEE');
    const fetchMock = vi
      .fn()
      .mockResolvedValue(
        ok({ id: 1, meetingId: 5, menteeId: 2, status: 'APPLIED', appliedAt: 'x' }, 201),
      );
    vi.stubGlobal('fetch', fetchMock);

    const result = await enrollmentsApi.apply(5);

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/meetings/5/enrollments');
    expect(init.method).toBe('POST');
    expect(init.headers.Authorization).toBe('Bearer me-tok');
    expect(result.status).toBe('APPLIED');
  });

  it('cancel DELETEs the mine route', async () => {
    setSession('me-tok', 'MENTEE');
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal('fetch', fetchMock);

    await enrollmentsApi.cancel(5);

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/meetings/5/enrollments/mine');
    expect(init.method).toBe('DELETE');
  });

  it('listMine GETs the enrollments mine route', async () => {
    setSession('me-tok', 'MENTEE');
    const fetchMock = vi.fn().mockResolvedValue(ok([]));
    vi.stubGlobal('fetch', fetchMock);

    await enrollmentsApi.listMine();

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/enrollments/mine');
    expect(init.method ?? 'GET').toBe('GET');
  });

  it('listApplicants GETs the applicants route', async () => {
    setSession('m-tok', 'MENTOR');
    const fetchMock = vi.fn().mockResolvedValue(ok([]));
    vi.stubGlobal('fetch', fetchMock);

    await enrollmentsApi.listApplicants(5);

    const [url] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/meetings/5/applicants');
  });
});
