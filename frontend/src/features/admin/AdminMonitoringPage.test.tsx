import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { AdminMonitoringPage } from './AdminMonitoringPage';
import type { ApprovalQueues, MeetingMonitorRow } from '@/api';
import { errorResponse, jsonResponse, renderWithProviders } from '@/test/test-utils';

afterEach(() => vi.unstubAllGlobals());

const emptyQueues: ApprovalQueues = {
  creation: [],
  recruitConfirm: [],
  start: [],
  meetingComplete: [],
  menteeComplete: [],
};

/** Route fetch by URL: monitoring rows and approval queues live on different endpoints. */
function stubFetch(monitoring: MeetingMonitorRow[], queues: ApprovalQueues) {
  vi.stubGlobal(
    'fetch',
    vi.fn((url: string) => {
      if (String(url).includes('/api/admin/monitoring')) {
        return Promise.resolve(jsonResponse(monitoring));
      }
      return Promise.resolve(jsonResponse(queues));
    }),
  );
}

describe('AdminMonitoringPage', () => {
  it('renders an empty state when there are no meetings', async () => {
    stubFetch([], emptyQueues);
    renderWithProviders(<AdminMonitoringPage />, { auth: { token: 't', role: 'ADMIN' } });
    expect(await screen.findByTestId('admin-monitoring-empty')).toBeInTheDocument();
    // The queue summary still renders with a zero total.
    expect(screen.getByTestId('queue-pending-total')).toHaveTextContent('0');
  });

  it('renders the monitoring table and queue counts on success', async () => {
    const rows: MeetingMonitorRow[] = [
      {
        meetingId: 1,
        title: 'React 스터디',
        mentorId: 10,
        status: 'IN_PROGRESS',
        capacity: 6,
        applicantCount: 4,
        participantCount: 4,
        attendanceRatePercent: 75,
        completionCandidates: 2,
        completedCount: 1,
      },
    ];
    const queues: ApprovalQueues = {
      ...emptyQueues,
      creation: [{ id: 5, title: '새 모임', mentorId: 11, status: 'PENDING_APPROVAL', capacity: 8 }],
    };
    stubFetch(rows, queues);

    renderWithProviders(<AdminMonitoringPage />, { auth: { token: 't', role: 'ADMIN' } });

    const row = await screen.findByTestId('monitor-row-1');
    expect(row).toHaveTextContent('React 스터디');
    expect(row).toHaveTextContent('진행중');
    expect(row).toHaveTextContent('4/6');
    expect(screen.getByTestId('monitor-rate-1')).toHaveTextContent('75%');
    // One creation-queue item -> count badge shows 1, total pending = 1.
    expect(screen.getByTestId('queue-count-creation')).toHaveTextContent('1');
    expect(screen.getByTestId('queue-pending-total')).toHaveTextContent('1');
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
