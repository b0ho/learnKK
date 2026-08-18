import type { ReactElement, ReactNode } from 'react';
import { render, type RenderOptions } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '@/auth/AuthProvider';
import { setSession, type Role } from '@/api';

interface ProvidersOptions extends Omit<RenderOptions, 'wrapper'> {
  /** Initial router entry, e.g. '/meetings'. */
  route?: string;
  /** Seed an authenticated session before rendering. */
  auth?: { token: string; role: Role };
}

export function renderWithProviders(ui: ReactElement, options: ProvidersOptions = {}) {
  const { route = '/', auth, ...renderOptions } = options;

  if (auth) {
    setSession(auth.token, auth.role);
  }

  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <MemoryRouter
        initialEntries={[route]}
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <AuthProvider>{children}</AuthProvider>
      </MemoryRouter>
    );
  }

  return render(ui, { wrapper: Wrapper, ...renderOptions });
}

/** Build a fetch Response with a JSON body. */
export function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

/** Build an empty (204) fetch Response. */
export function emptyResponse(status = 204): Response {
  return new Response(null, { status });
}

/** Build an ErrorPayload fetch Response. */
export function errorResponse(status: number, code: string, message: string): Response {
  return jsonResponse({ code, message }, status);
}
