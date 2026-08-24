import { afterEach, describe, expect, it, vi } from 'vitest';
import { sessionsApi } from './sessions';
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

describe('sessionsApi', () => {
  it('listSessions GETs the meeting sessions route', async () => {
    setSession('t', 'MENTEE');
    const fetchMock = vi.fn().mockResolvedValue(ok([]));
    vi.stubGlobal('fetch', fetchMock);

    await sessionsApi.listSessions(10);

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/meetings/10/sessions');
    expect(init.method ?? 'GET').toBe('GET');
  });

  it('addSession POSTs the create body with auth', async () => {
    setSession('m-tok', 'MENTOR');
    const fetchMock = vi
      .fn()
      .mockResolvedValue(
        ok({ id: 5, meetingId: 10, week: 1, scheduledAt: 'x', checkInWindowMinutes: 120 }, 201),
      );
    vi.stubGlobal('fetch', fetchMock);

    const res = await sessionsApi.addSession(10, {
      week: 1,
      scheduledAt: '2026-01-01T10:00:00Z',
      checkInWindowMinutes: 120,
    });

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/meetings/10/sessions');
    expect(init.method).toBe('POST');
    expect(init.headers.Authorization).toBe('Bearer m-tok');
    expect(JSON.parse(init.body).week).toBe(1);
    expect(res.id).toBe(5);
  });

  it('updateSession PUTs the reschedule body', async () => {
    setSession('m-tok', 'MENTOR');
    const fetchMock = vi
      .fn()
      .mockResolvedValue(
        ok({ id: 5, meetingId: 10, week: 1, scheduledAt: 'y', checkInWindowMinutes: 120 }),
      );
    vi.stubGlobal('fetch', fetchMock);

    await sessionsApi.updateSession(5, { scheduledAt: '2026-01-02T10:00:00Z' });

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/sessions/5');
    expect(init.method).toBe('PUT');
  });

  it('checkIn POSTs the attendance route', async () => {
    setSession('me-tok', 'MENTEE');
    const fetchMock = vi
      .fn()
      .mockResolvedValue(ok({ sessionId: 5, menteeId: 2, checkedInAt: 'x' }, 201));
    vi.stubGlobal('fetch', fetchMock);

    await sessionsApi.checkIn(5);

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/sessions/5/attendance');
    expect(init.method).toBe('POST');
  });

  it('getMyAttendance GETs the my-attendance route', async () => {
    setSession('me-tok', 'MENTEE');
    const fetchMock = vi
      .fn()
      .mockResolvedValue(ok({ attended: 3, totalScheduled: 4, rate: 0.75 }));
    vi.stubGlobal('fetch', fetchMock);

    const res = await sessionsApi.getMyAttendance(10);

    const [url] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/meetings/10/my-attendance');
    expect(res.rate).toBe(0.75);
  });

  it('computeCompletions POSTs the compute route', async () => {
    setSession('m-tok', 'MENTOR');
    const fetchMock = vi.fn().mockResolvedValue(ok([]));
    vi.stubGlobal('fetch', fetchMock);

    await sessionsApi.computeCompletions(10);

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/meetings/10/completions/compute');
    expect(init.method).toBe('POST');
  });

  it('listCompletions GETs the completions route', async () => {
    setSession('m-tok', 'MENTOR');
    const fetchMock = vi.fn().mockResolvedValue(ok([]));
    vi.stubGlobal('fetch', fetchMock);

    await sessionsApi.listCompletions(10);

    const [url] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/meetings/10/completions');
  });

  it('approveCompletion POSTs the admin approve route', async () => {
    setSession('a-tok', 'ADMIN');
    const fetchMock = vi
      .fn()
      .mockResolvedValue(
        ok({ meetingId: 10, menteeId: 2, status: 'COMPLETED', attendedCount: 4, totalScheduled: 5 }),
      );
    vi.stubGlobal('fetch', fetchMock);

    const res = await sessionsApi.approveCompletion(10, 2);

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/admin/meetings/10/completions/2/approve');
    expect(init.method).toBe('POST');
    expect(res.status).toBe('COMPLETED');
  });
});
