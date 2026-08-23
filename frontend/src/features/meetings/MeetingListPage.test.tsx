import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
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
});
