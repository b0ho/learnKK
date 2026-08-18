package com.learnkk.auth.web;

import com.learnkk.auth.dto.LoginRequest;
import com.learnkk.auth.dto.SessionResponse;
import com.learnkk.auth.dto.SignupRequest;
import com.learnkk.auth.dto.UserResponse;
import com.learnkk.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Registration, login and logout endpoints. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private static final String BEARER_PREFIX = "Bearer ";

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/signup")
  public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
  }

  @PostMapping("/login")
  public ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    if (StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)) {
      authService.logout(authorization.substring(BEARER_PREFIX.length()).trim());
    }
    return ResponseEntity.noContent().build();
  }
}
