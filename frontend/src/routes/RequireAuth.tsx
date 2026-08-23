import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '@/auth/useAuth';
import { PATHS } from './paths';

/** Gate protected routes: redirect unauthenticated users to the login page. */
export function RequireAuth() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to={PATHS.login} replace state={{ from: location.pathname }} />;
  }

  return <Outlet />;
}
