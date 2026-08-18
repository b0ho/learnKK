package com.learnkk.kernel.config;

import com.learnkk.auth.web.SessionAuthInterceptor;
import com.learnkk.kernel.security.AuthPrincipalArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Wires the session-auth interceptor and the {@code @AuthPrincipal} argument resolver. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final SessionAuthInterceptor sessionAuthInterceptor;

  public WebConfig(SessionAuthInterceptor sessionAuthInterceptor) {
    this.sessionAuthInterceptor = sessionAuthInterceptor;
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
