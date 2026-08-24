package com.learnkk.meeting.service;

/**
 * Read seam for the "all scheduled sessions ended" precondition of T6 (IN_PROGRESS -&gt; COMPLETED,
 * BR-U3-5).
 *
 * <p>The session module (U5) is not built until Bolt 6, so completion cannot yet be gated on real
 * session state. This interface isolates that forward dependency: Bolt 2 ships a permissive stub
 * (a permissive no-op) and Bolt 6 replaces it with an implementation backed by the
 * real {@code SessionService} read (ADR-007 R-2). The status write (COMPLETED) stays owned by U3
 * regardless.
 */
public interface SessionCompletionGate {

  /**
   * @param meetingId the meeting whose sessions are being checked
   * @return {@code true} when every scheduled session of the meeting has ended (or the meeting has
   *     no session constraint yet), {@code false} when completion must be blocked
   */
  boolean allScheduledSessionsEnded(Long meetingId);
}
