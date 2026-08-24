import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AdminApprovalPage } from './AdminApprovalPage';
import type { MeetingStatus, MeetingSummary } from '@/api';
import { errorResponse, jsonResponse, renderWithProviders } from '@/test/test-utils';

afterEach(() => vi.unstubAllGlobals());

function summary(status: MeetingStatus): MeetingSummary {
  return { id: 7, title: '모임 제목', topic: null, weeks: 4, capacity: 6, status };
}

function page(content: MeetingSummary[]) {
  return jsonResponse({
    content,
    page: 0,
    size: 100,
    totalElements: content.length,
    totalPages: 1,
  });
}

/**
 * Fetch mock: the admin queue GETs `/api/admin/meetings?status=X` per section — return the meeting
 * only for `showIn`. A POST to `/{action}` returns `nextStatus` (used for reload assertions).
 */
function queueFetch(showIn: MeetingStatus, opts: { completions?: unknown[] } = {}) {
  return vi.fn(async (url: string) => {
    const u = String(url);
    if (u.includes('/api/admin/meetings?status=')) {
      const status = new URL(u).searchParams.get('status');
      return page(status === showIn ? [summary(showIn)] : []);
    }
    if (u.endsWith('/completions')) return jsonResponse(opts.completions ?? []);
    // any POST transition
    return jsonResponse({});
  });
}

describe('AdminApprovalPage (queue)', () => {
  it('lists a pending meeting in its section with approve/reject actions', async () => {
    vi.stubGlobal('fetch', queueFetch('PENDING_APPROVAL'));
    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });

    expect(await screen.findByTestId('admin-approve-7')).toBeInTheDocument();
    expect(screen.getByTestId('admin-reject-7')).toBeInTheDocument();
    // no forward action from another section
    expect(screen.queryByTestId('admin-start-7')).not.toBeInTheDocument();
  });

  it('approve requires a confirm dialog before firing the request', async () => {
    const fetchMock = queueFetch('PENDING_APPROVAL');
    vi.stubGlobal('fetch', fetchMock);
    const user = userEvent.setup();
    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });

    await user.click(await screen.findByTestId('admin-approve-7'));
    // confirm dialog appears; no POST yet
    expect(await screen.findByTestId('confirm-dialog')).toBeInTheDocument();
    const postsBefore = fetchMock.mock.calls.filter((c) => c[1]?.method === 'POST').length;
    expect(postsBefore).toBe(0);

    await user.click(screen.getByTestId('confirm-ok'));
    await waitFor(() => {
      const posts = fetchMock.mock.calls.filter(
        (c) => String(c[0]).endsWith('/approve') && c[1]?.method === 'POST',
      );
      expect(posts.length).toBe(1);
    });
  });

  it('rejects via the reason dialog', async () => {
    const fetchMock = queueFetch('PENDING_APPROVAL');
    vi.stubGlobal('fetch', fetchMock);
    const user = userEvent.setup();
    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });

    await user.click(await screen.findByTestId('admin-reject-7'));
    await user.type(await screen.findByTestId('reason-input'), '기준 미달');
    await user.click(screen.getByTestId('reason-confirm'));

    await waitFor(() => {
      const posts = fetchMock.mock.calls.filter(
        (c) => String(c[0]).endsWith('/reject') && c[1]?.method === 'POST',
      );
      expect(posts.length).toBe(1);
    });
  });

  it('shows a revert action for a recruiting meeting and fires revert after confirm', async () => {
    const fetchMock = queueFetch('RECRUITING');
    vi.stubGlobal('fetch', fetchMock);
    const user = userEvent.setup();
    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });

    expect(await screen.findByTestId('admin-revert-7')).toBeInTheDocument();
    await user.click(screen.getByTestId('admin-revert-7'));
    await user.click(await screen.findByTestId('confirm-ok'));

    await waitFor(() => {
      const posts = fetchMock.mock.calls.filter(
        (c) => String(c[0]).endsWith('/revert') && c[1]?.method === 'POST',
      );
      expect(posts.length).toBe(1);
    });
  });

  it('renders the completion panel for an in-progress meeting', async () => {
    vi.stubGlobal('fetch', queueFetch('IN_PROGRESS'));
    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });

    expect(await screen.findByTestId('admin-completion-7')).toBeInTheDocument();
    expect(screen.getByTestId('admin-complete-7')).toBeInTheDocument();
  });

  it('validates a required reason before rejecting', async () => {
    vi.stubGlobal('fetch', queueFetch('PENDING_APPROVAL'));
    const user = userEvent.setup();
    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });

    await user.click(await screen.findByTestId('admin-reject-7'));
    await user.click(await screen.findByTestId('reason-confirm'));
    expect(await screen.findByTestId('reason-error')).toBeInTheDocument();
  });
});

describe('AdminApprovalPage — completion ④', () => {
  it('computes candidates and approves a candidate', async () => {
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
      if (u.includes("/api/admin/meetings?status=")) {
        const status = new URL(u, 'http://x').searchParams.get('status');
        return page(status === 'IN_PROGRESS' ? [summary('IN_PROGRESS')] : []);
      }
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
      return jsonResponse({});
    });
    vi.stubGlobal('fetch', fetchMock);
    const user = userEvent.setup();
    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });

    await user.click(await screen.findByTestId('admin-completion-compute-7'));
    expect(await screen.findByTestId('admin-completion-status-7-2')).toHaveTextContent('수료후보');

    await user.click(screen.getByTestId('admin-completion-approve-7-2'));
    await waitFor(() =>
      expect(screen.getByTestId('admin-completion-status-7-2')).toHaveTextContent('수료확정'),
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
      if (u.includes("/api/admin/meetings?status=")) {
        const status = new URL(u, 'http://x').searchParams.get('status');
        return page(status === 'IN_PROGRESS' ? [summary('IN_PROGRESS')] : []);
      }
      if (u.includes('/completions/2/approve') && init?.method === 'POST') {
        return errorResponse(409, 'COMPLETION_ALREADY_APPROVED', '이미 확정됨');
      }
      if (u.endsWith('/completions')) return jsonResponse([candidate]);
      return jsonResponse({});
    });
    vi.stubGlobal('fetch', fetchMock);
    const user = userEvent.setup();
    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });

    await user.click(await screen.findByTestId('admin-completion-approve-7-2'));
    expect(await screen.findByTestId('admin-completion-action-error-7')).toHaveTextContent(
      '이미 수료 확정된 멘티입니다',
    );
  });
});
