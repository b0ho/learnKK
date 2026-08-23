import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  feedbackApi,
  meetingsApi,
  resolveErrorMessage,
  type MeetingResponse,
} from '@/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';

/**
 * Course feedback submission form for a participating mentee (US-8.1). Submission is accepted while
 * the meeting is IN_PROGRESS or COMPLETED; other states / non-participants map 409/403 to Korean.
 */
export function FeedbackPage() {
  const { id } = useParams<{ id: string }>();
  const meetingId = Number(id);

  const [meeting, setMeeting] = useState<MeetingResponse | null>(null);
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setMeeting(await meetingsApi.get(meetingId));
    } catch (err) {
      setError(resolveErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [meetingId]);

  useEffect(() => {
    void load();
  }, [load]);

  async function handleSubmit() {
    setSubmitting(true);
    setSubmitError(null);
    setSubmitted(false);
    try {
      await feedbackApi.submit(meetingId, content);
      setSubmitted(true);
    } catch (err) {
      setSubmitError(resolveErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return (
      <p className="text-sm text-muted-foreground" data-testid="feedback-loading">
        불러오는 중...
      </p>
    );
  }

  if (error) {
    return (
      <p role="alert" className="text-sm text-destructive" data-testid="feedback-error">
        {error}
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-4" data-testid="feedback-page">
      <h2 className="text-xl font-bold">과정 피드백</h2>
      <p className="text-sm text-muted-foreground">{meeting?.title}</p>

      <Card>
        <CardContent className="pt-4">
          <form
            className="flex flex-col gap-3"
            data-testid="feedback-form"
            onSubmit={(e) => {
              e.preventDefault();
              void handleSubmit();
            }}
          >
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="feedback-content">피드백 내용</Label>
              <Textarea
                id="feedback-content"
                data-testid="feedback-content"
                rows={6}
                value={content}
                onChange={(e) => setContent(e.target.value)}
                placeholder="과정에 대한 의견을 남겨 주세요."
              />
            </div>

            {submitError && (
              <p role="alert" className="text-sm text-destructive" data-testid="feedback-submit-error">
                {submitError}
              </p>
            )}

            {submitted && (
              <p className="text-sm text-foreground" data-testid="feedback-submit-success">
                피드백이 제출되었습니다.
              </p>
            )}

            <Button
              type="submit"
              className="self-start"
              disabled={submitting || content.trim().length === 0}
              data-testid="feedback-submit"
            >
              {submitting ? '제출 중...' : '피드백 제출'}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
