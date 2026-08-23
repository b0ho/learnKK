package com.learnkk.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.auth.dto.LoginRequest;
import com.learnkk.auth.dto.SessionResponse;
import com.learnkk.auth.dto.SignupRequest;
import com.learnkk.auth.dto.UserResponse;
import com.learnkk.auth.entity.Session;
import com.learnkk.auth.entity.User;
import com.learnkk.auth.repository.ProfileRepository;
import com.learnkk.auth.repository.SessionRepository;
import com.learnkk.auth.repository.UserRepository;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ConflictException;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.UnauthorizedException;
import com.learnkk.kernel.error.ValidationException;
import com.learnkk.kernel.security.Principal;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private ProfileRepository profileRepository;
  @Mock private SessionRepository sessionRepository;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private AuthService authService;

  @Test
  void signup_happyPath_createsUserAndEmptyProfile() {
    SignupRequest request = new SignupRequest("dev", "password1", " e-123 ", "MENTEE");
    when(userRepository.existsByEmployeeNo("E-123")).thenReturn(false);
    when(userRepository.existsByNickname("dev")).thenReturn(false);
    when(passwordEncoder.encode("password1")).thenReturn("hashed");
    when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    UserResponse response = authService.signup(request);

    assertThat(response.nickname()).isEqualTo("dev");
    assertThat(response.employeeNo()).isEqualTo("E-123"); // normalized upper+trim
    assertThat(response.role()).isEqualTo(Role.MENTEE);
    verify(profileRepository).save(any());
  }

  @Test
  void signup_duplicateEmployeeNo_conflict() {
    SignupRequest request = new SignupRequest("dev", "password1", "E-1", "MENTOR");
    when(userRepository.existsByEmployeeNo("E-1")).thenReturn(true);

    assertThatThrownBy(() -> authService.signup(request))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.DUPLICATE_EMPLOYEE_NO);
    verify(userRepository, never()).saveAndFlush(any());
  }

  @Test
  void signup_duplicateNickname_conflict() {
    SignupRequest request = new SignupRequest("dev", "password1", "E-1", "MENTOR");
    when(userRepository.existsByEmployeeNo("E-1")).thenReturn(false);
    when(userRepository.existsByNickname("dev")).thenReturn(true);

    assertThatThrownBy(() -> authService.signup(request))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.DUPLICATE_NICKNAME);
  }

  @Test
  void signup_adminRole_forbidden400() {
    SignupRequest request = new SignupRequest("boss", "password1", "E-9", "ADMIN");

    assertThatThrownBy(() -> authService.signup(request))
        .isInstanceOf(ValidationException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.ADMIN_SIGNUP_FORBIDDEN);
  }

  @Test
  void signup_unknownRole_validation400() {
    SignupRequest request = new SignupRequest("x", "password1", "E-9", "WIZARD");

    assertThatThrownBy(() -> authService.signup(request))
        .isInstanceOf(ValidationException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.VALIDATION_FAILED);
  }

  @Test
  void login_happyPath_returnsSessionToken() {
    User user = new User("dev", "hashed", "E-1", Role.MENTOR);
    when(userRepository.findByNickname("dev")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password1", "hashed")).thenReturn(true);

    SessionResponse response = authService.login(new LoginRequest("dev", "password1"));

    assertThat(response.token()).isNotBlank();
    assertThat(response.role()).isEqualTo(Role.MENTOR);
    verify(sessionRepository).save(any(Session.class));
  }

  @Test
  void login_wrongPassword_unauthorized401() {
    User user = new User("dev", "hashed", "E-1", Role.MENTOR);
    when(userRepository.findByNickname("dev")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("bad", "hashed")).thenReturn(false);

    assertThatThrownBy(() -> authService.login(new LoginRequest("dev", "bad")))
        .isInstanceOf(UnauthorizedException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.AUTH_INVALID_CREDENTIALS);
  }

  @Test
  void login_unknownNickname_unauthorized401() {
    when(userRepository.findByNickname("ghost")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "whatever")))
        .isInstanceOf(UnauthorizedException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.AUTH_INVALID_CREDENTIALS);
    verify(passwordEncoder, never()).matches(anyString(), anyString());
  }

  @Test
  void validateSession_expired_unauthorized401() {
    Session session = new Session("tok", 1L, Role.MENTOR, OffsetDateTime.now().minusHours(1));
    when(sessionRepository.findById("tok")).thenReturn(Optional.of(session));

    assertThatThrownBy(() -> authService.validateSession("tok"))
        .isInstanceOf(UnauthorizedException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.AUTH_SESSION_EXPIRED);
  }

  @Test
  void validateSession_active_returnsPrincipal() {
    Session session = new Session("tok", 42L, Role.ADMIN, OffsetDateTime.now().plusHours(1));
    when(sessionRepository.findById("tok")).thenReturn(Optional.of(session));

    Principal principal = authService.validateSession("tok");

    assertThat(principal.userId()).isEqualTo(42L);
    assertThat(principal.role()).isEqualTo(Role.ADMIN);
  }

  @Test
  void validateSession_missing_unauthorized401() {
    when(sessionRepository.findById("nope")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.validateSession("nope"))
        .isInstanceOf(UnauthorizedException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.AUTH_UNAUTHENTICATED);
  }
}
