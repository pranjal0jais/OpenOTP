package com.pranjal.otp_service.exception;

public class ResendCooldownException extends RuntimeException {
    public ResendCooldownException(String message) {
        super(message);
    }
}