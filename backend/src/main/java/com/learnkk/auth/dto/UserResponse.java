package com.learnkk.auth.dto;

import com.learnkk.auth.entity.User;
import com.learnkk.kernel.domain.Role;

/** Public user view (never carries the password hash). */
public record UserResponse(Long id, String nickname, String employeeNo, Role role) {

  public static UserResponse from(User user) {
    return new UserResponse(user.getId(), user.getNickname(), user.getEmployeeNo(), user.getRole());
  }
}
