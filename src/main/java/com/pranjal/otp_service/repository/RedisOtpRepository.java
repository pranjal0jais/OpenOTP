package com.pranjal.otp_service.repository;

import com.pranjal.otp_service.dto.RedisOtpRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RedisOtpRepository {
    private final RedisTemplate<String, RedisOtpRecord> redisTemplate;

    public void save(RedisOtpRecord redisOtpRecord) {
        String key = getKey(redisOtpRecord.getEmail());

        redisTemplate.opsForValue().set(key, redisOtpRecord, Duration.ofMinutes(5));
        redisTemplate.opsForValue().get(key);
    }

    public Optional<RedisOtpRecord> findByEmail(String email) {
        String key = getKey(email);
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    public void deleteByEmail(String email) {
        String key = getKey(email);
        redisTemplate.delete(key);
    }

    private String getKey(String email){
        return "otp:" + email;
    }
}
