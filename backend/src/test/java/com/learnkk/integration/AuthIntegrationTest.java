package com.learnkk.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkk.kernel.error.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** End-to-end: signup -> login -> validated session access, plus employee-no uniqueness (409). */
class AuthIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void signupThenLoginThenAccessProtectedResource() throws Exception {
    String signup =
        """
        {"nickname":"mentorA","password":"password1","employeeNo":"emp-1","role":"MENTOR"}
        """;
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signup))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.employeeNo").value("EMP-1")); // normalized upper-case

    MvcResult loginResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"nickname":"mentorA","password":"password1"}
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("MENTOR"))
            .andReturn();

    JsonNode session = objectMapper.readTree(loginResult.getResponse().getContentAsString());
    String token = session.get("token").asText();

    // The session validates against a protected route.
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/users/me/profile")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nickname").value("mentorA"));
  }

  @Test
  void duplicateEmployeeNo_returns409() throws Exception {
    String first =
        """
        {"nickname":"userB","password":"password1","employeeNo":"dup-1","role":"MENTEE"}
        """;
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(first))
        .andExpect(status().isCreated());

    // Same employee number (different nickname) — unique constraint yields 409.
    String second =
        """
        {"nickname":"userC","password":"password1","employeeNo":"dup-1","role":"MENTEE"}
        """;
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(second))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCodes.DUPLICATE_EMPLOYEE_NO));
  }

  @Test
  void protectedRouteWithoutToken_returns401() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/api/users/me/profile"))
        .andExpect(status().isUnauthorized());
  }
}
