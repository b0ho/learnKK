import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MyLearningPage } from './MyLearningPage';
import type {
  ApplicantResponse,
  EnrollmentResponse,
  MeetingResponse,
  MeetingSessionResponse,
  MeetingSummary,
  PageResponse,
} from '@/api';
import { emptyResponse, jsonResponse, renderWithProviders } from '@/test/test-utils';

afterEach(() => vi.unstubAllGlobals());

function meetingPage(content: MeetingSummary[]): PageResponse<MeetingSummary> {
  return { content, page: 0, size: 50, totalElements: content.length, totalPages: 1 };
}

function meeting(id: number, status: MeetingResponse['status']): MeetingResponse {
  return {
    id,
    mentorId: 1,
    title: `모임 ${id}`,
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

function session(id: number, scheduledAt: string, window = 120): MeetingSessionResponse {
  return { id, meetingId: 4, week: 1, scheduledAt, checkInWindowMinutes: window, completed: false };
}

describe('MyLearningPage — MENTEE', () => {
  it('lists the mentee own enrollments with composed meeting info', async () => {
    const enrollments: EnrollmentResponse[] = [
      { id: 11, meetingId: 3, menteeId: 2, status: 'APPLIED', appliedAt: '2026-01-02T00:00:00Z' },
    ];
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => {
        if (String(url).includes('/api/enrollments/mine')) {
          return Promise.resolve(jsonResponse(enrollments));
        }
        return Promise.resolve(jsonResponse(meeting(3, 'RECRUITING')));
      }),
    );

    renderWithProviders(<MyLearningPage />, { auth: { token: 't', role: 'MENTEE' } });

    expect(await screen.findByTestId('mentee-enrollment-11')).toHaveTextContent('모임 3');
    expect(screen.getByTestId('mentee-enrollment-status-11')).toHaveTextContent('신청됨');
    expect(screen.getByTestId('mentee-cancel-11')).toBeInTheDocument();
  });

  it('hides the cancel button once the meeting has started', async () => {
    const enrollments: EnrollmentResponse[] = [
      { id: 12, meetingId: 4, menteeId: 2, status: 'APPLIED', appliedAt: '2026-01-02T00:00:00Z' },
    ];
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => {
        const u = String(url);
        if (u.includes('/api/enrollments/mine')) return Promise.resolve(jsonResponse(enrollments));
        if (u.includes('/my-attendance'))
          return Promise.resolve(jsonResponse({ attended: 0, totalScheduled: 0, rate: 0 }));
        if (u.includes('/sessions')) return Promise.resolve(jsonResponse([]));
        return Promise.resolve(jsonResponse(meeting(4, 'IN_PROGRESS')));
      }),
    );

    renderWithProviders(<MyLearningPage />, { auth: { token: 't', role: 'MENTEE' } });

    await screen.findByTestId('mentee-enrollment-12');
    expect(screen.queryByTestId('mentee-cancel-12')).not.toBeInTheDocument();
  });

  it('cancels an enrollment and reloads', async () => {
    const enrollments: EnrollmentResponse[] = [
      { id: 13, meetingId: 5, menteeId: 2, status: 'APPLIED', appliedAt: '2026-01-02T00:00:00Z' },
    ];
    let cancelled = false;
    const fetchMock = vi.fn((url: string, init?: RequestInit) => {
      const u = String(url);
      if (u.includes('/api/enrollments/mine')) {
        return Promise.resolve(
          jsonResponse(
            cancelled
              ? enrollments.map((e) => ({ ...e, status: 'CANCELLED' as const }))
              : enrollments,
          ),
        );
      }
      if (u.includes('/meetings/5/enrollments/mine') && init?.method === 'DELETE') {
        cancelled = true;
        return Promise.resolve(emptyResponse(204));
      }
      return Promise.resolve(jsonResponse(meeting(5, 'RECRUITING')));
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithProviders(<MyLearningPage />, { auth: { token: 't', role: 'MENTEE' } });

    const cancelBtn = await screen.findByTestId('mentee-cancel-13');
    await userEvent.click(cancelBtn);

    await waitFor(() =>
      expect(screen.getByTestId('mentee-enrollment-status-13')).toHaveTextContent('취소됨'),
    );
  });

  it('shows the empty state when the mentee has no enrollments', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse([])));
    renderWithProviders(<MyLearningPage />, { auth: { token: 't', role: 'MENTEE' } });
    expect(await screen.findByTestId('mentee-learning-empty')).toBeInTheDocument();
  });

  it('shows sessions, attendance rate and checks in within the window', async () => {
    const enrollments: EnrollmentResponse[] = [
      { id: 20, meetingId: 4, menteeId: 2, status: 'APPLIED', appliedAt: '2026-01-02T00:00:00Z' },
    ];
    const openAt = new Date(Date.now() - 60_000).toISOString(); // 1 min ago → within window
    let attended = 0;
    const fetchMock = vi.fn((url: string, init?: RequestInit) => {
      const u = String(url);
      if (u.includes('/api/enrollments/mine')) return Promise.resolve(jsonResponse(enrollments));
      if (u.endsWith('/attendance') && init?.method === 'POST') {
        attended = 1;
        return Promise.resolve(jsonResponse({ sessionId: 30, menteeId: 2, checkedInAt: 'x' }, 201));
      }
      if (u.includes('/my-attendance'))
        return Promise.resolve(
          jsonResponse({ attended, totalScheduled: 1, rate: attended === 1 ? 1 : 0 }),
        );
      if (u.endsWith('/sessions')) return Promise.resolve(jsonResponse([session(30, openAt)]));
      return Promise.resolve(jsonResponse(meeting(4, 'IN_PROGRESS')));
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithProviders(<MyLearningPage />, { auth: { token: 't', role: 'MENTEE' } });

    const checkIn = await screen.findByTestId('mentee-checkin-30');
    await userEvent.click(checkIn);

    await waitFor(() =>
      expect(screen.getByTestId('mentee-session-done-30')).toBeInTheDocument(),
    );
    expect(screen.getByTestId('mentee-attendance-4')).toHaveTextContent('1/1');
  });

  it('surfaces a 409 window-closed error on check-in', async () => {
    const enrollments: EnrollmentResponse[] = [
      { id: 21, meetingId: 4, menteeId: 2, status: 'APPLIED', appliedAt: '2026-01-02T00:00:00Z' },
    ];
    const openAt = new Date(Date.now() - 60_000).toISOString();
    const fetchMock = vi.fn((url: string, init?: RequestInit) => {
      const u = String(url);
      if (u.includes('/api/enrollments/mine')) return Promise.resolve(jsonResponse(enrollments));
      if (u.endsWith('/attendance') && init?.method === 'POST') {
        return Promise.resolve(
          jsonResponse(
            { code: 'ATTENDANCE_WINDOW_CLOSED', message: '출석 가능 시간이 아닙니다.' },
            409,
          ),
        );
      }
      if (u.includes('/my-attendance'))
        return Promise.resolve(jsonResponse({ attended: 0, totalScheduled: 1, rate: 0 }));
      if (u.endsWith('/sessions')) return Promise.resolve(jsonResponse([session(31, openAt)]));
      return Promise.resolve(jsonResponse(meeting(4, 'IN_PROGRESS')));
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithProviders(<MyLearningPage />, { auth: { token: 't', role: 'MENTEE' } });

    await userEvent.click(await screen.findByTestId('mentee-checkin-31'));
    expect(await screen.findByTestId('mentee-checkin-error-4')).toHaveTextContent(
      '출석 가능 시간이 아닙니다',
    );
  });

  it('marks a future session as not checkable', async () => {
    const enrollments: EnrollmentResponse[] = [
      { id: 22, meetingId: 4, menteeId: 2, status: 'APPLIED', appliedAt: '2026-01-02T00:00:00Z' },
    ];
    const future = new Date(Date.now() + 86_400_000).toISOString();
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => {
        const u = String(url);
        if (u.includes('/api/enrollments/mine')) return Promise.resolve(jsonResponse(enrollments));
        if (u.includes('/sessions')) return Promise.resolve(jsonResponse([session(32, future)]));
        if (u.includes('/my-attendance'))
          return Promise.resolve(jsonResponse({ attended: 0, totalScheduled: 1, rate: 0 }));
        return Promise.resolve(jsonResponse(meeting(4, 'IN_PROGRESS')));
      }),
    );

    renderWithProviders(<MyLearningPage />, { auth: { token: 't', role: 'MENTEE' } });

    expect(await screen.findByTestId('mentee-session-closed-32')).toBeInTheDocument();
    expect(screen.queryByTestId('mentee-checkin-32')).not.toBeInTheDocument();
  });
});

