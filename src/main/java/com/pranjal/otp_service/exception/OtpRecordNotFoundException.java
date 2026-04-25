package com.pranjal.otp_service.exception;

public class OtpRecordNotFoundException extends RuntimeException{
    public OtpRecordNotFoundException(String message){
        super(message);
    }
}
