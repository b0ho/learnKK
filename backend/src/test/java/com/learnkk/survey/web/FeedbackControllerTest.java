package com.learnkk.survey.web;

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
import com.learnkk.survey.dto.FeedbackResponse;
import com.learnkk.survey.service.FeedbackService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FeedbackController.class)
class FeedbackControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AuthService authService;
  @MockBean private FeedbackService feedbackService;

  private static final String BODY = "{\"content\":\"좋았습니다\"}";

  @Test
  void submit_asMentee_returns201() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(feedbackService.submitFeedback(any(), eq(10L), eq("좋았습니다")))
        .thenReturn(
            new FeedbackResponse(1L, 2L, "좋았습니다", OffsetDateTime.parse("2026-01-01T00:00Z")));

    mockMvc
        .perform(
            post("/api/meetings/10/feedback")
                .header("Authorization", "Bearer me-tok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.content").value("좋았습니다"));
  }

  @Test
  void submit_noToken_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/meetings/10/feedback").contentType(MediaType.APPLICATION_JSON).content(BODY))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void list_owningMentor_returns200() throws Exception {
    when(authService.validateSession("m-tok")).thenReturn(new Principal(1L, Role.MENTOR));
    when(feedbackService.listFeedback(any(), eq(10L)))
        .thenReturn(
            List.of(
                new FeedbackResponse(1L, 2L, "좋았습니다", OffsetDateTime.parse("2026-01-01T00:00Z"))));

    mockMvc
        .perform(get("/api/meetings/10/feedback").header("Authorization", "Bearer m-tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].menteeId").value(2))
        .andExpect(jsonPath("$[0].content").value("좋았습니다"));
  }

  @Test
  void list_otherMentor_returns403() throws Exception {
    when(authService.validateSession("m-tok")).thenReturn(new Principal(5L, Role.MENTOR));
    when(feedbackService.listFeedback(any(), eq(10L)))
        .thenThrow(new ForbiddenException(ErrorCodes.FEEDBACK_FORBIDDEN, "권한 없음"));

    mockMvc
        .perform(get("/api/meetings/10/feedback").header("Authorization", "Bearer m-tok"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(ErrorCodes.FEEDBACK_FORBIDDEN));
  }

  @Test
  void list_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/meetings/10/feedback")).andExpect(status().isUnauthorized());
  }
}
