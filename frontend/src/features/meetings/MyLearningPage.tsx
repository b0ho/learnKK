import { useState, type FormEvent } from 'react';
import { meetingsApi, resolveErrorMessage, type MeetingResponse } from '@/api';
import { useAuth } from '@/auth/useAuth';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { meetingStatusLabel, meetingStatusVariant } from '@/features/shared/meetingStatus';

/**
 * "내 러닝" tab. Role-adaptive minimal slice for Bolt 1:
 *  - MENTOR: lookup-by-id operation hub. The backend exposes no listMyMeetings
 *    endpoint yet, so mentors track a meeting by id (placeholder for the future
 *    "my meetings" listing).
 *  - MENTEE: application area is not implemented in Bolt 1 (guidance only).
 */
export function MyLearningPage() {
  const { role } = useAuth();

  if (role === 'MENTOR') {
    return <MentorHub />;
  }

  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-xl font-bold">내 러닝</h2>
      <Card>
        <CardContent className="pt-4">
          <p className="text-sm text-muted-foreground" data-testid="my-learning-placeholder">
            모임 신청/참여 기능은 다음 단계에서 제공될 예정입니다. 지금은 &lsquo;모임&rsquo; 탭에서
            모집중인 모임을 둘러볼 수 있습니다.
          </p>
        </CardContent>
      </Card>
    </div>
  );
}

function MentorHub() {
  const [idInput, setIdInput] = useState('');
  const [meeting, setMeeting] = useState<MeetingResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleLookup(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setMeeting(null);
    const id = Number(idInput);
    if (!idInput.trim() || Number.isNaN(id) || id <= 0) {
      setError('유효한 모임 ID를 입력해 주세요.');
      return;
    }
    setLoading(true);
    try {
      setMeeting(await meetingsApi.get(id));
    } catch (err) {
      setError(resolveErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-xl font-bold">내 모임 (운영)</h2>
      <p className="text-sm text-muted-foreground" data-testid="mentor-hub-note">
        내 모임 목록 조회는 다음 단계에서 제공됩니다. 지금은 개설한 모임 ID로 상태를 확인할 수 있습니다.
      </p>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">모임 상태 확인</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="flex items-end gap-2" onSubmit={handleLookup} noValidate>
            <div className="flex flex-1 flex-col gap-1.5">
              <Label htmlFor="mentor-meeting-id">모임 ID</Label>
              <Input
                id="mentor-meeting-id"
                data-testid="mentor-meeting-id"
                type="number"
                min={1}
                value={idInput}
                onChange={(e) => setIdInput(e.target.value)}
              />
            </div>
            <Button type="submit" data-testid="mentor-lookup" disabled={loading}>
              {loading ? '조회 중...' : '조회'}
            </Button>
          </form>
          {error && (
            <p role="alert" className="mt-2 text-sm text-destructive" data-testid="mentor-lookup-error">
              {error}
            </p>
          )}
        </CardContent>
      </Card>

      {meeting && (
        <Card data-testid="mentor-meeting-detail">
          <CardHeader className="flex-row items-start justify-between gap-2 space-y-0">
            <CardTitle className="text-base">{meeting.title}</CardTitle>
            <Badge variant={meetingStatusVariant(meeting.status)}>
              {meetingStatusLabel(meeting.status)}
            </Badge>
          </CardHeader>
          <CardContent className="flex flex-col gap-1 text-sm text-muted-foreground">
            {meeting.topic && <span>주제: {meeting.topic}</span>}
            <span>기간: {meeting.weeks}주</span>
            <span>정원: {meeting.capacity}명</span>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
