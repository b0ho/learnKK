package com.learnkk.integration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for full-stack integration tests: real PostgreSQL via Testcontainers, Flyway migrations, and
 * MockMvc against the complete Spring context (interceptors + advice included).
 *
 * <p>Uses the singleton-container pattern: one container is started for the whole JVM and shared by
 * every integration test class, so the cached Spring context's datasource always points at a
 * running database.
 *
 * <p>Because the container (and its data) is shared across every test class, each test method must
 * start from a clean slate — otherwise fixtures collide (duplicate nicknames) and global-count
 * assertions (e.g. the recruiting-meeting list) depend on class execution order. {@link
 * #resetDatabase()} truncates every application table (all but Flyway's history) before each test,
 * making the whole integration suite deterministic and order-independent.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("learnkk")
          .withUsername("learnkk")
          .withPassword("learnkk");

  static {
    POSTGRES.start();
  }

  @Autowired private DataSource dataSource;

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  /**
   * Truncate all application tables (keeping the Flyway-migrated schema) before every test so each
   * test method sees an empty database. {@code RESTART IDENTITY CASCADE} resets sequences and
   * follows foreign keys; {@code flyway_schema_history} is preserved so migrations are not re-run.
   */
  @BeforeEach
  void resetDatabase() throws Exception {
    try (Connection conn = dataSource.getConnection();
        Statement st = conn.createStatement()) {
      List<String> tables = new ArrayList<>();
      try (ResultSet rs =
          st.executeQuery(
              "SELECT tablename FROM pg_tables WHERE schemaname = 'public' "
                  + "AND tablename <> 'flyway_schema_history'")) {
        while (rs.next()) {
          tables.add('"' + rs.getString("tablename") + '"');
        }
      }
      if (!tables.isEmpty()) {
        st.execute("TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE");
      }
    }
  }
}
