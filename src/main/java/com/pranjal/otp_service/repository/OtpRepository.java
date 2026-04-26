package com.pranjal.otp_service.repository;

import com.pranjal.otp_service.entity.OtpRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpRecord, Long> {
    Optional<OtpRecord> findTopByEmailAndVerifiedFalseOrderByCreatedAtDesc(String email);

    Optional<OtpRecord> findTopByEmailOrderByCreatedAtDesc(String email);

    List<OtpRecord> deleteAllByExpiresAtBefore(LocalDateTime time);
}
