import { useCallback, useEffect, useState } from 'react';
import {
  adminApi,
  isApiErrorCode,
  resolveErrorMessage,
  sessionsApi,
  type MeetingStatus,
  type MeetingSummary,
  type MenteeCompletionResponse,
} from '@/api';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { meetingStatusLabel, meetingStatusVariant } from '@/features/shared/meetingStatus';
import {
  completionStatusLabel,
  completionStatusVariant,
  formatRate,
} from '@/features/shared/completionStatus';

/**
 * 관리자 승인 큐(FR-2/FR-3): 처리 대기 모임을 상태(승인 유형)별 영역으로 나눠 목록으로 보여주고, 각 카드에서
 * 상태에 맞는 액션을 수행한다. 모든 승인/시작/완료/모집확정/되돌리기는 확인 다이얼로그(FR-6)를 거친다.
 * 되돌리기(FR-5)는 전진 승인(RECRUITING/READY_TO_START/IN_PROGRESS/COMPLETED)에서만 노출한다.
 */

// 승인 유형별 영역 정의(표시 순서).
const SECTIONS: { status: MeetingStatus; title: string }[] = [
  { status: 'PENDING_APPROVAL', title: '① 개설 승인 대기' },
  { status: 'RECRUITING', title: '모집 확정 대기 (모집중)' },
  { status: 'READY_TO_START', title: '② 시작 대기' },
  { status: 'IN_PROGRESS', title: '③ 완료 · 수료 판정 (진행중)' },
  { status: 'COMPLETED', title: '완료됨' },
];

type ConfirmState = {
  title: string;
  message: string;
  run: () => Promise<unknown>;
} | null;

type ReasonState = { mode: 'reject' | 'cancel'; meetingId: number } | null;

