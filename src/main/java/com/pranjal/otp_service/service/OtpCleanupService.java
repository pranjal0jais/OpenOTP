package com.pranjal.otp_service.service;

import com.pranjal.otp_service.repository.OtpRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpCleanupService {
    private final OtpRepository otpRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanExpiredOtpRecords() {
        otpRepository.deleteAllByExpiresAtBefore(LocalDateTime.now());
        log.info("Expired OTP records cleanup at {}", LocalDateTime.now());
    }
}
