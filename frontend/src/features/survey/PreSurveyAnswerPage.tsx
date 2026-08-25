import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  meetingsApi,
  surveyApi,
  resolveErrorMessage,
  type MeetingResponse,
  type SurveyAnswerItem,
  type SurveyQuestionDto,
} from '@/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Spinner } from '@/components/ui/spinner';
import { Textarea } from '@/components/ui/textarea';

/**
 * Pre-application survey answer form for a participating mentee (US-3.6). The form is only usable
 * once the meeting is IN_PROGRESS (②시작 이후); before that a guidance note is shown. Submission
 * maps 409/400 to Korean messages.
 */
export function PreSurveyAnswerPage() {
  const { id } = useParams<{ id: string }>();
  const meetingId = Number(id);

  const [meeting, setMeeting] = useState<MeetingResponse | null>(null);
  const [questions, setQuestions] = useState<SurveyQuestionDto[]>([]);
  const [answers, setAnswers] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [m, qs] = await Promise.all([
        meetingsApi.get(meetingId),
        meetingsApi.getQuestions(meetingId),
      ]);
      setMeeting(m);
      setQuestions(qs);
    } catch (err) {
      setError(resolveErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [meetingId]);

  useEffect(() => {
    void load();
  }, [load]);

  function setAnswer(questionId: number, value: string) {
    setAnswers((prev) => ({ ...prev, [questionId]: value }));
  }

  async function handleSubmit() {
    setSubmitting(true);
    setSubmitError(null);
    setSubmitted(false);
    try {
      const items: SurveyAnswerItem[] = questions
        .filter((q) => q.id != null)
        .map((q) => ({ questionId: q.id as number, answerText: answers[q.id as number] ?? '' }));
      await surveyApi.submitAnswers(meetingId, items);
      setSubmitted(true);
    } catch (err) {
      // 409 PRESURVEY_NOT_OPEN / 400 PRESURVEY_REQUIRED_MISSING / 403 all map to Korean.
      setSubmitError(resolveErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return (
<Spinner data-testid="presurvey-loading" />
    );
  }

  if (error) {
    return (
      <p role="alert" className="text-sm text-destructive" data-testid="presurvey-error">
        {error}
      </p>
    );
  }

  const open = meeting?.status === 'IN_PROGRESS';

  return (
    <div className="flex flex-col gap-4" data-testid="presurvey-page">
      <h2 className="text-xl font-bold">사전설문 응답</h2>
      <p className="text-sm text-muted-foreground">{meeting?.title}</p>

      {!open && (
        <Card>
          <CardContent className="pt-4">
            <p className="text-sm text-muted-foreground" data-testid="presurvey-not-open">
              모임 시작 후 응답 가능합니다.
            </p>
          </CardContent>
        </Card>
      )}

      {open && questions.length === 0 && (
        <p className="text-sm text-muted-foreground" data-testid="presurvey-empty">
          등록된 사전설문 문항이 없습니다.
        </p>
      )}

      {open && questions.length > 0 && (
        <form
          className="flex flex-col gap-4"
          data-testid="presurvey-form"
          onSubmit={(e) => {
            e.preventDefault();
            void handleSubmit();
          }}
        >
          {questions.map((q) => {
            const qid = q.id as number;
            const required = q.required ?? true;
            return (
              <Card key={qid} data-testid={`presurvey-question-${qid}`}>
                <CardHeader>
                  <CardTitle className="text-base">
                    {q.text}
                    {required && (
                      <span className="text-destructive" data-testid={`presurvey-required-${qid}`}>
                        {' '}
                        *
                      </span>
                    )}
                  </CardTitle>
                </CardHeader>
                <CardContent className="flex flex-col gap-2">
                  {q.type === 'CHOICE' ? (
                    <div
                      className="flex flex-col gap-1.5"
                      role="radiogroup"
                      aria-label={q.text}
                      data-testid={`presurvey-choice-${qid}`}
                    >
                      {(q.options ?? []).map((opt) => (
                        <label key={opt} className="flex items-center gap-2 text-sm">
                          <input
                            type="radio"
                            name={`q-${qid}`}
                            value={opt}
                            checked={answers[qid] === opt}
                            onChange={() => setAnswer(qid, opt)}
                            data-testid={`presurvey-option-${qid}-${opt}`}
                          />
                          {opt}
                        </label>
                      ))}
                    </div>
                  ) : q.type === 'LONG_TEXT' ? (
                    <>
                      <Label htmlFor={`presurvey-input-${qid}`} className="sr-only">
                        {q.text}
                      </Label>
                      <Textarea
                        id={`presurvey-input-${qid}`}
                        data-testid={`presurvey-input-${qid}`}
                        value={answers[qid] ?? ''}
                        onChange={(e) => setAnswer(qid, e.target.value)}
                      />
                    </>
                  ) : (
                    <>
                      <Label htmlFor={`presurvey-input-${qid}`} className="sr-only">
                        {q.text}
                      </Label>
                      <Input
                        id={`presurvey-input-${qid}`}
                        data-testid={`presurvey-input-${qid}`}
                        value={answers[qid] ?? ''}
                        onChange={(e) => setAnswer(qid, e.target.value)}
                      />
                    </>
                  )}
                </CardContent>
              </Card>
            );
          })}

          {submitError && (
            <p role="alert" className="text-sm text-destructive" data-testid="presurvey-submit-error">
              {submitError}
            </p>
          )}

          {submitted && (
            <p className="text-sm text-foreground" data-testid="presurvey-submit-success">
              응답이 저장되었습니다.
            </p>
          )}

          <Button
            type="submit"
            className="self-start"
            disabled={submitting}
            data-testid="presurvey-submit"
          >
            {submitting ? '제출 중...' : '응답 제출'}
          </Button>
        </form>
      )}
    </div>
  );
}
