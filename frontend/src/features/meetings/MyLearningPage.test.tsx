import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MyLearningPage } from './MyLearningPage';
import type {
  ApplicantResponse,
  EnrollmentResponse,
  MeetingResponse,
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
    // A RECRUITING meeting is cancellable.
    expect(screen.getByTestId('mentee-cancel-11')).toBeInTheDocument();
  });

  it('hides the cancel button once the meeting has started', async () => {
    const enrollments: EnrollmentResponse[] = [
      { id: 12, meetingId: 4, menteeId: 2, status: 'APPLIED', appliedAt: '2026-01-02T00:00:00Z' },
    ];
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string) => {
        if (String(url).includes('/api/enrollments/mine')) {
          return Promise.resolve(jsonResponse(enrollments));
        }
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
});

describe('MyLearningPage — MENTOR', () => {
  function mentorFetch(meetings: MeetingSummary[], applicants: ApplicantResponse[]) {
    return vi.fn((url: string) => {
      const u = String(url);
      if (u.includes('/applicants')) {
        return Promise.resolve(jsonResponse(applicants));
      }
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
  });

  it('shows the empty state when the mentor has no meetings', async () => {
    vi.stubGlobal('fetch', mentorFetch([], []));
    renderWithProviders(<MyLearningPage />, { auth: { token: 't', role: 'MENTOR' } });
    expect(await screen.findByTestId('mentor-hub-empty')).toBeInTheDocument();
  });
});
