import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import type { SurveyQuestionType } from '@/api';

export interface DraftQuestion {
  text: string;
  type: SurveyQuestionType;
  options: string[];
  required: boolean;
}

export function createEmptyQuestion(): DraftQuestion {
  return { text: '', type: 'SHORT_TEXT', options: [], required: false };
}

const TYPE_OPTIONS: { value: SurveyQuestionType; label: string }[] = [
  { value: 'SHORT_TEXT', label: '단답' },
  { value: 'LONG_TEXT', label: '장문' },
  { value: 'CHOICE', label: '선택' },
];

interface SurveyBuilderProps {
  questions: DraftQuestion[];
  onChange: (questions: DraftQuestion[]) => void;
}

/** Add / remove / reorder pre-survey questions with per-type option editing. */
export function SurveyBuilder({ questions, onChange }: SurveyBuilderProps) {
  function update(index: number, patch: Partial<DraftQuestion>) {
    onChange(questions.map((q, i) => (i === index ? { ...q, ...patch } : q)));
  }

  function add() {
    onChange([...questions, createEmptyQuestion()]);
  }

  function remove(index: number) {
    onChange(questions.filter((_, i) => i !== index));
  }

  function move(index: number, direction: -1 | 1) {
    const target = index + direction;
    if (target < 0 || target >= questions.length) {
      return;
    }
    const next = [...questions];
    [next[index], next[target]] = [next[target], next[index]];
    onChange(next);
  }

  return (
    <div className="flex flex-col gap-3" data-testid="survey-builder">
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium">사전 설문 문항</span>
        <Button type="button" size="sm" variant="outline" data-testid="survey-add" onClick={add}>
          문항 추가
        </Button>
      </div>

      {questions.length === 0 && (
        <p className="text-sm text-muted-foreground" data-testid="survey-empty">
          아직 등록된 문항이 없습니다.
        </p>
      )}

      {questions.map((question, index) => (
        <div
          key={index}
          className="flex flex-col gap-2 rounded-md border p-3"
          data-testid={`survey-question-${index}`}
        >
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium">문항 {index + 1}</span>
            <div className="flex gap-1">
              <Button
                type="button"
                size="sm"
                variant="ghost"
                aria-label={`문항 ${index + 1} 위로`}
                data-testid={`survey-up-${index}`}
                disabled={index === 0}
                onClick={() => move(index, -1)}
              >
                ↑
              </Button>
              <Button
                type="button"
                size="sm"
                variant="ghost"
                aria-label={`문항 ${index + 1} 아래로`}
                data-testid={`survey-down-${index}`}
                disabled={index === questions.length - 1}
                onClick={() => move(index, 1)}
              >
                ↓
              </Button>
              <Button
                type="button"
                size="sm"
                variant="ghost"
                aria-label={`문항 ${index + 1} 삭제`}
                data-testid={`survey-remove-${index}`}
                onClick={() => remove(index)}
              >
                삭제
              </Button>
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor={`survey-text-${index}`}>문항 내용</Label>
            <Input
              id={`survey-text-${index}`}
              data-testid={`survey-text-${index}`}
              value={question.text}
              onChange={(e) => update(index, { text: e.target.value })}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor={`survey-type-${index}`}>유형</Label>
            <select
              id={`survey-type-${index}`}
              data-testid={`survey-type-${index}`}
              className="h-10 rounded-md border border-input bg-background px-3 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              value={question.type}
              onChange={(e) =>
                update(index, {
                  type: e.target.value as SurveyQuestionType,
                  options: e.target.value === 'CHOICE' ? question.options : [],
                })
              }
            >
              {TYPE_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          {question.type === 'CHOICE' && (
            <div className="flex flex-col gap-1.5">
              <Label htmlFor={`survey-options-${index}`}>선택지 (쉼표로 구분)</Label>
              <Input
                id={`survey-options-${index}`}
                data-testid={`survey-options-${index}`}
                value={question.options.join(', ')}
                placeholder="예: 초급, 중급, 고급"
                onChange={(e) =>
                  update(index, {
                    options: e.target.value
                      .split(',')
                      .map((o) => o.trim())
                      .filter(Boolean),
                  })
                }
              />
            </div>
          )}

          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              data-testid={`survey-required-${index}`}
              checked={question.required}
              onChange={(e) => update(index, { required: e.target.checked })}
            />
            필수 응답
          </label>
        </div>
      ))}
    </div>
  );
}
