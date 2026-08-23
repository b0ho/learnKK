import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  feedbackApi,
  meetingsApi,
  surveyApi,
  resolveErrorMessage,
  type FeedbackResponse,
  type MeetingResponse,
  type SurveyAnswerResponse,
} from '@/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

/**
 * Feedback + pre-survey answer review for the owning mentor or admin (US-8.2, W4). Lists every
 * feedback entry for the meeting and, per mentee, their pre-survey answers (getMenteeAnswers). The
 * route is guarded by RequireRole; the server re-checks ownership (other-mentor 403).
 */
export function FeedbackViewPage() {
  const { id } = useParams<{ id: string }>();
  const meetingId = Number(id);

  const [meeting, setMeeting] = useState<MeetingResponse | null>(null);
  const [feedback, setFeedback] = useState<FeedbackResponse[]>([]);
  const [answersByMentee, setAnswersByMentee] = useState<Record<number, SurveyAnswerResponse[]>>(
    {},
  );
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [m, list] = await Promise.all([
        meetingsApi.get(meetingId),
        feedbackApi.list(meetingId),
      ]);
      setMeeting(m);
      setFeedback(list);

      const pairs = await Promise.all(
        list.map(async (f) => {
          try {
            return [f.menteeId, await surveyApi.getMenteeAnswers(meetingId, f.menteeId)] as const;
          } catch {
            return [f.menteeId, [] as SurveyAnswerResponse[]] as const;
          }
        }),
      );
      setAnswersByMentee(Object.fromEntries(pairs));
    } catch (err) {
      // 403 FEEDBACK_FORBIDDEN for a non-owning mentor maps to Korean.
      setError(resolveErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [meetingId]);

  useEffect(() => {
    void load();
  }, [load]);

  if (loading) {
    return (
      <p className="text-sm text-muted-foreground" data-testid="feedback-view-loading">
        불러오는 중...
      </p>
    );
  }

  if (error) {
    return (
      <p role="alert" className="text-sm text-destructive" data-testid="feedback-view-error">
        {error}
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-4" data-testid="feedback-view-page">
      <h2 className="text-xl font-bold">피드백 및 사전설문 열람</h2>
      <p className="text-sm text-muted-foreground">{meeting?.title}</p>

      {feedback.length === 0 && (
        <p className="text-sm text-muted-foreground" data-testid="feedback-view-empty">
          아직 제출된 피드백이 없습니다.
        </p>
      )}

      {feedback.length > 0 && (
        <ul className="flex flex-col gap-3" data-testid="feedback-view-list">
          {feedback.map((f) => {
            const answers = answersByMentee[f.menteeId] ?? [];
            return (
              <li key={f.id}>
                <Card data-testid={`feedback-item-${f.id}`}>
                  <CardHeader>
                    <CardTitle className="text-base">멘티 #{f.menteeId}</CardTitle>
                  </CardHeader>
                  <CardContent className="flex flex-col gap-2 text-sm">
                    <p data-testid={`feedback-content-${f.id}`}>{f.content}</p>
                    <span className="text-xs text-muted-foreground">
                      {new Date(f.createdAt).toLocaleDateString('ko-KR')}
                    </span>

                    <div className="mt-2 flex flex-col gap-1">
                      <span className="font-medium">사전설문 응답</span>
                      {answers.length === 0 ? (
                        <span
                          className="text-muted-foreground"
                          data-testid={`survey-answers-empty-${f.menteeId}`}
                        >
                          응답 없음
                        </span>
                      ) : (
                        <ul
                          className="flex flex-col gap-0.5 pl-3"
                          data-testid={`survey-answers-${f.menteeId}`}
                        >
                          {answers.map((a) => (
                            <li key={a.questionId} data-testid={`survey-answer-${f.menteeId}-${a.questionId}`}>
                              {a.answerText}
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
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
