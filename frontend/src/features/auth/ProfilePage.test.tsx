import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ProfilePage } from './ProfilePage';
import type { ProfileResponse } from '@/api';
import { emptyResponse, jsonResponse, renderWithProviders } from '@/test/test-utils';

afterEach(() => vi.unstubAllGlobals());

const profile: ProfileResponse = {
  nickname: '길동',
  employeeNo: 'EMP1234',
  tags: ['java'],
  intro: '안녕하세요',
};

describe('ProfilePage', () => {
  it('loads and displays the current profile', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(profile)));
    renderWithProviders(<ProfilePage />, { auth: { token: 't', role: 'MENTEE' } });

    expect(await screen.findByTestId('profile-nickname')).toHaveTextContent('길동');
    expect(screen.getByTestId('profile-employeeNo')).toHaveTextContent('EMP1234');
    expect(screen.getByTestId('profile-role')).toHaveTextContent('멘티');
  });

  it('adds a tag and saves the profile', async () => {
    const fetchMock = vi.fn(async (_url: string, init?: RequestInit) => {
      if (init?.method === 'PUT') {
        return jsonResponse({ ...profile, tags: ['java', 'react'] });
      }
      return jsonResponse(profile);
    });
    vi.stubGlobal('fetch', fetchMock);
    const user = userEvent.setup();

    renderWithProviders(<ProfilePage />, { auth: { token: 't', role: 'MENTEE' } });
    await screen.findByTestId('profile-nickname');

    await user.type(screen.getByTestId('profile-tag-input'), 'react');
    await user.click(screen.getByTestId('profile-tag-add'));
    await user.click(screen.getByTestId('profile-save'));

    expect(await screen.findByTestId('profile-saved')).toBeInTheDocument();
    const putCall = fetchMock.mock.calls.find((c) => (c[1] as RequestInit)?.method === 'PUT');
    expect(putCall).toBeDefined();
    expect(String((putCall![1] as RequestInit).body)).toContain('react');
  });

  it('logs out and clears the session', async () => {
    const fetchMock = vi.fn(async (_url: string, init?: RequestInit) => {
      if (init?.method === 'POST') {
        return emptyResponse(204);
      }
      return jsonResponse(profile);
    });
    vi.stubGlobal('fetch', fetchMock);
    const user = userEvent.setup();

    renderWithProviders(<ProfilePage />, { auth: { token: 't', role: 'MENTEE' } });
    await screen.findByTestId('profile-nickname');

    await user.click(screen.getByTestId('logout-button'));

    await waitFor(() => expect(sessionStorage.getItem('learnkk.token')).toBeNull());
  });
});
