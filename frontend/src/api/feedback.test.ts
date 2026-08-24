import { afterEach, describe, expect, it, vi } from 'vitest';
import { feedbackApi } from './feedback';
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

describe('feedbackApi', () => {
  it('submit POSTs the content with auth', async () => {
    setSession('me-tok', 'MENTEE');
    const fetchMock = vi
      .fn()
      .mockResolvedValue(ok({ id: 1, menteeId: 2, content: '좋음', createdAt: 'x' }, 201));
    vi.stubGlobal('fetch', fetchMock);

    const result = await feedbackApi.submit(5, '좋음');

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/meetings/5/feedback');
    expect(init.method).toBe('POST');
    expect(JSON.parse(init.body)).toEqual({ content: '좋음' });
    expect(result.id).toBe(1);
  });

  it('list GETs the feedback route', async () => {
    setSession('m-tok', 'MENTOR');
    const fetchMock = vi.fn().mockResolvedValue(ok([]));
    vi.stubGlobal('fetch', fetchMock);

    await feedbackApi.list(5);

    const [url] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/meetings/5/feedback');
  });
});
