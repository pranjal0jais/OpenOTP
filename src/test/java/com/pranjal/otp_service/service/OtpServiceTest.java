package com.pranjal.otp_service.service;

import com.pranjal.otp_service.dto.OtpEmailMessage;
import com.pranjal.otp_service.dto.RedisOtpRecord;
import com.pranjal.otp_service.exception.*;
import com.pranjal.otp_service.repository.RedisOtpRepository;
import com.pranjal.otp_service.utility.OtpGenerator;
import com.pranjal.otp_service.utility.OtpSignatureUtility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RedisOtpRepository redisOtpRepository;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private OtpSignatureUtility otpSignatureUtility;

    @InjectMocks
    private OtpService otpService;

    @Test
    public void sendOtp_shouldThrowRateLimitRateLimitExceededException(){
        when(rateLimiterService.consume(any())).thenReturn(false);

        Assertions.assertThrowsExactly(
                RateLimitExceededException.class,
                () -> otpService.sendOtp("testmail@Gmail.com")
        );
    }

    @Test
    public void sendOtp_shouldThrowResendCooldownException(){
        LocalDateTime now = LocalDateTime.now();
        RedisOtpRecord  redisOtpRecord = RedisOtpRecord.builder()
                .email("testmail@gmail.com")
                .otpCode("123456")
                .attemptCount(0)
                .signature("vkMMFIIfKLdKf3jPMQRE8bkNynitdJaZeURI2YBUBEc=")
                .expiresAt(now.plusMinutes(5))
                .createdAt(now)
                .build();

        when(rateLimiterService.consume(any())).thenReturn(true);
        when(redisOtpRepository.findByEmail("testmail@gmail.com"))
                .thenReturn(Optional.of(redisOtpRecord));

        ReflectionTestUtils.setField(otpService, "resendCooldownSeconds", 50);
        Assertions.assertThrowsExactly(
                ResendCooldownException.class,
                () -> {
                    otpService.sendOtp("testmail@gmail.com");
                }
        );
    }

    @Test
    void sendOtp_shouldSuccessfullySendOtp() {

        String email = "testmail@gmail.com";
        String otp = "123456";
        String signature = "signedOtp";

        when(rateLimiterService.consume(email)).thenReturn(true);

        when(redisOtpRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        when(otpSignatureUtility.generateHmac(email, otp))
                .thenReturn(signature);

        try (MockedStatic<OtpGenerator> mockedStatic =
                     Mockito.mockStatic(OtpGenerator.class)) {

            mockedStatic.when(OtpGenerator::generate)
                    .thenReturn(otp);

            otpService.sendOtp(email);

            verify(redisOtpRepository).save(argThat(record ->
                    record.getEmail().equals(email) &&
                            record.getOtpCode().equals(otp) &&
                            record.getSignature().equals(signature)
            ));

            verify(rabbitTemplate).convertAndSend(
                    eq("otp.exchange"),
                    eq("otp.routing"),
                    argThat((OtpEmailMessage message) ->
                            message.getEmail().equals(email) &&
                                    message.getOtpCode().equals(otp)
                    )
            );
        }
    }

    @Test
    void verifyOtp_shouldThrowOtpRecordNotFoundException(){
        when(redisOtpRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        Assertions.assertThrowsExactly(OtpRecordNotFoundException.class,
                ()->otpService.verifyOtp("testmail@gmail.com", "123456")
        );
    }

    @Test
    void verifyOtp_shouldThrowInvalidHmacException() {
        RedisOtpRecord record = RedisOtpRecord.builder()
                .email("testmail@gmail.com")
                .otpCode("123456")
                .signature("invalid-signature")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .build();

        when(redisOtpRepository.findByEmail(anyString())).thenReturn(Optional.of(record));
        when(otpSignatureUtility.verify(anyString(), anyString(), anyString()))
                .thenReturn(false);

        Assertions.assertThrowsExactly(
                InvalidHmacException.class,
                ()->otpService.verifyOtp("testmail@gmail.com", "123456")
        );
    }

    @Test
    void verifyOtp_shouldThrowOtpMaxAttemptsException(){
        RedisOtpRecord record = RedisOtpRecord.builder()
                .email("testmail@gmail.com")
                .otpCode("123456")
                .signature("invalid-signature")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(3)
                .build();

        when(redisOtpRepository.findByEmail(anyString())).thenReturn(Optional.of(record));
        when(otpSignatureUtility.verify(anyString(), anyString(), anyString()))
                .thenReturn(true);

        Assertions.assertThrowsExactly(
                OtpMaxAttemptsException.class,
                ()->otpService.verifyOtp("testmail@gmail.com", "123456")
        );
    }

    @Test
    public void verifyOtp_shouldThrowInvalidOtpException(){
        RedisOtpRecord record = RedisOtpRecord.builder()
                .email("testmail@gmail.com")
                .otpCode("123456")
                .signature("invalid-signature")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .build();

        when(redisOtpRepository.findByEmail(anyString())).thenReturn(Optional.of(record));
        when(otpSignatureUtility.verify(anyString(), anyString(), anyString()))
                .thenReturn(true);

        Assertions.assertThrowsExactly(
                InvalidOtpException.class,
                ()->otpService.verifyOtp("testmail@gmail.com", "123457")
        );
    }

    @Test
    public void verifyOtp_OtpSuccessfullyVerified(){
        RedisOtpRecord record = RedisOtpRecord.builder()
                .email("testmail@gmail.com")
                .otpCode("123456")
                .signature("invalid-signature")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .build();

        when(redisOtpRepository.findByEmail(anyString())).thenReturn(Optional.of(record));
        when(otpSignatureUtility.verify(anyString(), anyString(), anyString()))
                .thenReturn(true);

        otpService.verifyOtp("testmail@gmail.com", "123456");

        verify(redisOtpRepository).deleteByEmail("testmail@gmail.com");
    }

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(
                otpService,
                "expiryMinutes",
                5
        );

        ReflectionTestUtils.setField(
                otpService,
                "resendCooldownSeconds",
                50
        );

        ReflectionTestUtils.setField(
                otpService,
                "maxAttempts",
                3
        );

        ReflectionTestUtils.setField(
                otpService,
                "exchange",
                "otp.exchange"
        );

        ReflectionTestUtils.setField(
                otpService,
                "routingKey",
                "otp.routing"
        );
    }
}