import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AdminApprovalPage } from './AdminApprovalPage';
import type { MeetingResponse, MeetingStatus } from '@/api';
import { errorResponse, jsonResponse, renderWithProviders } from '@/test/test-utils';

afterEach(() => vi.unstubAllGlobals());

function meeting(status: MeetingStatus, rejectReason: string | null = null): MeetingResponse {
  return {
    id: 7,
    mentorId: 1,
    title: '모임 제목',
    topic: null,
    weeks: 4,
    recruitStart: null,
    recruitEnd: null,
    capacity: 6,
    format: null,
    initialContent: null,
    status,
    rejectReason,
  };
}

async function lookup(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByTestId('admin-meeting-id'), '7');
  await user.click(screen.getByTestId('admin-lookup'));
}

/** Fetch mock: GET returns a meeting in `initial`; a POST to `/{action}` returns `next`. */
function transitionFetch(initial: MeetingStatus, action: string, next: MeetingResponse) {
  return vi.fn(async (url: string, init?: RequestInit) => {
    if (String(url).endsWith(`/${action}`) && init?.method === 'POST') {
      return jsonResponse(next);
    }
    return jsonResponse(meeting(initial));
  });
}

describe('AdminApprovalPage', () => {
  it('shows approve/reject actions only for a pending meeting', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(meeting('PENDING_APPROVAL'))));
    const user = userEvent.setup();
    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });
    await lookup(user);

    expect(await screen.findByTestId('admin-approve')).toBeInTheDocument();
    expect(screen.getByTestId('admin-reject-open')).toBeInTheDocument();
    expect(screen.queryByTestId('admin-confirm-recruitment')).not.toBeInTheDocument();
  });

  it('approves a pending meeting and reflects the new status', async () => {
    vi.stubGlobal('fetch', transitionFetch('PENDING_APPROVAL', 'approve', meeting('RECRUITING')));
    const user = userEvent.setup();
    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });
    await lookup(user);

    await user.click(await screen.findByTestId('admin-approve'));
    await waitFor(() =>
      expect(screen.getByTestId('admin-meeting-status')).toHaveTextContent('모집중'),
    );
  });

  it('rejects via the reason dialog', async () => {
    vi.stubGlobal(
      'fetch',
      transitionFetch('PENDING_APPROVAL', 'reject', meeting('REJECTED', '기준 미달')),
    );
    const user = userEvent.setup();
    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });
    await lookup(user);

    await user.click(await screen.findByTestId('admin-reject-open'));
    await user.type(await screen.findByTestId('reason-input'), '기준 미달');
    await user.click(screen.getByTestId('reason-confirm'));

    await waitFor(() =>
      expect(screen.getByTestId('admin-meeting-status')).toHaveTextContent('반려'),
    );
  });

  it('confirms recruitment (T3) for a recruiting meeting', async () => {
    vi.stubGlobal(
      'fetch',
      transitionFetch('RECRUITING', 'confirm-recruitment', meeting('READY_TO_START')),
    );
    const user = userEvent.setup();
    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });
    await lookup(user);

    await user.click(await screen.findByTestId('admin-confirm-recruitment'));
    await waitFor(() =>
      expect(screen.getByTestId('admin-meeting-status')).toHaveTextContent('시작대기'),
    );
  });

  it('cancels recruitment (T4) via the reason dialog', async () => {
    vi.stubGlobal(
      'fetch',
      transitionFetch('RECRUITING', 'confirm-recruitment', meeting('CANCELLED', '정원 미달')),
    );
    const user = userEvent.setup();
    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });
    await lookup(user);

    await user.click(await screen.findByTestId('admin-cancel-recruitment-open'));
    await user.type(await screen.findByTestId('reason-input'), '정원 미달');
    await user.click(screen.getByTestId('reason-confirm'));

    await waitFor(() =>
      expect(screen.getByTestId('admin-meeting-status')).toHaveTextContent('취소'),
    );
  });

  it('starts a ready meeting (T5)', async () => {
    vi.stubGlobal(
      'fetch',
      transitionFetch('READY_TO_START', 'approve-start', meeting('IN_PROGRESS')),
    );
    const user = userEvent.setup();
    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });
    await lookup(user);

    await user.click(await screen.findByTestId('admin-approve-start'));
    await waitFor(() =>
      expect(screen.getByTestId('admin-meeting-status')).toHaveTextContent('진행중'),
    );
  });

  it('completes an in-progress meeting (T6)', async () => {
    vi.stubGlobal('fetch', transitionFetch('IN_PROGRESS', 'complete', meeting('COMPLETED')));
    const user = userEvent.setup();
    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });
    await lookup(user);

    await user.click(await screen.findByTestId('admin-complete'));
    await waitFor(() =>
      expect(screen.getByTestId('admin-meeting-status')).toHaveTextContent('완료'),
    );
  });

  it('shows no actions for a terminal (completed) meeting', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(meeting('COMPLETED'))));
    const user = userEvent.setup();
    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });
    await lookup(user);

    expect(await screen.findByTestId('admin-no-action')).toBeInTheDocument();
  });

  it('surfaces a 409 sessions-not-ended error on complete', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        if (String(url).endsWith('/complete') && init?.method === 'POST') {
          return errorResponse(
            409,
            'MEETING_SESSIONS_NOT_ENDED',
            '모든 세션이 종료되어야 완료할 수 있습니다.',
          );
        }
        return jsonResponse(meeting('IN_PROGRESS'));
      }),
    );
    const user = userEvent.setup();
    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });
    await lookup(user);

    await user.click(await screen.findByTestId('admin-complete'));
    expect(await screen.findByTestId('admin-action-error')).toHaveTextContent(
      '모든 세션이 종료되어야 완료할 수 있습니다.',
    );
  });

  it('validates a required reason before cancelling recruitment', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(meeting('RECRUITING'))));
    const user = userEvent.setup();
    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });
    await lookup(user);

    await user.click(await screen.findByTestId('admin-cancel-recruitment-open'));
    await user.click(await screen.findByTestId('reason-confirm'));
    expect(await screen.findByTestId('reason-error')).toBeInTheDocument();
  });
});

