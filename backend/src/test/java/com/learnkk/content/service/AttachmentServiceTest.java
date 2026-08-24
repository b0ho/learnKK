package com.learnkk.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.content.domain.AllowedAttachmentType;
import com.learnkk.content.dto.AttachmentResponse;
import com.learnkk.content.entity.Post;
import com.learnkk.content.entity.PostAttachment;
import com.learnkk.content.repository.PostAttachmentRepository;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ValidationException;
import com.learnkk.kernel.security.Principal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

  @Mock private PostAttachmentRepository attachmentRepository;
  @Mock private PostService postService;
  @Mock private ContentAccessService accessService;

  @InjectMocks private AttachmentService attachmentService;

  private final Principal mentor = new Principal(1L, Role.MENTOR);
  private final Principal mentee = new Principal(2L, Role.MENTEE);

  private Post post() {
    return new Post(10L, 1L, 1, "본문");
  }

  @Test
  void upload_validPdf_ok() {
    when(postService.loadPost(100L)).thenReturn(post());
    when(attachmentRepository.countByPostId(100L)).thenReturn(0);
    when(attachmentRepository.save(any(PostAttachment.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    MockMultipartFile file =
        new MockMultipartFile("file", "week1.pdf", "application/pdf", new byte[] {1, 2, 3});

    AttachmentResponse response = attachmentService.upload(mentor, 100L, file);

    assertThat(response.fileName()).isEqualTo("week1.pdf");
    assertThat(response.contentType()).isEqualTo("application/pdf");
    assertThat(response.uploaderId()).isEqualTo(1L);
    verify(accessService).assertOwningMentor(mentor, 10L);
  }

  @Test
  void upload_disallowedType_validation400() {
    when(postService.loadPost(100L)).thenReturn(post());
    MockMultipartFile file =
        new MockMultipartFile("file", "evil.exe", "application/x-msdownload", new byte[] {1, 2, 3});

    assertThatThrownBy(() -> attachmentService.upload(mentor, 100L, file))
        .isInstanceOf(ValidationException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.ATTACHMENT_TYPE_NOT_ALLOWED);
    verify(attachmentRepository, never()).save(any());
  }

  @Test
  void upload_svg_rejectedAsStoredXssVector() {
    when(postService.loadPost(100L)).thenReturn(post());
    MockMultipartFile file =
        new MockMultipartFile("file", "x.svg", "image/svg+xml", new byte[] {1});

    assertThatThrownBy(() -> attachmentService.upload(mentor, 100L, file))
        .isInstanceOf(ValidationException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.ATTACHMENT_TYPE_NOT_ALLOWED);
  }

  @Test
  void upload_overSizeCap_tooLarge400() {
    when(postService.loadPost(100L)).thenReturn(post());
    byte[] big = new byte[(int) AllowedAttachmentType.MAX_SIZE_BYTES + 1];
    MockMultipartFile file = new MockMultipartFile("file", "big.pdf", "application/pdf", big);

    assertThatThrownBy(() -> attachmentService.upload(mentor, 100L, file))
        .isInstanceOf(ValidationException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.ATTACHMENT_TOO_LARGE);
    verify(attachmentRepository, never()).save(any());
  }

  @Test
  void upload_emptyFile_validation400() {
    when(postService.loadPost(100L)).thenReturn(post());
    MockMultipartFile file = new MockMultipartFile("file", "e.pdf", "application/pdf", new byte[0]);

    assertThatThrownBy(() -> attachmentService.upload(mentor, 100L, file))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  void upload_contentTypeWithCharset_stripped() {
    when(postService.loadPost(100L)).thenReturn(post());
    when(attachmentRepository.countByPostId(100L)).thenReturn(0);
    when(attachmentRepository.save(any(PostAttachment.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    MockMultipartFile file =
        new MockMultipartFile("file", "note.txt", "text/plain; charset=utf-8", new byte[] {1});

    AttachmentResponse response = attachmentService.upload(mentor, 100L, file);

    assertThat(response.contentType()).isEqualTo("text/plain");
  }

  @Test
  void download_participant_returnsPayload() {
    PostAttachment attachment =
        new PostAttachment(100L, "week1.pdf", "application/pdf", 3, new byte[] {1, 2, 3}, 1L);
    when(attachmentRepository.findById(7L)).thenReturn(Optional.of(attachment));
    when(postService.loadPost(100L)).thenReturn(post());

    PostAttachment result = attachmentService.download(mentee, 7L);

    assertThat(result.getData()).hasSize(3);
    verify(accessService).assertParticipant(mentee, 10L);
  }
}
