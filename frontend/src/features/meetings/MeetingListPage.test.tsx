import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MeetingListPage } from './MeetingListPage';
import type { EnrollmentResponse, MeetingSummary, PageResponse } from '@/api';
import { errorResponse, jsonResponse, renderWithProviders } from '@/test/test-utils';

afterEach(() => vi.unstubAllGlobals());

function page(content: MeetingSummary[]): PageResponse<MeetingSummary> {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: 1 };
}

function enrollment(meetingId: number, status: EnrollmentResponse['status'] = 'APPLIED'): EnrollmentResponse {
  return { id: meetingId * 10, meetingId, menteeId: 2, status, appliedAt: '2026-01-01T00:00:00Z' };
}

/**
 * Routes fetch by URL. The list load fires listRecruiting AND (for mentees)
 * listMine() in parallel, so a single catch-all mock is no longer sufficient.
 */
function routedFetch(opts: {
  meetings?: MeetingSummary[];
  meetingsError?: Response;
  enrollments?: EnrollmentResponse[];
  onApply?: () => Response;
}) {
  return vi.fn((url: string, init?: RequestInit) => {
    const u = String(url);
    if (u.includes('/enrollments/mine')) {
      return Promise.resolve(jsonResponse(opts.enrollments ?? []));
    }
    if (u.includes('/enrollments') && init?.method === 'POST') {
      return Promise.resolve(
        opts.onApply
          ? opts.onApply()
          : jsonResponse(enrollment(1), 201),
      );
    }
    if (opts.meetingsError) return Promise.resolve(opts.meetingsError);
    return Promise.resolve(jsonResponse(page(opts.meetings ?? [])));
  });
}

const RECRUITING: MeetingSummary[] = [
  { id: 1, title: 'React 스터디', topic: 'FE', weeks: 4, capacity: 6, status: 'RECRUITING' },
];

describe('MeetingListPage', () => {
  it('renders an empty state', async () => {
    vi.stubGlobal('fetch', routedFetch({ meetings: [] }));
    renderWithProviders(<MeetingListPage />, { auth: { token: 't', role: 'MENTEE' } });
    expect(await screen.findByTestId('meetings-empty')).toBeInTheDocument();
  });

  it('renders recruiting meetings on success', async () => {
    vi.stubGlobal('fetch', routedFetch({ meetings: RECRUITING }));

    renderWithProviders(<MeetingListPage />, { auth: { token: 't', role: 'MENTEE' } });

    expect(await screen.findByTestId('meeting-card-1')).toHaveTextContent('React 스터디');
    expect(screen.getByTestId('meeting-card-1')).toHaveTextContent('모집중');
  });

  it('renders an error state', async () => {
    vi.stubGlobal(
      'fetch',
      routedFetch({ meetingsError: errorResponse(500, 'INTERNAL', '서버 오류') }),
    );
    renderWithProviders(<MeetingListPage />, { auth: { token: 't', role: 'MENTEE' } });
    expect(await screen.findByTestId('meetings-error')).toBeInTheDocument();
  });

  it('shows the create action only for mentors', async () => {
    vi.stubGlobal('fetch', routedFetch({ meetings: [] }));
    renderWithProviders(<MeetingListPage />, { auth: { token: 't', role: 'MENTOR' } });
    expect(await screen.findByTestId('open-create-meeting')).toBeInTheDocument();
    expect(screen.queryByTestId('open-admin-queue')).not.toBeInTheDocument();
  });

  it('shows the apply button only for mentees on recruiting cards', async () => {
    vi.stubGlobal('fetch', routedFetch({ meetings: RECRUITING }));

    renderWithProviders(<MeetingListPage />, { auth: { token: 't', role: 'MENTEE' } });

    expect(await screen.findByTestId('apply-button-1')).toBeInTheDocument();
  });

  it('does not show the apply button for mentors', async () => {
    vi.stubGlobal('fetch', routedFetch({ meetings: RECRUITING }));

    renderWithProviders(<MeetingListPage />, { auth: { token: 't', role: 'MENTOR' } });

    await screen.findByTestId('meeting-card-1');
    expect(screen.queryByTestId('apply-button-1')).not.toBeInTheDocument();
  });

  it('reflects an already-applied meeting as applied on initial load', async () => {
    vi.stubGlobal(
      'fetch',
      routedFetch({ meetings: RECRUITING, enrollments: [enrollment(1, 'APPLIED')] }),
    );

    renderWithProviders(<MeetingListPage />, { auth: { token: 't', role: 'MENTEE' } });

    const button = await screen.findByTestId('apply-button-1');
    expect(button).toBeDisabled();
    expect(button).toHaveTextContent('신청완료');
  });

  it('does not mark a cancelled enrollment as applied', async () => {
    vi.stubGlobal(
      'fetch',
      routedFetch({ meetings: RECRUITING, enrollments: [enrollment(1, 'CANCELLED')] }),
    );

    renderWithProviders(<MeetingListPage />, { auth: { token: 't', role: 'MENTEE' } });

    const button = await screen.findByTestId('apply-button-1');
    expect(button).not.toBeDisabled();
    expect(button).toHaveTextContent('신청');
  });

  it('applies successfully and marks the card as applied', async () => {
    vi.stubGlobal('fetch', routedFetch({ meetings: RECRUITING }));

    renderWithProviders(<MeetingListPage />, { auth: { token: 't', role: 'MENTEE' } });

    await userEvent.click(await screen.findByTestId('apply-button-1'));

    expect(await screen.findByTestId('apply-feedback-1')).toHaveTextContent('신청이 완료');
    expect(screen.getByTestId('apply-button-1')).toBeDisabled();
  });

  it('maps a 409 ENROLLMENT_FULL to a Korean error message', async () => {
    vi.stubGlobal(
      'fetch',
      routedFetch({
        meetings: RECRUITING,
        onApply: () => errorResponse(409, 'ENROLLMENT_FULL', '모집 정원이 마감되었습니다.'),
      }),
    );

    renderWithProviders(<MeetingListPage />, { auth: { token: 't', role: 'MENTEE' } });

    await userEvent.click(await screen.findByTestId('apply-button-1'));

    expect(await screen.findByTestId('apply-feedback-1')).toHaveTextContent('마감');
  });
});
