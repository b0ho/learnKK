import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MyLearningPage } from './MyLearningPage';
import type { MeetingResponse } from '@/api';
import { jsonResponse, renderWithProviders } from '@/test/test-utils';

afterEach(() => vi.unstubAllGlobals());

describe('MyLearningPage', () => {
  it('shows the mentee placeholder for mentees', () => {
    vi.stubGlobal('fetch', vi.fn());
    renderWithProviders(<MyLearningPage />, { auth: { token: 't', role: 'MENTEE' } });
    expect(screen.getByTestId('my-learning-placeholder')).toBeInTheDocument();
  });

  it('renders the mentor hub and looks up a meeting by id', async () => {
    const meeting: MeetingResponse = {
      id: 3,
      mentorId: 1,
      title: '내 모임',
      topic: null,
      weeks: 4,
      recruitStart: null,
      recruitEnd: null,
      capacity: 6,
      format: null,
      initialContent: null,
      status: 'PENDING_APPROVAL',
      rejectReason: null,
    };
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(meeting)));
    const user = userEvent.setup();

    renderWithProviders(<MyLearningPage />, { auth: { token: 't', role: 'MENTOR' } });
    expect(screen.getByTestId('mentor-hub-note')).toBeInTheDocument();

    await user.type(screen.getByTestId('mentor-meeting-id'), '3');
    await user.click(screen.getByTestId('mentor-lookup'));

    expect(await screen.findByTestId('mentor-meeting-detail')).toHaveTextContent('내 모임');
  });
});
