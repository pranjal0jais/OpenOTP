package com.pranjal.otp_service.exception;

public class InvalidHmacException extends RuntimeException{
    public InvalidHmacException(String message){
        super(message);
    }
}
