package com.learnkk.content.repository;

import com.learnkk.content.entity.Post;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

  /** Posts of a meeting ordered by week then recency — the week-by-week 자료실 listing. */
  List<Post> findByMeetingIdOrderByWeekAscCreatedAtDesc(Long meetingId);
}
