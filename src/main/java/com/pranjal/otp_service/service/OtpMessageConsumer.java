package com.pranjal.otp_service.service;

import com.pranjal.otp_service.dto.OtpEmailMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class OtpMessageConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = "${otp.queue.name}")
    public void consume(OtpEmailMessage message){
        log.info("Consumed OTP email message for: {}", message.getEmail());
        emailService.sendEmail(message.getEmail(), message.getOtpCode());
    }
}
