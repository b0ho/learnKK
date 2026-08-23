import { Navigate, Route, Routes } from 'react-router-dom';
import { LoginPage } from '@/features/auth/LoginPage';
import { SignupPage } from '@/features/auth/SignupPage';
import { ProfilePage } from '@/features/auth/ProfilePage';
import { MeetingListPage } from '@/features/meetings/MeetingListPage';
import { MeetingCreatePage } from '@/features/meetings/MeetingCreatePage';
import { AdminApprovalPage } from '@/features/meetings/AdminApprovalPage';
import { MyLearningPage } from '@/features/meetings/MyLearningPage';
import { AppShell } from './AppShell';
import { RequireAuth } from './RequireAuth';
import { RequireRole } from './RequireRole';
import { PATHS } from './paths';

export function AppRouter() {
  return (
    <Routes>
      <Route path={PATHS.login} element={<LoginPage />} />
      <Route path={PATHS.signup} element={<SignupPage />} />

      <Route element={<RequireAuth />}>
        <Route element={<AppShell />}>
          <Route index element={<Navigate to={PATHS.meetings} replace />} />
          <Route path={PATHS.meetings} element={<MeetingListPage />} />
          <Route element={<RequireRole allow={['MENTOR']} />}>
            <Route path={PATHS.meetingCreate} element={<MeetingCreatePage />} />
          </Route>
          <Route element={<RequireRole allow={['ADMIN']} />}>
            <Route path={PATHS.adminApproval} element={<AdminApprovalPage />} />
          </Route>
          <Route path={PATHS.myLearning} element={<MyLearningPage />} />
          <Route path={PATHS.profile} element={<ProfilePage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to={PATHS.meetings} replace />} />
    </Routes>
  );
}
