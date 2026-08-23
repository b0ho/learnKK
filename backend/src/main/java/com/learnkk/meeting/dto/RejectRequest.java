package com.learnkk.meeting.dto;

/** Rejection payload. Reason is optional in Bolt 1 (mandatory reason deferred to Bolt 2+). */
public record RejectRequest(String reason) {}
