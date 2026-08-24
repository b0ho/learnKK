package com.learnkk.messaging.dto;

/** Total unread messages for the caller — drives the polling badge. */
public record UnreadCountResponse(long count) {}
