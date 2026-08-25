import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AdminMonitoringPage } from './AdminMonitoringPage';
import type { MeetingMonitorRow, MenteeCompletionResponse } from '@/api';
import { errorResponse, jsonResponse, renderWithProviders } from '@/test/test-utils';

afterEach(() => vi.unstubAllGlobals());

/** Route fetch by URL: monitoring rows and per-meeting completions differ by endpoint. */
function stubFetch(monitoring: MeetingMonitorRow[], completions: MenteeCompletionResponse[] = []) {
  vi.stubGlobal(
    'fetch',
    vi.fn((url: string) => {
      const u = String(url);
      if (u.includes('/api/admin/monitoring')) return Promise.resolve(jsonResponse(monitoring));
      if (u.includes('/completions')) return Promise.resolve(jsonResponse(completions));
      return Promise.resolve(jsonResponse([]));
    }),
  );
}

function row(overrides: Partial<MeetingMonitorRow> & Pick<MeetingMonitorRow, 'meetingId'>): MeetingMonitorRow {
  return {
    title: `모임${overrides.meetingId}`,
    mentorId: 10,
    status: 'IN_PROGRESS',
    mentorCompletionStatus: 'PENDING',
    capacity: 6,
    applicantCount: 4,
    participantCount: 4,
    attendanceRatePercent: 75,
    completionCandidates: 0,
    completedCount: 0,
    ...overrides,
  };
}

const sampleRows: MeetingMonitorRow[] = [
  row({ meetingId: 1, title: 'React 스터디', status: 'IN_PROGRESS', completionCandidates: 2, completedCount: 1 }),
  row({
    meetingId: 2,
    title: 'CS 완료반',
    status: 'COMPLETED',
    mentorCompletionStatus: 'COMPLETED',
    capacity: 5,
    applicantCount: 5,
    completedCount: 3,
  }),
];

describe('AdminMonitoringPage', () => {
  it('renders an empty state when there are no meetings', async () => {
    stubFetch([]);
    renderWithProviders(<AdminMonitoringPage />, { auth: { token: 't', role: 'ADMIN' } });
    expect(await screen.findByTestId('admin-monitoring-empty')).toBeInTheDocument();
    expect(screen.getByTestId('queue-pending-total')).toHaveTextContent('0');
  });

  it('derives queue counts from monitoring rows and shows 정원/신청/수료', async () => {
    const rows: MeetingMonitorRow[] = [
      row({ meetingId: 1, status: 'PENDING_APPROVAL', capacity: 8, applicantCount: 0 }),
      row({ meetingId: 2, status: 'RECRUITING' }),
      row({
        meetingId: 3,
        title: 'React 스터디',
        status: 'IN_PROGRESS',
        capacity: 6,
        applicantCount: 4,
        completionCandidates: 2,
        completedCount: 1,
      }),
    ];
    stubFetch(rows);

    renderWithProviders(<AdminMonitoringPage />, { auth: { token: 't', role: 'ADMIN' } });

    const r3 = await screen.findByTestId('monitor-row-3');
    expect(r3).toHaveTextContent('React 스터디');
    expect(r3).toHaveTextContent('진행중');
    // 정원/신청/수료 = 6 / 4 / 1 (공백 포함).
    expect(r3).toHaveTextContent('6 / 4 / 1');
    // 참여·출석율 열은 메인 표에서 제외됨(상세 다이얼로그에만 표시).
    expect(screen.queryByTestId('monitor-rate-3')).not.toBeInTheDocument();

    // 카운트는 상태별 행 수 + 수료 후보 합계로 계산되어 승인 큐 화면과 동기화된다.
    expect(screen.getByTestId('queue-count-creation')).toHaveTextContent('1');
    expect(screen.getByTestId('queue-count-recruitConfirm')).toHaveTextContent('1');
    expect(screen.getByTestId('queue-count-meetingComplete')).toHaveTextContent('1');
    expect(screen.getByTestId('queue-count-menteeComplete')).toHaveTextContent('2');
    expect(screen.getByTestId('queue-pending-total')).toHaveTextContent('5');
  });

  it('filters rows by mentor completion status (완료 현황)', async () => {
    stubFetch(sampleRows);
    const user = userEvent.setup();
    renderWithProviders(<AdminMonitoringPage />, { auth: { token: 't', role: 'ADMIN' } });

    // Both rows visible initially.
    expect(await screen.findByTestId('monitor-row-1')).toBeInTheDocument();
    expect(screen.getByTestId('monitor-row-2')).toBeInTheDocument();

    // Filter to mentor completion = 수료(COMPLETED) → only the completed meeting remains.
    await user.selectOptions(screen.getByTestId('filter-mentor-completion'), 'COMPLETED');
    expect(screen.queryByTestId('monitor-row-1')).not.toBeInTheDocument();
    expect(screen.getByTestId('monitor-row-2')).toBeInTheDocument();
    expect(screen.getByTestId('filter-count')).toHaveTextContent('1/2건');
  });

  it('filters rows by title search', async () => {
    stubFetch(sampleRows);
    const user = userEvent.setup();
    renderWithProviders(<AdminMonitoringPage />, { auth: { token: 't', role: 'ADMIN' } });

    await screen.findByTestId('monitor-row-1');
    await user.type(screen.getByTestId('filter-search'), 'CS');
    expect(screen.queryByTestId('monitor-row-1')).not.toBeInTheDocument();
    expect(screen.getByTestId('monitor-row-2')).toBeInTheDocument();
  });

  it('opens a detail dialog with per-mentee completions when a row is clicked', async () => {
    const completions: MenteeCompletionResponse[] = [
      { meetingId: 2, menteeId: 21, status: 'COMPLETED', attendedCount: 4, totalScheduled: 4 },
      {
        meetingId: 2,
        menteeId: 22,
        status: 'COMPLETION_CANDIDATE',
        attendedCount: 3,
        totalScheduled: 4,
      },
    ];
    stubFetch(sampleRows, completions);
    const user = userEvent.setup();
    renderWithProviders(<AdminMonitoringPage />, { auth: { token: 't', role: 'ADMIN' } });

    await user.click(await screen.findByTestId('monitor-row-2'));

    const detail = await screen.findByTestId('monitor-detail');
    expect(detail).toHaveTextContent('CS 완료반');
    expect(await screen.findByTestId('detail-completion-21')).toHaveTextContent('수료 확정');
    expect(screen.getByTestId('detail-completion-22')).toHaveTextContent('수료 후보');
  });

  it('renders an error state when the request fails', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(errorResponse(500, 'INTERNAL_ERROR', '서버 오류')),
    );
    renderWithProviders(<AdminMonitoringPage />, { auth: { token: 't', role: 'ADMIN' } });
    expect(await screen.findByTestId('admin-monitoring-error')).toBeInTheDocument();
  });
});
