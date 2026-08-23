package com.learnkk.content.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.learnkk.auth.service.AuthService;
import com.learnkk.content.dto.PostResponse;
import com.learnkk.content.service.PostService;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PostController.class)
class PostControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AuthService authService;
  @MockBean private PostService postService;

  private PostResponse samplePost() {
    return new PostResponse(
        1L, 10L, 1L, 1, "본문", List.of(), OffsetDateTime.parse("2026-01-01T00:00Z"), null);
  }

  @Test
  void create_asMentor_returns201() throws Exception {
    when(authService.validateSession("m-tok")).thenReturn(new Principal(1L, Role.MENTOR));
    when(postService.createPost(any(), eq(10L), any())).thenReturn(samplePost());

    mockMvc
        .perform(
            post("/api/meetings/10/posts")
                .header("Authorization", "Bearer m-tok")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"week\":1,\"body\":\"본문\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.attachments").isArray());
  }

  @Test
  void create_missingBody_returns400() throws Exception {
    when(authService.validateSession("m-tok")).thenReturn(new Principal(1L, Role.MENTOR));

    mockMvc
        .perform(
            post("/api/meetings/10/posts")
                .header("Authorization", "Bearer m-tok")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"week\":1}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_FAILED));
  }

  @Test
  void create_noToken_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/meetings/10/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"week\":1,\"body\":\"본문\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void list_participant_returns200() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(postService.listPosts(any(), eq(10L))).thenReturn(List.of(samplePost()));

    mockMvc
        .perform(get("/api/meetings/10/posts").header("Authorization", "Bearer me-tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].week").value(1));
  }

  @Test
  void list_nonParticipant_returns403() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(postService.listPosts(any(), eq(10L)))
        .thenThrow(new ForbiddenException(ErrorCodes.CONTENT_FORBIDDEN, "권한 없음"));

    mockMvc
        .perform(get("/api/meetings/10/posts").header("Authorization", "Bearer me-tok"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(ErrorCodes.CONTENT_FORBIDDEN));
  }

  @Test
  void list_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/meetings/10/posts")).andExpect(status().isUnauthorized());
  }
}
