import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  adminApi,
  sessionsApi,
  resolveErrorMessage,
  type MeetingMonitorRow,
  type MeetingStatus,
  type MentorCompletionStatus,
  type MenteeCompletionResponse,
  type CompletionStatus,
} from '@/api';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '@/components/ui/dialog';
import { PATHS } from '@/routes/paths';
import { meetingStatusLabel, meetingStatusVariant } from '@/features/shared/meetingStatus';
import type { BadgeVariant } from '@/features/shared/meetingStatus';

type LoadState = 'loading' | 'error' | 'empty' | 'ready';

type QueueKey = 'creation' | 'recruitConfirm' | 'start' | 'meetingComplete' | 'menteeComplete';

/** 승인 큐 요약 카운트 정의(표시 순서). 액션은 승인 큐 화면(AdminApprovalPage)에서 수행한다. */
const QUEUE_LABELS: { key: QueueKey; label: string; testId: string }[] = [
  { key: 'creation', label: '개설 승인', testId: 'creation' },
  { key: 'recruitConfirm', label: '모집 확정', testId: 'recruitConfirm' },
  { key: 'start', label: '시작 승인', testId: 'start' },
  { key: 'meetingComplete', label: '모임 완료', testId: 'meetingComplete' },
  { key: 'menteeComplete', label: '멘티 수료', testId: 'menteeComplete' },
];

/**
 * 승인 대기 요약 카운트를 모니터링 행에서 직접 계산한다(동일 데이터 소스 → 표와 항상 일치, 승인 큐 화면의
 * 상태별 섹션과도 동기화). 멘티 수료는 전체 모임의 수료 후보(COMPLETION_CANDIDATE) 합계.
 */
function computeQueueCounts(rows: MeetingMonitorRow[]): Record<QueueKey, number> {
  const byStatus = (s: MeetingStatus) => rows.filter((r) => r.status === s).length;
  return {
    creation: byStatus('PENDING_APPROVAL'),
    recruitConfirm: byStatus('RECRUITING'),
    start: byStatus('READY_TO_START'),
    meetingComplete: byStatus('IN_PROGRESS'),
    menteeComplete: rows.reduce((sum, r) => sum + r.completionCandidates, 0),
  };
}

/** 상태 필터 옵션(전체 + 각 모임 상태). */
const STATUS_FILTERS: { value: MeetingStatus | 'ALL'; label: string }[] = [
  { value: 'ALL', label: '전체 상태' },
  { value: 'PENDING_APPROVAL', label: '승인대기' },
  { value: 'RECRUITING', label: '모집중' },
  { value: 'READY_TO_START', label: '시작대기' },
  { value: 'IN_PROGRESS', label: '진행중' },
  { value: 'COMPLETED', label: '완료' },
  { value: 'REJECTED', label: '반려' },
  { value: 'CANCELLED', label: '취소' },
];

/** 멘토 수료 판정 필터 옵션(완료 현황 조회용). */
const MENTOR_FILTERS: { value: MentorCompletionStatus | 'ALL'; label: string }[] = [
  { value: 'ALL', label: '전체 수료판정' },
  { value: 'PENDING', label: '판정 전' },
  { value: 'COMPLETED', label: '수료' },
  { value: 'NOT_COMPLETED', label: '미수료' },
];

const MENTOR_COMPLETION_LABEL: Record<MentorCompletionStatus, string> = {
  PENDING: '판정 전',
  COMPLETED: '수료',
  NOT_COMPLETED: '미수료',
};

const MENTOR_COMPLETION_VARIANT: Record<MentorCompletionStatus, BadgeVariant> = {
  PENDING: 'outline',
  COMPLETED: 'default',
  NOT_COMPLETED: 'destructive',
};

const MENTEE_COMPLETION_LABEL: Record<CompletionStatus, string> = {
  NOT_COMPLETED: '미수료',
  COMPLETION_CANDIDATE: '수료 후보',
  COMPLETED: '수료 확정',
};

function mentorStatusOf(row: MeetingMonitorRow): MentorCompletionStatus {
  return row.mentorCompletionStatus ?? 'PENDING';
}

const selectClass =
  'h-9 rounded-md border border-input bg-background px-2 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring';

/**
 * 관리자 운영 현황 대시보드(U9, US-9.1/9.2). 상단에 5개 승인 큐 대기 건수를 요약하고(조회 전용 — 실제 승인 액션은
 * '승인 큐' 화면), 하단에 모임별 운영 현황(상태·멘토 수료 판정·신청/정원·출석율·수료 진행)을 표로 보여준다.
 * 다양한 필터(상태·멘토 수료 판정·제목 검색)로 완료 현황 등 원하는 모임만 좁혀 볼 수 있고, 각 행을 클릭하면 상세 정보를
 * 다이얼로그로 확인할 수 있다.
 */
