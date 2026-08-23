package com.learnkk.kernel.error;

import org.springframework.http.HttpStatus;

/** 401 Unauthorized — missing, invalid, revoked, or expired session / credentials. */
public class UnauthorizedException extends DomainException {

  public UnauthorizedException(String code, String message) {
    super(code, HttpStatus.UNAUTHORIZED, message);
  }
}
