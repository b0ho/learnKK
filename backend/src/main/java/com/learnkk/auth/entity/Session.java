package com.learnkk.auth.entity;

import com.learnkk.kernel.domain.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

/** Server-side session token record. The opaque token is the primary key. */
@Entity
@Table(name = "sessions")
public class Session {

  @Id
  @Column(name = "token")
  private String token;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "expires_at", nullable = false)
  private OffsetDateTime expiresAt;

  @Column(name = "revoked_at")
  private OffsetDateTime revokedAt;

  protected Session() {}

  public Session(String token, Long userId, Role role, OffsetDateTime expiresAt) {
    this.token = token;
    this.userId = userId;
    this.role = role;
    this.expiresAt = expiresAt;
  }

  public String getToken() {
    return token;
  }

  public Long getUserId() {
    return userId;
  }

  public Role getRole() {
    return role;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  public OffsetDateTime getRevokedAt() {
    return revokedAt;
  }

  public void revoke(OffsetDateTime at) {
    this.revokedAt = at;
  }

  public boolean isActive(OffsetDateTime now) {
    return revokedAt == null && expiresAt.isAfter(now);
  }
}
