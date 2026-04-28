package com.pranjal.otp_service.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Async
    public void sendEmail(String to, String otp) {
        MimeMessage mailMessage = mailSender.createMimeMessage();

        Context context = new Context();
        context.setVariable("otpCode", otp);

        try {
            MimeMessageHelper  helper = new MimeMessageHelper(mailMessage, true);

            String htmlContent = templateEngine.process("otp-email", context);

            helper.setTo(to);
            helper.setSubject("Your otp code");
            helper.setText(htmlContent, true);

            mailSender.send(mailMessage);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
