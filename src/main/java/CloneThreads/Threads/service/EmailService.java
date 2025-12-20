package CloneThreads.Threads.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@threads.com}")
    private String fromEmail;

    @Async
    public void sendVerificationEmail(String to, String verificationCode) {
        // 1. Log ra console để dev test nhanh
        log.info("========================================");
        log.info("MOCK EMAIL TO: {}", to);
        log.info("VERIFICATION CODE: {}", verificationCode);
        log.info("========================================");

        // 2. Gửi mail thật (Nếu đã cấu hình trong application.properties)
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Threads - Verify your email");
            message.setText("Your verification code is: " + verificationCode);
            
            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            log.warn("Could not send email (checking SMTP config...): {}", e.getMessage());
        }
    }
}
