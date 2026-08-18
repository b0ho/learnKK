import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import {
  meetingsApi,
  resolveErrorMessage,
  type MeetingResponse,
  type SurveyQuestionDto,
} from '@/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { FieldError } from '@/components/FieldError';
import { PATHS } from '@/routes/paths';
import { SurveyBuilder, type DraftQuestion } from './SurveyBuilder';
import {
  emptyMeetingForm,
  toIsoOrNull,
  validateMeeting,
  type MeetingFieldErrors,
  type MeetingFormValues,
} from './meetingValidation';

function toQuestionDtos(drafts: DraftQuestion[]): SurveyQuestionDto[] {
  return drafts.map((q, index) => ({
    orderNo: index + 1,
    text: q.text.trim(),
    type: q.type,
    options: q.type === 'CHOICE' ? q.options : [],
    required: q.required,
  }));
}

export function MeetingCreatePage() {
  const [values, setValues] = useState<MeetingFormValues>(emptyMeetingForm);
  const [questions, setQuestions] = useState<DraftQuestion[]>([]);
  const [errors, setErrors] = useState<MeetingFieldErrors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [created, setCreated] = useState<MeetingResponse | null>(null);

  function setField<K extends keyof MeetingFormValues>(key: K, value: string) {
    setValues((prev) => ({ ...prev, [key]: value }));
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setFormError(null);
    const nextErrors = validateMeeting(values);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      return;
    }

    const filledQuestions = questions.filter((q) => q.text.trim().length > 0);
    if (filledQuestions.length !== questions.length) {
      setFormError('문항 내용을 모두 입력하거나 빈 문항을 삭제해 주세요.');
      return;
    }

    setSubmitting(true);
    try {
      const meeting = await meetingsApi.create({
        title: values.title.trim(),
        topic: values.topic.trim() || null,
        weeks: Number(values.weeks),
        recruitStart: toIsoOrNull(values.recruitStart),
        recruitEnd: toIsoOrNull(values.recruitEnd),
        capacity: Number(values.capacity),
        format: values.format.trim() || null,
        initialContent: values.initialContent.trim() || null,
      });

      if (filledQuestions.length > 0) {
        await meetingsApi.putQuestions(meeting.id, toQuestionDtos(filledQuestions));
      }
      setCreated(meeting);
    } catch (error) {
      // 403 (non-mentor) / 400 (validation) -> server Korean message.
      setFormError(resolveErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  }

  if (created) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>개설 신청 완료</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          <p className="text-sm text-muted-foreground" data-testid="create-success">
            모임(#{created.id})이 개설 신청되었습니다. 관리자 승인 대기 상태입니다.
          </p>
          <Button asChild variant="outline" data-testid="back-to-meetings">
            <Link to={PATHS.meetings}>모임 목록으로</Link>
          </Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>모임 개설</CardTitle>
      </CardHeader>
      <CardContent>
        <form className="flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
          {formError && (
            <p role="alert" className="text-sm text-destructive" data-testid="create-error">
              {formError}
            </p>
          )}

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="meeting-title">제목</Label>
            <Input
              id="meeting-title"
              data-testid="meeting-title"
              value={values.title}
              aria-invalid={Boolean(errors.title)}
              aria-describedby={errors.title ? 'meeting-title-error' : undefined}
              onChange={(e) => setField('title', e.target.value)}
            />
            <FieldError id="meeting-title-error" message={errors.title} />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="meeting-topic">주제</Label>
            <Input
              id="meeting-topic"
              data-testid="meeting-topic"
              value={values.topic}
              onChange={(e) => setField('topic', e.target.value)}
            />
          </div>

          <div className="flex gap-3">
            <div className="flex flex-1 flex-col gap-1.5">
              <Label htmlFor="meeting-weeks">기간(주)</Label>
              <Input
                id="meeting-weeks"
                data-testid="meeting-weeks"
                type="number"
                min={1}
                value={values.weeks}
                aria-invalid={Boolean(errors.weeks)}
                aria-describedby={errors.weeks ? 'meeting-weeks-error' : undefined}
                onChange={(e) => setField('weeks', e.target.value)}
              />
              <FieldError id="meeting-weeks-error" message={errors.weeks} />
            </div>
            <div className="flex flex-1 flex-col gap-1.5">
              <Label htmlFor="meeting-capacity">정원</Label>
              <Input
                id="meeting-capacity"
                data-testid="meeting-capacity"
                type="number"
                min={1}
                value={values.capacity}
                aria-invalid={Boolean(errors.capacity)}
                aria-describedby={errors.capacity ? 'meeting-capacity-error' : undefined}
                onChange={(e) => setField('capacity', e.target.value)}
              />
              <FieldError id="meeting-capacity-error" message={errors.capacity} />
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="meeting-recruitStart">모집 시작</Label>
            <Input
              id="meeting-recruitStart"
              data-testid="meeting-recruitStart"
              type="datetime-local"
              value={values.recruitStart}
              onChange={(e) => setField('recruitStart', e.target.value)}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="meeting-recruitEnd">모집 종료</Label>
            <Input
              id="meeting-recruitEnd"
              data-testid="meeting-recruitEnd"
              type="datetime-local"
              value={values.recruitEnd}
              aria-invalid={Boolean(errors.recruitEnd)}
              aria-describedby={errors.recruitEnd ? 'meeting-recruitEnd-error' : undefined}
              onChange={(e) => setField('recruitEnd', e.target.value)}
            />
            <FieldError id="meeting-recruitEnd-error" message={errors.recruitEnd} />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="meeting-format">형식</Label>
            <Input
              id="meeting-format"
              data-testid="meeting-format"
              value={values.format}
              placeholder="온라인 / 오프라인 등"
              onChange={(e) => setField('format', e.target.value)}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="meeting-initialContent">소개 내용</Label>
            <Textarea
              id="meeting-initialContent"
              data-testid="meeting-initialContent"
              value={values.initialContent}
              onChange={(e) => setField('initialContent', e.target.value)}
            />
          </div>

          <SurveyBuilder questions={questions} onChange={setQuestions} />

          <Button type="submit" data-testid="meeting-submit" disabled={submitting}>
            {submitting ? '개설 중...' : '개설 신청'}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
