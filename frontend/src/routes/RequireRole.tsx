import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '@/auth/useAuth';
import type { Role } from '@/api';
import { PATHS } from './paths';

/** Restrict a route subtree to specific roles; others bounce to the meetings tab. */
export function RequireRole({ allow }: { allow: Role[] }) {
  const { role } = useAuth();
  if (!role || !allow.includes(role)) {
    return <Navigate to={PATHS.meetings} replace />;
  }
  return <Outlet />;
}
