package com.pranjal.otp_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class OtpServiceApplicationTests {

    @MockitoBean
    private JavaMailSender javaMailSender;

	@Test
	void contextLoads() {
	}
}
