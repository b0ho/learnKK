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

describe('AdminApprovalPage', () => {
  it('approves a pending meeting and reflects the new status', async () => {
    const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
      if (String(url).endsWith('/approve') && init?.method === 'POST') {
        return jsonResponse(meeting('RECRUITING'));
      }
      return jsonResponse(meeting('PENDING_APPROVAL'));
    });
    vi.stubGlobal('fetch', fetchMock);
    const user = userEvent.setup();

    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });
    await lookup(user);

    expect(await screen.findByTestId('admin-approve')).toBeInTheDocument();
    await user.click(screen.getByTestId('admin-approve'));

    await waitFor(() =>
      expect(screen.getByTestId('admin-meeting-status')).toHaveTextContent('모집중'),
    );
  });

  it('rejects via the reason dialog', async () => {
    const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
      if (String(url).endsWith('/reject') && init?.method === 'POST') {
        return jsonResponse(meeting('REJECTED', '기준 미달'));
      }
      return jsonResponse(meeting('PENDING_APPROVAL'));
    });
    vi.stubGlobal('fetch', fetchMock);
    const user = userEvent.setup();

    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });
    await lookup(user);

    await user.click(await screen.findByTestId('admin-reject-open'));
    await user.type(await screen.findByTestId('reject-reason'), '기준 미달');
    await user.click(screen.getByTestId('admin-reject-confirm'));

    await waitFor(() =>
      expect(screen.getByTestId('admin-meeting-status')).toHaveTextContent('반려'),
    );
  });

  it('surfaces a 409 invalid-transition error', async () => {
    const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
      if (String(url).endsWith('/approve') && init?.method === 'POST') {
        return errorResponse(409, 'MEETING_INVALID_TRANSITION', '현재 상태에서는 승인할 수 없습니다.');
      }
      return jsonResponse(meeting('PENDING_APPROVAL'));
    });
    vi.stubGlobal('fetch', fetchMock);
    const user = userEvent.setup();

    renderWithProviders(<AdminApprovalPage />, { auth: { token: 't', role: 'ADMIN' } });
    await lookup(user);
    await user.click(await screen.findByTestId('admin-approve'));

    expect(await screen.findByTestId('admin-action-error')).toHaveTextContent(
      '현재 상태에서는 승인할 수 없습니다.',
    );
  });
});
