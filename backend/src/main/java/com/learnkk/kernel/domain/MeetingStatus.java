package com.learnkk.kernel.domain;

/**
 * Meeting lifecycle status (contract #3). Physical representation is varchar + CHECK in the DB.
 *
 * <p>Bolt 1 exercises only PENDING_APPROVAL -&gt; RECRUITING (T1) and PENDING_APPROVAL -&gt;
 * REJECTED (T2). The remaining transitions are declared for the schema but wired in Bolt 2+.
 */
public enum MeetingStatus {
  PENDING_APPROVAL,
  RECRUITING,
  READY_TO_START,
  IN_PROGRESS,
  COMPLETED,
  REJECTED,
  CANCELLED
}
