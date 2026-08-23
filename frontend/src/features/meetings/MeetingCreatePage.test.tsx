import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MeetingCreatePage } from './MeetingCreatePage';
import type { MeetingResponse } from '@/api';
import { jsonResponse, renderWithProviders } from '@/test/test-utils';

afterEach(() => vi.unstubAllGlobals());

const meeting: MeetingResponse = {
  id: 42,
  mentorId: 1,
  title: 'React 스터디',
  topic: 'FE',
  weeks: 4,
  recruitStart: null,
  recruitEnd: null,
  capacity: 6,
  format: null,
  initialContent: null,
  status: 'PENDING_APPROVAL',
  rejectReason: null,
};

describe('MeetingCreatePage', () => {
  it('blocks submission when required fields are invalid', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    const user = userEvent.setup();

    renderWithProviders(<MeetingCreatePage />, { auth: { token: 't', role: 'MENTOR' } });
    await user.click(screen.getByTestId('meeting-submit'));

    expect(await screen.findByTestId('meeting-title-error')).toBeInTheDocument();
    expect(screen.getByTestId('meeting-weeks-error')).toBeInTheDocument();
    expect(screen.getByTestId('meeting-capacity-error')).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('creates a meeting with survey questions and confirms pending approval', async () => {
    const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
      if (String(url).endsWith('/questions') && init?.method === 'PUT') {
        return jsonResponse([{ orderNo: 1, text: '경력?', type: 'SHORT_TEXT', options: [], required: true }]);
      }
      return jsonResponse(meeting, 201);
    });
    vi.stubGlobal('fetch', fetchMock);
    const user = userEvent.setup();

    renderWithProviders(<MeetingCreatePage />, { auth: { token: 't', role: 'MENTOR' } });
    await user.type(screen.getByTestId('meeting-title'), 'React 스터디');
    await user.type(screen.getByTestId('meeting-weeks'), '4');
    await user.type(screen.getByTestId('meeting-capacity'), '6');

    await user.click(screen.getByTestId('survey-add'));
    await user.type(screen.getByTestId('survey-text-0'), '경력?');

    await user.click(screen.getByTestId('meeting-submit'));

    expect(await screen.findByTestId('create-success')).toHaveTextContent('#42');

    const calledUrls = fetchMock.mock.calls.map((c) => String(c[0]));
    expect(calledUrls.some((u) => u.endsWith('/api/meetings'))).toBe(true);
    expect(calledUrls.some((u) => u.endsWith('/api/meetings/42/questions'))).toBe(true);
  });
});
