package com.learnkk.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Request to send a direct message to another user. */
public record SendMessageRequest(
    @NotNull(message = "받는 사람을 지정해 주세요.") Long recipientId,
    @NotBlank(message = "쪽지 내용을 입력해 주세요.") String body) {}
