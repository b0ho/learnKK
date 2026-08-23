package com.learnkk.meeting.dto;

import com.learnkk.meeting.entity.SurveyQuestion;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

/**
 * Survey question contract shape. {@code id} is populated on read (from the persisted entity) so
 * that answer submission can map answers to question ids; the upsert/write path ignores {@code id}
 * and always persists new question rows.
 */
public record SurveyQuestionDto(
    Long id,
    int orderNo,
    @NotBlank(message = "문항 내용은 필수입니다.") String text,
    @NotBlank(message = "문항 유형은 필수입니다.") String type,
    List<String> options,
    Boolean required) {

  public static SurveyQuestionDto from(SurveyQuestion q) {
    return new SurveyQuestionDto(
        q.getId(),
        q.getOrderNo(),
        q.getText(),
        q.getType(),
        q.getOptions() == null ? new ArrayList<>() : q.getOptions(),
        q.isRequired());
  }
}
