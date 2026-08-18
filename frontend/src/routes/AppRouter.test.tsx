import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { AppRouter } from './AppRouter';
import type { MeetingSummary, PageResponse } from '@/api';
import { jsonResponse, renderWithProviders } from '@/test/test-utils';

afterEach(() => vi.unstubAllGlobals());

const emptyPage: PageResponse<MeetingSummary> = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
};

describe('AppRouter guards', () => {
  it('redirects unauthenticated users to the login page', () => {
    vi.stubGlobal('fetch', vi.fn());
    renderWithProviders(<AppRouter />, { route: '/meetings' });
    expect(screen.getByTestId('login-submit')).toBeInTheDocument();
  });

  it('renders the app shell with three tabs when authenticated', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(emptyPage)));
    renderWithProviders(<AppRouter />, { route: '/meetings', auth: { token: 't', role: 'MENTEE' } });

    expect(await screen.findByTestId('meetings-empty')).toBeInTheDocument();
    expect(screen.getByTestId('tab-meetings')).toBeInTheDocument();
    expect(screen.getByTestId('tab-my-learning')).toBeInTheDocument();
    expect(screen.getByTestId('tab-profile')).toBeInTheDocument();
  });

  it('blocks a mentee from the mentor-only create route', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(emptyPage)));
    renderWithProviders(<AppRouter />, {
      route: '/meetings/new',
      auth: { token: 't', role: 'MENTEE' },
    });
    // RequireRole bounces to the meetings tab.
    expect(await screen.findByTestId('meetings-empty')).toBeInTheDocument();
    expect(screen.queryByTestId('meeting-submit')).not.toBeInTheDocument();
  });

  it('blocks a non-admin from the admin approval route', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(emptyPage)));
    renderWithProviders(<AppRouter />, {
      route: '/admin/meetings',
      auth: { token: 't', role: 'MENTOR' },
    });
    expect(await screen.findByTestId('meetings-empty')).toBeInTheDocument();
    expect(screen.queryByTestId('admin-lookup')).not.toBeInTheDocument();
  });

  it('allows a mentor into the create route', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(emptyPage)));
    renderWithProviders(<AppRouter />, {
      route: '/meetings/new',
      auth: { token: 't', role: 'MENTOR' },
    });
    expect(await screen.findByTestId('meeting-submit')).toBeInTheDocument();
  });
});
