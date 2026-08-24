package com.learnkk.session.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * 세션 생성 요청(W1). {@code checkInWindowMinutes} 미지정 시 기본 120분(BR-U5-2)을 적용한다.
 */
public record CreateSessionRequest(
    @NotNull @Min(1) Integer week,
    @NotNull OffsetDateTime scheduledAt,
    @Min(1) Integer checkInWindowMinutes) {}
