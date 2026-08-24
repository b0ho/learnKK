import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  enrollmentsApi,
  feedbackApi,
  meetingsApi,
  surveyApi,
  resolveErrorMessage,
  type ApplicantResponse,
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
  // 사전설문 응답은 참여자(신청자) 기준으로 독립 표시한다(FR-11). 응답이 있는 참여자만 남긴다.
  const [surveyRespondents, setSurveyRespondents] = useState<
    { menteeId: number; answers: SurveyAnswerResponse[] }[]
  >([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      // 참여자(신청자) 목록으로 사전설문 대상을 정한다 — 피드백 제출 여부와 무관(FR-11).
      const [m, list, applicants] = await Promise.all([
        meetingsApi.get(meetingId),
        feedbackApi.list(meetingId),
        enrollmentsApi.listApplicants(meetingId).catch(() => [] as ApplicantResponse[]),
      ]);
      setMeeting(m);
      setFeedback(list);

      const withAnswers = await Promise.all(
        applicants.map(async (a) => {
          try {
            return { menteeId: a.menteeId, answers: await surveyApi.getMenteeAnswers(meetingId, a.menteeId) };
          } catch {
            return { menteeId: a.menteeId, answers: [] as SurveyAnswerResponse[] };
          }
        }),
      );
      setSurveyRespondents(withAnswers.filter((r) => r.answers.length > 0));
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
    <div className="flex flex-col gap-6" data-testid="feedback-view-page">
      <div>
        <h2 className="text-xl font-bold">피드백 및 사전설문 열람</h2>
        <p className="text-sm text-muted-foreground">{meeting?.title}</p>
      </div>

      {/* 섹션 1: 과정 피드백 (FR-11 분리) */}
      <section className="flex flex-col gap-3" data-testid="feedback-section">
        <h3 className="text-lg font-semibold">과정 피드백</h3>
        {feedback.length === 0 ? (
          <p className="text-sm text-muted-foreground" data-testid="feedback-view-empty">
            아직 제출된 피드백이 없습니다.
          </p>
        ) : (
          <ul className="flex flex-col gap-3" data-testid="feedback-view-list">
            {feedback.map((f) => (
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
                  </CardContent>
                </Card>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* 섹션 2: 사전설문 응답 (FR-11 분리) */}
      <section className="flex flex-col gap-3" data-testid="survey-section">
        <h3 className="text-lg font-semibold">사전설문 응답</h3>
        {surveyRespondents.length === 0 ? (
          <p className="text-sm text-muted-foreground" data-testid="survey-view-empty">
            아직 사전설문 응답이 없습니다.
          </p>
        ) : (
          <ul className="flex flex-col gap-3" data-testid="survey-view-list">
            {surveyRespondents.map(({ menteeId, answers }) => (
              <li key={menteeId}>
                <Card data-testid={`survey-item-${menteeId}`}>
                  <CardHeader>
                    <CardTitle className="text-base">멘티 #{menteeId}</CardTitle>
                  </CardHeader>
                  <CardContent className="text-sm">
                    <ul
                      className="flex flex-col gap-0.5 pl-3"
                      data-testid={`survey-answers-${menteeId}`}
                    >
                      {answers.map((a) => (
                        <li
                          key={a.questionId}
                          data-testid={`survey-answer-${menteeId}-${a.questionId}`}
                        >
                          {a.answerText}
                        </li>
                      ))}
                    </ul>
                  </CardContent>
                </Card>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
