package com.learnkk.auth.service;

import com.learnkk.auth.dto.LoginRequest;
import com.learnkk.auth.dto.SessionResponse;
import com.learnkk.auth.dto.SignupRequest;
import com.learnkk.auth.dto.UserResponse;
import com.learnkk.auth.entity.Profile;
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
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration, login, session validation and logout. */
@Service
public class AuthService {

  /** Session lifetime — 12h (U2 assumption). */
  static final long SESSION_TTL_HOURS = 12;

  private static final String INVALID_CREDENTIALS_MESSAGE = "닉네임 또는 비밀번호가 올바르지 않습니다.";

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;
  private final SessionRepository sessionRepository;
  private final PasswordEncoder passwordEncoder;
  private final SecureRandom secureRandom = new SecureRandom();

  public AuthService(
      UserRepository userRepository,
      ProfileRepository profileRepository,
      SessionRepository sessionRepository,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.profileRepository = profileRepository;
    this.sessionRepository = sessionRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public UserResponse signup(SignupRequest request) {
    Role role = parseSignupRole(request.role());

    String employeeNo = request.employeeNo().trim().toUpperCase(Locale.ROOT);
    String nickname = request.nickname().trim();

    if (userRepository.existsByEmployeeNo(employeeNo)) {
      throw new ConflictException(ErrorCodes.DUPLICATE_EMPLOYEE_NO, "이미 등록된 사번입니다.");
    }
    if (userRepository.existsByNickname(nickname)) {
      throw new ConflictException(ErrorCodes.DUPLICATE_NICKNAME, "이미 사용 중인 닉네임입니다.");
    }

    String passwordHash = passwordEncoder.encode(request.password());
    User user = new User(nickname, passwordHash, employeeNo, role);
    try {
      user = userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException race) {
      // Concurrent insert beat the pre-check — map the DB unique violation to 409.
      String detail =
          String.valueOf(race.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
      if (detail.contains("nickname")) {
        throw new ConflictException(ErrorCodes.DUPLICATE_NICKNAME, "이미 사용 중인 닉네임입니다.");
      }
      throw new ConflictException(ErrorCodes.DUPLICATE_EMPLOYEE_NO, "이미 등록된 사번입니다.");
    }

    profileRepository.save(new Profile(user.getId()));
    return UserResponse.from(user);
  }

  @Transactional
  public SessionResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByNickname(request.nickname().trim())
            .orElseThrow(
                () ->
                    new UnauthorizedException(
                        ErrorCodes.AUTH_INVALID_CREDENTIALS, INVALID_CREDENTIALS_MESSAGE));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new UnauthorizedException(
          ErrorCodes.AUTH_INVALID_CREDENTIALS, INVALID_CREDENTIALS_MESSAGE);
    }

    OffsetDateTime expiresAt = OffsetDateTime.now().plus(SESSION_TTL_HOURS, ChronoUnit.HOURS);
    Session session = new Session(generateToken(), user.getId(), user.getRole(), expiresAt);
    sessionRepository.save(session);
    return new SessionResponse(session.getToken(), user.getRole());
  }

  @Transactional(readOnly = true)
  public Principal validateSession(String token) {
    Session session =
        sessionRepository
            .findById(token)
            .orElseThrow(
                () -> new UnauthorizedException(ErrorCodes.AUTH_UNAUTHENTICATED, "세션이 유효하지 않습니다."));
    if (!session.isActive(OffsetDateTime.now())) {
      throw new UnauthorizedException(ErrorCodes.AUTH_SESSION_EXPIRED, "세션이 만료되었습니다.");
    }
    return new Principal(session.getUserId(), session.getRole());
  }

  @Transactional
  public void logout(String token) {
    sessionRepository.findById(token).ifPresent(session -> session.revoke(OffsetDateTime.now()));
  }

  private Role parseSignupRole(String rawRole) {
    Role role;
    try {
      role = Role.valueOf(rawRole.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new ValidationException(ErrorCodes.VALIDATION_FAILED, "역할은 MENTOR 또는 MENTEE여야 합니다.");
    }
    if (role == Role.ADMIN) {
      throw new ValidationException(ErrorCodes.ADMIN_SIGNUP_FORBIDDEN, "관리자 계정은 가입할 수 없습니다.");
    }
    return role;
  }

  private String generateToken() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