describe('MyLearningPage — MENTOR', () => {
  function mentorFetch(meetings: MeetingSummary[], applicants: ApplicantResponse[]) {
    return vi.fn((url: string) => {
      const u = String(url);
      if (u.includes('/applicants')) return Promise.resolve(jsonResponse(applicants));
      if (u.includes('/sessions')) return Promise.resolve(jsonResponse([]));
      return Promise.resolve(jsonResponse(meetingPage(meetings)));
    });
  }

  it('lists own meetings with status, next action and applicant count', async () => {
    const meetings: MeetingSummary[] = [
      { id: 3, title: '내 모임', topic: null, weeks: 4, capacity: 6, status: 'RECRUITING' },
    ];
    const applicants: ApplicantResponse[] = [
      { menteeId: 2, nickname: '멘티하나', appliedAt: '2026-01-01T00:00:00Z' },
    ];
    vi.stubGlobal('fetch', mentorFetch(meetings, applicants));

    renderWithProviders(<MyLearningPage />, { auth: { token: 't', role: 'MENTOR' } });

    expect(await screen.findByTestId('mentor-meeting-3')).toHaveTextContent('내 모임');
    expect(screen.getByTestId('mentor-meeting-status-3')).toHaveTextContent('모집중');
    expect(screen.getByTestId('mentor-meeting-next-3')).toHaveTextContent('모집 확정');
    await waitFor(() =>
      expect(screen.getByTestId('applicant-count-3')).toHaveTextContent('신청자: 1명'),
    );
    expect(screen.getByTestId('applicant-3-2')).toHaveTextContent('멘티하나');
    // A RECRUITING meeting shows no session management.
    expect(screen.queryByTestId('mentor-sessions-3')).not.toBeInTheDocument();
  });

  it('shows the empty state when the mentor has no meetings', async () => {
    vi.stubGlobal('fetch', mentorFetch([], []));
    renderWithProviders(<MyLearningPage />, { auth: { token: 't', role: 'MENTOR' } });
    expect(await screen.findByTestId('mentor-hub-empty')).toBeInTheDocument();
  });

  it('adds a session to an in-progress meeting', async () => {
    const meetings: MeetingSummary[] = [
      { id: 9, title: '진행 모임', topic: null, weeks: 4, capacity: 6, status: 'IN_PROGRESS' },
    ];
    let added = false;
    const fetchMock = vi.fn((url: string, init?: RequestInit) => {
      const u = String(url);
      if (u.includes('/applicants')) return Promise.resolve(jsonResponse([]));
      if (u.includes('/sessions') && init?.method === 'POST') {
        added = true;
        return Promise.resolve(
          jsonResponse(
            { id: 40, meetingId: 9, week: 1, scheduledAt: 'x', checkInWindowMinutes: 120 },
            201,
          ),
        );
      }
      if (u.includes('/sessions')) {
        return Promise.resolve(
          jsonResponse(
            added
              ? [{ id: 40, meetingId: 9, week: 1, scheduledAt: '2026-05-01T10:00:00Z', checkInWindowMinutes: 120 }]
              : [],
          ),
        );
      }
      return Promise.resolve(jsonResponse(meetingPage(meetings)));
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithProviders(<MyLearningPage />, { auth: { token: 't', role: 'MENTOR' } });

    await screen.findByTestId('mentor-sessions-9');
    await userEvent.click(screen.getByTestId('mentor-session-add-submit-9'));

    await waitFor(() => expect(screen.getByTestId('mentor-session-40')).toBeInTheDocument());
  });

  it('reschedules an existing session', async () => {
    const meetings: MeetingSummary[] = [
      { id: 9, title: '진행 모임', topic: null, weeks: 4, capacity: 6, status: 'IN_PROGRESS' },
    ];
    const fetchMock = vi.fn((url: string, init?: RequestInit) => {
      const u = String(url);
      if (u.includes('/applicants')) return Promise.resolve(jsonResponse([]));
      if (u.includes('/api/sessions/50') && init?.method === 'PUT') {
        return Promise.resolve(
          jsonResponse({ id: 50, meetingId: 9, week: 2, scheduledAt: 'y', checkInWindowMinutes: 120 }),
        );
      }
      if (u.includes('/sessions')) {
        return Promise.resolve(
          jsonResponse([
            { id: 50, meetingId: 9, week: 2, scheduledAt: '2026-06-01T10:00:00Z', checkInWindowMinutes: 120 },
          ]),
        );
      }
      return Promise.resolve(jsonResponse(meetingPage(meetings)));
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithProviders(<MyLearningPage />, { auth: { token: 't', role: 'MENTOR' } });

    await userEvent.click(await screen.findByTestId('mentor-session-edit-50'));
    const form = await screen.findByTestId('mentor-session-edit-form-50');
    const input = within(form).getByTestId('mentor-session-edit-input-50');
    await userEvent.clear(input);
    await userEvent.type(input, '2026-06-02T11:00');
    await userEvent.click(screen.getByTestId('mentor-session-edit-save-50'));

    await waitFor(() => {
      const calls = fetchMock.mock.calls.filter(
        ([u, init]) => String(u).includes('/api/sessions/50') && (init as RequestInit)?.method === 'PUT',
      );
      expect(calls.length).toBe(1);
    });
  });
});
