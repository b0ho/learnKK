package com.learnkk.auth.web;

import com.learnkk.auth.dto.ProfileResponse;
import com.learnkk.auth.dto.ProfileUpdateRequest;
import com.learnkk.auth.service.UserService;
import com.learnkk.kernel.security.AuthPrincipal;
import com.learnkk.kernel.security.Principal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Current-user profile endpoints. */
@RestController
@RequestMapping("/api/users/me")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/profile")
  public ResponseEntity<ProfileResponse> getProfile(@AuthPrincipal Principal principal) {
    return ResponseEntity.ok(userService.getProfile(principal.userId()));
  }

  @PutMapping("/profile")
  public ResponseEntity<ProfileResponse> updateProfile(
      @AuthPrincipal Principal principal, @Valid @RequestBody ProfileUpdateRequest request) {
    return ResponseEntity.ok(
        userService.updateProfile(principal.userId(), principal.userId(), request));
  }
}
