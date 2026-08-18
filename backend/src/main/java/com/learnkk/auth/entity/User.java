package com.learnkk.auth.entity;

import com.learnkk.kernel.domain.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Registered user. Never exposed across the API boundary — controllers use DTOs only. */
@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String nickname;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "employee_no", nullable = false, unique = true)
  private String employeeNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  protected User() {}

  public User(String nickname, String passwordHash, String employeeNo, Role role) {
    this.nickname = nickname;
    this.passwordHash = passwordHash;
    this.employeeNo = employeeNo;
    this.role = role;
  }

  public Long getId() {
    return id;
  }

  public String getNickname() {
    return nickname;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getEmployeeNo() {
    return employeeNo;
  }

  public Role getRole() {
    return role;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
