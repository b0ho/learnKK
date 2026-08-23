package com.learnkk.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NoSessionsCompletionGateTest {

  private final NoSessionsCompletionGate gate = new NoSessionsCompletionGate();

  @Test
  void allScheduledSessionsEnded_alwaysTrue_untilSessionsModuleExists() {
    // Bolt 2 stub: no sessions module (U5/Bolt 6) yet, so completion is never blocked.
    assertThat(gate.allScheduledSessionsEnded(1L)).isTrue();
    assertThat(gate.allScheduledSessionsEnded(999L)).isTrue();
  }
}
