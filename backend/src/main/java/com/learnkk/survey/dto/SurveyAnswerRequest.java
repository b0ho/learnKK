package com.learnkk.survey.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Pre-application survey submission: one answer item per question (US-3.6). */
public record SurveyAnswerRequest(@NotNull @Valid List<AnswerItem> answers) {

  /** A single question answer. {@code answerText} may be blank for optional questions. */
  public record AnswerItem(@NotNull Long questionId, String answerText) {}
}
