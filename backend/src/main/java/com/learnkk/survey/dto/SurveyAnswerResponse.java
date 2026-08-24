package com.learnkk.survey.dto;

import com.learnkk.survey.entity.SurveyAnswer;

/** A stored pre-application survey answer (US-3.6 read). */
public record SurveyAnswerResponse(Long questionId, String answerText) {

  public static SurveyAnswerResponse from(SurveyAnswer a) {
    return new SurveyAnswerResponse(a.getQuestionId(), a.getAnswerText());
  }
}
