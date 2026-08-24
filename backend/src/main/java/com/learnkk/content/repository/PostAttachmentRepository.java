package com.learnkk.content.repository;

import com.learnkk.content.dto.AttachmentResponse;
import com.learnkk.content.entity.PostAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostAttachmentRepository extends JpaRepository<PostAttachment, Long> {

  /**
   * Attachment metadata for a post as a projection — selecting only the metadata columns so the
   * {@code bytea} payload is never loaded for listings (the metadata-vs-payload split from the
   * design). Payloads are read in full only on download via {@code findById}.
   */
  @Query(
      "select new com.learnkk.content.dto.AttachmentResponse("
          + "a.id, a.postId, a.fileName, a.contentType, a.sizeBytes, a.uploaderId, a.createdAt) "
          + "from PostAttachment a where a.postId = :postId order by a.createdAt asc")
  List<AttachmentResponse> findMetaByPostId(Long postId);

  int countByPostId(Long postId);
}
