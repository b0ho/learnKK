package com.learnkk.meeting.service;

import org.springframework.stereotype.Component;

/**
 * Bolt 2 stub implementation of {@link SessionCompletionGate}. The session module (U5) does not
 * exist yet, so there are no scheduled sessions that could block completion — this gate always
 * permits the IN_PROGRESS -&gt; COMPLETED transition.
 *
 * <p>Bolt 6 (U5) replaces this with a real implementation that reads {@code SessionService} to
 * verify every scheduled session has ended before allowing T6.
 */
@Component
public class NoSessionsCompletionGate implements SessionCompletionGate {

  @Override
  public boolean allScheduledSessionsEnded(Long meetingId) {
    // No sessions module yet (Bolt 6/U5) — nothing to block completion.
    return true;
  }
}
