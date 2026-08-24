package com.learnkk.session.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.learnkk.auth.service.AuthService;
import com.learnkk.kernel.domain.CompletionStatus;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ConflictException;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.security.Principal;
import com.learnkk.session.dto.MenteeCompletionResponse;
import com.learnkk.session.service.CompletionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CompletionController.class)
class CompletionControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AuthService authService;
  @MockBean private CompletionService completionService;

  private MenteeCompletionResponse candidate() {
    return new MenteeCompletionResponse(
        10L, 2L, CompletionStatus.COMPLETION_CANDIDATE, 4, 5, null);
  }

  @Test
  void compute_asMentor_returns200() throws Exception {
    when(authService.validateSession("m-tok")).thenReturn(new Principal(1L, Role.MENTOR));
    when(completionService.computeCompletion(any(), eq(10L))).thenReturn(List.of(candidate()));

    mockMvc
        .perform(
            post("/api/meetings/10/completions/compute").header("Authorization", "Bearer m-tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("COMPLETION_CANDIDATE"))
        .andExpect(jsonPath("$[0].attendedCount").value(4));
  }

  @Test
  void compute_noToken_returns401() throws Exception {
    mockMvc
        .perform(post("/api/meetings/10/completions/compute"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void list_asMentor_returns200() throws Exception {
    when(authService.validateSession("m-tok")).thenReturn(new Principal(1L, Role.MENTOR));
    when(completionService.getCompletions(any(), eq(10L))).thenReturn(List.of(candidate()));

    mockMvc
        .perform(get("/api/meetings/10/completions").header("Authorization", "Bearer m-tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].menteeId").value(2));
  }

  @Test
  void approve_asAdmin_returns200() throws Exception {
    when(authService.validateSession("a-tok")).thenReturn(new Principal(9L, Role.ADMIN));
    when(completionService.approveMenteeCompletion(any(), eq(10L), eq(2L)))
        .thenReturn(new MenteeCompletionResponse(10L, 2L, CompletionStatus.COMPLETED, 4, 5, null));

    mockMvc
        .perform(
            post("/api/admin/meetings/10/completions/2/approve")
                .header("Authorization", "Bearer a-tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"));
  }

  @Test
  void approve_alreadyApproved_returns409() throws Exception {
    when(authService.validateSession("a-tok")).thenReturn(new Principal(9L, Role.ADMIN));
    when(completionService.approveMenteeCompletion(any(), eq(10L), eq(2L)))
        .thenThrow(
            new ConflictException(ErrorCodes.COMPLETION_ALREADY_APPROVED, "이미 확정됨"));

    mockMvc
        .perform(
            post("/api/admin/meetings/10/completions/2/approve")
                .header("Authorization", "Bearer a-tok"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCodes.COMPLETION_ALREADY_APPROVED));
  }

  @Test
  void approve_noToken_returns401() throws Exception {
    mockMvc
        .perform(post("/api/admin/meetings/10/completions/2/approve"))
        .andExpect(status().isUnauthorized());
  }
}