export function AdminMonitoringPage() {
  const [rows, setRows] = useState<MeetingMonitorRow[]>([]);
  const [state, setState] = useState<LoadState>('loading');
  const [error, setError] = useState<string | null>(null);

  // 필터 상태.
  const [statusFilter, setStatusFilter] = useState<MeetingStatus | 'ALL'>('ALL');
  const [mentorFilter, setMentorFilter] = useState<MentorCompletionStatus | 'ALL'>('ALL');
  const [search, setSearch] = useState('');

  // 상세 다이얼로그 대상 행.
  const [selected, setSelected] = useState<MeetingMonitorRow | null>(null);

  useEffect(() => {
    let active = true;
    setState('loading');
    adminApi
      .monitoring()
      .then((monitoring) => {
        if (!active) return;
        setRows(monitoring);
        setState(monitoring.length === 0 ? 'empty' : 'ready');
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

  const queueCounts = useMemo(() => computeQueueCounts(rows), [rows]);
  const queueCount = (key: QueueKey): number => queueCounts[key];
  const pendingTotal = QUEUE_LABELS.reduce((sum, q) => sum + queueCount(q.key), 0);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return rows.filter((r) => {
      if (statusFilter !== 'ALL' && r.status !== statusFilter) return false;
      if (mentorFilter !== 'ALL' && mentorStatusOf(r) !== mentorFilter) return false;
      if (q && !r.title.toLowerCase().includes(q)) return false;
      return true;
    });
  }, [rows, statusFilter, mentorFilter, search]);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-bold">운영 현황</h2>
        <Button asChild size="sm" variant="outline" data-testid="go-admin-queue">
          <Link to={PATHS.adminApproval}>승인 큐</Link>
        </Button>
      </div>

      {state === 'loading' && (
        <p className="text-sm text-muted-foreground" data-testid="admin-monitoring-loading">
          불러오는 중...
        </p>
      )}

      {state === 'error' && (
        <p role="alert" className="text-sm text-destructive" data-testid="admin-monitoring-error">
          {error}
        </p>
      )}

      {(state === 'ready' || state === 'empty') && (
        <Card data-testid="queue-summary">
          <CardHeader className="pb-2">
            <CardTitle className="text-base">
              승인 대기{' '}
              <span className="text-muted-foreground" data-testid="queue-pending-total">
                ({pendingTotal})
              </span>
            </CardTitle>
          </CardHeader>
          <CardContent className="flex flex-wrap gap-2">
            {QUEUE_LABELS.map((q) => (
              <span
                key={q.key}
                className="inline-flex items-center gap-1 rounded-md border px-2 py-1 text-sm"
                data-testid={`queue-count-${q.testId}`}
              >
                {q.label}
                <Badge variant={queueCount(q.key) > 0 ? 'default' : 'outline'}>
                  {queueCount(q.key)}
                </Badge>
              </span>
            ))}
          </CardContent>
        </Card>
      )}

      {state === 'empty' && (
        <p className="text-sm text-muted-foreground" data-testid="admin-monitoring-empty">
          등록된 모임이 없습니다.
        </p>
      )}

      {state === 'ready' && (
        <>
          {/* 다양한 필터: 상태 · 멘토 수료 판정(완료 현황) · 제목 검색. */}
          <div className="flex flex-wrap items-center gap-2" data-testid="monitor-filters">
            <select
              aria-label="상태 필터"
              data-testid="filter-status"
              className={selectClass}
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as MeetingStatus | 'ALL')}
            >
              {STATUS_FILTERS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
            <select
              aria-label="멘토 수료 판정 필터"
              data-testid="filter-mentor-completion"
              className={selectClass}
              value={mentorFilter}
              onChange={(e) => setMentorFilter(e.target.value as MentorCompletionStatus | 'ALL')}
            >
              {MENTOR_FILTERS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
            <Input
              type="search"
              aria-label="모임 제목 검색"
              data-testid="filter-search"
              placeholder="모임 제목 검색"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="h-9 w-40"
            />
            {(statusFilter !== 'ALL' || mentorFilter !== 'ALL' || search.trim()) && (
              <Button
                type="button"
                size="sm"
                variant="ghost"
                data-testid="filter-reset"
                onClick={() => {
                  setStatusFilter('ALL');
                  setMentorFilter('ALL');
                  setSearch('');
                }}
              >
                필터 초기화
              </Button>
            )}
            <span className="ml-auto text-sm text-muted-foreground" data-testid="filter-count">
              {filtered.length}/{rows.length}건
            </span>
          </div>

          {filtered.length === 0 ? (
            <p className="text-sm text-muted-foreground" data-testid="monitor-filtered-empty">
              조건에 맞는 모임이 없습니다.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full border-collapse text-sm" data-testid="admin-monitoring-table">
                <thead>
                  <tr className="border-b text-left text-muted-foreground">
                    <th className="py-2 pr-2 font-medium">모임</th>
                    <th className="py-2 pr-2 text-center font-medium">상태</th>
                    <th className="py-2 pr-2 text-center font-medium">멘토 수료</th>
                    <th className="py-2 text-center font-medium">정원/신청/수료</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((r) => {
                    const mentor = mentorStatusOf(r);
                    return (
                      <tr
                        key={r.meetingId}
                        role="button"
                        tabIndex={0}
                        className="cursor-pointer border-b align-top hover:bg-muted/50 focus:bg-muted/50 focus:outline-none"
                        data-testid={`monitor-row-${r.meetingId}`}
                        onClick={() => setSelected(r)}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter' || e.key === ' ') {
                            e.preventDefault();
                            setSelected(r);
                          }
                        }}
                      >
                        <td className="py-2 pr-2 font-medium text-foreground">{r.title}</td>
                        <td className="py-2 pr-2 text-center">
                          <Badge variant={meetingStatusVariant(r.status)}>
                            {meetingStatusLabel(r.status)}
                          </Badge>
                        </td>
                        <td className="py-2 pr-2 text-center">
                          <Badge variant={MENTOR_COMPLETION_VARIANT[mentor]}>
                            {MENTOR_COMPLETION_LABEL[mentor]}
                          </Badge>
                        </td>
                        <td className="py-2 text-center tabular-nums">
                          {r.capacity} / {r.applicantCount} / {r.completedCount}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      <MonitorDetailDialog row={selected} onClose={() => setSelected(null)} />
    </div>
  );
}

/** 행 클릭 상세: 모임 요약 지표 + 멘티별 수료 판정 내역(조회 전용). */
function MonitorDetailDialog({
  row,
  onClose,
}: {
  row: MeetingMonitorRow | null;
  onClose: () => void;
}) {
  const [completions, setCompletions] = useState<MenteeCompletionResponse[]>([]);
  const [detailState, setDetailState] = useState<LoadState>('loading');
  const [detailError, setDetailError] = useState<string | null>(null);

  useEffect(() => {
    if (!row) return;
    let active = true;
    setDetailState('loading');
    setCompletions([]);
    sessionsApi
      .listCompletions(row.meetingId)
      .then((list) => {
        if (!active) return;
        setCompletions(list);
        setDetailState(list.length === 0 ? 'empty' : 'ready');
      })
      .catch((err) => {
        if (!active) return;
        setDetailError(resolveErrorMessage(err));
        setDetailState('error');
      });
    return () => {
      active = false;
    };
  }, [row]);

  const mentor = row ? mentorStatusOf(row) : 'PENDING';

  return (
    <Dialog open={row !== null} onOpenChange={(open) => (!open ? onClose() : undefined)}>
      <DialogContent data-testid="monitor-detail" className="max-h-[85vh] overflow-y-auto">
        {row && (
          <>
            <DialogHeader>
              <DialogTitle>{row.title}</DialogTitle>
              <DialogDescription>모임 운영·수료 상세 현황</DialogDescription>
            </DialogHeader>

            <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
              <dt className="text-muted-foreground">상태</dt>
              <dd>
                <Badge variant={meetingStatusVariant(row.status)}>
                  {meetingStatusLabel(row.status)}
                </Badge>
              </dd>
              <dt className="text-muted-foreground">멘토 수료 판정</dt>
              <dd>
                <Badge variant={MENTOR_COMPLETION_VARIANT[mentor]}>
                  {MENTOR_COMPLETION_LABEL[mentor]}
                </Badge>
              </dd>
              <dt className="text-muted-foreground">신청/정원</dt>
              <dd className="tabular-nums">
                {row.applicantCount}/{row.capacity}
              </dd>
              <dt className="text-muted-foreground">참여 멘티</dt>
              <dd className="tabular-nums">{row.participantCount}명</dd>
              <dt className="text-muted-foreground">출석율</dt>
              <dd className="tabular-nums">{row.attendanceRatePercent}%</dd>
              <dt className="text-muted-foreground">멘티 수료(후보/확정)</dt>
              <dd className="tabular-nums">
                {row.completionCandidates}/{row.completedCount}
              </dd>
            </dl>

            <div className="flex flex-col gap-2">
              <h4 className="text-sm font-semibold">멘티별 수료 판정</h4>
              {detailState === 'loading' && (
                <p className="text-sm text-muted-foreground" data-testid="detail-loading">
                  불러오는 중...
                </p>
              )}
              {detailState === 'error' && (
                <p role="alert" className="text-sm text-destructive" data-testid="detail-error">
                  {detailError}
                </p>
              )}
              {detailState === 'empty' && (
                <p className="text-sm text-muted-foreground" data-testid="detail-empty">
                  아직 수료 판정 내역이 없습니다.
                </p>
              )}
              {detailState === 'ready' && (
                <ul className="flex flex-col gap-1" data-testid="detail-completions">
                  {completions.map((c) => (
                    <li
                      key={c.menteeId}
                      className="flex items-center justify-between gap-2 rounded-md border px-2 py-1 text-sm"
                      data-testid={`detail-completion-${c.menteeId}`}
                    >
                      <span>멘티 #{c.menteeId}</span>
                      <span className="flex items-center gap-2">
                        <span className="tabular-nums text-muted-foreground">
                          출석 {c.attendedCount}/{c.totalScheduled}
                        </span>
                        <Badge
                          variant={c.status === 'COMPLETED' ? 'default' : 'outline'}
                        >
                          {MENTEE_COMPLETION_LABEL[c.status]}
                        </Badge>
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
