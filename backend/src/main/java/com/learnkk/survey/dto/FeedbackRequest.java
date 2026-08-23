package com.learnkk.survey.dto;

import jakarta.validation.constraints.NotBlank;

/** Course feedback submission (US-8.1). Free-form text content. */
public record FeedbackRequest(@NotBlank(message = "피드백 내용은 필수입니다.") String content) {}
