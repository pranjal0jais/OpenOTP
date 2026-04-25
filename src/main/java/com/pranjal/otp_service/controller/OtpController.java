package com.pranjal.otp_service.controller;

import com.pranjal.otp_service.dto.ApiResponse;
import com.pranjal.otp_service.dto.OtpRequest;
import com.pranjal.otp_service.dto.OtpVerificationRequest;
import com.pranjal.otp_service.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/")
@RequiredArgsConstructor
public class OtpController {
    private final OtpService otpService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse> send(@RequestBody @Valid OtpRequest request) {
        otpService.sendOtp(request.getEmail());
        return ResponseEntity.ok().body(
                new ApiResponse("OTP sent to %s".formatted(request.getEmail())
                ));

    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse> verify(@RequestBody @Valid
                                                OtpVerificationRequest request) {
        otpService.verifyOtp(request.getEmail(), request.getOtpCode());
        return ResponseEntity.ok().body(new ApiResponse("OTP verified"));
    }
}
