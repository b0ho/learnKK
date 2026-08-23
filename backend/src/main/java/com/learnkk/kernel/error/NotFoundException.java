package com.learnkk.kernel.error;

import org.springframework.http.HttpStatus;

/** 404 Not Found — referenced resource does not exist. */
public class NotFoundException extends DomainException {

  public NotFoundException(String code, String message) {
    super(code, HttpStatus.NOT_FOUND, message);
  }
}
