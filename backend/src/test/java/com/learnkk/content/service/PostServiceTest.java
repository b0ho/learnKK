package com.learnkk.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.content.dto.PostCreateRequest;
import com.learnkk.content.dto.PostResponse;
import com.learnkk.content.entity.Post;
import com.learnkk.content.repository.PostAttachmentRepository;
import com.learnkk.content.repository.PostRepository;
import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ValidationException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.meeting.dto.MeetingResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

  @Mock private PostRepository postRepository;
  @Mock private PostAttachmentRepository attachmentRepository;
  @Mock private ContentAccessService accessService;

  @InjectMocks private PostService postService;

  private final Principal mentor = new Principal(1L, Role.MENTOR);

  private MeetingResponse meeting(int weeks) {
    return new MeetingResponse(
        10L,
        1L,
        "Spring",
        "backend",
        weeks,
        null,
        null,
        5,
        "online",
        "intro",
        MeetingStatus.IN_PROGRESS,
        null, null);
  }

  @Test
  void createPost_bodyOnly_noAttachments_ok() {
    when(accessService.assertOwningMentor(mentor, 10L)).thenReturn(meeting(8));
    when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

    PostResponse response =
        postService.createPost(mentor, 10L, new PostCreateRequest(3, "이번 주 자료입니다."));

    assertThat(response.week()).isEqualTo(3);
    assertThat(response.body()).isEqualTo("이번 주 자료입니다.");
    assertThat(response.attachments()).isEmpty();
  }

  @Test
  void createPost_weekOutOfRange_validation400() {
    when(accessService.assertOwningMentor(mentor, 10L)).thenReturn(meeting(8));

    assertThatThrownBy(() -> postService.createPost(mentor, 10L, new PostCreateRequest(9, "본문")))
        .isInstanceOf(ValidationException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.CONTENT_VALIDATION);
    verify(postRepository, never()).save(any());
  }

  @Test
  void listPosts_participant_returnsPostsWithAttachmentMeta() {
    Post post = new Post(10L, 1L, 1, "본문");
    when(postRepository.findByMeetingIdOrderByWeekAscCreatedAtDesc(10L)).thenReturn(List.of(post));
    when(attachmentRepository.findMetaByPostId(any())).thenReturn(List.of());

    List<PostResponse> posts = postService.listPosts(mentor, 10L);

    assertThat(posts).hasSize(1);
    verify(accessService).assertParticipant(mentor, 10L);
  }
}
