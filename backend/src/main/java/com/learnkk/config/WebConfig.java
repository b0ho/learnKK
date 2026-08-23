package com.learnkk.config;

import com.learnkk.auth.web.SessionAuthInterceptor;
import com.learnkk.kernel.security.AuthPrincipalArgumentResolver;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Application-level web wiring: registers the session-auth interceptor and the
 * {@code @AuthPrincipal} argument resolver.
 *
 * <p>Lives in the app-level {@code com.learnkk.config} package rather than {@code kernel.config}
 * because it composes the {@code auth} and {@code kernel} modules; keeping it out of {@code kernel}
 * preserves the C0 leaf invariant (kernel must not depend on {@code auth}) — see ADR-007.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final SessionAuthInterceptor sessionAuthInterceptor;

  /**
   * Browser origins allowed to call the API (comma-separated). Auth uses a Bearer token, not
   * cookies, so credentials are not required. Defaults to the local Vite dev server.
   */
  @Value("${learnkk.cors.allowed-origins:http://localhost:5173}")
  private String[] allowedOrigins;

  public WebConfig(SessionAuthInterceptor sessionAuthInterceptor) {
    this.sessionAuthInterceptor = sessionAuthInterceptor;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins(allowedOrigins)
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(false);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(sessionAuthInterceptor).addPathPatterns("/api/**");
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new AuthPrincipalArgumentResolver());
  }
}
