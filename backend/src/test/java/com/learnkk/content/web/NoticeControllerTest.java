package com.learnkk.content.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.learnkk.auth.service.AuthService;
import com.learnkk.content.dto.NoticeResponse;
import com.learnkk.content.service.NoticeService;
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

@WebMvcTest(NoticeController.class)
class NoticeControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AuthService authService;
  @MockBean private NoticeService noticeService;

  private NoticeResponse notice() {
    return new NoticeResponse(1L, 10L, 1L, "공지입니다.", OffsetDateTime.parse("2026-01-01T00:00Z"));
  }

  @Test
  void post_asMentor_returns201() throws Exception {
    when(authService.validateSession("m-tok")).thenReturn(new Principal(1L, Role.MENTOR));
    when(noticeService.postNotice(any(), eq(10L), any())).thenReturn(notice());

    mockMvc
        .perform(
            post("/api/meetings/10/notices")
                .header("Authorization", "Bearer m-tok")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"공지입니다.\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.body").value("공지입니다."));
  }

  @Test
  void post_nonOwner_returns403() throws Exception {
    when(authService.validateSession("m-tok")).thenReturn(new Principal(5L, Role.MENTOR));
    when(noticeService.postNotice(any(), eq(10L), any()))
        .thenThrow(new ForbiddenException(ErrorCodes.CONTENT_FORBIDDEN, "권한 없음"));

    mockMvc
        .perform(
            post("/api/meetings/10/notices")
                .header("Authorization", "Bearer m-tok")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"공지입니다.\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(ErrorCodes.CONTENT_FORBIDDEN));
  }

  @Test
  void list_participant_returns200() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(noticeService.listNotices(any(), eq(10L))).thenReturn(List.of(notice()));

    mockMvc
        .perform(get("/api/meetings/10/notices").header("Authorization", "Bearer me-tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].body").value("공지입니다."));
  }

  @Test
  void list_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/meetings/10/notices")).andExpect(status().isUnauthorized());
  }
}
