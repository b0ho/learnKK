import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { MessagesPage } from './MessagesPage';
import type { RecipientResponse, ThreadSummaryResponse } from '@/api';
import { jsonResponse, renderWithProviders } from '@/test/test-utils';

afterEach(() => vi.unstubAllGlobals());

const threads: ThreadSummaryResponse[] = [
  {
    threadId: 100,
    partnerId: 1,
    partnerNickname: '멘토김',
    lastMessageBody: '안녕하세요',
    lastMessageAt: '2026-01-02T09:00:00Z',
    unreadCount: 2,
  },
];

const recipients: RecipientResponse[] = [{ userId: 1, nickname: '멘토김', role: 'MENTOR' }];

describe('MessagesPage', () => {
  it('lists threads with an unread badge', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => {
        if (String(url).includes('/api/messages/threads')) {
          return Promise.resolve(jsonResponse(threads));
        }
        return Promise.resolve(jsonResponse([]));
      }),
    );

    renderWithProviders(<MessagesPage />, { auth: { token: 't', role: 'MENTEE' } });

    const thread = await screen.findByTestId('thread-100');
    expect(thread).toHaveTextContent('멘토김');
    expect(screen.getByTestId('thread-unread-100')).toHaveTextContent('2');
  });

  it('shows an empty state when there are no threads', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() => Promise.resolve(jsonResponse([]))),
    );

    renderWithProviders(<MessagesPage />, { auth: { token: 't', role: 'MENTEE' } });

    expect(await screen.findByTestId('messages-empty')).toBeInTheDocument();
  });

  it('sends a new message through the composer and refreshes', async () => {
    let sent = false;
    const fetchMock = vi.fn((url: string, init?: RequestInit) => {
      const u = String(url);
      if (u.includes('/api/messages/recipients')) {
        return Promise.resolve(jsonResponse(recipients));
      }
      if (u.endsWith('/api/messages') && init?.method === 'POST') {
        sent = true;
        return Promise.resolve(
          jsonResponse(
            { id: 5, threadId: 100, senderId: 2, body: '질문', readAt: null, createdAt: 'x' },
            201,
          ),
        );
      }
      // thread list
      return Promise.resolve(jsonResponse(sent ? threads : []));
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithProviders(<MessagesPage />, { auth: { token: 't', role: 'MENTEE' } });

    await screen.findByTestId('messages-empty');
    await userEvent.click(screen.getByTestId('new-message-button'));

    const select = await screen.findByTestId('recipient-select');
    await userEvent.selectOptions(select, '1');
    await userEvent.type(screen.getByTestId('message-body-input'), '질문');
    await userEvent.click(screen.getByTestId('send-message-button'));

    await waitFor(() => expect(sent).toBe(true));
    const body = JSON.parse(
      (
        fetchMock.mock.calls.find(
          ([u, init]) =>
            String(u).endsWith('/api/messages') && (init as RequestInit)?.method === 'POST',
        )?.[1] as RequestInit
      ).body as string,
    );
    expect(body).toEqual({ recipientId: 1, body: '질문' });
  });

  it('navigates to the thread view on click', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => {
        if (String(url).includes('/api/messages/threads/100')) {
          return Promise.resolve(
            jsonResponse({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 }),
          );
        }
        if (String(url).includes('/api/messages/threads')) {
          return Promise.resolve(jsonResponse(threads));
        }
        return Promise.resolve(jsonResponse([]));
      }),
    );

    renderWithProviders(
      <Routes>
        <Route path="/messages" element={<MessagesPage />} />
        <Route path="/messages/:id" element={<div data-testid="thread-view-stub">대화</div>} />
      </Routes>,
      { auth: { token: 't', role: 'MENTEE' }, route: '/messages' },
    );

    await userEvent.click(await screen.findByTestId('thread-100'));
    expect(await screen.findByTestId('thread-view-stub')).toBeInTheDocument();
  });
});
