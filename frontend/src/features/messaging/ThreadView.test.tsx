import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { ThreadView } from './ThreadView';
import type { MessageResponse, PageResponse, ThreadSummaryResponse } from '@/api';
import { jsonResponse, renderWithProviders } from '@/test/test-utils';

afterEach(() => vi.unstubAllGlobals());

const threads: ThreadSummaryResponse[] = [
  {
    threadId: 100,
    partnerId: 1,
    partnerNickname: '멘토김',
    lastMessageBody: '네',
    lastMessageAt: '2026-01-02T09:00:00Z',
    unreadCount: 0,
  },
];

function transcript(content: MessageResponse[]): PageResponse<MessageResponse> {
  return { content, page: 0, size: 100, totalElements: content.length, totalPages: 1 };
}

function renderThread() {
  return renderWithProviders(
    <Routes>
      <Route path="/messages/:id" element={<ThreadView />} />
    </Routes>,
    { auth: { token: 't', role: 'MENTEE' }, route: '/messages/100' },
  );
}

describe('ThreadView', () => {
  it('renders the transcript distinguishing my messages from the partner', async () => {
    const messages: MessageResponse[] = [
      {
        id: 1,
        threadId: 100,
        senderId: 1,
        body: '무엇이든 물어보세요',
        readAt: null,
        createdAt: 'a',
      },
      { id: 2, threadId: 100, senderId: 2, body: '감사합니다', readAt: null, createdAt: 'b' },
    ];
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => {
        if (String(url).includes('/api/messages/threads/100')) {
          return Promise.resolve(jsonResponse(transcript(messages)));
        }
        return Promise.resolve(jsonResponse(threads));
      }),
    );

    renderThread();

    // Partner (senderId 1) message is not "mine"; my (senderId 2) message is.
    expect(await screen.findByTestId('message-1')).toHaveAttribute('data-mine', 'false');
    expect(screen.getByTestId('message-2')).toHaveAttribute('data-mine', 'true');
  });

  it('sends a reply to the partner and reloads', async () => {
    let replied = false;
    const fetchMock = vi.fn((url: string, init?: RequestInit) => {
      const u = String(url);
      if (u.endsWith('/api/messages') && init?.method === 'POST') {
        replied = true;
        return Promise.resolve(
          jsonResponse(
            { id: 9, threadId: 100, senderId: 2, body: '넵', readAt: null, createdAt: 'z' },
            201,
          ),
        );
      }
      if (u.includes('/api/messages/threads/100')) {
        return Promise.resolve(
          jsonResponse(
            transcript(
              replied
                ? [{ id: 9, threadId: 100, senderId: 2, body: '넵', readAt: null, createdAt: 'z' }]
                : [],
            ),
          ),
        );
      }
      return Promise.resolve(jsonResponse(threads));
    });
    vi.stubGlobal('fetch', fetchMock);

    renderThread();

    await screen.findByTestId('message-list');
    await userEvent.type(screen.getByTestId('reply-input'), '넵');
    await userEvent.click(screen.getByTestId('reply-send'));

    await waitFor(() => expect(replied).toBe(true));
    const post = fetchMock.mock.calls.find(
      ([u, init]) =>
        String(u).endsWith('/api/messages') && (init as RequestInit)?.method === 'POST',
    );
    const body = JSON.parse((post?.[1] as RequestInit).body as string);
    expect(body).toEqual({ recipientId: 1, body: '넵' });
  });

  it('shows a forbidden error when the boundary rejects a read', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => {
        if (String(url).includes('/api/messages/threads/100')) {
          return Promise.resolve(
            jsonResponse({ code: 'MESSAGING_FORBIDDEN', message: '권한이 없습니다.' }, 403),
          );
        }
        return Promise.resolve(jsonResponse(threads));
      }),
    );

    renderThread();

    expect(await screen.findByTestId('thread-error')).toHaveTextContent('권한이 없습니다.');
  });
});
