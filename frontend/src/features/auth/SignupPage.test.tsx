import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { SignupPage } from './SignupPage';
import { errorResponse, jsonResponse, renderWithProviders } from '@/test/test-utils';

afterEach(() => vi.unstubAllGlobals());

describe('SignupPage', () => {
  it('validates employeeNo format and password length', async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    const user = userEvent.setup();

    renderWithProviders(<SignupPage />, { route: '/signup' });
    await user.type(screen.getByTestId('signup-employeeNo'), 'ab');
    await user.type(screen.getByTestId('signup-nickname'), '길동');
    await user.type(screen.getByTestId('signup-password'), '123');
    await user.click(screen.getByTestId('signup-submit'));

    expect(await screen.findByTestId('signup-employeeNo-error')).toBeInTheDocument();
    expect(screen.getByTestId('signup-password-error')).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('submits a valid signup and shows success', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValue(
          jsonResponse({ id: 1, nickname: '길동', employeeNo: 'EMP1234', role: 'MENTEE' }, 201),
        ),
    );
    const user = userEvent.setup();

    renderWithProviders(<SignupPage />, { route: '/signup' });
    await user.type(screen.getByTestId('signup-employeeNo'), 'EMP1234');
    await user.type(screen.getByTestId('signup-nickname'), '길동');
    await user.type(screen.getByTestId('signup-password'), 'password1');
    await user.click(screen.getByTestId('signup-submit'));

    expect(await screen.findByTestId('signup-success')).toBeInTheDocument();
  });

  it('maps a 409 duplicate to the server message', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(errorResponse(409, 'DUPLICATE_EMPLOYEE_NO', '이미 등록된 사번입니다.')),
    );
    const user = userEvent.setup();

    renderWithProviders(<SignupPage />, { route: '/signup' });
    await user.type(screen.getByTestId('signup-employeeNo'), 'EMP1234');
    await user.type(screen.getByTestId('signup-nickname'), '길동');
    await user.type(screen.getByTestId('signup-password'), 'password1');
    await user.click(screen.getByTestId('signup-submit'));

    expect(await screen.findByTestId('signup-error')).toHaveTextContent('이미 등록된 사번입니다.');
  });

  it('does not offer an ADMIN role option', () => {
    vi.stubGlobal('fetch', vi.fn());
    renderWithProviders(<SignupPage />, { route: '/signup' });
    expect(screen.getByTestId('role-mentee')).toBeInTheDocument();
    expect(screen.getByTestId('role-mentor')).toBeInTheDocument();
    expect(screen.queryByText('관리자')).not.toBeInTheDocument();
  });
});
