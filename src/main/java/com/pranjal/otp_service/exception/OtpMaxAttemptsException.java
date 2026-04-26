package com.pranjal.otp_service.exception;

public class OtpMaxAttemptsException extends RuntimeException{
    public OtpMaxAttemptsException(String message){
        super(message);
    }
}