describe('AdminApprovalPage — completion ④', () => {
  it('computes candidates and approves a candidate for an in-progress meeting', async () => {
    let approved = false;
    const candidate = {
      meetingId: 7,
      menteeId: 2,
      status: 'COMPLETION_CANDIDATE' as const,
      attendedCount: 4,
      totalScheduled: 5,
      approvedAt: null,
    };
    const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
      const u = String(url);
      if (u.endsWith('/completions/compute') && init?.method === 'POST') {
        return jsonResponse([candidate]);
      }
      if (u.includes('/completions/2/approve') && init?.method === 'POST') {
        approved = true;
        return jsonResponse({ ...candidate, status: 'COMPLETED', approvedAt: 'x' });
      }
      if (u.endsWith('/completions')) {
        return jsonResponse(approved ? [{ ...candidate, status: 'COMPLETED', approvedAt: 'x' }] : []);
      }
      return jsonResponse(meeting('IN_PROGRESS'));
    });
    vi.stubGlobal('fetch', fetchMock);
    const user = userEvent.setup();
    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });
    await lookup(user);

    // Initially empty until judged.
    expect(await screen.findByTestId('admin-completion-empty')).toBeInTheDocument();

    await user.click(screen.getByTestId('admin-completion-compute'));
    expect(await screen.findByTestId('admin-completion-status-2')).toHaveTextContent('수료후보');

    await user.click(screen.getByTestId('admin-completion-approve-2'));
    await waitFor(() =>
      expect(screen.getByTestId('admin-completion-status-2')).toHaveTextContent('수료확정'),
    );
  });

  it('surfaces a 409 already-approved error on approve', async () => {
    const candidate = {
      meetingId: 7,
      menteeId: 2,
      status: 'COMPLETION_CANDIDATE' as const,
      attendedCount: 5,
      totalScheduled: 5,
      approvedAt: null,
    };
    const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
      const u = String(url);
      if (u.includes('/completions/2/approve') && init?.method === 'POST') {
        return errorResponse(409, 'COMPLETION_ALREADY_APPROVED', '이미 확정됨');
      }
      if (u.endsWith('/completions')) return jsonResponse([candidate]);
      return jsonResponse(meeting('IN_PROGRESS'));
    });
    vi.stubGlobal('fetch', fetchMock);
    const user = userEvent.setup();
    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });
    await lookup(user);

    await user.click(await screen.findByTestId('admin-completion-approve-2'));
    expect(await screen.findByTestId('admin-completion-action-error')).toHaveTextContent(
      '이미 수료 확정된 멘티입니다',
    );
  });
});
