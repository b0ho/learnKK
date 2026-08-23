import { useEffect, useState } from 'react';
import { meetingsApi, resolveErrorMessage, type MeetingSummary } from '@/api';
import { useAuth } from '@/auth/useAuth';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { meetingStatusLabel, meetingStatusVariant } from '@/features/shared/meetingStatus';
import type { MeetingStatus } from '@/api';

/**
 * "내 러닝" tab. Role-adaptive slice:
 *  - MENTOR: operations hub listing the mentor's own meetings (listMyMeetings) with status and a
 *    next-action hint. Applicant list (U4/Bolt 3) and pre-survey answers (U8/Bolt 7) composition
 *    is deferred — see the placeholder note below.
 *  - MENTEE: application area is not implemented yet (U4/Bolt 3) — guidance only.
 */
export function MyLearningPage() {
  const { role } = useAuth();

  if (role === 'MENTOR') {
    return <MentorHub />;
  }

  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-xl font-bold">내 러닝</h2>
      <Card>
        <CardContent className="pt-4">
          <p className="text-sm text-muted-foreground" data-testid="my-learning-placeholder">
            모임 신청/참여 기능은 다음 단계에서 제공될 예정입니다. 지금은 &lsquo;모임&rsquo; 탭에서
            모집중인 모임을 둘러볼 수 있습니다.
          </p>
        </CardContent>
      </Card>
    </div>
  );
}

/** Mentor-facing next-action guidance keyed by the meeting's current status. */
const NEXT_ACTION: Record<MeetingStatus, string> = {
  PENDING_APPROVAL: '관리자 개설 승인을 기다리고 있습니다.',
  RECRUITING: '모집 중입니다. 관리자의 모집 확정을 기다립니다.',
  READY_TO_START: '모집이 확정되었습니다. 관리자의 시작 승인을 기다립니다.',
  IN_PROGRESS: '진행 중입니다. 모든 세션 종료 후 완료 처리됩니다.',
  COMPLETED: '완료된 모임입니다.',
  REJECTED: '개설이 반려되었습니다.',
  CANCELLED: '모집이 취소되었습니다.',
};

function MentorHub() {
  const [meetings, setMeetings] = useState<MeetingSummary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    meetingsApi
      .listMine({ size: 50 })
      .then((page) => {
        if (active) setMeetings(page.content);
      })
      .catch((err) => {
        if (active) setError(resolveErrorMessage(err));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-xl font-bold">내 모임 (운영)</h2>
      <p className="text-sm text-muted-foreground" data-testid="mentor-hub-note">
        내가 개설한 모임의 상태와 다음 단계를 확인할 수 있습니다. 신청자 목록·사전 설문 응답은 다음
        단계에서 제공될 예정입니다.
      </p>

      {loading && (
        <p className="text-sm text-muted-foreground" data-testid="mentor-hub-loading">
          불러오는 중...
        </p>
      )}

      {error && (
        <p role="alert" className="text-sm text-destructive" data-testid="mentor-hub-error">
          {error}
        </p>
      )}

      {!loading && !error && meetings.length === 0 && (
        <Card>
          <CardContent className="pt-4">
            <p className="text-sm text-muted-foreground" data-testid="mentor-hub-empty">
              아직 개설한 모임이 없습니다. &lsquo;모임 개설&rsquo;에서 새 모임을 만들어 보세요.
            </p>
          </CardContent>
        </Card>
      )}

      {!loading && !error && meetings.length > 0 && (
        <ul className="flex flex-col gap-3" data-testid="mentor-meeting-list">
          {meetings.map((m) => (
            <li key={m.id}>
              <Card data-testid={`mentor-meeting-${m.id}`}>
                <CardHeader className="flex-row items-start justify-between gap-2 space-y-0">
                  <CardTitle className="text-base">{m.title}</CardTitle>
                  <Badge
                    variant={meetingStatusVariant(m.status)}
                    data-testid={`mentor-meeting-status-${m.id}`}
                  >
                    {meetingStatusLabel(m.status)}
                  </Badge>
                </CardHeader>
                <CardContent className="flex flex-col gap-1 text-sm text-muted-foreground">
                  {m.topic && <span>주제: {m.topic}</span>}
                  <span>기간: {m.weeks}주</span>
                  <span>정원: {m.capacity}명</span>
                  <span data-testid={`mentor-meeting-next-${m.id}`}>{NEXT_ACTION[m.status]}</span>
                </CardContent>
              </Card>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
