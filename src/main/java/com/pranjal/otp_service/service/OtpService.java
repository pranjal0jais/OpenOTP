package com.pranjal.otp_service.service;

import com.pranjal.otp_service.entity.OtpRecord;
import com.pranjal.otp_service.exception.InvalidOtpException;
import com.pranjal.otp_service.exception.OtpExpiredException;
import com.pranjal.otp_service.exception.OtpRecordNotFoundException;
import com.pranjal.otp_service.exception.ResendCooldownException;
import com.pranjal.otp_service.repository.OtpRepository;
import com.pranjal.otp_service.utility.OtpGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OtpService {
    private final EmailService emailService;
    private final OtpRepository otpRepository;

    public void sendOtp(String to){
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
        emailService.sendEmail(to, otp);
    }

    public boolean verifyOtp(String email, String otp){
        OtpRecord record = otpRepository
                .findTopByEmailAndVerifiedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(()-> new OtpRecordNotFoundException("No active OTP found for that email"));

        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new OtpExpiredException("OTP found but past expiry time");
        }

        if (!record.getOtpCode().equals(otp)) {
            throw new InvalidOtpException("Wrong OTP code submitted");
        }
        record.setVerified(true);
        record.setVerifiedAt(LocalDateTime.now());
        otpRepository.save(record);
        return true;
    }
}
