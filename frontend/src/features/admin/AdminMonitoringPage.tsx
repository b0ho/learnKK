import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  adminApi,
  resolveErrorMessage,
  type ApprovalQueues,
  type MeetingMonitorRow,
} from '@/api';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { PATHS } from '@/routes/paths';
import { meetingStatusLabel, meetingStatusVariant } from '@/features/shared/meetingStatus';

type LoadState = 'loading' | 'error' | 'empty' | 'ready';

/** 승인 큐 요약 카운트 정의(표시 순서). 액션은 승인 큐 화면(AdminApprovalPage)에서 수행한다. */
const QUEUE_LABELS: { key: keyof ApprovalQueues; label: string; testId: string }[] = [
  { key: 'creation', label: '① 개설 승인', testId: 'creation' },
  { key: 'recruitConfirm', label: '모집 확정', testId: 'recruitConfirm' },
  { key: 'start', label: '② 시작 승인', testId: 'start' },
  { key: 'meetingComplete', label: '③ 모임 완료', testId: 'meetingComplete' },
  { key: 'menteeComplete', label: '④ 멘티 수료', testId: 'menteeComplete' },
];

/**
 * 관리자 운영 현황 대시보드(U9, US-9.1/9.2). 상단에 5개 승인 큐의 대기 건수를 요약하고(조회 전용 — 실제 승인 액션은
 * '승인 큐' 화면으로 이동), 하단에 모임별 운영 현황(상태·신청/정원·세션 기준 출석율·수료 진행)을 표로 보여준다.
 */
export function AdminMonitoringPage() {
  const [rows, setRows] = useState<MeetingMonitorRow[]>([]);
  const [queues, setQueues] = useState<ApprovalQueues | null>(null);
  const [state, setState] = useState<LoadState>('loading');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setState('loading');
    Promise.all([adminApi.monitoring(), adminApi.queues()])
      .then(([monitoring, q]) => {
        if (!active) return;
        setRows(monitoring);
        setQueues(q);
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

  const queueCount = (key: keyof ApprovalQueues): number => queues?.[key]?.length ?? 0;
  const pendingTotal = QUEUE_LABELS.reduce((sum, q) => sum + queueCount(q.key), 0);

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
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-sm" data-testid="admin-monitoring-table">
            <thead>
              <tr className="border-b text-left text-muted-foreground">
                <th className="py-2 pr-2 font-medium">모임</th>
                <th className="py-2 pr-2 font-medium">상태</th>
                <th className="py-2 pr-2 font-medium">신청/정원</th>
                <th className="py-2 pr-2 font-medium">참여</th>
                <th className="py-2 pr-2 font-medium">출석율</th>
                <th className="py-2 font-medium">수료(후보/확정)</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr
                  key={r.meetingId}
                  className="border-b align-top"
                  data-testid={`monitor-row-${r.meetingId}`}
                >
                  <td className="py-2 pr-2 font-medium text-foreground">{r.title}</td>
                  <td className="py-2 pr-2">
                    <Badge variant={meetingStatusVariant(r.status)}>
                      {meetingStatusLabel(r.status)}
                    </Badge>
                  </td>
                  <td className="py-2 pr-2 tabular-nums">
                    {r.applicantCount}/{r.capacity}
                  </td>
                  <td className="py-2 pr-2 tabular-nums">{r.participantCount}</td>
                  <td className="py-2 pr-2 tabular-nums" data-testid={`monitor-rate-${r.meetingId}`}>
                    {r.attendanceRatePercent}%
                  </td>
                  <td className="py-2 tabular-nums">
                    {r.completionCandidates}/{r.completedCount}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
