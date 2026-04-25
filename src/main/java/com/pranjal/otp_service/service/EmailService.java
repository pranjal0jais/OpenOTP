package com.pranjal.otp_service.service;

import com.pranjal.otp_service.entity.OtpRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Async
    public void sendEmail(String to, String otp) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setSubject("Your OTP Code");
        mailMessage.setText("""
                Hello,
                
                Your OTP code is: %s
                This code expires in 5 minutes.
                
                If you did not request this, please ignore this email.
                """.formatted(otp));
        mailSender.send(mailMessage);
    }
}
