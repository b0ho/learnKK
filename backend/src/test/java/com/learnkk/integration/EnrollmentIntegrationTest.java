package com.learnkk.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkk.auth.entity.User;
import com.learnkk.auth.repository.UserRepository;
import com.learnkk.kernel.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * End-to-end enrollment flow: apply -> applicant listing -> cancel -> re-apply by another mentee
 * once the seat frees, exercising capacity, duplicate and cancel rules through the full stack.
 */
class EnrollmentIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void applyListCancelReapply_endToEnd() throws Exception {
    String mentorToken = signupAndLogin("mentorE", "enr-mentor", "MENTOR");
    String adminToken = createAdminAndLogin("adminE", "enr-admin");
    String menteeAToken = signupAndLogin("menteeA", "enr-a", "MENTEE");
    String menteeBToken = signupAndLogin("menteeB", "enr-b", "MENTEE");

    // Mentor creates a capacity-1 meeting; admin approves -> RECRUITING.
    long meetingId = createMeeting(mentorToken, 1);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/admin/meetings/" + meetingId + "/approve")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RECRUITING"));

    // Mentee A applies -> 201 APPLIED.
    mockMvc
        .perform(apply(meetingId, menteeAToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("APPLIED"));

    // Owning mentor sees exactly one applicant with the nickname.
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/meetings/" + meetingId + "/applicants")
                .header("Authorization", "Bearer " + mentorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].nickname").value("menteeA"));

    // Capacity is full: mentee B is rejected.
    mockMvc
        .perform(apply(meetingId, menteeBToken))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ENROLLMENT_FULL"));

    // Mentee A cancels -> 204, freeing the seat.
    mockMvc
        .perform(
            MockMvcRequestBuilders.delete("/api/meetings/" + meetingId + "/enrollments/mine")
                .header("Authorization", "Bearer " + menteeAToken))
        .andExpect(status().isNoContent());

    // Seat freed: mentee B now succeeds.
    mockMvc
        .perform(apply(meetingId, menteeBToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("APPLIED"));

    // An already-APPLIED mentee re-applying is the true duplicate (BR-U4-2).
    mockMvc
        .perform(apply(meetingId, menteeBToken))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ENROLLMENT_DUPLICATE"));

    // Mentee A may re-apply (FR-12: a CANCELLED row is reused), but the seat is taken by B,
    // so the capacity check rejects with ENROLLMENT_FULL — the row stays CANCELLED.
    mockMvc
        .perform(apply(meetingId, menteeAToken))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ENROLLMENT_FULL"));

    // Mentee A's own enrollment listing shows the cancelled application.
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/enrollments/mine")
                .header("Authorization", "Bearer " + menteeAToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].status").value("CANCELLED"));

    // A non-owning, non-admin mentee cannot read the applicant list.
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/meetings/" + meetingId + "/applicants")
                .header("Authorization", "Bearer " + menteeBToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ENROLLMENT_FORBIDDEN"));
  }

  @Test
  void applyToNonRecruitingMeeting_returns409NotOpen() throws Exception {
    String mentorToken = signupAndLogin("mentorNR", "nr-mentor", "MENTOR");
    String menteeToken = signupAndLogin("menteeNR", "nr-mentee", "MENTEE");
    // Meeting is created but never approved -> PENDING_APPROVAL.
    long meetingId = createMeeting(mentorToken, 5);

    mockMvc
        .perform(apply(meetingId, menteeToken))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ENROLLMENT_NOT_OPEN"));
  }

  @Test
  void mentorApply_returns403() throws Exception {
    String mentorToken = signupAndLogin("mentorF", "f-mentor", "MENTOR");
    String adminToken = createAdminAndLogin("adminF", "f-admin");
    long meetingId = createMeeting(mentorToken, 5);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/admin/meetings/" + meetingId + "/approve")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());

    // A mentor is not an application subject.
    mockMvc
        .perform(apply(meetingId, mentorToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ENROLLMENT_FORBIDDEN"));
  }

  private org.springframework.test.web.servlet.RequestBuilder apply(long meetingId, String token) {
    return MockMvcRequestBuilders.post("/api/meetings/" + meetingId + "/enrollments")
        .header("Authorization", "Bearer " + token);
  }

  private long createMeeting(String mentorToken, int capacity) throws Exception {
    MvcResult created =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/meetings")
                    .header("Authorization", "Bearer " + mentorToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        String.format(
                            "{\"title\":\"신청 테스트\",\"topic\":\"backend\",\"weeks\":8,"
                                + "\"capacity\":%d,\"format\":\"online\"}",
                            capacity)))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
  }

  private String signupAndLogin(String nickname, String employeeNo, String role) throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    String.format(
                        "{\"nickname\":\"%s\",\"password\":\"password1\",\"employeeNo\":\"%s\",\"role\":\"%s\"}",
                        nickname, employeeNo, role)))
        .andExpect(status().isCreated());
    return login(nickname);
  }

  private String createAdminAndLogin(String nickname, String employeeNo) throws Exception {
    userRepository.save(
        new User(
            nickname, passwordEncoder.encode("password1"), employeeNo.toUpperCase(), Role.ADMIN));
    return login(nickname);
  }

  private String login(String nickname) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        String.format(
                            "{\"nickname\":\"%s\",\"password\":\"password1\"}", nickname)))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
    return node.get("token").asText();
  }
}