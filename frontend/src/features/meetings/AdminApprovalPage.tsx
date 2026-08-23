import { useState, type FormEvent } from 'react';
import {
  adminApi,
  meetingsApi,
  resolveErrorMessage,
  type MeetingResponse,
} from '@/api';
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
 * Bolt 1 admin approval flow. The backend exposes no "pending approval" listing,
 * so the admin looks a meeting up by id (getMeeting) and then approves/rejects.
 * Placeholder: a dedicated approval-queue listing lands in a later Bolt.
 */
export function AdminApprovalPage() {
  const [idInput, setIdInput] = useState('');
  const [meeting, setMeeting] = useState<MeetingResponse | null>(null);
  const [lookupError, setLookupError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [working, setWorking] = useState(false);

  const [rejectOpen, setRejectOpen] = useState(false);
  const [rejectReason, setRejectReason] = useState('');

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
      const found = await meetingsApi.get(id);
      setMeeting(found);
    } catch (error) {
      setLookupError(resolveErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  async function handleApprove() {
    if (!meeting) return;
    setActionError(null);
    setWorking(true);
    try {
      const updated = await adminApi.approveMeeting(meeting.id);
      setMeeting(updated);
    } catch (error) {
      // 409 MEETING_INVALID_TRANSITION on illegal state -> server Korean message.
      setActionError(resolveErrorMessage(error));
    } finally {
      setWorking(false);
    }
  }

  async function handleReject() {
    if (!meeting) return;
    setActionError(null);
    setWorking(true);
    try {
      const updated = await adminApi.rejectMeeting(meeting.id, rejectReason.trim());
      setMeeting(updated);
      setRejectOpen(false);
      setRejectReason('');
    } catch (error) {
      setActionError(resolveErrorMessage(error));
    } finally {
      setWorking(false);
    }
  }

  const canAct = meeting?.status === 'PENDING_APPROVAL';

  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-xl font-bold">개설 승인</h2>

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
            <p role="alert" className="mt-2 text-sm text-destructive" data-testid="admin-lookup-error">
              {lookupError}
            </p>
          )}
        </CardContent>
      </Card>

      {meeting && (
        <Card data-testid="admin-meeting-detail">
          <CardHeader className="flex-row items-start justify-between gap-2 space-y-0">
            <CardTitle className="text-base">{meeting.title}</CardTitle>
            <Badge variant={meetingStatusVariant(meeting.status)} data-testid="admin-meeting-status">
              {meetingStatusLabel(meeting.status)}
            </Badge>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            <div className="flex flex-col gap-1 text-sm text-muted-foreground">
              {meeting.topic && <span>주제: {meeting.topic}</span>}
              <span>기간: {meeting.weeks}주</span>
              <span>정원: {meeting.capacity}명</span>
              {meeting.rejectReason && <span>반려 사유: {meeting.rejectReason}</span>}
            </div>

            {actionError && (
              <p role="alert" className="text-sm text-destructive" data-testid="admin-action-error">
                {actionError}
              </p>
            )}

            {canAct ? (
              <div className="flex gap-2">
                <Button
                  className="flex-1"
                  data-testid="admin-approve"
                  disabled={working}
                  onClick={handleApprove}
                >
                  승인
                </Button>
                <Button
                  className="flex-1"
                  variant="destructive"
                  data-testid="admin-reject-open"
                  disabled={working}
                  onClick={() => setRejectOpen(true)}
                >
                  반려
                </Button>
              </div>
            ) : (
              <p className="text-sm text-muted-foreground" data-testid="admin-no-action">
                승인 대기 상태의 모임만 처리할 수 있습니다.
              </p>
            )}
          </CardContent>
        </Card>
      )}

      <Dialog open={rejectOpen} onOpenChange={setRejectOpen}>
        <DialogContent data-testid="reject-dialog">
          <DialogHeader>
            <DialogTitle>반려 사유</DialogTitle>
          </DialogHeader>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="reject-reason">사유</Label>
            <Textarea
              id="reject-reason"
              data-testid="reject-reason"
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
            />
          </div>
          <DialogFooter>
            <Button
              variant="destructive"
              data-testid="admin-reject-confirm"
              disabled={working}
              onClick={handleReject}
            >
              반려 확정
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
