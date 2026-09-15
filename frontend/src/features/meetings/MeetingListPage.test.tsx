import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MeetingListPage } from './MeetingListPage';
import type { MeetingSummary, PageResponse } from '@/api';
import { errorResponse, jsonResponse, renderWithProviders } from '@/test/test-utils';

afterEach(() => vi.unstubAllGlobals());

function page(content: MeetingSummary[]): PageResponse<MeetingSummary> {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: 1 };
}

describe('MeetingListPage', () => {
  it('renders an empty state', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(page([]))));
    renderWithProviders(<MeetingListPage />, { auth: { token: 't', role: 'MENTEE' } });
    expect(await screen.findByTestId('meetings-empty')).toBeInTheDocument();
  });

  it('renders recruiting meetings on success', async () => {
    const meetings: MeetingSummary[] = [
      { id: 1, title: 'React 스터디', topic: 'FE', weeks: 4, capacity: 6, status: 'RECRUITING' },
    ];
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(page(meetings))));

    renderWithProviders(<MeetingListPage />, { auth: { token: 't', role: 'MENTEE' } });

    expect(await screen.findByTestId('meeting-card-1')).toHaveTextContent('React 스터디');
    expect(screen.getByTestId('meeting-card-1')).toHaveTextContent('모집중');
  });

  it('renders an error state', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(errorResponse(500, 'INTERNAL', '서버 오류')),
    );
    renderWithProviders(<MeetingListPage />, { auth: { token: 't', role: 'MENTEE' } });
    expect(await screen.findByTestId('meetings-error')).toBeInTheDocument();
  });

  it('shows the create action only for mentors', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(page([]))));
    renderWithProviders(<MeetingListPage />, { auth: { token: 't', role: 'MENTOR' } });
    expect(await screen.findByTestId('open-create-meeting')).toBeInTheDocument();
    expect(screen.queryByTestId('open-admin-queue')).not.toBeInTheDocument();
  });

  it('shows the apply button only for mentees on recruiting cards', async () => {
    const meetings: MeetingSummary[] = [
      { id: 1, title: 'React 스터디', topic: 'FE', weeks: 4, capacity: 6, status: 'RECRUITING' },
    ];
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(page(meetings))));

    renderWithProviders(<MeetingListPage />, { auth: { token: 't', role: 'MENTEE' } });

    expect(await screen.findByTestId('apply-button-1')).toBeInTheDocument();
  });

  it('does not show the apply button for mentors', async () => {
    const meetings: MeetingSummary[] = [
      { id: 1, title: 'React 스터디', topic: 'FE', weeks: 4, capacity: 6, status: 'RECRUITING' },
    ];
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(page(meetings))));

    renderWithProviders(<MeetingListPage />, { auth: { token: 't', role: 'MENTOR' } });

    await screen.findByTestId('meeting-card-1');
    expect(screen.queryByTestId('apply-button-1')).not.toBeInTheDocument();
  });

  it('applies successfully and marks the card as applied', async () => {
    const meetings: MeetingSummary[] = [
      { id: 1, title: 'React 스터디', topic: 'FE', weeks: 4, capacity: 6, status: 'RECRUITING' },
    ];
    const fetchMock = vi.fn((url: string, init?: RequestInit) => {
      if (String(url).includes('/enrollments') && init?.method === 'POST') {
        return Promise.resolve(
          jsonResponse(
            { id: 9, meetingId: 1, menteeId: 2, status: 'APPLIED', appliedAt: '2026-01-01T00:00:00Z' },
            201,
          ),
        );
      }
      return Promise.resolve(jsonResponse(page(meetings)));
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithProviders(<MeetingListPage />, { auth: { token: 't', role: 'MENTEE' } });

    await userEvent.click(await screen.findByTestId('apply-button-1'));

    expect(await screen.findByTestId('apply-feedback-1')).toHaveTextContent('신청이 완료');
    expect(screen.getByTestId('apply-button-1')).toBeDisabled();
  });

  it('reflects an existing enrollment on load (신청완료, disabled)', async () => {
    const meetings: MeetingSummary[] = [
      { id: 1, title: 'React 스터디', topic: 'FE', weeks: 4, capacity: 6, status: 'RECRUITING' },
    ];
    const fetchMock = vi.fn((url: string) => {
      if (String(url).includes('/api/enrollments/mine')) {
        return Promise.resolve(
          jsonResponse([
            { id: 9, meetingId: 1, menteeId: 2, status: 'APPLIED', appliedAt: '2026-01-01T00:00:00Z' },
          ]),
        );
      }
      return Promise.resolve(jsonResponse(page(meetings)));
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithProviders(<MeetingListPage />, { auth: { token: 't', role: 'MENTEE' } });

    const button = await screen.findByTestId('apply-button-1');
    await waitFor(() => expect(button).toBeDisabled());
    expect(button).toHaveTextContent('신청완료');
  });

  it('marks a full meeting as 마감 with a disabled button', async () => {
    const meetings: MeetingSummary[] = [
      {
        id: 2,
        title: 'Full 스터디',
        topic: 'FE',
        weeks: 4,
        capacity: 3,
        status: 'RECRUITING',
        enrolledCount: 3,
        full: true,
      },
    ];
    const fetchMock = vi.fn((url: string) => {
      if (String(url).includes('/api/enrollments/mine')) {
        return Promise.resolve(jsonResponse([]));
      }
      return Promise.resolve(jsonResponse(page(meetings)));
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithProviders(<MeetingListPage />, { auth: { token: 't', role: 'MENTEE' } });

    expect(await screen.findByTestId('full-badge-2')).toHaveTextContent('마감');
    const button = screen.getByTestId('apply-button-2');
    expect(button).toBeDisabled();
    expect(button).toHaveTextContent('마감');
  });

  it('maps a 409 ENROLLMENT_FULL to a Korean error message', async () => {
    const meetings: MeetingSummary[] = [
      { id: 1, title: 'React 스터디', topic: 'FE', weeks: 4, capacity: 6, status: 'RECRUITING' },
    ];
    const fetchMock = vi.fn((url: string, init?: RequestInit) => {
      if (String(url).includes('/enrollments') && init?.method === 'POST') {
        return Promise.resolve(errorResponse(409, 'ENROLLMENT_FULL', '모집 정원이 마감되었습니다.'));
      }
      return Promise.resolve(jsonResponse(page(meetings)));
    });
    vi.stubGlobal('fetch', fetchMock);

    renderWithProviders(<MeetingListPage />, { auth: { token: 't', role: 'MENTEE' } });

    await userEvent.click(await screen.findByTestId('apply-button-1'));

    expect(await screen.findByTestId('apply-feedback-1')).toHaveTextContent('마감');
  });
});
