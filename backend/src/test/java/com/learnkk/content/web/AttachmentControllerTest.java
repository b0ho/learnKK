package com.learnkk.content.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.learnkk.auth.service.AuthService;
import com.learnkk.content.dto.AttachmentResponse;
import com.learnkk.content.entity.PostAttachment;
import com.learnkk.content.service.AttachmentService;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.error.ValidationException;
import com.learnkk.kernel.security.Principal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AttachmentController.class)
class AttachmentControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AuthService authService;
  @MockBean private AttachmentService attachmentService;

  @Test
  void upload_asMentor_returns201() throws Exception {
    when(authService.validateSession("m-tok")).thenReturn(new Principal(1L, Role.MENTOR));
    when(attachmentService.upload(any(), eq(100L), any()))
        .thenReturn(
            new AttachmentResponse(
                7L,
                100L,
                "week1.pdf",
                "application/pdf",
                3,
                1L,
                OffsetDateTime.parse("2026-01-01T00:00Z")));
    MockMultipartFile file =
        new MockMultipartFile("file", "week1.pdf", "application/pdf", new byte[] {1, 2, 3});

    mockMvc
        .perform(
            multipart("/api/posts/100/attachments")
                .file(file)
                .header("Authorization", "Bearer m-tok"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.fileName").value("week1.pdf"));
  }

  @Test
  void upload_disallowedType_returns400() throws Exception {
    when(authService.validateSession("m-tok")).thenReturn(new Principal(1L, Role.MENTOR));
    when(attachmentService.upload(any(), eq(100L), any()))
        .thenThrow(new ValidationException(ErrorCodes.ATTACHMENT_TYPE_NOT_ALLOWED, "허용되지 않는 형식"));
    MockMultipartFile file =
        new MockMultipartFile("file", "evil.exe", "application/x-msdownload", new byte[] {1});

    mockMvc
        .perform(
            multipart("/api/posts/100/attachments")
                .file(file)
                .header("Authorization", "Bearer m-tok"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCodes.ATTACHMENT_TYPE_NOT_ALLOWED));
  }

  @Test
  void upload_noToken_returns401() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "week1.pdf", "application/pdf", new byte[] {1});
    mockMvc
        .perform(multipart("/api/posts/100/attachments").file(file))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void download_participant_returnsFileWithAttachmentDisposition() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    PostAttachment attachment =
        new PostAttachment(100L, "week1.pdf", "application/pdf", 3, new byte[] {1, 2, 3}, 1L);
    when(attachmentService.download(any(), eq(7L))).thenReturn(attachment);

    mockMvc
        .perform(get("/api/attachments/7").header("Authorization", "Bearer me-tok"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    org.hamcrest.Matchers.containsString("attachment")));
  }

  @Test
  void download_nonParticipant_returns403() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(attachmentService.download(any(), eq(7L)))
        .thenThrow(new ForbiddenException(ErrorCodes.CONTENT_FORBIDDEN, "권한 없음"));

    mockMvc
        .perform(get("/api/attachments/7").header("Authorization", "Bearer me-tok"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(ErrorCodes.CONTENT_FORBIDDEN));
  }

  @Test
  void download_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/attachments/7")).andExpect(status().isUnauthorized());
  }
}
