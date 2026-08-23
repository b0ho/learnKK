package com.learnkk.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;

/** Meeting creation payload. Numeric/period rules are enforced in {@code MeetingService}. */
public record MeetingCreateRequest(
    @NotBlank(message = "제목은 필수입니다.") String title,
    String topic,
    Integer weeks,
    OffsetDateTime recruitStart,
    OffsetDateTime recruitEnd,
    Integer capacity,
    String format,
    String initialContent) {}
