package com.learnkk.enrollment.repository;

import com.learnkk.enrollment.domain.EnrollmentStatus;
import com.learnkk.enrollment.entity.Enrollment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

  int countByMeetingIdAndStatus(Long meetingId, EnrollmentStatus status);

  boolean existsByMeetingIdAndMenteeIdAndStatus(
      Long meetingId, Long menteeId, EnrollmentStatus status);

  Optional<Enrollment> findByMeetingIdAndMenteeId(Long meetingId, Long menteeId);

  List<Enrollment> findByMeetingIdAndStatus(Long meetingId, EnrollmentStatus status);

  List<Enrollment> findByMenteeIdOrderByAppliedAtDesc(Long menteeId);

  /** Whether a mentee holds an active (APPLIED) enrollment in any of the given meetings. */
  boolean existsByMeetingIdInAndMenteeIdAndStatus(
      Collection<Long> meetingIds, Long menteeId, EnrollmentStatus status);

  /** Meeting ids in which the mentee holds an active enrollment. */
  @Query("SELECT e.meetingId FROM Enrollment e WHERE e.menteeId = :menteeId AND e.status = :status")
  List<Long> findMeetingIdsByMenteeIdAndStatus(
      @Param("menteeId") Long menteeId, @Param("status") EnrollmentStatus status);

  /** Distinct mentee ids holding an active enrollment in any of the given meetings. */
  @Query(
      "SELECT DISTINCT e.menteeId FROM Enrollment e "
          + "WHERE e.meetingId IN :meetingIds AND e.status = :status")
  List<Long> findMenteeIdsByMeetingIdInAndStatus(
      @Param("meetingIds") Collection<Long> meetingIds, @Param("status") EnrollmentStatus status);

  /**
   * Active-enrollment counts grouped by meeting for a set of meetings — a single batch query that
   * avoids N+1 counting when a list endpoint needs each meeting's fill (U4 capacity display). Each
   * row is {@code [meetingId (Long), count (Long)]}; meetings with zero active enrollments are
   * simply absent from the result.
   */
  @Query(
      "SELECT e.meetingId, COUNT(e) FROM Enrollment e "
          + "WHERE e.meetingId IN :meetingIds AND e.status = :status GROUP BY e.meetingId")
  List<Object[]> countByMeetingIdInAndStatusGrouped(
      @Param("meetingIds") Collection<Long> meetingIds, @Param("status") EnrollmentStatus status);

  /**
   * Acquires a transaction-scoped PostgreSQL advisory lock keyed by the meeting id (BR-U4-1). The
   * lock is held until the surrounding transaction commits, so the next {@code apply} for the same
   * meeting observes the committed insert before it counts — making count-then-insert race-free
   * without ever locking the U3-owned meetings row.
   */
  @Query(value = "SELECT pg_advisory_xact_lock(:key)", nativeQuery = true)
  void lockMeeting(@Param("key") long key);
}
