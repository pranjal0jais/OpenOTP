package com.pranjal.otp_service.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class OtpRequest {
    @Email(message = "Enter a valid email.")
    private String email;
}
