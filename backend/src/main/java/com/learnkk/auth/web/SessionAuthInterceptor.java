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
    return "PUT".equalsIgnoreCase(method) && MEETING_QUESTIONS.matcher(path).matches();
  }
}
