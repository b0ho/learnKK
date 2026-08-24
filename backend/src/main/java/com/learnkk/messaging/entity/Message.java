package com.learnkk.messaging.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A single message within a {@link MessageThread} (C6, U7). Foreign keys are held by id. The
 * read-receipt is recorded on {@code readAt} and set idempotently (only while still null).
 */
@Entity
@Table(name = "message")
public class Message {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "thread_id", nullable = false)
  private Long threadId;

  @Column(name = "sender_id", nullable = false)
  private Long senderId;

  @Column(nullable = false)
  private String body;

  @Column(name = "read_at")
  private OffsetDateTime readAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  protected Message() {}

  public Message(Long threadId, Long senderId, String body) {
    this.threadId = threadId;
    this.senderId = senderId;
    this.body = body;
  }

  public Long getId() {
    return id;
  }

  public Long getThreadId() {
    return threadId;
  }

  public Long getSenderId() {
    return senderId;
  }

  public String getBody() {
    return body;
  }

  public OffsetDateTime getReadAt() {
    return readAt;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
