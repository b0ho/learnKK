import { afterEach, describe, expect, it, vi } from 'vitest';
import { surveyApi } from './survey';
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

describe('surveyApi', () => {
  it('submitAnswers POSTs the answers with auth', async () => {
    setSession('me-tok', 'MENTEE');
    const fetchMock = vi.fn().mockResolvedValue(ok([{ questionId: 100, answerText: '답변' }]));
    vi.stubGlobal('fetch', fetchMock);

    const result = await surveyApi.submitAnswers(5, [{ questionId: 100, answerText: '답변' }]);

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/meetings/5/survey-answers');
    expect(init.method).toBe('POST');
    expect(JSON.parse(init.body)).toEqual({ answers: [{ questionId: 100, answerText: '답변' }] });
    expect(result[0].questionId).toBe(100);
  });

  it('getMyAnswers GETs the mine route', async () => {
    setSession('me-tok', 'MENTEE');
    const fetchMock = vi.fn().mockResolvedValue(ok([]));
    vi.stubGlobal('fetch', fetchMock);

    await surveyApi.getMyAnswers(5);

    const [url] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/meetings/5/survey-answers/mine');
  });

  it('getMenteeAnswers GETs the mentee-scoped route', async () => {
    setSession('m-tok', 'MENTOR');
    const fetchMock = vi.fn().mockResolvedValue(ok([]));
    vi.stubGlobal('fetch', fetchMock);

    await surveyApi.getMenteeAnswers(5, 2);

    const [url] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/meetings/5/mentees/2/survey-answers');
  });
});
