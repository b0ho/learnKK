package com.learnkk.messaging.dto;

import java.time.OffsetDateTime;

/**
 * One row of the caller's thread list: the partner, the latest message preview and unread count.
 */
public record ThreadSummaryResponse(
    Long threadId,
    Long partnerId,
    String partnerNickname,
    String lastMessageBody,
    OffsetDateTime lastMessageAt,
    int unreadCount) {}
