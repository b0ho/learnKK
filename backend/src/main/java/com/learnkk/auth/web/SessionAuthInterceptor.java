package com.learnkk.auth.web;

import com.learnkk.auth.service.AuthService;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.UnauthorizedException;
import com.learnkk.kernel.security.AuthPrincipalArgumentResolver;
import com.learnkk.kernel.security.Principal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Pre-processes requests to protected routes: parses {@code Authorization: Bearer <token>},
 * validates the session, and stores the resolved {@link Principal} as a request attribute for the
 * {@code @AuthPrincipal} resolver. Unauthenticated access to a protected route yields 401.
 */
@Component
public class SessionAuthInterceptor implements HandlerInterceptor {

  private static final String BEARER_PREFIX = "Bearer ";
  private static final Pattern MEETING_QUESTIONS =
      Pattern.compile("^/api/meetings/\\d+/questions$");
  private static final Pattern MEETING_ENROLLMENTS =
      Pattern.compile("^/api/meetings/\\d+/enrollments$");
  private static final Pattern MEETING_ENROLLMENTS_MINE =
      Pattern.compile("^/api/meetings/\\d+/enrollments/mine$");
  private static final Pattern MEETING_APPLICANTS =
      Pattern.compile("^/api/meetings/\\d+/applicants$");
  // Session routes (U5) — all authenticated.
  private static final Pattern MEETING_SESSIONS =
      Pattern.compile("^/api/meetings/\\d+/sessions$");
  private static final Pattern SESSION_BY_ID = Pattern.compile("^/api/sessions/\\d+$");
  private static final Pattern SESSION_ATTENDANCE =
      Pattern.compile("^/api/sessions/\\d+/attendance$");
  private static final Pattern MEETING_MY_ATTENDANCE =
      Pattern.compile("^/api/meetings/\\d+/my-attendance$");
  private static final Pattern MEETING_COMPLETIONS =
      Pattern.compile("^/api/meetings/\\d+/completions$");
  private static final Pattern MEETING_COMPLETIONS_COMPUTE =
      Pattern.compile("^/api/meetings/\\d+/completions/compute$");

  private final AuthService authService;

  public SessionAuthInterceptor(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    // CORS preflight (OPTIONS) requests carry no credentials and must never be rejected here;
    // otherwise the browser blocks the real request to protected routes (e.g. /api/users/**).
    if (CorsUtils.isPreFlightRequest(request)) {
      return true;
    }
    String token = extractToken(request);
    boolean requiresAuth = isProtected(request.getMethod(), request.getRequestURI());

    if (token != null) {
      // validateSession throws UnauthorizedException on missing/revoked/expired tokens.
      Principal principal = authService.validateSession(token);
      request.setAttribute(AuthPrincipalArgumentResolver.PRINCIPAL_ATTRIBUTE, principal);
    } else if (requiresAuth) {
      throw new UnauthorizedException(ErrorCodes.AUTH_UNAUTHENTICATED, "인증이 필요합니다.");
    }
    return true;
  }

  private String extractToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
      String token = header.substring(BEARER_PREFIX.length()).trim();
      return token.isEmpty() ? null : token;
    }
    return null;
  }

  private boolean isProtected(String method, String path) {
    if (path.startsWith("/api/admin/")) {
      return true;
    }
    if (path.startsWith("/api/users/")) {
      return true;
    }
    if ("/api/auth/logout".equals(path)) {
      return true;
    }
    if ("GET".equalsIgnoreCase(method) && "/api/meetings/mine".equals(path)) {
      return true;
    }
    if ("POST".equalsIgnoreCase(method) && "/api/meetings".equals(path)) {
      return true;
    }
    if ("PUT".equalsIgnoreCase(method) && MEETING_QUESTIONS.matcher(path).matches()) {
      return true;
    }
    // Enrollment routes (U4) are all authenticated — none are public.
    if ("POST".equalsIgnoreCase(method) && MEETING_ENROLLMENTS.matcher(path).matches()) {
      return true;
    }
    if ("DELETE".equalsIgnoreCase(method) && MEETING_ENROLLMENTS_MINE.matcher(path).matches()) {
      return true;
    }
    if ("GET".equalsIgnoreCase(method) && MEETING_APPLICANTS.matcher(path).matches()) {
      return true;
    }
    if ("GET".equalsIgnoreCase(method) && "/api/enrollments/mine".equals(path)) {
      return true;
    }
    // Messaging routes (U7) are all authenticated — none are public.
    if (path.startsWith("/api/messages")) {
      return true;
    }
    // Session routes (U5) — all authenticated (POST 세션생성/출석, PUT 일정변경, GET 현황/조회).
    if ("POST".equalsIgnoreCase(method) && MEETING_SESSIONS.matcher(path).matches()) {
      return true;
    }
    if ("GET".equalsIgnoreCase(method) && MEETING_SESSIONS.matcher(path).matches()) {
      return true;
    }
    if ("PUT".equalsIgnoreCase(method) && SESSION_BY_ID.matcher(path).matches()) {
      return true;
    }
    if ("POST".equalsIgnoreCase(method) && SESSION_ATTENDANCE.matcher(path).matches()) {
      return true;
    }
    if ("GET".equalsIgnoreCase(method) && MEETING_MY_ATTENDANCE.matcher(path).matches()) {
      return true;
    }
    if ("GET".equalsIgnoreCase(method) && MEETING_COMPLETIONS.matcher(path).matches()) {
      return true;
    }
    return "POST".equalsIgnoreCase(method) && MEETING_COMPLETIONS_COMPUTE.matcher(path).matches();
  }
}
