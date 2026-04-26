package com.pranjal.otp_service.exception;

import com.pranjal.otp_service.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionManager {

    @ExceptionHandler(OtpRecordNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOtpRecordNotFoundException(OtpRecordNotFoundException e){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .message(e.getMessage())
                        .status("OTP_NOT_FOUND")
                        .statusCode(HttpStatus.NOT_FOUND.value())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .message(e.getMessage())
                        .status("INTERNAL_SERVER_ERROR")
                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .build());
    }

    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<ErrorResponse> handleOtpExpiredException(OtpExpiredException e){
        return ResponseEntity.status(HttpStatus.GONE)
                .body(ErrorResponse.builder()
                        .message(e.getMessage())
                        .status("OTP_EXPIRED")
                        .statusCode(HttpStatus.GONE.value())
                        .build());
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOtpException(InvalidOtpException e){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .message(e.getMessage())
                        .status("INVALID_OTP")
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .build());
    }

    @ExceptionHandler(ResendCooldownException.class)
    public ResponseEntity<ErrorResponse> handleResendCooldownException(ResendCooldownException e){
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ErrorResponse.builder()
                        .message(e.getMessage())
                        .status("RESEND_COOLDOWN")
                        .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
                        .build());
    }

    @ExceptionHandler(OtpMaxAttemptsException.class)
    public ResponseEntity<ErrorResponse> handleOtpMaxAttemptsException(OtpMaxAttemptsException e){
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ErrorResponse.builder()
                        .message(e.getMessage())
                        .status("MAX_ATTEMPTS_REACHED")
                        .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .message(e.getMessage())
                        .status("INVALID_FORMAT")
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .build());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ErrorResponse.builder()
                        .message(e.getMessage())
                        .status("RATE_LIMIT_EXCEEDED")
                        .statusCode(HttpStatus.TOO_MANY_REQUESTS.value())
                        .build());
    }
}
