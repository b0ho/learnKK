package com.learnkk.kernel.security;

import com.learnkk.kernel.domain.Role;

/** Authenticated caller identity resolved from a valid session token. */
public record Principal(Long userId, Role role) {

  public boolean isAdmin() {
    return role == Role.ADMIN;
  }

  public boolean isMentor() {
    return role == Role.MENTOR;
  }

  public boolean isMentee() {
    return role == Role.MENTEE;
  }
}
