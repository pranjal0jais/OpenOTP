package com.pranjal.otp_service.utility;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class OtpSignatureUtilityTest {

    private final OtpSignatureUtility otpSignatureUtility = new OtpSignatureUtility(
            "be0fr2c37ff4bcb13a571fd8343253be4eecaf0178cfa2f7873d0874ca9d242f");

    @Test
    public void generateHmac_shouldGenerateSameSignatureForSameInputs(){
        String email = "testmail@gmail.com";
        String otp = "123456";

        String generatedSignature1 = otpSignatureUtility.generateHmac(email, otp);
        String generatedSignature2 = otpSignatureUtility.generateHmac(email, otp);

        Assertions.assertEquals(generatedSignature1, generatedSignature2);
    }

    @Test
    public void generateHmac_shouldGenerateDifferentSignatureForDifferentInputs(){
        String email1 = "testmail1@gmail.com";
        String otp1 = "987654";
        String email2 = "test2mail@gmail.com";
        String otp2 = "123456";

        String generatedSignature1 = otpSignatureUtility.generateHmac(email1, otp1);
        String generatedSignature2 = otpSignatureUtility.generateHmac(email2, otp2);

        Assertions.assertNotEquals(generatedSignature1, generatedSignature2);
    }

    @Test
    public void verify_shouldReturnTrueForValidSignature(){
        String email1 = "testmail1@gmail.com";
        String otp1 = "987654";
        String signature =  otpSignatureUtility.generateHmac(email1, otp1);
        Assertions.assertTrue(otpSignatureUtility.verify(email1, otp1, signature));
    }

    @Test
    public void verify_shouldReturnFalseForInvalidSignature(){
        String email1 = "testmail1@gmail.com";
        String otp1 = "987654";
        String signature =  otpSignatureUtility.generateHmac(email1, otp1);

        String tamperedOtp = "968376";
        Assertions.assertFalse(otpSignatureUtility.verify(email1, tamperedOtp, signature));
    }
}