import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  meetingsApi,
  resolveErrorMessage,
  type MeetingResponse,
  type SurveyQuestionDto,
  type SurveyQuestionType,
} from '@/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Spinner } from '@/components/ui/spinner';
import { PATHS } from '@/routes/paths';
import { SurveyBuilder, createEmptyQuestion, type DraftQuestion } from './SurveyBuilder';

function toDrafts(dtos: SurveyQuestionDto[]): DraftQuestion[] {
  return dtos
    .slice()
    .sort((a, b) => a.orderNo - b.orderNo)
    .map((q) => ({
      text: q.text,
      type: q.type as SurveyQuestionType,
      options: q.options ?? [],
      required: q.required ?? true,
    }));
}

function toDtos(drafts: DraftQuestion[]): SurveyQuestionDto[] {
  return drafts.map((q, i) => ({
    orderNo: i + 1,
    text: q.text.trim(),
    type: q.type,
    options: q.type === 'CHOICE' ? q.options : [],
    required: q.required,
  }));
}

/**
 * 기존 모임의 사전설문 문항을 추가/수정하는 멘토용 화면(FR-10). 백엔드는 모집확정(READY_TO_START)까지
 * 수정을 허용하고 진행 중부터 잠근다(409 MEETING_QUESTIONS_LOCKED) — 그 메시지를 그대로 노출한다.
 */
export function MeetingQuestionsEditPage() {
  const { id } = useParams<{ id: string }>();
  const meetingId = Number(id);

  const [meeting, setMeeting] = useState<MeetingResponse | null>(null);
  const [questions, setQuestions] = useState<DraftQuestion[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [m, qs] = await Promise.all([
        meetingsApi.get(meetingId),
        meetingsApi.getQuestions(meetingId),
      ]);
      setMeeting(m);
      setQuestions(qs.length > 0 ? toDrafts(qs) : [createEmptyQuestion()]);
    } catch (err) {
      setError(resolveErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [meetingId]);

  useEffect(() => {
    void load();
  }, [load]);

  async function handleSave() {
    setSaveError(null);
    setSaved(false);
    const filled = questions.filter((q) => q.text.trim().length > 0);
    if (filled.length !== questions.length) {
      setSaveError('문항 내용을 모두 입력하거나 빈 문항을 삭제해 주세요.');
      return;
    }
    setSaving(true);
    try {
      await meetingsApi.putQuestions(meetingId, toDtos(filled));
      setSaved(true);
    } catch (err) {
      // 진행 중 이후 모임이면 409 MEETING_QUESTIONS_LOCKED → 서버 한글 메시지 노출.
      setSaveError(resolveErrorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
<Spinner data-testid="questions-edit-loading" />
    );
  }

  if (error) {
    return (
      <p role="alert" className="text-sm text-destructive" data-testid="questions-edit-error">
        {error}
      </p>
    );
  }

  return (
    <Card data-testid="questions-edit-page">
      <CardHeader>
        <CardTitle>사전설문 문항 관리</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <p className="text-sm text-muted-foreground">{meeting?.title}</p>

        {saveError && (
          <p role="alert" className="text-sm text-destructive" data-testid="questions-save-error">
            {saveError}
          </p>
        )}
        {saved && (
          <p className="text-sm text-primary" data-testid="questions-save-success">
            문항을 저장했습니다.
          </p>
        )}

        <SurveyBuilder questions={questions} onChange={setQuestions} />

        <div className="flex gap-2">
          <Button onClick={handleSave} disabled={saving} data-testid="questions-save">
            {saving ? '저장 중...' : '문항 저장'}
          </Button>
          <Button asChild variant="outline" data-testid="questions-back">
            <Link to={PATHS.myLearning}>내 러닝으로</Link>
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
