package com.learnkk.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.learnkk.auth.service.AuthService;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.UnauthorizedException;
import com.learnkk.kernel.security.AuthPrincipalArgumentResolver;
import com.learnkk.kernel.security.Principal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SessionAuthInterceptorTest {

  private final AuthService authService = mock(AuthService.class);
  private final SessionAuthInterceptor interceptor = new SessionAuthInterceptor(authService);

  @Test
  void preHandle_allowsCorsPreflight_onProtectedRoute_withoutAuth() {
    // A browser CORS preflight: OPTIONS + Origin + Access-Control-Request-Method, no Bearer token.
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/users/me/profile");
    request.addHeader(HttpHeaders.ORIGIN, "http://localhost:5173");
    request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");

    boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

    assertThat(result).isTrue();
    verifyNoInteractions(authService);
  }

  @Test
  void preHandle_rejectsProtectedRoute_withoutToken() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me/profile");

    assertThatThrownBy(
            () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void preHandle_rejectsEnrollmentApply_withoutToken() {
    MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/meetings/5/enrollments");

    assertThatThrownBy(
            () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void preHandle_rejectsMyEnrollments_withoutToken() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/enrollments/mine");

    assertThatThrownBy(
            () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void preHandle_rejectsApplicantList_withoutToken() {
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/api/meetings/5/applicants");

    assertThatThrownBy(
            () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void preHandle_bindsPrincipal_whenBearerTokenValid() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me/profile");
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer good-token");
    Principal principal = new Principal(7L, Role.MENTEE);
    when(authService.validateSession("good-token")).thenReturn(principal);

    boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

    assertThat(result).isTrue();
    assertThat(request.getAttribute(AuthPrincipalArgumentResolver.PRINCIPAL_ATTRIBUTE))
        .isEqualTo(principal);
  }
}
