package com.learnkk.session.entity;

import java.io.Serializable;
import java.util.Objects;

/** {@link MenteeCompletion} 복합키(meetingId, menteeId). */
public class MenteeCompletionId implements Serializable {

  private Long meetingId;
  private Long menteeId;

  public MenteeCompletionId() {}

  public MenteeCompletionId(Long meetingId, Long menteeId) {
    this.meetingId = meetingId;
    this.menteeId = menteeId;
  }

  public Long getMeetingId() {
    return meetingId;
  }

  public Long getMenteeId() {
    return menteeId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MenteeCompletionId that)) {
      return false;
    }
    return Objects.equals(meetingId, that.meetingId) && Objects.equals(menteeId, that.menteeId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(meetingId, menteeId);
  }
}
