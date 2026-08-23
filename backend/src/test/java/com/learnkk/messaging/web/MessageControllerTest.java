package com.learnkk.messaging.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.learnkk.auth.service.AuthService;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.kernel.web.PageResponse;
import com.learnkk.messaging.dto.MessageResponse;
import com.learnkk.messaging.dto.ThreadSummaryResponse;
import com.learnkk.messaging.dto.UnreadCountResponse;
import com.learnkk.messaging.service.MessageService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MessageController.class)
class MessageControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AuthService authService;
  @MockBean private MessageService messageService;

  private static final String SEND_BODY = "{\"recipientId\":1,\"body\":\"안녕하세요\"}";

  private MessageResponse sample() {
    return new MessageResponse(
        5L, 100L, 2L, "안녕하세요", null, OffsetDateTime.parse("2026-01-01T00:00Z"));
  }

  @Test
  void send_asMentee_returns201() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(messageService.send(any(), eq(1L), eq("안녕하세요"))).thenReturn(sample());

    mockMvc
        .perform(
            post("/api/messages")
                .header("Authorization", "Bearer me-tok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SEND_BODY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(5))
        .andExpect(jsonPath("$.threadId").value(100))
        .andExpect(jsonPath("$.senderId").value(2));
  }

  @Test
  void send_noToken_returns401() throws Exception {
    mockMvc
        .perform(post("/api/messages").contentType(MediaType.APPLICATION_JSON).content(SEND_BODY))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void send_forbidden_returns403() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(messageService.send(any(), eq(1L), any()))
        .thenThrow(new ForbiddenException(ErrorCodes.MESSAGING_FORBIDDEN, "권한 없음"));

    mockMvc
        .perform(
            post("/api/messages")
                .header("Authorization", "Bearer me-tok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SEND_BODY))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(ErrorCodes.MESSAGING_FORBIDDEN));
  }

  @Test
  void send_emptyBody_returns400() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));

    mockMvc
        .perform(
            post("/api/messages")
                .header("Authorization", "Bearer me-tok")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientId\":1,\"body\":\"   \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_FAILED));
  }

  @Test
  void listThreads_returns200() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(messageService.listThreads(any()))
        .thenReturn(
            List.of(
                new ThreadSummaryResponse(
                    100L, 1L, "멘토", "안녕", OffsetDateTime.parse("2026-01-01T00:00Z"), 2)));

    mockMvc
        .perform(get("/api/messages/threads").header("Authorization", "Bearer me-tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].threadId").value(100))
        .andExpect(jsonPath("$[0].partnerNickname").value("멘토"))
        .andExpect(jsonPath("$[0].unreadCount").value(2));
  }

  @Test
  void getThread_returns200() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(messageService.getThread(any(), eq(100L), any()))
        .thenReturn(new PageResponse<>(List.of(sample()), 0, 20, 1, 1));

    mockMvc
        .perform(get("/api/messages/threads/100").header("Authorization", "Bearer me-tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].body").value("안녕하세요"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void getThread_forbidden_returns403() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(messageService.getThread(any(), eq(100L), any()))
        .thenThrow(new ForbiddenException(ErrorCodes.MESSAGING_FORBIDDEN, "권한 없음"));

    mockMvc
        .perform(get("/api/messages/threads/100").header("Authorization", "Bearer me-tok"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(ErrorCodes.MESSAGING_FORBIDDEN));
  }

  @Test
  void unreadCount_returns200() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(messageService.unreadCount(any())).thenReturn(new UnreadCountResponse(4L));

    mockMvc
        .perform(get("/api/messages/unread-count").header("Authorization", "Bearer me-tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(4));
  }

  @Test
  void listRecipients_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/messages/recipients")).andExpect(status().isUnauthorized());
  }
}
