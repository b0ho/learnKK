package com.learnkk.content.service;

import com.learnkk.content.dto.AttachmentResponse;
import com.learnkk.content.dto.PostCreateRequest;
import com.learnkk.content.dto.PostResponse;
import com.learnkk.content.entity.Post;
import com.learnkk.content.repository.PostAttachmentRepository;
import com.learnkk.content.repository.PostRepository;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.NotFoundException;
import com.learnkk.kernel.error.ValidationException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.meeting.dto.MeetingResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Week-post workflow (C5, U6): the owning mentor creates posts (body required, attachments 0..n),
 * participants list them with attachment metadata (US-4.1a/4.2). Participant authorization is
 * delegated to {@link ContentAccessService}.
 */
@Service
public class PostService {

  private final PostRepository postRepository;
  private final PostAttachmentRepository attachmentRepository;
  private final ContentAccessService accessService;

  public PostService(
      PostRepository postRepository,
      PostAttachmentRepository attachmentRepository,
      ContentAccessService accessService) {
    this.postRepository = postRepository;
    this.attachmentRepository = attachmentRepository;
    this.accessService = accessService;
  }

  /**
   * Create a week post (US-4.1a). Owning mentor only (403). Body required (400); week must fall in
   * the meeting's [1, weeks] range (400). Attachments are added afterwards via upload — a post with
   * no attachments is valid.
   */
  @Transactional
  public PostResponse createPost(Principal principal, Long meetingId, PostCreateRequest request) {
    MeetingResponse meeting = accessService.assertOwningMentor(principal, meetingId);
    if (request.week() > meeting.weeks()) {
      throw new ValidationException(
          ErrorCodes.CONTENT_VALIDATION, "주차는 모임 진행 주차(" + meeting.weeks() + ") 이내여야 합니다.");
    }
    Post saved =
        postRepository.save(
            new Post(meetingId, principal.userId(), request.week(), request.body()));
    return PostResponse.from(saved, List.of());
  }

  /** List a meeting's posts with attachment metadata (US-4.2). Participants only (403). */
  @Transactional(readOnly = true)
  public List<PostResponse> listPosts(Principal principal, Long meetingId) {
    accessService.assertParticipant(principal, meetingId);
    return postRepository.findByMeetingIdOrderByWeekAscCreatedAtDesc(meetingId).stream()
        .map(p -> PostResponse.from(p, attachmentMeta(p.getId())))
        .toList();
  }

  /** Load a post enforcing participant access; used by the attachment upload/list flow. */
  @Transactional(readOnly = true)
  public Post requireParticipantPost(Principal principal, Long postId) {
    Post post = loadPost(postId);
    accessService.assertParticipant(principal, post.getMeetingId());
    return post;
  }

  Post loadPost(Long postId) {
    return postRepository
        .findById(postId)
        .orElseThrow(() -> new NotFoundException(ErrorCodes.POST_NOT_FOUND, "게시글을 찾을 수 없습니다."));
  }

  private List<AttachmentResponse> attachmentMeta(Long postId) {
    return attachmentRepository.findMetaByPostId(postId);
  }
}
