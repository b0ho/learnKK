package com.learnkk.messaging.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A 1:1 direct-message conversation between two users (C6, U7). Participants are stored normalized
 * (participantA &lt; participantB) so a conversation maps to exactly one row (UNIQUE backstop).
 * Foreign keys are held by id (no ORM associations across module boundaries).
 */
@Entity
@Table(name = "message_thread")
public class MessageThread {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "participant_a", nullable = false)
  private Long participantA;

  @Column(name = "participant_b", nullable = false)
  private Long participantB;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "last_message_at")
  private OffsetDateTime lastMessageAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  protected MessageThread() {}

  private MessageThread(Long participantA, Long participantB) {
    this.participantA = participantA;
    this.participantB = participantB;
  }

  /** Create a thread for a pair of users, normalizing the participants so the lower id is A. */
  public static MessageThread of(Long userX, Long userY) {
    long a = Math.min(userX, userY);
    long b = Math.max(userX, userY);
    return new MessageThread(a, b);
  }

  /** The normalized (min, max) key for a pair of users — matches how threads are stored. */
  public static long[] normalizedPair(Long userX, Long userY) {
    return new long[] {Math.min(userX, userY), Math.max(userX, userY)};
  }

  /** Record that a message was just posted, advancing the conversation's ordering timestamp. */
  public void touch(OffsetDateTime at) {
    this.lastMessageAt = at;
  }

  public boolean hasParticipant(Long userId) {
    return participantA.equals(userId) || participantB.equals(userId);
  }

  /** The other participant relative to the given user (null if the user is not a participant). */
  public Long partnerOf(Long userId) {
    if (participantA.equals(userId)) {
      return participantB;
    }
    if (participantB.equals(userId)) {
      return participantA;
    }
    return null;
  }

  public Long getId() {
    return id;
  }

  public Long getParticipantA() {
    return participantA;
  }

  public Long getParticipantB() {
    return participantB;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getLastMessageAt() {
    return lastMessageAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
