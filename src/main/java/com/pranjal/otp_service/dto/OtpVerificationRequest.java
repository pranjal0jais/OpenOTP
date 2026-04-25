package com.pranjal.otp_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OtpVerificationRequest {
    @Email(message = "Enter a valid email.")
    private String email;
    @NotBlank(message = "OTP cannot be null")
    @Pattern(regexp="\\d{6}", message = "OTP should be of 6 digits")
    private String otpCode;
}
