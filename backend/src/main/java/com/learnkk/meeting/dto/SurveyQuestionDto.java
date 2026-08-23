package com.learnkk.meeting.dto;

import com.learnkk.meeting.entity.SurveyQuestion;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

/** Survey question contract shape. */
public record SurveyQuestionDto(
    int orderNo,
    @NotBlank(message = "문항 내용은 필수입니다.") String text,
    @NotBlank(message = "문항 유형은 필수입니다.") String type,
    List<String> options,
    Boolean required) {

  public static SurveyQuestionDto from(SurveyQuestion q) {
    return new SurveyQuestionDto(
        q.getOrderNo(),
        q.getText(),
        q.getType(),
        q.getOptions() == null ? new ArrayList<>() : q.getOptions(),
        q.isRequired());
  }
}
