package com.example.notification_service.event;

import com.example.notification_service.dto.UserRegisteredEvent;
import com.example.notification_service.service.EmailService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumer {

    private final EmailService emailService;

    public KafkaConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "user-registered", groupId = "notification-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(UserRegisteredEvent event) {
        System.out.println("Received event: " + event);
        sendUserRegisteredEmail(event.getEmail());
    }

    public void sendUserRegisteredEmail(String email) {
        String subject = "Thông báo đăng ký thành công";
        String text = "Chúc mừng bạn đã đăng ký thành công!";
        emailService.sendEmail(email, subject, text);
    }
}
