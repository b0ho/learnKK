import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  enrollmentsApi,
  meetingsApi,
  resolveErrorMessage,
  type MeetingSummary,
} from '@/api';
import { useAuth } from '@/auth/useAuth';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { PATHS } from '@/routes/paths';
import { meetingStatusLabel, meetingStatusVariant } from '@/features/shared/meetingStatus';

type LoadState = 'loading' | 'error' | 'empty' | 'ready';

/** Per-meeting apply feedback shown inline on the card. */
interface ApplyFeedback {
  kind: 'success' | 'error';
  message: string;
}

export function MeetingListPage() {
  const { role } = useAuth();
  const [meetings, setMeetings] = useState<MeetingSummary[]>([]);
  const [state, setState] = useState<LoadState>('loading');
  const [error, setError] = useState<string | null>(null);
  const [applyingId, setApplyingId] = useState<number | null>(null);
  const [applied, setApplied] = useState<Record<number, boolean>>({});
  const [feedback, setFeedback] = useState<Record<number, ApplyFeedback>>({});

  useEffect(() => {
    let active = true;
    setState('loading');
    meetingsApi
      .listRecruiting({ page: 0, size: 20 })
      .then((page) => {
        if (!active) return;
        setMeetings(page.content);
        setState(page.content.length === 0 ? 'empty' : 'ready');
      })
      .catch((err) => {
        if (!active) return;
        setError(resolveErrorMessage(err));
        setState('error');
      });
    return () => {
      active = false;
    };
  }, []);

  async function handleApply(meetingId: number) {
    setApplyingId(meetingId);
    setFeedback((prev) => {
      const next = { ...prev };
      delete next[meetingId];
      return next;
    });
    try {
      await enrollmentsApi.apply(meetingId);
      setApplied((prev) => ({ ...prev, [meetingId]: true }));
      setFeedback((prev) => ({
        ...prev,
        [meetingId]: { kind: 'success', message: '신청이 완료되었습니다.' },
      }));
    } catch (err) {
      // 409 (ENROLLMENT_FULL / DUPLICATE / NOT_OPEN) and others map to a Korean message.
      setFeedback((prev) => ({
        ...prev,
        [meetingId]: { kind: 'error', message: resolveErrorMessage(err) },
      }));
    } finally {
      setApplyingId(null);
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold">모집중 모임</h2>
        {role === 'MENTOR' && (
          <Button asChild size="sm" data-testid="open-create-meeting">
            <Link to={PATHS.meetingCreate}>모임 개설</Link>
          </Button>
        )}
        {role === 'ADMIN' && (
          <Button asChild size="sm" variant="outline" data-testid="open-admin-queue">
            <Link to={PATHS.adminApproval}>개설 승인</Link>
          </Button>
        )}
      </div>

      {state === 'loading' && <p data-testid="meetings-loading">모임을 불러오는 중...</p>}

      {state === 'error' && (
        <p role="alert" className="text-destructive" data-testid="meetings-error">
          {error}
        </p>
      )}

      {state === 'empty' && (
        <p className="text-muted-foreground" data-testid="meetings-empty">
          현재 모집중인 모임이 없습니다.
        </p>
      )}

      {state === 'ready' && (
        <ul className="flex flex-col gap-3" data-testid="meetings-list">
          {meetings.map((meeting) => {
            const meetingFeedback = feedback[meeting.id];
            const isApplied = applied[meeting.id];
            return (
              <li key={meeting.id}>
                <Card data-testid={`meeting-card-${meeting.id}`}>
                  <CardHeader className="flex-row items-start justify-between gap-2 space-y-0">
                    <CardTitle className="text-base">{meeting.title}</CardTitle>
                    <Badge variant={meetingStatusVariant(meeting.status)}>
                      {meetingStatusLabel(meeting.status)}
                    </Badge>
                  </CardHeader>
                  <CardContent className="flex flex-col gap-2 text-sm text-muted-foreground">
                    <div className="flex flex-col gap-1">
                      {meeting.topic && <span>주제: {meeting.topic}</span>}
                      <span>기간: {meeting.weeks}주</span>
                      <span>정원: {meeting.capacity}명</span>
                    </div>

                    {role === 'MENTEE' && meeting.status === 'RECRUITING' && (
                      <div className="flex flex-col gap-1">
                        <Button
                          size="sm"
                          className="self-start"
                          disabled={applyingId === meeting.id || isApplied}
                          onClick={() => handleApply(meeting.id)}
                          data-testid={`apply-button-${meeting.id}`}
                        >
                          {isApplied ? '신청완료' : applyingId === meeting.id ? '신청 중...' : '신청'}
                        </Button>
                        {meetingFeedback && (
                          <p
                            role={meetingFeedback.kind === 'error' ? 'alert' : undefined}
                            className={
                              meetingFeedback.kind === 'error'
                                ? 'text-destructive'
                                : 'text-foreground'
                            }
                            data-testid={`apply-feedback-${meeting.id}`}
                          >
                            {meetingFeedback.message}
                          </p>
                        )}
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