export function AdminApprovalPage() {
  const [groups, setGroups] = useState<Record<string, MeetingSummary[]>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [working, setWorking] = useState(false);

  const [confirm, setConfirm] = useState<ConfirmState>(null);
  const [reason, setReason] = useState('');
  const [reasonState, setReasonState] = useState<ReasonState>(null);
  const [reasonError, setReasonError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const results = await Promise.all(
        SECTIONS.map((s) =>
          adminApi
            .listByStatus(s.status, { size: 100 })
            .then((page) => [s.status, page.content] as const)
            .catch(() => [s.status, [] as MeetingSummary[]] as const),
        ),
      );
      setGroups(Object.fromEntries(results));
    } catch (err) {
      setError(resolveErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  // 확인 다이얼로그를 열고, 확인 시 지정한 액션을 실행한 뒤 목록을 새로고침한다(FR-6).
  function ask(title: string, message: string, run: () => Promise<unknown>) {
    setActionError(null);
    setConfirm({ title, message, run });
  }

  async function runConfirmed() {
    if (!confirm) return;
    setWorking(true);
    setActionError(null);
    try {
      await confirm.run();
      setConfirm(null);
      await load();
    } catch (err) {
      setActionError(resolveErrorMessage(err));
      setConfirm(null);
    } finally {
      setWorking(false);
    }
  }

  function openReason(mode: 'reject' | 'cancel', meetingId: number) {
    setReason('');
    setReasonError(null);
    setReasonState({ mode, meetingId });
  }

  async function submitReason() {
    if (!reasonState) return;
    const trimmed = reason.trim();
    if (!trimmed) {
      setReasonError('사유를 입력해 주세요.');
      return;
    }
    setWorking(true);
    setActionError(null);
    try {
      if (reasonState.mode === 'reject') {
        await adminApi.rejectMeeting(reasonState.meetingId, trimmed);
      } else {
        await adminApi.confirmRecruitment(reasonState.meetingId, false, trimmed);
      }
      setReasonState(null);
      await load();
    } catch (err) {
      setActionError(resolveErrorMessage(err));
    } finally {
      setWorking(false);
    }
  }

  const revertBtn = (m: MeetingSummary) => (
    <Button
      size="sm"
      variant="ghost"
      disabled={working}
      data-testid={`admin-revert-${m.id}`}
      onClick={() =>
        ask('승인 되돌리기', `"${m.title}"을(를) 직전 단계로 되돌릴까요?`, () =>
          adminApi.revert(m.id),
        )
      }
    >
      되돌리기
    </Button>
  );

  function actionsFor(m: MeetingSummary) {
    switch (m.status) {
      case 'PENDING_APPROVAL':
        return (
          <>
            <Button
              size="sm"
              disabled={working}
              data-testid={`admin-approve-${m.id}`}
              onClick={() =>
                ask('개설 승인', `"${m.title}"을(를) 승인할까요?`, () => adminApi.approveMeeting(m.id))
              }
            >
              승인
            </Button>
            <Button
              size="sm"
              variant="destructive"
              disabled={working}
              data-testid={`admin-reject-${m.id}`}
              onClick={() => openReason('reject', m.id)}
            >
              반려
            </Button>
          </>
        );
      case 'RECRUITING':
        return (
          <>
            <Button
              size="sm"
              disabled={working}
              data-testid={`admin-confirm-${m.id}`}
              onClick={() =>
                ask('모집 확정', `"${m.title}"의 모집을 확정(진행)할까요?`, () =>
                  adminApi.confirmRecruitment(m.id, true),
                )
              }
            >
              모집 확정
            </Button>
            <Button
              size="sm"
              variant="destructive"
              disabled={working}
              data-testid={`admin-cancel-${m.id}`}
              onClick={() => openReason('cancel', m.id)}
            >
              모집 취소
            </Button>
            {revertBtn(m)}
          </>
        );
      case 'READY_TO_START':
        return (
          <>
            <Button
              size="sm"
              disabled={working}
              data-testid={`admin-start-${m.id}`}
              onClick={() =>
                ask('시작 승인', `"${m.title}"을(를) 시작할까요?`, () => adminApi.approveStart(m.id))
              }
            >
              시작 승인
            </Button>
            {revertBtn(m)}
          </>
        );
      case 'IN_PROGRESS':
        return (
          <>
            <Button
              size="sm"
              disabled={working}
              data-testid={`admin-complete-${m.id}`}
              onClick={() =>
                ask('모임 완료', `"${m.title}"을(를) 완료 처리할까요? (모든 세션이 종료되어야 합니다)`, () =>
                  adminApi.complete(m.id),
                )
              }
            >
              완료 처리
            </Button>
            {revertBtn(m)}
          </>
        );
      case 'COMPLETED':
        return revertBtn(m);
      default:
        return null;
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-xl font-bold">모임 승인 · 운영</h2>

      {loading && (
        <p className="text-sm text-muted-foreground" data-testid="admin-loading">
          불러오는 중...
        </p>
      )}
      {error && (
        <p role="alert" className="text-sm text-destructive" data-testid="admin-error">
          {error}
        </p>
      )}
      {actionError && (
        <p role="alert" className="text-sm text-destructive" data-testid="admin-action-error">
          {actionError}
        </p>
      )}

      {!loading &&
        !error &&
        SECTIONS.map((section) => {
          const list = groups[section.status] ?? [];
          return (
            <section
              key={section.status}
              className="flex flex-col gap-2"
              data-testid={`admin-section-${section.status}`}
            >
              <h3 className="text-base font-semibold">
                {section.title} <span className="text-muted-foreground">({list.length})</span>
              </h3>
              {list.length === 0 ? (
                <p
                  className="text-sm text-muted-foreground"
                  data-testid={`admin-section-empty-${section.status}`}
                >
                  대기 중인 모임이 없습니다.
                </p>
              ) : (
                <ul className="flex flex-col gap-2">
                  {list.map((m) => (
                    <li key={m.id}>
                      <Card data-testid={`admin-meeting-${m.id}`}>
                        <CardHeader className="flex-row items-start justify-between gap-2 space-y-0">
                          <CardTitle className="text-base">{m.title}</CardTitle>
                          <Badge
                            variant={meetingStatusVariant(m.status)}
                            data-testid={`admin-meeting-status-${m.id}`}
                          >
                            {meetingStatusLabel(m.status)}
                          </Badge>
                        </CardHeader>
                        <CardContent className="flex flex-col gap-3">
                          <div className="flex flex-col gap-0.5 text-sm text-muted-foreground">
                            {m.topic && <span>주제: {m.topic}</span>}
                            <span>
                              기간 {m.weeks}주 · 정원 {m.capacity}명
                            </span>
                          </div>
                          <div className="flex flex-wrap gap-2">{actionsFor(m)}</div>
                          {(m.status === 'IN_PROGRESS' || m.status === 'COMPLETED') && (
                            <CompletionPanel meetingId={m.id} />
                          )}
                        </CardContent>
                      </Card>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          );
        })}

      {/* 확인 다이얼로그 (FR-6) */}
      <Dialog open={confirm !== null} onOpenChange={(open) => !open && setConfirm(null)}>
        <DialogContent data-testid="confirm-dialog">
          <DialogHeader>
            <DialogTitle>{confirm?.title}</DialogTitle>
          </DialogHeader>
          <p className="text-sm">{confirm?.message}</p>
          <DialogFooter>
            <Button
              variant="outline"
              disabled={working}
              data-testid="confirm-cancel"
              onClick={() => setConfirm(null)}
            >
              취소
            </Button>
            <Button disabled={working} data-testid="confirm-ok" onClick={runConfirmed}>
              {working ? '처리 중...' : '확인'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 사유 입력 다이얼로그 (반려 / 모집취소) */}
      <Dialog open={reasonState !== null} onOpenChange={(open) => !open && setReasonState(null)}>
        <DialogContent data-testid="reason-dialog">
          <DialogHeader>
            <DialogTitle>{reasonState?.mode === 'reject' ? '반려 사유' : '모집 취소 사유'}</DialogTitle>
          </DialogHeader>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="reason-input">사유</Label>
            <Textarea
              id="reason-input"
              data-testid="reason-input"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
            {reasonError && (
              <p role="alert" className="text-sm text-destructive" data-testid="reason-error">
                {reasonError}
              </p>
            )}
          </div>
          <DialogFooter>
            <Button
              variant="destructive"
              disabled={working}
              data-testid="reason-confirm"
              onClick={submitReason}
            >
              {reasonState?.mode === 'reject' ? '반려 확정' : '취소 확정'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

/**
 * ④ Completion panel (admin). 80% 자동 판정(computeCompletions) → 후보 목록 → 각 후보 수료 확정(approveCompletion).
 */
function CompletionPanel({ meetingId }: { meetingId: number }) {
  const [rows, setRows] = useState<MenteeCompletionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [computing, setComputing] = useState(false);
  const [approvingId, setApprovingId] = useState<number | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setRows(await sessionsApi.listCompletions(meetingId));
    } catch (err) {
      setError(resolveErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [meetingId]);

  useEffect(() => {
    void load();
  }, [load]);

  async function handleCompute() {
    setComputing(true);
    setActionError(null);
    try {
      setRows(await sessionsApi.computeCompletions(meetingId));
    } catch (err) {
      setActionError(resolveErrorMessage(err));
    } finally {
      setComputing(false);
    }
  }

  async function handleApprove(menteeId: number) {
    setApprovingId(menteeId);
    setActionError(null);
    try {
      await sessionsApi.approveCompletion(meetingId, menteeId);
      await load();
    } catch (err) {
      if (isApiErrorCode(err, 'COMPLETION_ALREADY_APPROVED')) {
        setActionError('이미 수료 확정된 멘티입니다.');
      } else if (isApiErrorCode(err, 'COMPLETION_NOT_ELIGIBLE')) {
        setActionError('수료 기준을 충족하지 않아 확정할 수 없습니다.');
      } else {
        setActionError(resolveErrorMessage(err));
      }
    } finally {
      setApprovingId(null);
    }
  }

  return (
    <div className="flex flex-col gap-2 border-t pt-3" data-testid={`admin-completion-${meetingId}`}>
      <div className="flex items-center justify-between gap-2">
        <span className="font-medium">수료 판정 (④)</span>
        <Button
          size="sm"
          variant="outline"
          disabled={computing}
          onClick={handleCompute}
          data-testid={`admin-completion-compute-${meetingId}`}
        >
          {computing ? '판정 중...' : '수료 판정 실행'}
        </Button>
      </div>

      {loading && (
        <p className="text-sm text-muted-foreground" data-testid={`admin-completion-loading-${meetingId}`}>
          불러오는 중...
        </p>
      )}
      {error && (
        <p role="alert" className="text-sm text-destructive" data-testid={`admin-completion-error-${meetingId}`}>
          {error}
        </p>
      )}
      {actionError && (
        <p role="alert" className="text-sm text-destructive" data-testid={`admin-completion-action-error-${meetingId}`}>
          {actionError}
        </p>
      )}

      {!loading && !error && rows.length === 0 && (
        <p className="text-sm text-muted-foreground" data-testid={`admin-completion-empty-${meetingId}`}>
          아직 판정 내역이 없습니다. &lsquo;수료 판정 실행&rsquo;을 눌러 판정하세요.
        </p>
      )}

      {rows.length > 0 && (
        <ul className="flex flex-col gap-1.5" data-testid={`admin-completion-list-${meetingId}`}>
          {rows.map((r) => (
            <li
              key={r.menteeId}
              className="flex items-center justify-between gap-2 text-sm"
              data-testid={`admin-completion-row-${meetingId}-${r.menteeId}`}
            >
              <span>
                멘티 #{r.menteeId} · {r.attendedCount}/{r.totalScheduled} (
                {formatRate(r.totalScheduled > 0 ? r.attendedCount / r.totalScheduled : 0)})
              </span>
              <div className="flex items-center gap-2">
                <Badge
                  variant={completionStatusVariant(r.status)}
                  data-testid={`admin-completion-status-${meetingId}-${r.menteeId}`}
                >
                  {completionStatusLabel(r.status)}
                </Badge>
                {r.status === 'COMPLETION_CANDIDATE' && (
                  <Button
                    size="sm"
                    disabled={approvingId === r.menteeId}
                    onClick={() => handleApprove(r.menteeId)}
                    data-testid={`admin-completion-approve-${meetingId}-${r.menteeId}`}
                  >
                    {approvingId === r.menteeId ? '확정 중...' : '수료 확정'}
                  </Button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
