package com.learnkk.enrollment.web;

import com.learnkk.enrollment.dto.ApplicantResponse;
import com.learnkk.enrollment.dto.EnrollmentResponse;
import com.learnkk.enrollment.service.EnrollmentService;
import com.learnkk.kernel.security.AuthPrincipal;
import com.learnkk.kernel.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Enrollment endpoints: apply / cancel / applicant listing / the mentee's own enrollments. */
@RestController
public class EnrollmentController {

  private final EnrollmentService enrollmentService;

  public EnrollmentController(EnrollmentService enrollmentService) {
    this.enrollmentService = enrollmentService;
  }

  @PostMapping("/api/meetings/{id}/enrollments")
  public ResponseEntity<EnrollmentResponse> apply(
      @AuthPrincipal Principal principal, @PathVariable Long id) {
    return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.apply(principal, id));
  }

  @DeleteMapping("/api/meetings/{id}/enrollments/mine")
  public ResponseEntity<Void> cancel(@AuthPrincipal Principal principal, @PathVariable Long id) {
    enrollmentService.cancel(principal, id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/api/meetings/{id}/applicants")
  public ResponseEntity<List<ApplicantResponse>> listApplicants(
      @AuthPrincipal Principal principal, @PathVariable Long id) {
    return ResponseEntity.ok(enrollmentService.listApplicants(principal, id));
  }

  @GetMapping("/api/enrollments/mine")
  public ResponseEntity<List<EnrollmentResponse>> listMine(@AuthPrincipal Principal principal) {
    return ResponseEntity.ok(enrollmentService.listMyEnrollments(principal));
  }
}
