package com.pranjal.otp_service.service;

import com.pranjal.otp_service.dto.OtpEmailMessage;
import com.pranjal.otp_service.dto.RedisOtpRecord;
import com.pranjal.otp_service.exception.*;
import com.pranjal.otp_service.repository.RedisOtpRepository;
import com.pranjal.otp_service.utility.OtpGenerator;
import com.pranjal.otp_service.utility.OtpSignatureUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

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
        if(!rateLimiterService.consume(to)){
            throw new RateLimitExceededException("Too many OTP requests. Try again later.");
        }
        Optional<RedisOtpRecord> existingOtp = redisOtpRepository.findByEmail(to);
        if(existingOtp.isPresent()){
            long difference = ChronoUnit.SECONDS.between(existingOtp.get().getCreatedAt(),
                    LocalDateTime.now());
            if(difference < resendCooldownSeconds){
                throw new ResendCooldownException("Please wait " + (resendCooldownSeconds - difference) + " " +
                        "seconds " +
                        "before " +
                        "resending.");
            }
        }
        String otp = OtpGenerator.generate();
        String signature = otpSignatureUtility.generateHmac(to, otp);
        RedisOtpRecord redisOtpRecord = RedisOtpRecord.builder()
                .email(to)
                .otpCode(otp)
                .attemptCount(0)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .signature(signature)
                .build();
        
        redisOtpRepository.save(redisOtpRecord);
        rabbitTemplate.convertAndSend(exchange, routingKey, new OtpEmailMessage(to, otp));
    }

    public void verifyOtp(String email, String otp){
        RedisOtpRecord record = redisOtpRepository
                .findByEmail(email)
                .orElseThrow(()-> new OtpRecordNotFoundException("No active OTP found for that email"));

        if(!otpSignatureUtility.verify(email, record.getOtpCode(),  record.getSignature())){
            throw new InvalidHmacException("HMAC verification failed");
        }

        int attempt = record.getAttemptCount();

        if(attempt >= maxAttempts){
            throw new OtpMaxAttemptsException("Max attempts reached. Request a new OTP.");
        }

        if (!record.getOtpCode().equals(otp)) {
            record.setAttemptCount(record.getAttemptCount() + 1);
            redisOtpRepository.save(record);
            throw new InvalidOtpException("Wrong OTP code submitted");
        }

        redisOtpRepository.deleteByEmail(record.getEmail());
    }
}
