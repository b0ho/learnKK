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

/** End-to-end: mentor creates a meeting -> admin approves (T1) -> it appears in recruiting list. */
class MeetingIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void createApproveAndListRecruiting() throws Exception {
    String mentorToken = signupAndLogin("mentorM", "meng-1", "MENTOR");

    // Create meeting -> PENDING_APPROVAL.
    MvcResult created =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/meetings")
                    .header("Authorization", "Bearer " + mentorToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"title":"Spring 스터디","topic":"backend","weeks":8,"capacity":5,"format":"online"}
                        """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
            .andReturn();
    long meetingId =
        objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

    // Recruiting list is empty before approval.
    mockMvc
        .perform(MockMvcRequestBuilders.get("/api/meetings").param("status", "recruiting"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));

    // Admin approves (T1).
    String adminToken = createAdminAndLogin("adminX", "adm-1");
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/admin/meetings/" + meetingId + "/approve")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RECRUITING"));

    // Now it is exposed in the recruiting list.
    mockMvc
        .perform(MockMvcRequestBuilders.get("/api/meetings").param("status", "recruiting"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].id").value(meetingId))
        .andExpect(jsonPath("$.content[0].status").value("RECRUITING"));

    // Double approve is a 409 conflict (conditional UPDATE affects 0 rows).
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/admin/meetings/" + meetingId + "/approve")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isConflict());
  }

  @Test
  void nonAdminApprove_returns403() throws Exception {
    String mentorToken = signupAndLogin("mentorN", "meng-2", "MENTOR");
    MvcResult created =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/meetings")
                    .header("Authorization", "Bearer " + mentorToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"title":"t","weeks":4,"capacity":3}
                        """))
            .andExpect(status().isCreated())
            .andReturn();
    long meetingId =
        objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

    // Mentor (non-admin) cannot approve.
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/admin/meetings/" + meetingId + "/approve")
                .header("Authorization", "Bearer " + mentorToken))
        .andExpect(status().isForbidden());
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
    // ADMIN cannot self-register — seed directly.
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
