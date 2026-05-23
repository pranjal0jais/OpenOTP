package com.pranjal.otp_service.utility;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class OtpSignatureUtility {

    private final String secretKey;

    public OtpSignatureUtility(String secretKey) {
        this.secretKey = secretKey;
    }

    public String generateHmac(String email, String otpCode) {
        Mac mac = null;
        try {
            mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e.getMessage());
        }
        byte[] rawHmac = mac.doFinal((email + otpCode).getBytes());
        return Base64.getEncoder().encodeToString(rawHmac);
    }

    public boolean verify(String email, String otpCode, String signature) {
        String original = generateHmac(email, otpCode);
        return MessageDigest.isEqual(original.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }
}
