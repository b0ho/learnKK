import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ClipboardCheck, Users } from 'lucide-react';
import {
  adminApi,
  resolveErrorMessage,
  type MeetingMonitoringSummary,
  type MeetingStatus,
} from '@/api';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Spinner } from '@/components/ui/spinner';
import { meetingStatusLabel, meetingStatusVariant } from '@/features/shared/meetingStatus';
import { formatRate } from '@/features/shared/completionStatus';
import { PATHS } from '@/routes/paths';

/**
 * 관리자 운영 현황 모니터링(US-9.2, U9): 전체 모임 목록·상태와 모임별 출석율(세션 기준)·수료 진행을
 * read 전용으로 보여준다. 승인 액션은 승인 큐(/admin/meetings)에서 수행한다.
 */

/** 상태 필터(전체 + 개별 상태). */
const FILTERS: { value: MeetingStatus | ''; label: string }[] = [
  { value: '', label: '전체' },
  { value: 'RECRUITING', label: '모집중' },
  { value: 'READY_TO_START', label: '시작대기' },
  { value: 'IN_PROGRESS', label: '진행중' },
  { value: 'COMPLETED', label: '완료' },
];

export function AdminMonitoringPage() {
  const [rows, setRows] = useState<MeetingMonitoringSummary[]>([]);
  const [statusFilter, setStatusFilter] = useState<MeetingStatus | ''>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (status: MeetingStatus | '') => {
    setLoading(true);
    setError(null);
    try {
      const page = await adminApi.listMonitoring({
        status: status === '' ? undefined : status,
        size: 100,
      });
      setRows(page.content);
    } catch (err) {
      setError(resolveErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load(statusFilter);
  }, [load, statusFilter]);

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-base font-bold">운영 현황</h2>
        <Button asChild variant="outline" size="sm" data-testid="go-approval-queue">
          <Link to={PATHS.adminApproval}>승인 큐</Link>
        </Button>
      </div>

      <div className="flex flex-wrap gap-1.5" role="group" aria-label="상태 필터">
        {FILTERS.map((f) => (
          <Button
            key={f.value || 'ALL'}
            type="button"
            size="sm"
            variant={statusFilter === f.value ? 'default' : 'outline'}
            data-testid={`monitoring-filter-${f.value || 'ALL'}`}
            aria-pressed={statusFilter === f.value}
            onClick={() => setStatusFilter(f.value)}
          >
            {f.label}
          </Button>
        ))}
      </div>

      {loading && (
        <div className="flex justify-center py-8" data-testid="monitoring-loading">
          <Spinner />
        </div>
      )}

      {error && (
        <p className="text-sm text-destructive" role="alert" data-testid="monitoring-error">
          {error}
        </p>
      )}

      {!loading && !error && rows.length === 0 && (
        <p className="py-8 text-center text-sm text-muted-foreground" data-testid="monitoring-empty">
          조회된 모임이 없습니다.
        </p>
      )}

      {!loading &&
        rows.map((m) => (
          <Card key={m.id} data-testid={`monitoring-card-${m.id}`}>
            <CardHeader className="pb-2">
              <div className="flex items-start justify-between gap-2">
                <CardTitle className="text-sm font-semibold">{m.title}</CardTitle>
                <Badge variant={meetingStatusVariant(m.status)}>
                  {meetingStatusLabel(m.status)}
                </Badge>
              </div>
              <p className="text-xs text-muted-foreground">
                멘토 {m.mentorNickname ?? `#${m.mentorId}`}
              </p>
            </CardHeader>
            <CardContent className="space-y-1.5 text-sm">
              <p className="flex items-center gap-1.5">
                <Users className="h-4 w-4 text-muted-foreground" aria-hidden="true" />
                멘티 {m.menteeCount}명 · 세션 {m.endedSessionCount}/{m.sessionCount} 종료
              </p>
              <p data-testid={`monitoring-attendance-${m.id}`}>
                출석율(세션 기준) <span className="font-semibold">{formatRate(m.attendanceRate)}</span>
              </p>
              <p
                className="flex items-center gap-1.5"
                data-testid={`monitoring-completion-${m.id}`}
              >
                <ClipboardCheck className="h-4 w-4 text-muted-foreground" aria-hidden="true" />
                수료 확정 {m.completedMenteeCount} · 후보 {m.completionCandidateCount} · 멘티{' '}
                {m.menteeCount}
              </p>
            </CardContent>
          </Card>
        ))}
    </div>
  );
}
