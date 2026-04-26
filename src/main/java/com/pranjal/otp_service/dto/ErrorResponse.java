package com.pranjal.otp_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponse {
    private String message;
    private String status;
    private int statusCode;
}
