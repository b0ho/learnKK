import { afterEach, describe, expect, it, vi } from 'vitest';
import { Route, Routes } from 'react-router-dom';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { PreSurveyAnswerPage } from './PreSurveyAnswerPage';
import type { MeetingResponse, SurveyQuestionDto } from '@/api';
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

const questions: SurveyQuestionDto[] = [
  { id: 100, orderNo: 1, text: '목표는?', type: 'SHORT_TEXT', options: [], required: true },
];

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path="/meetings/:id/survey-answer" element={<PreSurveyAnswerPage />} />
    </Routes>,
    { route: '/meetings/5/survey-answer', auth: { token: 't', role: 'MENTEE' } },
  );
}

function stubFetch(status: MeetingResponse['status'], submit?: () => Promise<Response>) {
  vi.stubGlobal(
    'fetch',
    vi.fn((url: string, init?: RequestInit) => {
      const u = String(url);
      if (u.includes('/questions')) {
        return Promise.resolve(jsonResponse(questions));
      }
      if (u.includes('/survey-answers') && init?.method === 'POST') {
        return submit ? submit() : Promise.resolve(jsonResponse([{ questionId: 100, answerText: 'x' }]));
      }
      return Promise.resolve(jsonResponse(meeting(status)));
    }),
  );
}

describe('PreSurveyAnswerPage', () => {
  it('shows guidance when the meeting has not started (②전)', async () => {
    stubFetch('READY_TO_START');
    renderPage();
    expect(await screen.findByTestId('presurvey-not-open')).toBeInTheDocument();
    expect(screen.queryByTestId('presurvey-form')).not.toBeInTheDocument();
  });

  it('submits answers when the meeting is IN_PROGRESS', async () => {
    stubFetch('IN_PROGRESS');
    renderPage();

    const input = await screen.findByTestId('presurvey-input-100');
    await userEvent.type(input, '성장하기');
    await userEvent.click(screen.getByTestId('presurvey-submit'));

    expect(await screen.findByTestId('presurvey-submit-success')).toBeInTheDocument();
  });

  it('maps a 409 PRESURVEY_NOT_OPEN to a Korean error', async () => {
    stubFetch('IN_PROGRESS', () =>
      Promise.resolve(errorResponse(409, 'PRESURVEY_NOT_OPEN', '모임 시작 이후에만 응답할 수 있습니다.')),
    );
    renderPage();

    await userEvent.type(await screen.findByTestId('presurvey-input-100'), '답');
    await userEvent.click(screen.getByTestId('presurvey-submit'));

    await waitFor(() =>
      expect(screen.getByTestId('presurvey-submit-error')).toHaveTextContent('시작 이후'),
    );
  });

  it('maps a 400 required-missing to a Korean error', async () => {
    stubFetch('IN_PROGRESS', () =>
      Promise.resolve(errorResponse(400, 'PRESURVEY_REQUIRED_MISSING', '필수 문항에 모두 응답해야 합니다.')),
    );
    renderPage();

    await screen.findByTestId('presurvey-submit');
    await userEvent.click(screen.getByTestId('presurvey-submit'));

    await waitFor(() =>
      expect(screen.getByTestId('presurvey-submit-error')).toHaveTextContent('필수 문항'),
    );
  });
});
