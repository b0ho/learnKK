package com.learnkk.kernel.domain;

/**
 * Mentor completion judgment for a meeting (ux-bugfixes-2 FR-7). Unlike mentee completion
 * ({@link CompletionStatus}, driven by the 80% attendance rule), the mentor's completion is decided
 * by the administrator's judgment alone — there is no automatic computation. {@code PENDING} is the
 * initial state before the admin has judged; {@code COMPLETED} / {@code NOT_COMPLETED} are the two
 * terminal verdicts.
 */
public enum MentorCompletionStatus {
  PENDING,
  COMPLETED,
  NOT_COMPLETED
}
