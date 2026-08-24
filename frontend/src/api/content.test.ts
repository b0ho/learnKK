import { afterEach, describe, expect, it, vi } from 'vitest';
import { contentApi } from './content';
import { clearSession, setSession } from './session';

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

describe('contentApi', () => {
  it('createPost POSTs the meeting posts route with auth and JSON body', async () => {
    setSession('m-tok', 'MENTOR');
    const fetchMock = vi
      .fn()
      .mockResolvedValue(
        ok({ id: 1, meetingId: 5, authorId: 1, week: 2, body: 'b', attachments: [] }, 201),
      );
    vi.stubGlobal('fetch', fetchMock);

    const result = await contentApi.createPost(5, { week: 2, body: 'b' });

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/meetings/5/posts');
    expect(init.method).toBe('POST');
    expect(init.headers.Authorization).toBe('Bearer m-tok');
    expect(init.headers['Content-Type']).toBe('application/json');
    expect(result.week).toBe(2);
  });

  it('listPosts GETs the meeting posts route', async () => {
    setSession('me-tok', 'MENTEE');
    const fetchMock = vi.fn().mockResolvedValue(ok([]));
    vi.stubGlobal('fetch', fetchMock);

    await contentApi.listPosts(5);

    const [url] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/meetings/5/posts');
  });

  it('uploadAttachment sends FormData without a JSON content-type', async () => {
    setSession('m-tok', 'MENTOR');
    const fetchMock = vi.fn().mockResolvedValue(
      ok(
        {
          id: 7,
          postId: 3,
          fileName: 'a.pdf',
          contentType: 'application/pdf',
          sizeBytes: 3,
        },
        201,
      ),
    );
    vi.stubGlobal('fetch', fetchMock);

    const file = new File([new Uint8Array([1, 2, 3])], 'a.pdf', { type: 'application/pdf' });
    await contentApi.uploadAttachment(3, file);

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/posts/3/attachments');
    expect(init.method).toBe('POST');
    expect(init.body).toBeInstanceOf(FormData);
    // The browser must set the multipart boundary itself — no explicit JSON content type.
    expect(init.headers['Content-Type']).toBeUndefined();
  });

  it('downloadAttachment returns the blob and filename from Content-Disposition', async () => {
    setSession('me-tok', 'MENTEE');
    const blob = new Blob([new Uint8Array([1, 2, 3])], { type: 'application/pdf' });
    const response = new Response(blob, {
      status: 200,
      headers: {
        'Content-Type': 'application/pdf',
        'Content-Disposition': 'attachment; filename="week1.pdf"',
      },
    });
    const fetchMock = vi.fn().mockResolvedValue(response);
    vi.stubGlobal('fetch', fetchMock);

    const result = await contentApi.downloadAttachment(7, 'fallback.pdf');

    const [url] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/attachments/7');
    expect(result.fileName).toBe('week1.pdf');
    expect(result.blob).toBeInstanceOf(Blob);
  });

  it('createNotice POSTs the notices route', async () => {
    setSession('m-tok', 'MENTOR');
    const fetchMock = vi
      .fn()
      .mockResolvedValue(ok({ id: 1, meetingId: 5, authorId: 1, body: 'n', createdAt: 'x' }, 201));
    vi.stubGlobal('fetch', fetchMock);

    await contentApi.createNotice(5, { body: 'n' });

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/meetings/5/notices');
    expect(init.method).toBe('POST');
  });

  it('listNotices GETs the notices route', async () => {
    setSession('me-tok', 'MENTEE');
    const fetchMock = vi.fn().mockResolvedValue(ok([]));
    vi.stubGlobal('fetch', fetchMock);

    await contentApi.listNotices(5);

    const [url] = fetchMock.mock.calls[0];
    expect(String(url)).toContain('/api/meetings/5/notices');
  });
});
