import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LoginPage } from './LoginPage';
import { getToken } from '@/api';
import { errorResponse, jsonResponse, renderWithProviders } from '@/test/test-utils';

afterEach(() => vi.unstubAllGlobals());

describe('LoginPage', () => {
  it('shows client validation errors and does not call the API', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    const user = userEvent.setup();

    renderWithProviders(<LoginPage />, { route: '/login' });
    await user.click(screen.getByTestId('login-submit'));

    expect(await screen.findByTestId('login-nickname-error')).toBeInTheDocument();
    expect(screen.getByTestId('login-password-error')).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('logs in and stores the session on success', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(jsonResponse({ token: 'tok-1', role: 'MENTOR' })),
    );
    const user = userEvent.setup();

    renderWithProviders(<LoginPage />, { route: '/login' });
    await user.type(screen.getByTestId('login-nickname'), '멘토');
    await user.type(screen.getByTestId('login-password'), 'password1');
    await user.click(screen.getByTestId('login-submit'));

    await waitFor(() => expect(getToken()).toBe('tok-1'));
  });

  it('maps a 401 to the server error message', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValue(
          errorResponse(401, 'AUTH_INVALID_CREDENTIALS', '닉네임 또는 비밀번호가 올바르지 않습니다.'),
        ),
    );
    const user = userEvent.setup();

    renderWithProviders(<LoginPage />, { route: '/login' });
    await user.type(screen.getByTestId('login-nickname'), 'x');
    await user.type(screen.getByTestId('login-password'), 'password1');
    await user.click(screen.getByTestId('login-submit'));

    expect(await screen.findByTestId('login-error')).toHaveTextContent(
      '닉네임 또는 비밀번호가 올바르지 않습니다.',
    );
  });
});
