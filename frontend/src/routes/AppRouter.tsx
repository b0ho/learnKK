import { Navigate, Route, Routes } from 'react-router-dom';
import { LoginPage } from '@/features/auth/LoginPage';
import { SignupPage } from '@/features/auth/SignupPage';
import { ProfilePage } from '@/features/auth/ProfilePage';
import { MeetingListPage } from '@/features/meetings/MeetingListPage';
import { MeetingCreatePage } from '@/features/meetings/MeetingCreatePage';
import { AdminApprovalPage } from '@/features/meetings/AdminApprovalPage';
import { AdminMonitoringPage } from '@/features/admin/AdminMonitoringPage';
import { MyLearningPage } from '@/features/meetings/MyLearningPage';
import { MeetingQuestionsEditPage } from '@/features/meetings/MeetingQuestionsEditPage';
import { MeetingContentPage } from '@/features/content/MeetingContentPage';
import { MessagesPage } from '@/features/messaging/MessagesPage';
import { ThreadView } from '@/features/messaging/ThreadView';
import { PreSurveyAnswerPage } from '@/features/survey/PreSurveyAnswerPage';
import { FeedbackPage } from '@/features/survey/FeedbackPage';
import { FeedbackViewPage } from '@/features/survey/FeedbackViewPage';
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
            <Route path="/meetings/:id/questions-edit" element={<MeetingQuestionsEditPage />} />
          </Route>
          <Route element={<RequireRole allow={['ADMIN']} />}>
            <Route path={PATHS.adminApproval} element={<AdminApprovalPage />} />
            <Route path={PATHS.adminMonitoring} element={<AdminMonitoringPage />} />
          </Route>
          <Route path={PATHS.myLearning} element={<MyLearningPage />} />
          <Route path="/meetings/:id/content" element={<MeetingContentPage />} />
          <Route path={PATHS.messages} element={<MessagesPage />} />
          <Route path="/messages/:id" element={<ThreadView />} />
          <Route path="/meetings/:id/survey-answer" element={<PreSurveyAnswerPage />} />
          <Route path="/meetings/:id/feedback" element={<FeedbackPage />} />
          <Route element={<RequireRole allow={['MENTOR', 'ADMIN']} />}>
            <Route path="/meetings/:id/feedback-view" element={<FeedbackViewPage />} />
          </Route>
          <Route path={PATHS.profile} element={<ProfilePage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to={PATHS.meetings} replace />} />
    </Routes>
  );
}
