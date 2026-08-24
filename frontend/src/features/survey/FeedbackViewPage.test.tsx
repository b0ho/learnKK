import { afterEach, describe, expect, it, vi } from 'vitest';
import { Route, Routes } from 'react-router-dom';
import { screen, waitFor } from '@testing-library/react';
import { FeedbackViewPage } from './FeedbackViewPage';
import type { FeedbackResponse, MeetingResponse, SurveyAnswerResponse } from '@/api';
import { errorResponse, jsonResponse, renderWithProviders } from '@/test/test-utils';

afterEach(() => vi.unstubAllGlobals());

function meeting(): MeetingResponse {
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
    status: 'COMPLETED',
    rejectReason: null,
  };
}

function renderPage(role: 'MENTOR' | 'ADMIN' = 'MENTOR') {
  return renderWithProviders(
    <Routes>
      <Route path="/meetings/:id/feedback-view" element={<FeedbackViewPage />} />
    </Routes>,
    { route: '/meetings/5/feedback-view', auth: { token: 't', role } },
  );
}

describe('FeedbackViewPage', () => {
  it('lists feedback with each mentee pre-survey answers', async () => {
    const feedback: FeedbackResponse[] = [
      { id: 1, menteeId: 2, content: '유익했습니다', createdAt: '2026-01-01T00:00:00Z' },
    ];
    const answers: SurveyAnswerResponse[] = [{ questionId: 100, answerText: '성장' }];
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => {
        const u = String(url);
        if (u.includes('/mentees/2/survey-answers')) {
          return Promise.resolve(jsonResponse(answers));
        }
        if (u.includes('/feedback')) {
          return Promise.resolve(jsonResponse(feedback));
        }
        return Promise.resolve(jsonResponse(meeting()));
      }),
    );

    renderPage();

    expect(await screen.findByTestId('feedback-item-1')).toHaveTextContent('유익했습니다');
    await waitFor(() =>
      expect(screen.getByTestId('survey-answer-2-100')).toHaveTextContent('성장'),
    );
  });

  it('shows an error when the caller is not authorized (403)', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => {
        const u = String(url);
        if (u.includes('/feedback')) {
          return Promise.resolve(errorResponse(403, 'FEEDBACK_FORBIDDEN', '피드백을 조회할 권한이 없습니다.'));
        }
        return Promise.resolve(jsonResponse(meeting()));
      }),
    );

    renderPage();

    expect(await screen.findByTestId('feedback-view-error')).toHaveTextContent('권한이 없습니다');
  });

  it('shows the empty state when there is no feedback', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => {
        const u = String(url);
        if (u.includes('/feedback')) {
          return Promise.resolve(jsonResponse([]));
        }
        return Promise.resolve(jsonResponse(meeting()));
      }),
    );

    renderPage('ADMIN');

    expect(await screen.findByTestId('feedback-view-empty')).toBeInTheDocument();
  });
});
