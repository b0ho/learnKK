import { afterEach, describe, expect, it, vi } from 'vitest';
import { Route, Routes } from 'react-router-dom';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { FeedbackPage } from './FeedbackPage';
import type { MeetingResponse } from '@/api';
import { errorResponse, jsonResponse, renderWithProviders } from '@/test/test-utils';

afterEach(() => vi.unstubAllGlobals());

function meeting(status: MeetingResponse['status']): MeetingResponse {
  return {
    id: 5,
    mentorId: 1,
    title: '스프링 모임',
    topic: 'backend',
    weeks: 4,
    recruitStart: null,
    recruitEnd: null,
    capacity: 5,
    format: 'online',
    initialContent: null,
    status,
    rejectReason: null,
  };
}

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path="/meetings/:id/feedback" element={<FeedbackPage />} />
    </Routes>,
    { route: '/meetings/5/feedback', auth: { token: 't', role: 'MENTEE' } },
  );
}

describe('FeedbackPage', () => {
  it('submits feedback content', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string, init?: RequestInit) => {
        if (String(url).includes('/feedback') && init?.method === 'POST') {
          return Promise.resolve(jsonResponse({ id: 1, menteeId: 2, content: '좋음', createdAt: 'x' }, 201));
        }
        return Promise.resolve(jsonResponse(meeting('COMPLETED')));
      }),
    );
    renderPage();

    const textarea = await screen.findByTestId('feedback-content');
    await userEvent.type(textarea, '유익했습니다');
    await userEvent.click(screen.getByTestId('feedback-submit'));

    expect(await screen.findByTestId('feedback-submit-success')).toBeInTheDocument();
  });

  it('maps a 409 FEEDBACK_NOT_OPEN to a Korean error', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string, init?: RequestInit) => {
        if (String(url).includes('/feedback') && init?.method === 'POST') {
          return Promise.resolve(
            errorResponse(409, 'FEEDBACK_NOT_OPEN', '진행 중이거나 완료된 모임에만 제출할 수 있습니다.'),
          );
        }
        return Promise.resolve(jsonResponse(meeting('IN_PROGRESS')));
      }),
    );
    renderPage();

    await userEvent.type(await screen.findByTestId('feedback-content'), '내용');
    await userEvent.click(screen.getByTestId('feedback-submit'));

    await waitFor(() =>
      expect(screen.getByTestId('feedback-submit-error')).toHaveTextContent('제출할 수 있습니다'),
    );
  });
});
