package com.learnkk.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A meeting notice authored by the owning mentor (C5, U6, US-4.3). Read access is restricted to
 * participants (BR-U6-4), enforced in the service layer via U3/U4 reads.
 */
@Entity
@Table(name = "notice")
public class Notice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "meeting_id", nullable = false)
  private Long meetingId;

  @Column(name = "author_id", nullable = false)
  private Long authorId;

  @Column(nullable = false)
  private String body;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  protected Notice() {}

  public Notice(Long meetingId, Long authorId, String body) {
    this.meetingId = meetingId;
    this.authorId = authorId;
    this.body = body;
  }

  public Long getId() {
    return id;
  }

  public Long getMeetingId() {
    return meetingId;
  }

  public Long getAuthorId() {
    return authorId;
  }

  public String getBody() {
    return body;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
