package com.learnkk.session.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.learnkk.auth.service.AuthService;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.session.dto.SessionResponse;
import com.learnkk.session.service.SessionService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SessionController.class)
class SessionControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AuthService authService;
  @MockBean private SessionService sessionService;

  private final SessionResponse sample =
      new SessionResponse(5L, 10L, 1, OffsetDateTime.parse("2026-01-01T10:00Z"), 120);

  @Test
  void addSession_asMentor_returns201() throws Exception {
    when(authService.validateSession("m-tok")).thenReturn(new Principal(1L, Role.MENTOR));
    when(sessionService.addSession(any(), eq(10L), any())).thenReturn(sample);

    mockMvc
        .perform(
            post("/api/meetings/10/sessions")
                .header("Authorization", "Bearer m-tok")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"week\":1,\"scheduledAt\":\"2026-01-01T10:00Z\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(5))
        .andExpect(jsonPath("$.checkInWindowMinutes").value(120));
  }

  @Test
  void addSession_noToken_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/meetings/10/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"week\":1,\"scheduledAt\":\"2026-01-01T10:00Z\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void addSession_nonOwner_returns403() throws Exception {
    when(authService.validateSession("m-tok")).thenReturn(new Principal(2L, Role.MENTOR));
    when(sessionService.addSession(any(), eq(10L), any()))
        .thenThrow(new ForbiddenException(ErrorCodes.SESSION_FORBIDDEN, "권한 없음"));

    mockMvc
        .perform(
            post("/api/meetings/10/sessions")
                .header("Authorization", "Bearer m-tok")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"week\":1,\"scheduledAt\":\"2026-01-01T10:00Z\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(ErrorCodes.SESSION_FORBIDDEN));
  }

  @Test
  void updateSession_asMentor_returns200() throws Exception {
    when(authService.validateSession("m-tok")).thenReturn(new Principal(1L, Role.MENTOR));
    when(sessionService.updateSession(any(), eq(5L), any())).thenReturn(sample);

    mockMvc
        .perform(
            put("/api/sessions/5")
                .header("Authorization", "Bearer m-tok")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"scheduledAt\":\"2026-01-02T10:00Z\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(5));
  }

  @Test
  void listSessions_returns200() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(sessionService.listSessions(10L)).thenReturn(List.of(sample));

    mockMvc
        .perform(get("/api/meetings/10/sessions").header("Authorization", "Bearer me-tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(5));
  }

  @Test
  void listSessions_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/meetings/10/sessions")).andExpect(status().isUnauthorized());
  }
}
