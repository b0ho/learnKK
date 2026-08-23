import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  enrollmentsApi,
  meetingsApi,
  resolveErrorMessage,
  type ApplicantResponse,
  type EnrollmentResponse,
  type MeetingResponse,
  type MeetingStatus,
  type MeetingSummary,
} from '@/api';
import { useAuth } from '@/auth/useAuth';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { PATHS } from '@/routes/paths';
import { meetingStatusLabel, meetingStatusVariant } from '@/features/shared/meetingStatus';

/**
 * "내 러닝" tab. Role-adaptive slice:
 *  - MENTOR: operations hub listing the mentor's own meetings with status, next-action hint and the
 *    live applicant list/count per meeting (U4). Pre-survey answers (U8/Bolt 7) stay deferred.
 *  - MENTEE: the mentee's own enrollments (compose meeting info per enrollment), with a cancel
 *    action before start ②. Session schedule (U5/Bolt 6) stays a placeholder.
 */
export function MyLearningPage() {
  const { role } = useAuth();

  if (role === 'MENTOR') {
    return <MentorHub />;
  }

  return <MenteeLearning />;
}

/** Meeting states in which a mentee may still cancel (server re-validates — BR-U4-3). */
const CANCELLABLE: MeetingStatus[] = ['RECRUITING', 'READY_TO_START'];

interface MenteeEntry {
  enrollment: EnrollmentResponse;
  meeting: MeetingResponse | null;
}

