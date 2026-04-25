package com.pranjal.otp_service.utility;

import java.security.SecureRandom;

public class OtpGenerator {
    public static String generate() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}
