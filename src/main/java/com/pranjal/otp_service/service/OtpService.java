package com.pranjal.otp_service.service;

import com.pranjal.otp_service.dto.OtpEmailMessage;
import com.pranjal.otp_service.dto.RedisOtpRecord;
import com.pranjal.otp_service.exception.*;
import com.pranjal.otp_service.repository.RedisOtpRepository;
import com.pranjal.otp_service.utility.OtpGenerator;
import com.pranjal.otp_service.utility.OtpSignatureUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {
    private final RabbitTemplate rabbitTemplate;
    private final RedisOtpRepository redisOtpRepository;
    private final RateLimiterService rateLimiterService;
    private final OtpSignatureUtility otpSignatureUtility;

    @Value("${otp.queue.routing-key}")
    private String routingKey;
    @Value("${otp.queue.exchange}")
    private String exchange;
    @Value("${otp.expiry-minutes}")
    private int expiryMinutes;
    @Value("${otp.resend-cooldown-seconds}")
    private int resendCooldownSeconds;
    @Value("${otp.max-attempts}")
    private int maxAttempts;

    public void sendOtp(String to){
        log.info("OTP requested for {}", to);
        if(!rateLimiterService.consume(to)){
            log.warn("Rate limit exceeded for {}", to);
            throw new RateLimitExceededException("Too many OTP requests. Try again later.");
        }
        Optional<RedisOtpRecord> existingOtp = redisOtpRepository.findByEmail(to);
        if(existingOtp.isPresent()){
            long difference = ChronoUnit.SECONDS.between(existingOtp.get().getCreatedAt(),
                    LocalDateTime.now());
            if(difference < resendCooldownSeconds){
                int remainingTime = (int) (resendCooldownSeconds - difference);
                log.warn("Resend cooldown for {} is {} seconds", to, remainingTime);
                throw new ResendCooldownException("Please wait " + remainingTime + " " +
                        "seconds " +
                        "before " +
                        "resending.");
            }
        }
        String otp = OtpGenerator.generate();
        String signature = otpSignatureUtility.generateHmac(to, otp);
        LocalDateTime now = LocalDateTime.now();
        RedisOtpRecord redisOtpRecord = RedisOtpRecord.builder()
                .email(to)
                .otpCode(otp)
                .attemptCount(0)
                .createdAt(now)
                .expiresAt(now.plusMinutes(expiryMinutes))
                .signature(signature)
                .build();
        
        redisOtpRepository.save(redisOtpRecord);
        try{
            rabbitTemplate.convertAndSend(exchange, routingKey, new OtpEmailMessage(to, otp));
            log.info("OTP queued successfully for {}", to);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }
    }

    public void verifyOtp(String email, String otp){
        RedisOtpRecord record = redisOtpRepository
                .findByEmail(email)
                .orElseThrow(()->
                        {
                            log.warn("Otp email {} not found", email);
                            return new OtpRecordNotFoundException("No active OTP found for " +
                                "that email");
                        }
                );

        if(!otpSignatureUtility.verify(email, record.getOtpCode(),  record.getSignature())){
            log.error("HMAC verification failed for {}", email);
            throw new InvalidHmacException("HMAC verification failed");
        }

        int attempt = record.getAttemptCount();

        if(attempt >= maxAttempts){
            redisOtpRepository.deleteByEmail(email);
            log.warn("Max attempts exceeded for {}", email);
            throw new OtpMaxAttemptsException("Max attempts reached. Request a new OTP.");
        }

        if (!record.getOtpCode().equals(otp)) {
            record.setAttemptCount(record.getAttemptCount() + 1);
            redisOtpRepository.update(record);
            log.warn("Invalid OTP sent by {} | Verification failed", email);
            throw new InvalidOtpException("Wrong OTP code submitted");
        }

        redisOtpRepository.deleteByEmail(record.getEmail());
        log.info("OTP verification successful for {}", email);
    }
}
