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
import { Spinner } from '@/components/ui/spinner';
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

  // FR-1: 로그인한 멘티는 로드 시 본인 신청 내역을 반영해 이미 신청한 모임을 "신청완료"로 표시한다.
  // 조회 실패는 목록 렌더를 막지 않는다(신청 상태 반영만 생략).
  useEffect(() => {
    if (role !== 'MENTEE') return;
    let active = true;
    enrollmentsApi
      .listMine()
      .then((list) => {
        if (!active || !Array.isArray(list)) return;
        const mine: Record<number, boolean> = {};
        for (const e of list) {
          if (e.status === 'APPLIED') mine[e.meetingId] = true;
        }
        // 방금 신청한 상태(prev)가 서버 스냅샷보다 우선한다.
        setApplied((prev) => ({ ...mine, ...prev }));
      })
      .catch(() => {
        /* FR-1: 신청 내역 조회 실패는 목록 표시를 방해하지 않는다. */
      });
    return () => {
      active = false;
    };
  }, [role]);

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
        {/* FR-9: 관리자 모임 목록의 '개설 승인' 진입 버튼 제거 — 모든 관리 액션은 하단 '관리' 탭에서 수행한다. */}
      </div>

      {state === 'loading' && <Spinner data-testid="meetings-loading" label="모임을 불러오는 중" />}

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
            // FR-2/FR-3: 내 신청 상태가 우선. 신청 안 했고 정원이 찬 모임만 "마감"으로 표시.
            const isFull = !isApplied && meeting.full === true;
            return (
              <li key={meeting.id}>
                <Card data-testid={`meeting-card-${meeting.id}`}>
                  <CardHeader className="flex-row items-start justify-between gap-2 space-y-0">
                    <CardTitle className="text-base">{meeting.title}</CardTitle>
                    <div className="flex shrink-0 items-center gap-1.5">
                      <Badge variant={meetingStatusVariant(meeting.status)}>
                        {meetingStatusLabel(meeting.status)}
                      </Badge>
                      {isFull && (
                        <Badge variant="destructive" data-testid={`full-badge-${meeting.id}`}>
                          마감
                        </Badge>
                      )}
                    </div>
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
                          disabled={applyingId === meeting.id || isApplied || isFull}
                          onClick={() => handleApply(meeting.id)}
                          data-testid={`apply-button-${meeting.id}`}
                        >
                          {isApplied
                            ? '신청완료'
                            : isFull
                              ? '마감'
                              : applyingId === meeting.id
                                ? '신청 중...'
                                : '신청'}
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
