import { afterEach, describe, expect, it, vi } from 'vitest';
import { adminApi } from './admin';
import { meetingsApi } from './meetings';
import { setSession, clearSession } from './session';

afterEach(() => {
  vi.unstubAllGlobals();
  clearSession();
});

function okMeeting() {
  return new Response(JSON.stringify({ id: 5, status: 'READY_TO_START' }), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('adminApi Bolt 2 transitions', () => {
  it('confirmRecruitment posts proceed and reason to the confirm-recruitment route', async () => {
    setSession('a-tok', 'ADMIN');
    const fetchMock = vi.fn().mockResolvedValue(okMeeting());
    vi.stubGlobal('fetch', fetchMock);

    await adminApi.confirmRecruitment(5, false, '정원 미달');

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/admin/meetings/5/confirm-recruitment');
    expect(init.method).toBe('POST');
    expect(JSON.parse(init.body)).toEqual({ proceed: false, reason: '정원 미달' });
    expect(init.headers.Authorization).toBe('Bearer a-tok');
  });

  it('approveStart posts to the approve-start route', async () => {
    setSession('a-tok', 'ADMIN');
    const fetchMock = vi.fn().mockResolvedValue(okMeeting());
    vi.stubGlobal('fetch', fetchMock);

    await adminApi.approveStart(5);

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/admin/meetings/5/approve-start');
    expect(init.method).toBe('POST');
  });

  it('complete posts to the complete route', async () => {
    setSession('a-tok', 'ADMIN');
    const fetchMock = vi.fn().mockResolvedValue(okMeeting());
    vi.stubGlobal('fetch', fetchMock);

    await adminApi.complete(5);

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/admin/meetings/5/complete');
    expect(init.method).toBe('POST');
  });
});

describe('adminApi.listMonitoring (US-9.2)', () => {
  function okPage() {
    return new Response(
      JSON.stringify({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }),
      { status: 200, headers: { 'Content-Type': 'application/json' } },
    );
  }

  it('GETs the monitoring route without a status by default', async () => {
    setSession('a-tok', 'ADMIN');
    const fetchMock = vi.fn().mockResolvedValue(okPage());
    vi.stubGlobal('fetch', fetchMock);

    await adminApi.listMonitoring({ size: 100 });

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/admin/monitoring/meetings');
    expect(String(url)).toContain('size=100');
    expect(String(url)).not.toContain('status=');
    expect(init.headers.Authorization).toBe('Bearer a-tok');
  });

  it('passes the status filter as a query param', async () => {
    setSession('a-tok', 'ADMIN');
    const fetchMock = vi.fn().mockResolvedValue(okPage());
    vi.stubGlobal('fetch', fetchMock);

    await adminApi.listMonitoring({ status: 'IN_PROGRESS' });

    expect(String(fetchMock.mock.calls[0][0])).toContain('status=IN_PROGRESS');
  });
});

describe('meetingsApi.listMine', () => {
  it('GETs the mine route with auth and pagination params', async () => {
    setSession('m-tok', 'MENTOR');
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({ content: [], page: 0, size: 50, totalElements: 0, totalPages: 0 }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    );
    vi.stubGlobal('fetch', fetchMock);

    await meetingsApi.listMine({ size: 50 });

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/meetings/mine');
    expect(String(url)).toContain('size=50');
    expect(init.headers.Authorization).toBe('Bearer m-tok');
  });
});
