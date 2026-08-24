package com.learnkk.session.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/** 세션 일정 변경 요청(W1). 예정 시각만 갱신한다(멘티 현황 반영, A6). */
public record UpdateSessionRequest(@NotNull OffsetDateTime scheduledAt) {}
