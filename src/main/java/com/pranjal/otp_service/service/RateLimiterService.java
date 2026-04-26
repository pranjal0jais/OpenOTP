package com.pranjal.otp_service.service;

import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket createBucket(){
        return Bucket.builder()
                .addLimit(limit->limit
                        .capacity(3)
                        .refillIntervally(3, Duration.ofMinutes(10)))
                .build();
    }

    public boolean consume(String email){
        Bucket bucket = buckets.computeIfAbsent(email, key -> createBucket());
        return bucket.tryConsume(1);
    }
}
