package com.learnkk.kernel.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Translates domain exceptions and Bean Validation failures into the uniform {@link ErrorPayload}
 * body with the correct HTTP status (400/401/403/404/409). Any other unexpected exception is mapped
 * to a uniform 500 body so the {@code {code,message,details}} contract (NFR8) holds for every
 * non-2xx response.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ErrorPayload> handleDomain(DomainException ex) {
    return ResponseEntity.status(ex.getHttpStatus()).body(ex.toPayload());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorPayload> handleBeanValidation(MethodArgumentNotValidException ex) {
    Map<String, Object> details = new LinkedHashMap<>();
    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
      details.putIfAbsent(fe.getField(), fe.getDefaultMessage());
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorPayload.of(ErrorCodes.VALIDATION_FAILED, "입력값이 올바르지 않습니다.", details));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorPayload> handleConstraintViolation(ConstraintViolationException ex) {
    Map<String, Object> details = new LinkedHashMap<>();
    for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
      details.putIfAbsent(v.getPropertyPath().toString(), v.getMessage());
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorPayload.of(ErrorCodes.VALIDATION_FAILED, "입력값이 올바르지 않습니다.", details));
  }

  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ErrorPayload> handleBadRequest(Exception ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorPayload.of(ErrorCodes.VALIDATION_FAILED, "요청 파라미터가 올바르지 않습니다."));
  }

  /**
   * Upload exceeding the multipart container ceiling (a hard stop above the 20MB business cap).
   * Mapped to 413 with the same {@code ATTACHMENT_TOO_LARGE} code the service uses at the cap so
   * clients see one consistent contract for oversized attachments (BR-U6-2).
   */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorPayload> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .body(ErrorPayload.of(ErrorCodes.ATTACHMENT_TOO_LARGE, "첨부 파일은 20MB를 초과할 수 없습니다."));
  }

  /**
   * Catch-all for unexpected exceptions. Logs the cause server-side and returns the uniform {@link
   * ErrorPayload} without leaking internal details to the client.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorPayload> handleUnexpected(Exception ex) {
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorPayload.of(ErrorCodes.INTERNAL_ERROR, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."));
  }
}
