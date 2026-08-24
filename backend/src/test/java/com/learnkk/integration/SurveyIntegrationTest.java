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
 * End-to-end survey/feedback flow: create -> approve -> apply -> confirm -> start -> submit
 * pre-survey answers -> owning mentor reads; feedback submit -> mentor read -> other-mentor 403.
 */
class SurveyIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void preSurveyAndFeedback_endToEnd() throws Exception {
    String mentorToken = signupAndLogin("mentorS", "sur-mentor", "MENTOR");
    String otherMentorToken = signupAndLogin("mentorS2", "sur-mentor2", "MENTOR");
    String adminToken = createAdminAndLogin("adminS", "sur-admin");
    String menteeToken = signupAndLogin("menteeS", "sur-mentee", "MENTEE");

    long meetingId = createMeeting(mentorToken);

    // Admin approves -> RECRUITING.
    admin(adminToken, meetingId, "approve").andExpect(status().isOk());

    // Mentor sets one required question (editable while RECRUITING).
    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/api/meetings/" + meetingId + "/questions")
                .header("Authorization", "Bearer " + mentorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "[{\"orderNo\":1,\"text\":\"목표는?\",\"type\":\"TEXT\",\"options\":[],\"required\":true}]"))
        .andExpect(status().isOk());

    // Mentee applies while RECRUITING.
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/meetings/" + meetingId + "/enrollments")
                .header("Authorization", "Bearer " + menteeToken))
        .andExpect(status().isCreated());

    // Admin confirms recruitment -> READY_TO_START, then starts -> IN_PROGRESS.
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/admin/meetings/" + meetingId + "/confirm-recruitment")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"proceed\":true}"))
        .andExpect(status().isOk());
    admin(adminToken, meetingId, "approve-start").andExpect(status().isOk());

    // Read question id.
    MvcResult qResult =
        mockMvc
            .perform(MockMvcRequestBuilders.get("/api/meetings/" + meetingId + "/questions"))
            .andExpect(status().isOk())
            .andReturn();
    long questionId =
        objectMapper.readTree(qResult.getResponse().getContentAsString()).get(0).get("id").asLong();

    // Mentee submits pre-survey answers.
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/meetings/" + meetingId + "/survey-answers")
                .header("Authorization", "Bearer " + menteeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"answers\":[{\"questionId\":" + questionId + ",\"answerText\":\"성장\"}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].answerText").value("성장"));

    // Owning mentor reads the mentee's answers.
    long menteeId = userRepository.findByNickname("menteeS").orElseThrow().getId();
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                    "/api/meetings/" + meetingId + "/mentees/" + menteeId + "/survey-answers")
                .header("Authorization", "Bearer " + mentorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].answerText").value("성장"));

    // Mentee submits feedback.
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/meetings/" + meetingId + "/feedback")
                .header("Authorization", "Bearer " + menteeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"유익했습니다\"}"))
        .andExpect(status().isCreated());

    // Owning mentor reads feedback.
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/meetings/" + meetingId + "/feedback")
                .header("Authorization", "Bearer " + mentorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].content").value("유익했습니다"));

    // Another mentor cannot read this meeting's feedback.
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/meetings/" + meetingId + "/feedback")
                .header("Authorization", "Bearer " + otherMentorToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FEEDBACK_FORBIDDEN"));
  }

  private org.springframework.test.web.servlet.ResultActions admin(
      String adminToken, long meetingId, String action) throws Exception {
    return mockMvc.perform(
        MockMvcRequestBuilders.post("/api/admin/meetings/" + meetingId + "/" + action)
            .header("Authorization", "Bearer " + adminToken));
  }

  private long createMeeting(String mentorToken) throws Exception {
    MvcResult created =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/meetings")
                    .header("Authorization", "Bearer " + mentorToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"title\":\"설문 테스트\",\"topic\":\"backend\",\"weeks\":8,"
                            + "\"capacity\":5,\"format\":\"online\"}"))
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
