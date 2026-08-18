package com.learnkk.kernel.security;

import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.UnauthorizedException;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Resolves {@code @AuthPrincipal Principal} parameters from the request-scoped attribute. */
public class AuthPrincipalArgumentResolver implements HandlerMethodArgumentResolver {

  /** Request attribute key under which the session interceptor stores the resolved principal. */
  public static final String PRINCIPAL_ATTRIBUTE = "com.learnkk.principal";

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(AuthPrincipal.class)
        && Principal.class.isAssignableFrom(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    Object attr = webRequest.getAttribute(PRINCIPAL_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
    if (attr instanceof Principal principal) {
      return principal;
    }
    throw new UnauthorizedException(ErrorCodes.AUTH_UNAUTHENTICATED, "인증이 필요합니다.");
  }
}
