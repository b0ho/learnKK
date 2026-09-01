import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AdminMonitoringPage } from './AdminMonitoringPage';
import type { MeetingMonitoringSummary } from '@/api';
import { errorResponse, jsonResponse, renderWithProviders } from '@/test/test-utils';

afterEach(() => vi.unstubAllGlobals());

function row(overrides: Partial<MeetingMonitoringSummary> = {}): MeetingMonitoringSummary {
  return {
    id: 10,
    title: '자바 스터디',
    status: 'IN_PROGRESS',
    mentorId: 1,
    mentorNickname: '멘토닉',
    menteeCount: 5,
    sessionCount: 4,
    endedSessionCount: 2,
    attendanceRate: 0.85,
    completedMenteeCount: 1,
    completionCandidateCount: 3,
    mentorCompletionStatus: 'PENDING',
    ...overrides,
  };
}

function page(content: MeetingMonitoringSummary[]) {
  return jsonResponse({
    content,
    page: 0,
    size: 100,
    totalElements: content.length,
    totalPages: 1,
  });
}

describe('AdminMonitoringPage (US-9.2)', () => {
  it('lists meetings with status, session-based attendance rate and completion progress', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => page([row()])),
    );
    renderWithProviders(<AdminMonitoringPage />, { auth: { token: 't', role: 'ADMIN' } });

    const card = await screen.findByTestId('monitoring-card-10');
    expect(within(card).getByText('자바 스터디')).toBeInTheDocument();
    expect(within(card).getByText('진행중')).toBeInTheDocument();
    expect(screen.getByTestId('monitoring-attendance-10')).toHaveTextContent('85%');
    expect(screen.getByTestId('monitoring-completion-10')).toHaveTextContent('수료 확정 1');
    expect(screen.getByTestId('monitoring-completion-10')).toHaveTextContent('후보 3');
    expect(within(card).getByText(/멘토 멘토닉/)).toBeInTheDocument();
  });

  it('re-queries with the status query param when a filter is selected', async () => {
    const fetchMock = vi.fn(async (url: string) => {
      const status = new URL(String(url)).searchParams.get('status');
      return page(status === 'COMPLETED' ? [row({ id: 11, status: 'COMPLETED' })] : [row()]);
    });
    vi.stubGlobal('fetch', fetchMock);
    const user = userEvent.setup();
    renderWithProviders(<AdminMonitoringPage />, { auth: { token: 't', role: 'ADMIN' } });

    // 초기 조회는 status 없이 전체.
    await screen.findByTestId('monitoring-card-10');
    expect(new URL(String(fetchMock.mock.calls[0][0])).searchParams.get('status')).toBeNull();

    await user.click(screen.getByTestId('monitoring-filter-COMPLETED'));
    expect(await screen.findByTestId('monitoring-card-11')).toBeInTheDocument();
    await waitFor(() => {
      const last = fetchMock.mock.calls.at(-1);
      expect(new URL(String(last?.[0])).searchParams.get('status')).toBe('COMPLETED');
    });
  });

  it('shows an empty message when no meeting matches', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => page([])),
    );
    renderWithProviders(<AdminMonitoringPage />, { auth: { token: 't', role: 'ADMIN' } });

    expect(await screen.findByTestId('monitoring-empty')).toBeInTheDocument();
  });

  it('surfaces an API error', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () =>
        errorResponse(403, 'MONITORING_FORBIDDEN', '관리자만 운영 현황을 조회할 수 있습니다.'),
      ),
    );
    renderWithProviders(<AdminMonitoringPage />, { auth: { token: 't', role: 'ADMIN' } });

    expect(await screen.findByTestId('monitoring-error')).toBeInTheDocument();
  });
});
