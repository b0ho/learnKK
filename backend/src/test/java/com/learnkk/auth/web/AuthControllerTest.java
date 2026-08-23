package com.learnkk.auth.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkk.auth.dto.ProfileResponse;
import com.learnkk.auth.dto.SessionResponse;
import com.learnkk.auth.dto.UserResponse;
import com.learnkk.auth.service.AuthService;
import com.learnkk.auth.service.UserService;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ConflictException;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.UnauthorizedException;
import com.learnkk.kernel.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AuthController.class, UserController.class})
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private AuthService authService;
  @MockBean private UserService userService;

  @Test
  void signup_returns201AndUserResponse() throws Exception {
    when(authService.signup(any())).thenReturn(new UserResponse(1L, "dev", "E-1", Role.MENTEE));

    mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"nickname":"dev","password":"password1","employeeNo":"E-1","role":"MENTEE"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.nickname").value("dev"))
        .andExpect(jsonPath("$.role").value("MENTEE"));
  }

  @Test
  void signup_missingNickname_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"password":"password1","employeeNo":"E-1","role":"MENTEE"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_FAILED));
  }

  @Test
  void signup_duplicateEmployeeNo_returns409() throws Exception {
    when(authService.signup(any()))
        .thenThrow(new ConflictException(ErrorCodes.DUPLICATE_EMPLOYEE_NO, "이미 등록된 사번입니다."));

    mockMvc
        .perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"nickname":"dev","password":"password1","employeeNo":"E-1","role":"MENTEE"}
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCodes.DUPLICATE_EMPLOYEE_NO));
  }

  @Test
  void login_returns200AndSessionToken() throws Exception {
    when(authService.login(any())).thenReturn(new SessionResponse("tok-123", Role.MENTOR));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"nickname":"dev","password":"password1"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("tok-123"))
        .andExpect(jsonPath("$.role").value("MENTOR"));
  }

  @Test
  void login_badCredentials_returns401() throws Exception {
    when(authService.login(any()))
        .thenThrow(
            new UnauthorizedException(
                ErrorCodes.AUTH_INVALID_CREDENTIALS, "닉네임 또는 비밀번호가 올바르지 않습니다."));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"nickname":"dev","password":"bad"}
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorCodes.AUTH_INVALID_CREDENTIALS));
  }

  @Test
  void logout_withValidToken_returns204() throws Exception {
    when(authService.validateSession("tok-123")).thenReturn(new Principal(1L, Role.MENTOR));

    mockMvc
        .perform(post("/api/auth/logout").header("Authorization", "Bearer tok-123"))
        .andExpect(status().isNoContent());
    verify(authService).logout("tok-123");
  }

  @Test
  void logout_withoutToken_returns401() throws Exception {
    mockMvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
  }

  @Test
  void getProfile_authenticated_returns200() throws Exception {
    when(authService.validateSession("tok-123")).thenReturn(new Principal(1L, Role.MENTOR));
    when(userService.getProfile(1L))
        .thenReturn(new ProfileResponse("dev", "E-1", List.of("java"), "hi"));

    mockMvc
        .perform(get("/api/users/me/profile").header("Authorization", "Bearer tok-123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nickname").value("dev"))
        .andExpect(jsonPath("$.tags[0]").value("java"));
  }

  @Test
  void getProfile_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/users/me/profile")).andExpect(status().isUnauthorized());
  }

  @Test
  void updateProfile_authenticated_returns200() throws Exception {
    when(authService.validateSession("tok-123")).thenReturn(new Principal(1L, Role.MENTOR));
    when(userService.updateProfile(eq(1L), eq(1L), any()))
        .thenReturn(new ProfileResponse("dev", "E-1", List.of("a"), "intro"));

    mockMvc
        .perform(
            put("/api/users/me/profile")
                .header("Authorization", "Bearer tok-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"tags":["a"],"intro":"intro"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.intro").value("intro"));
  }
}
