import { afterEach, describe, expect, it, vi } from 'vitest';
import { Route, Routes } from 'react-router-dom';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MeetingContentPage } from './MeetingContentPage';
import type { MeetingResponse, NoticeResponse, PostResponse } from '@/api';
import { errorResponse, jsonResponse, renderWithProviders } from '@/test/test-utils';

afterEach(() => vi.unstubAllGlobals());

function meeting(): MeetingResponse {
  return {
    id: 5,
    mentorId: 1,
    title: '스프링 스터디',
    topic: 'backend',
    weeks: 8,
    recruitStart: null,
    recruitEnd: null,
    capacity: 5,
    format: 'online',
    initialContent: null,
    status: 'IN_PROGRESS',
    rejectReason: null,
  };
}

const post: PostResponse = {
  id: 20,
  meetingId: 5,
  authorId: 1,
  week: 1,
  body: '1주차 자료입니다.',
  attachments: [
    {
      id: 7,
      postId: 20,
      fileName: 'week1.pdf',
      contentType: 'application/pdf',
      sizeBytes: 2048,
      uploaderId: 1,
      createdAt: '2026-01-01T00:00:00Z',
    },
  ],
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: null,
};

const notice: NoticeResponse = {
  id: 30,
  meetingId: 5,
  authorId: 1,
  body: '이번 주 공지입니다.',
  createdAt: '2026-01-01T00:00:00Z',
};

function renderPage(role: 'MENTOR' | 'MENTEE') {
  return renderWithProviders(
    <Routes>
      <Route path="/meetings/:id/content" element={<MeetingContentPage />} />
    </Routes>,
    { route: '/meetings/5/content', auth: { token: 't', role } },
  );
}

describe('MeetingContentPage', () => {
  it('renders posts (with attachment) and notices for a participant', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => {
        const u = String(url);
        if (u.includes('/posts')) return Promise.resolve(jsonResponse([post]));
        if (u.includes('/notices')) return Promise.resolve(jsonResponse([notice]));
        return Promise.resolve(jsonResponse(meeting()));
      }),
    );

    renderPage('MENTEE');

    expect(await screen.findByTestId('post-20')).toHaveTextContent('1주차 자료입니다.');
    expect(screen.getByTestId('attachment-download-7')).toHaveTextContent('week1.pdf');
    expect(screen.getByTestId('notice-30')).toHaveTextContent('이번 주 공지입니다.');
    // A mentee sees no authoring forms.
    expect(screen.queryByTestId('post-form')).not.toBeInTheDocument();
    expect(screen.queryByTestId('notice-form')).not.toBeInTheDocument();
  });

  it('shows authoring forms for a mentor', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => {
        const u = String(url);
        if (u.includes('/posts')) return Promise.resolve(jsonResponse([post]));
        if (u.includes('/notices')) return Promise.resolve(jsonResponse([notice]));
        return Promise.resolve(jsonResponse(meeting()));
      }),
    );

    renderPage('MENTOR');

    expect(await screen.findByTestId('post-form')).toBeInTheDocument();
    expect(screen.getByTestId('notice-form')).toBeInTheDocument();
    expect(screen.getByTestId('attachment-upload-20')).toBeInTheDocument();
  });

  it('surfaces a 403 as an access error for a non-participant', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => {
        const u = String(url);
        if (u.includes('/posts') || u.includes('/notices')) {
          return Promise.resolve(
            errorResponse(403, 'CONTENT_FORBIDDEN', '이 모임의 자료를 열람할 권한이 없습니다.'),
          );
        }
        return Promise.resolve(jsonResponse(meeting()));
      }),
    );

    renderPage('MENTEE');

    expect(await screen.findByTestId('content-error')).toHaveTextContent('권한이 없습니다');
  });

  it('blocks a disallowed attachment client-side before upload', async () => {
    const fetchMock = vi.fn((url: string) => {
      const u = String(url);
      if (u.includes('/posts')) return Promise.resolve(jsonResponse([post]));
      if (u.includes('/notices')) return Promise.resolve(jsonResponse([notice]));
      return Promise.resolve(jsonResponse(meeting()));
    });
    vi.stubGlobal('fetch', fetchMock);

    renderPage('MENTOR');
    await screen.findByTestId('post-form');

    const input = screen.getByTestId('attachment-upload-20') as HTMLInputElement;
    const badFile = new File(['x'], 'evil.exe', { type: 'application/x-msdownload' });
    await userEvent.upload(input, badFile);

    expect(await screen.findByTestId('attachment-upload-error-20')).toHaveTextContent('형식');
    // No upload request was made (blocked before the network call).
    expect(fetchMock.mock.calls.some(([u]) => String(u).includes('/attachments'))).toBe(false);
  });
});
