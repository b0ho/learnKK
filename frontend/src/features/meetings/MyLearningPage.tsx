import { useCallback, useEffect, useState } from 'react';
import {
  enrollmentsApi,
  isApiErrorCode,
  meetingsApi,
  resolveErrorMessage,
  sessionsApi,
  type ApplicantResponse,
  type AttendanceSummaryResponse,
  type EnrollmentResponse,
  type MeetingResponse,
  type MeetingSessionResponse,
  type MeetingStatus,
  type MeetingSummary,
} from '@/api';
import { useAuth } from '@/auth/useAuth';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { meetingStatusLabel, meetingStatusVariant } from '@/features/shared/meetingStatus';
import { formatRate } from '@/features/shared/completionStatus';

/**
 * "내 러닝" tab. Role-adaptive slice:
 *  - MENTOR: operations hub listing the mentor's own meetings with status/applicants and, for
 *    IN_PROGRESS meetings, session scheduling (add / list / reschedule) — U5/Bolt 6.
 *  - MENTEE: the mentee's own enrollments, with cancel before start ②, plus per-meeting session
 *    list, pop-up time-window check-in and the attendance rate (U5/Bolt 6).
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

/** Format an ISO instant for a datetime-local input value (local time, minute precision). */
function toDateTimeLocal(iso?: string): string {
  const d = iso ? new Date(iso) : new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(
    d.getMinutes(),
  )}`;
}

function formatWhen(iso: string): string {
  return new Date(iso).toLocaleString('ko-KR');
}

// ---------------------------------------------------------------------------
// Mentee
// ---------------------------------------------------------------------------

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
            const showSessions =
              enrollment.status === 'APPLIED' && meeting?.status === 'IN_PROGRESS';
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
                    {showSessions && <MenteeSessions meetingId={enrollment.meetingId} />}
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

/** Mentee session list + pop-up time-window check-in + attendance rate for one meeting. */
function MenteeSessions({ meetingId }: { meetingId: number }) {
  const [sessions, setSessions] = useState<MeetingSessionResponse[]>([]);
  const [summary, setSummary] = useState<AttendanceSummaryResponse | null>(null);
  const [checkedIn, setCheckedIn] = useState<Record<number, boolean>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);
  // Re-render on a timer so a session entering its window becomes checkable (client timer, ADR-005).
  const [now, setNow] = useState<number>(Date.now());

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [list, attendance] = await Promise.all([
        sessionsApi.listSessions(meetingId),
        sessionsApi.getMyAttendance(meetingId),
      ]);
      setSessions(list);
      setSummary(attendance);
    } catch (err) {
      setError(resolveErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [meetingId]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 30_000);
    return () => clearInterval(timer);
  }, []);

  function isOpen(session: MeetingSessionResponse): boolean {
    const start = new Date(session.scheduledAt).getTime();
    const end = start + session.checkInWindowMinutes * 60_000;
    return now >= start && now <= end;
  }

  async function handleCheckIn(sessionId: number) {
    setBusyId(sessionId);
    setActionError(null);
    try {
      await sessionsApi.checkIn(sessionId);
      setCheckedIn((prev) => ({ ...prev, [sessionId]: true }));
      const attendance = await sessionsApi.getMyAttendance(meetingId);
      setSummary(attendance);
    } catch (err) {
      if (isApiErrorCode(err, 'ATTENDANCE_WINDOW_CLOSED')) {
        setActionError('출석 가능 시간이 아닙니다. 세션 시간에 다시 시도해 주세요.');
      } else {
        setActionError(resolveErrorMessage(err));
      }
    } finally {
      setBusyId(null);
    }
  }

  if (loading) {
    return (
      <p className="text-sm text-muted-foreground" data-testid={`mentee-sessions-loading-${meetingId}`}>
        세션 불러오는 중...
      </p>
    );
  }

  if (error) {
    return (
      <p role="alert" className="text-sm text-destructive" data-testid={`mentee-sessions-error-${meetingId}`}>
        {error}
      </p>
    );
  }

  const meetsThreshold =
    summary != null && summary.totalScheduled > 0 && summary.attended * 100 >= 80 * summary.totalScheduled;

  return (
    <div className="flex flex-col gap-2 border-t pt-2" data-testid={`mentee-sessions-${meetingId}`}>
      <span className="font-medium text-foreground">세션 일정</span>

      {summary && (
        <div className="flex items-center gap-2" data-testid={`mentee-attendance-${meetingId}`}>
          <span>
            출석율: {summary.attended}/{summary.totalScheduled} ({formatRate(summary.rate)})
          </span>
          {summary.totalScheduled > 0 && (
            <Badge
              variant={meetsThreshold ? 'default' : 'outline'}
              data-testid={`mentee-completion-estimate-${meetingId}`}
            >
              {meetsThreshold ? '수료 기준 충족' : '수료 기준 미달'}
            </Badge>
          )}
        </div>
      )}

      {actionError && (
        <p role="alert" className="text-sm text-destructive" data-testid={`mentee-checkin-error-${meetingId}`}>
          {actionError}
        </p>
      )}

      {sessions.length === 0 && (
        <p className="text-sm text-muted-foreground" data-testid={`mentee-sessions-empty-${meetingId}`}>
          등록된 세션이 없습니다.
        </p>
      )}

      {sessions.length > 0 && (
        <ul className="flex flex-col gap-1.5" data-testid={`mentee-session-list-${meetingId}`}>
          {sessions.map((s) => {
            const open = isOpen(s);
            const done = checkedIn[s.id];
            return (
              <li
                key={s.id}
                className="flex items-center justify-between gap-2"
                data-testid={`mentee-session-${s.id}`}
              >
                <span>
                  {s.week}주차 · {formatWhen(s.scheduledAt)}
                </span>
                {done ? (
                  <Badge variant="secondary" data-testid={`mentee-session-done-${s.id}`}>
                    출석 완료
                  </Badge>
                ) : open ? (
                  <Button
                    size="sm"
                    disabled={busyId === s.id}
                    onClick={() => handleCheckIn(s.id)}
                    data-testid={`mentee-checkin-${s.id}`}
                  >
                    {busyId === s.id ? '출석 중...' : '출석하기'}
                  </Button>
                ) : (
                  <span
                    className="text-xs text-muted-foreground"
                    data-testid={`mentee-session-closed-${s.id}`}
                  >
                    출석 시간 아님
                  </span>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Mentor
// ---------------------------------------------------------------------------

/** Mentor-facing next-action guidance keyed by the meeting's current status. */
const NEXT_ACTION: Record<MeetingStatus, string> = {
  PENDING_APPROVAL: '관리자 개설 승인을 기다리고 있습니다.',
  RECRUITING: '모집 중입니다. 관리자의 모집 확정을 기다립니다.',
  READY_TO_START: '모집이 확정되었습니다. 관리자의 시작 승인을 기다립니다.',
  IN_PROGRESS: '진행 중입니다. 세션을 등록하고 출석을 관리하세요.',
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
        내가 개설한 모임의 상태·신청자와 진행 중 모임의 세션 일정을 관리할 수 있습니다.
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

                    {m.status === 'IN_PROGRESS' && <MentorSessions meetingId={m.id} />}
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

/** Mentor session scheduling for one IN_PROGRESS meeting: list, add, reschedule (W1). */
function MentorSessions({ meetingId }: { meetingId: number }) {
  const [sessions, setSessions] = useState<MeetingSessionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const [week, setWeek] = useState('1');
  const [scheduledAt, setScheduledAt] = useState(() => toDateTimeLocal());
  const [windowMinutes, setWindowMinutes] = useState('120');

  const [editingId, setEditingId] = useState<number | null>(null);
  const [editAt, setEditAt] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setSessions(await sessionsApi.listSessions(meetingId));
    } catch (err) {
      setError(resolveErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [meetingId]);

  useEffect(() => {
    void load();
  }, [load]);

  async function handleAdd() {
    setActionError(null);
    const weekNum = Number(week);
    const windowNum = Number(windowMinutes);
    if (!Number.isInteger(weekNum) || weekNum < 1) {
      setActionError('주차는 1 이상의 정수여야 합니다.');
      return;
    }
    if (!scheduledAt) {
      setActionError('세션 일시를 입력해 주세요.');
      return;
    }
    setBusy(true);
    try {
      await sessionsApi.addSession(meetingId, {
        week: weekNum,
        scheduledAt: new Date(scheduledAt).toISOString(),
        checkInWindowMinutes: Number.isInteger(windowNum) && windowNum >= 1 ? windowNum : undefined,
      });
      await load();
    } catch (err) {
      setActionError(resolveErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  function startEdit(session: MeetingSessionResponse) {
    setEditingId(session.id);
    setEditAt(toDateTimeLocal(session.scheduledAt));
    setActionError(null);
  }

  async function handleReschedule(sessionId: number) {
    if (!editAt) {
      setActionError('변경할 일시를 입력해 주세요.');
      return;
    }
    setBusy(true);
    setActionError(null);
    try {
      await sessionsApi.updateSession(sessionId, { scheduledAt: new Date(editAt).toISOString() });
      setEditingId(null);
      await load();
    } catch (err) {
      setActionError(resolveErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="flex flex-col gap-2 border-t pt-2" data-testid={`mentor-sessions-${meetingId}`}>
      <span className="font-medium text-foreground">세션 관리</span>

      {loading && (
        <p className="text-sm text-muted-foreground" data-testid={`mentor-sessions-loading-${meetingId}`}>
          세션 불러오는 중...
        </p>
      )}

      {error && (
        <p role="alert" className="text-sm text-destructive" data-testid={`mentor-sessions-error-${meetingId}`}>
          {error}
        </p>
      )}

      {actionError && (
        <p role="alert" className="text-sm text-destructive" data-testid={`mentor-session-action-error-${meetingId}`}>
          {actionError}
        </p>
      )}

      {!loading && !error && sessions.length === 0 && (
        <p className="text-sm text-muted-foreground" data-testid={`mentor-sessions-empty-${meetingId}`}>
          등록된 세션이 없습니다. 아래에서 세션을 추가하세요.
        </p>
      )}

      {sessions.length > 0 && (
        <ul className="flex flex-col gap-1.5" data-testid={`mentor-session-list-${meetingId}`}>
          {sessions.map((s) => (
            <li key={s.id} className="flex flex-col gap-1" data-testid={`mentor-session-${s.id}`}>
              <div className="flex items-center justify-between gap-2">
                <span>
                  {s.week}주차 · {formatWhen(s.scheduledAt)}
                </span>
                <Button
                  size="sm"
                  variant="outline"
                  disabled={busy}
                  onClick={() => startEdit(s)}
                  data-testid={`mentor-session-edit-${s.id}`}
                >
                  시간 변경
                </Button>
              </div>
              {editingId === s.id && (
                <div className="flex items-end gap-2" data-testid={`mentor-session-edit-form-${s.id}`}>
                  <Input
                    type="datetime-local"
                    value={editAt}
                    onChange={(e) => setEditAt(e.target.value)}
                    data-testid={`mentor-session-edit-input-${s.id}`}
                  />
                  <Button
                    size="sm"
                    disabled={busy}
                    onClick={() => handleReschedule(s.id)}
                    data-testid={`mentor-session-edit-save-${s.id}`}
                  >
                    저장
                  </Button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}

      <div className="flex flex-col gap-1.5 rounded-md border p-2" data-testid={`mentor-session-add-${meetingId}`}>
        <span className="text-xs font-medium text-foreground">세션 추가</span>
        <div className="flex flex-wrap items-end gap-2">
          <div className="flex flex-col gap-1">
            <Label htmlFor={`session-week-${meetingId}`}>주차</Label>
            <Input
              id={`session-week-${meetingId}`}
              type="number"
              min={1}
              className="w-20"
              value={week}
              onChange={(e) => setWeek(e.target.value)}
              data-testid={`mentor-session-week-${meetingId}`}
            />
          </div>
          <div className="flex flex-col gap-1">
            <Label htmlFor={`session-at-${meetingId}`}>일시</Label>
            <Input
              id={`session-at-${meetingId}`}
              type="datetime-local"
              value={scheduledAt}
              onChange={(e) => setScheduledAt(e.target.value)}
              data-testid={`mentor-session-at-${meetingId}`}
            />
          </div>
          <div className="flex flex-col gap-1">
            <Label htmlFor={`session-window-${meetingId}`}>출석창(분)</Label>
            <Input
              id={`session-window-${meetingId}`}
              type="number"
              min={1}
              className="w-24"
              value={windowMinutes}
              onChange={(e) => setWindowMinutes(e.target.value)}
              data-testid={`mentor-session-window-${meetingId}`}
            />
          </div>
          <Button
            size="sm"
            disabled={busy}
            onClick={handleAdd}
            data-testid={`mentor-session-add-submit-${meetingId}`}
          >
            {busy ? '처리 중...' : '추가'}
          </Button>
        </div>
      </div>
    </div>
  );
}
