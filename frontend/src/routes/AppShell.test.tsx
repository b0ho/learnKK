import { afterEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import { AppShell } from './AppShell';
import { jsonResponse, renderWithProviders } from '@/test/test-utils';

afterEach(() => vi.unstubAllGlobals());

function renderShell() {
  return renderWithProviders(
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/messages" element={<div>쪽지함</div>} />
      </Route>
    </Routes>,
    { auth: { token: 't', role: 'MENTEE' }, route: '/messages' },
  );
}

describe('AppShell — messaging tab', () => {
  it('shows the unread badge when the poll returns a positive count', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() => Promise.resolve(jsonResponse({ count: 3 }))),
    );

    renderShell();

    expect(await screen.findByTestId('unread-badge')).toHaveTextContent('3');
    expect(screen.getByTestId('tab-messages')).toBeInTheDocument();
  });

  it('hides the badge when there are no unread messages', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() => Promise.resolve(jsonResponse({ count: 0 }))),
    );

    renderShell();

    // Give the initial poll a tick to resolve.
    await screen.findByTestId('tab-messages');
    expect(screen.queryByTestId('unread-badge')).not.toBeInTheDocument();
  });
});
