import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { SurveyBuilder, createEmptyQuestion, type DraftQuestion } from './SurveyBuilder';

function Harness() {
  const [questions, setQuestions] = useState<DraftQuestion[]>([]);
  return <SurveyBuilder questions={questions} onChange={setQuestions} />;
}

describe('SurveyBuilder', () => {
  it('creates an empty question with sane defaults', () => {
    expect(createEmptyQuestion()).toEqual({
      text: '',
      type: 'SHORT_TEXT',
      options: [],
      required: false,
    });
  });

  it('adds, edits type to CHOICE, and removes questions', async () => {
    const user = userEvent.setup();
    render(<Harness />);

    expect(screen.getByTestId('survey-empty')).toBeInTheDocument();

    await user.click(screen.getByTestId('survey-add'));
    expect(screen.getByTestId('survey-question-0')).toBeInTheDocument();

    await user.selectOptions(screen.getByTestId('survey-type-0'), 'CHOICE');
    expect(screen.getByTestId('survey-options-0')).toBeInTheDocument();

    await user.click(screen.getByTestId('survey-remove-0'));
    expect(screen.getByTestId('survey-empty')).toBeInTheDocument();
  });

  it('keeps commas while typing CHOICE options (FR-1 regression)', async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await user.click(screen.getByTestId('survey-add'));
    await user.selectOptions(screen.getByTestId('survey-type-0'), 'CHOICE');

    const options = screen.getByTestId('survey-options-0');
    await user.type(options, '초급, 중급');

    // 쉼표가 입력 도중 사라지지 않고 그대로 유지된다.
    expect(options).toHaveValue('초급, 중급');
  });

  it('reorders questions with the up control', async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await user.click(screen.getByTestId('survey-add'));
    await user.click(screen.getByTestId('survey-add'));
    await user.type(screen.getByTestId('survey-text-0'), '첫번째');
    await user.type(screen.getByTestId('survey-text-1'), '두번째');

    await user.click(screen.getByTestId('survey-up-1'));

    expect(screen.getByTestId('survey-text-0')).toHaveValue('두번째');
    expect(screen.getByTestId('survey-text-1')).toHaveValue('첫번째');
  });
});
