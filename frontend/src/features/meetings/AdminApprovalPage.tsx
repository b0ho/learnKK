import { useState, type FormEvent } from 'react';
import { adminApi, meetingsApi, resolveErrorMessage, type MeetingResponse } from '@/api';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
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

/**
 * Admin meeting lifecycle actions. The approval-queue listing is U9 (Bolt 8); until then the admin
 * looks a meeting up by id and drives the state machine with status-aware actions:
 *  - PENDING_APPROVAL: 승인 (T1) / 반려 (T2, reason required)
 *  - RECRUITING: 모집확정 진행 (T3) / 모집 취소 (T4, reason required)
 *  - READY_TO_START: ②시작 (T5)
 *  - IN_PROGRESS: ③완료 (T6)
 *  - terminal states (COMPLETED/REJECTED/CANCELLED): no actions
 * Illegal transitions surface the server's 409 Korean message.
 */
export function AdminApprovalPage() {
  const [idInput, setIdInput] = useState('');
  const [meeting, setMeeting] = useState<MeetingResponse | null>(null);
  const [lookupError, setLookupError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [working, setWorking] = useState(false);

  // Reason dialog is shared by 반려 (reject) and 모집 취소 (cancel recruitment).
  const [reasonOpen, setReasonOpen] = useState(false);
  const [reasonMode, setReasonMode] = useState<'reject' | 'cancel'>('reject');
  const [reason, setReason] = useState('');
  const [reasonError, setReasonError] = useState<string | null>(null);

  async function handleLookup(event: FormEvent) {
    event.preventDefault();
    setLookupError(null);
    setActionError(null);
    setMeeting(null);
    const id = Number(idInput);
    if (!idInput.trim() || Number.isNaN(id) || id <= 0) {
      setLookupError('유효한 모임 ID를 입력해 주세요.');
      return;
    }
    setLoading(true);
    try {
      setMeeting(await meetingsApi.get(id));
    } catch (error) {
      setLookupError(resolveErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  async function runAction(action: (id: number) => Promise<MeetingResponse>) {
    if (!meeting) return;
    setActionError(null);
    setWorking(true);
    try {
      setMeeting(await action(meeting.id));
    } catch (error) {
      setActionError(resolveErrorMessage(error));
    } finally {
      setWorking(false);
    }
  }

  function openReason(mode: 'reject' | 'cancel') {
    setReasonMode(mode);
    setReason('');
    setReasonError(null);
    setReasonOpen(true);
  }

  async function handleReasonConfirm() {
    if (!meeting) return;
    const trimmed = reason.trim();
    if (!trimmed) {
      setReasonError('사유를 입력해 주세요.');
      return;
    }
    setActionError(null);
    setWorking(true);
    try {
      const updated =
        reasonMode === 'reject'
          ? await adminApi.rejectMeeting(meeting.id, trimmed)
          : await adminApi.confirmRecruitment(meeting.id, false, trimmed);
      setMeeting(updated);
      setReasonOpen(false);
      setReason('');
    } catch (error) {
      setActionError(resolveErrorMessage(error));
    } finally {
      setWorking(false);
    }
  }

  const status = meeting?.status;

  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-xl font-bold">모임 승인·운영</h2>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">모임 조회</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="flex items-end gap-2" onSubmit={handleLookup} noValidate>
            <div className="flex flex-1 flex-col gap-1.5">
              <Label htmlFor="admin-meeting-id">모임 ID</Label>
              <Input
                id="admin-meeting-id"
                data-testid="admin-meeting-id"
                type="number"
                min={1}
                value={idInput}
                onChange={(e) => setIdInput(e.target.value)}
              />
            </div>
            <Button type="submit" data-testid="admin-lookup" disabled={loading}>
              {loading ? '조회 중...' : '조회'}
            </Button>
          </form>
          {lookupError && (
            <p
              role="alert"
              className="mt-2 text-sm text-destructive"
              data-testid="admin-lookup-error"
            >
              {lookupError}
            </p>
          )}
          {/* Approval-queue listing lands in Bolt 8 (U9); lookup-by-id for now. */}
        </CardContent>
      </Card>

      {meeting && (
        <Card data-testid="admin-meeting-detail">
          <CardHeader className="flex-row items-start justify-between gap-2 space-y-0">
            <CardTitle className="text-base">{meeting.title}</CardTitle>
            <Badge
              variant={meetingStatusVariant(meeting.status)}
              data-testid="admin-meeting-status"
            >
              {meetingStatusLabel(meeting.status)}
            </Badge>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            <div className="flex flex-col gap-1 text-sm text-muted-foreground">
              {meeting.topic && <span>주제: {meeting.topic}</span>}
              <span>기간: {meeting.weeks}주</span>
              <span>정원: {meeting.capacity}명</span>
              {meeting.rejectReason && <span>사유: {meeting.rejectReason}</span>}
            </div>

            {actionError && (
              <p role="alert" className="text-sm text-destructive" data-testid="admin-action-error">
                {actionError}
              </p>
            )}

            {status === 'PENDING_APPROVAL' && (
              <div className="flex gap-2">
                <Button
                  className="flex-1"
                  data-testid="admin-approve"
                  disabled={working}
                  onClick={() => runAction((id) => adminApi.approveMeeting(id))}
                >
                  승인
                </Button>
                <Button
                  className="flex-1"
                  variant="destructive"
                  data-testid="admin-reject-open"
                  disabled={working}
                  onClick={() => openReason('reject')}
                >
                  반려
                </Button>
              </div>
            )}

            {status === 'RECRUITING' && (
              <div className="flex gap-2">
                <Button
                  className="flex-1"
                  data-testid="admin-confirm-recruitment"
                  disabled={working}
                  onClick={() => runAction((id) => adminApi.confirmRecruitment(id, true))}
                >
                  모집 확정 (진행)
                </Button>
                <Button
                  className="flex-1"
                  variant="destructive"
                  data-testid="admin-cancel-recruitment-open"
                  disabled={working}
                  onClick={() => openReason('cancel')}
                >
                  모집 취소
                </Button>
              </div>
            )}

            {status === 'READY_TO_START' && (
              <Button
                data-testid="admin-approve-start"
                disabled={working}
                onClick={() => runAction((id) => adminApi.approveStart(id))}
              >
                시작 승인
              </Button>
            )}

            {status === 'IN_PROGRESS' && (
              <Button
                data-testid="admin-complete"
                disabled={working}
                onClick={() => runAction((id) => adminApi.complete(id))}
              >
                완료 처리
              </Button>
            )}

            {(status === 'COMPLETED' ||
              status === 'REJECTED' ||
              status === 'CANCELLED') && (
              <p className="text-sm text-muted-foreground" data-testid="admin-no-action">
                종료된 모임입니다. 추가 작업이 없습니다.
              </p>
            )}
          </CardContent>
        </Card>
      )}

      <Dialog open={reasonOpen} onOpenChange={setReasonOpen}>
        <DialogContent data-testid="reason-dialog">
          <DialogHeader>
            <DialogTitle>{reasonMode === 'reject' ? '반려 사유' : '모집 취소 사유'}</DialogTitle>
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
              data-testid="reason-confirm"
              disabled={working}
              onClick={handleReasonConfirm}
            >
              {reasonMode === 'reject' ? '반려 확정' : '취소 확정'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
