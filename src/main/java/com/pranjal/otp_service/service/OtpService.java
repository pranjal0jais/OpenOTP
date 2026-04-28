package com.pranjal.otp_service.service;

import com.pranjal.otp_service.dto.OtpEmailMessage;
import com.pranjal.otp_service.entity.OtpRecord;
import com.pranjal.otp_service.exception.*;
import com.pranjal.otp_service.repository.OtpRepository;
import com.pranjal.otp_service.utility.OtpGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OtpService {
    private final RabbitTemplate rabbitTemplate;
    private final OtpRepository otpRepository;
    private final RateLimiterService rateLimiterService;

    @Value("${otp.queue.routing-key}")
    private String routingKey;
    @Value("${otp.queue.exchange}")
    private String exchange;

    public void sendOtp(String to){
        if(!rateLimiterService.consume(to)){
            throw new RateLimitExceededException("Too many OTP requests. Try again later.");
        }
        Optional<OtpRecord> existingOtp = otpRepository.findTopByEmailOrderByCreatedAtDesc(to);
        if(existingOtp.isPresent()){
            long difference = ChronoUnit.SECONDS.between(existingOtp.get().getCreatedAt(),
                    LocalDateTime.now());
            if(difference < 50){
                throw new ResendCooldownException("Please wait " + (50 - difference) + " seconds " +
                        "before " +
                        "resending.");
            }
        }

        String otp = OtpGenerator.generate();
        OtpRecord otpRecord = OtpRecord.builder()
                .otpRecordId(UUID.randomUUID().toString())
                .email(to)
                .otpCode(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .build();

        otpRepository.save(otpRecord);
        rabbitTemplate.convertAndSend(exchange, routingKey, new OtpEmailMessage(to, otp));
    }

    public void verifyOtp(String email, String otp){
        OtpRecord record = otpRepository
                .findTopByEmailAndVerifiedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(()-> new OtpRecordNotFoundException("No active OTP found for that email"));

        int attempt = record.getAttemptCount();

        if(attempt >= 3){
            throw new OtpMaxAttemptsException("Max attempts reached. Request a new OTP.");
        }

        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new OtpExpiredException("OTP found but past expiry time");
        }

        if (!record.getOtpCode().equals(otp)) {
            record.setAttemptCount(record.getAttemptCount() + 1);
            otpRepository.save(record);
            throw new InvalidOtpException("Wrong OTP code submitted");
        }

        record.setVerified(true);
        record.setVerifiedAt(LocalDateTime.now());
        otpRepository.save(record);
    }
}
