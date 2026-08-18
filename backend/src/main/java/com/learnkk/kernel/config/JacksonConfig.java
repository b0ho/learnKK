package com.learnkk.kernel.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit JSON contract: camelCase property names (API boundary), ISO-8601 dates (not numeric
 * timestamps). Kept explicit rather than relying on framework defaults.
 */
@Configuration
public class JacksonConfig {

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
    return builder -> {
      builder.propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
      builder.modules(new JavaTimeModule());
      builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    };
  }
}
