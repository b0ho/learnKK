import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { MyLearningPage } from './MyLearningPage';
import type { MeetingSummary, PageResponse } from '@/api';
import { errorResponse, jsonResponse, renderWithProviders } from '@/test/test-utils';

afterEach(() => vi.unstubAllGlobals());

function page(content: MeetingSummary[]): PageResponse<MeetingSummary> {
  return { content, page: 0, size: 50, totalElements: content.length, totalPages: 1 };
}

describe('MyLearningPage', () => {
  it('shows the mentee placeholder for mentees', () => {
    vi.stubGlobal('fetch', vi.fn());
    renderWithProviders(<MyLearningPage />, { auth: { token: 't', role: 'MENTEE' } });
    expect(screen.getByTestId('my-learning-placeholder')).toBeInTheDocument();
  });

  it('lists the mentor own meetings with status and next action', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse(
          page([
            { id: 3, title: '내 모임', topic: null, weeks: 4, capacity: 6, status: 'RECRUITING' },
          ]),
        ),
      ),
    );

    renderWithProviders(<MyLearningPage />, { auth: { token: 't', role: 'MENTOR' } });

    expect(await screen.findByTestId('mentor-meeting-3')).toHaveTextContent('내 모임');
    expect(screen.getByTestId('mentor-meeting-status-3')).toHaveTextContent('모집중');
    expect(screen.getByTestId('mentor-meeting-next-3')).toHaveTextContent('모집 확정');
  });

  it('shows the empty state when the mentor has no meetings', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(page([]))));
    renderWithProviders(<MyLearningPage />, { auth: { token: 't', role: 'MENTOR' } });
    expect(await screen.findByTestId('mentor-hub-empty')).toBeInTheDocument();
  });

  it('shows an error state when the listing fails', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(errorResponse(403, 'MEETING_FORBIDDEN', '권한이 없습니다.')),
    );
    renderWithProviders(<MyLearningPage />, { auth: { token: 't', role: 'MENTOR' } });
    expect(await screen.findByTestId('mentor-hub-error')).toHaveTextContent('권한이 없습니다.');
  });
});