function MenteeLearning() {
  const [entries, setEntries] = useState<MenteeEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cancellingId, setCancellingId] = useState<number | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const enrollments = await enrollmentsApi.listMine();
      const composed = await Promise.all(
        enrollments.map(async (enrollment) => {
          try {
            const meeting = await meetingsApi.get(enrollment.meetingId);
            return { enrollment, meeting };
          } catch {
            // A missing/deleted meeting should not break the whole list.
            return { enrollment, meeting: null };
          }
        }),
      );
      setEntries(composed);
    } catch (err) {
      setError(resolveErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function handleCancel(meetingId: number) {
    setCancellingId(meetingId);
    setActionError(null);
    try {
      await enrollmentsApi.cancel(meetingId);
      await load();
    } catch (err) {
      setActionError(resolveErrorMessage(err));
    } finally {
      setCancellingId(null);
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-xl font-bold">내 러닝</h2>
      <p className="text-sm text-muted-foreground" data-testid="mentee-session-note">
        신청한 모임의 세션 일정은 다음 단계에서 제공될 예정입니다.
      </p>

      {loading && (
        <p className="text-sm text-muted-foreground" data-testid="mentee-learning-loading">
          불러오는 중...
        </p>
      )}

      {error && (
        <p role="alert" className="text-sm text-destructive" data-testid="mentee-learning-error">
          {error}
        </p>
      )}

      {actionError && (
        <p role="alert" className="text-sm text-destructive" data-testid="mentee-cancel-error">
          {actionError}
        </p>
      )}

      {!loading && !error && entries.length === 0 && (
        <Card>
          <CardContent className="pt-4">
            <p className="text-sm text-muted-foreground" data-testid="mentee-learning-empty">
              아직 신청한 모임이 없습니다. &lsquo;모임&rsquo; 탭에서 모집중인 모임에 신청해 보세요.
            </p>
          </CardContent>
        </Card>
      )}

      {!loading && !error && entries.length > 0 && (
        <ul className="flex flex-col gap-3" data-testid="mentee-enrollment-list">
          {entries.map(({ enrollment, meeting }) => {
            const title = meeting?.title ?? `모임 #${enrollment.meetingId}`;
            const canCancel =
              enrollment.status === 'APPLIED' &&
              meeting != null &&
              CANCELLABLE.includes(meeting.status);
            return (
              <li key={enrollment.id}>
                <Card data-testid={`mentee-enrollment-${enrollment.id}`}>
                  <CardHeader className="flex-row items-start justify-between gap-2 space-y-0">
                    <CardTitle className="text-base">{title}</CardTitle>
                    {meeting && (
                      <Badge
                        variant={meetingStatusVariant(meeting.status)}
                        data-testid={`mentee-enrollment-meeting-status-${enrollment.id}`}
                      >
                        {meetingStatusLabel(meeting.status)}
                      </Badge>
                    )}
                  </CardHeader>
                  <CardContent className="flex flex-col gap-2 text-sm text-muted-foreground">
                    <span data-testid={`mentee-enrollment-status-${enrollment.id}`}>
                      신청 상태: {enrollment.status === 'APPLIED' ? '신청됨' : '취소됨'}
                    </span>
                    <span>신청일: {new Date(enrollment.appliedAt).toLocaleDateString('ko-KR')}</span>
                    {canCancel && (
                      <Button
                        size="sm"
                        variant="outline"
                        className="self-start"
                        disabled={cancellingId === enrollment.meetingId}
                        onClick={() => handleCancel(enrollment.meetingId)}
                        data-testid={`mentee-cancel-${enrollment.id}`}
                      >
                        {cancellingId === enrollment.meetingId ? '취소 중...' : '신청 취소'}
                      </Button>
                    )}
                    {enrollment.status === 'APPLIED' && meeting?.status === 'IN_PROGRESS' && (
                      <div className="flex gap-2">
                        <Button
                          asChild
                          size="sm"
                          className="self-start"
                          data-testid={`mentee-survey-answer-${enrollment.id}`}
                        >
                          <Link to={PATHS.surveyAnswer(enrollment.meetingId)}>사전설문 응답</Link>
                        </Button>
                        <Button
                          asChild
                          size="sm"
                          variant="outline"
                          className="self-start"
                          data-testid={`mentee-feedback-${enrollment.id}`}
                        >
                          <Link to={PATHS.feedback(enrollment.meetingId)}>피드백</Link>
                        </Button>
                      </div>
                    )}
                  </CardContent>
                </Card>
              </li>
            );
          })}
        </ul>
      )}
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
  const [applicants, setApplicants] = useState<Record<number, ApplicantResponse[]>>({});
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);
    meetingsApi
      .listMine({ size: 50 })
      .then(async (page) => {
        if (!active) return;
        setMeetings(page.content);
        // Compose the applicant list/count per meeting (U4 read).
        const pairs = await Promise.all(
          page.content.map(async (m) => {
            try {
              return [m.id, await enrollmentsApi.listApplicants(m.id)] as const;
            } catch {
              return [m.id, [] as ApplicantResponse[]] as const;
            }
          }),
        );
        if (active) setApplicants(Object.fromEntries(pairs));
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
        내가 개설한 모임의 상태와 신청자 목록을 확인하고, 각 모임의 피드백과 사전설문 응답을 열람할 수
        있습니다.
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
          {meetings.map((m) => {
            const list = applicants[m.id] ?? [];
            return (
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
                  <CardContent className="flex flex-col gap-2 text-sm text-muted-foreground">
                    {m.topic && <span>주제: {m.topic}</span>}
                    <span>기간: {m.weeks}주</span>
                    <span>정원: {m.capacity}명</span>
                    <span data-testid={`mentor-meeting-next-${m.id}`}>{NEXT_ACTION[m.status]}</span>

                    <Button
                      asChild
                      size="sm"
                      variant="outline"
                      className="self-start"
                      data-testid={`mentor-feedback-view-${m.id}`}
                    >
                      <Link to={PATHS.feedbackView(m.id)}>피드백·사전설문 열람</Link>
                    </Button>

                    <div className="flex flex-col gap-1">
                      <span data-testid={`applicant-count-${m.id}`}>
                        신청자: {list.length}명 / 정원 {m.capacity}명
                      </span>
                      {list.length > 0 && (
                        <ul
                          className="flex flex-col gap-0.5 pl-3"
                          data-testid={`applicant-list-${m.id}`}
                        >
                          {list.map((a) => (
                            <li key={a.menteeId} data-testid={`applicant-${m.id}-${a.menteeId}`}>
                              {a.nickname ?? `멘티 #${a.menteeId}`}
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  </CardContent>
                </Card>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
