package com.fixzone.fixzon_backend.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${MAIL_SENDER:}")
    private String senderEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @jakarta.annotation.PostConstruct
    public void debugConfig() {
        log.info("EMAIL SERVICE INITIALIZED");
        log.info("   > SMTP Host: smtp-relay.brevo.com");
        log.info("   > Login ID: {}", fromEmail);
        log.info("   > Sender Email: {}", senderEmail);
        if (senderEmail.contains("example.com")) {
            log.warn("WARNING: Sender Email is still set to placeholder! Check your .env file.");
        }
    }

    public void sendWelcomeEmail(String toEmail, String fullName, String temporaryPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(senderEmail, "FixZone Team");
            helper.setTo(toEmail);
            helper.setSubject("Welcome to FixZone - Your Manager Account is Ready");

            String content = "<h1>Welcome to FixZone, " + fullName + "!</h1>" +
                    "<p>Your manager account has been created successfully.</p>" +
                    "<p>You can now log in to the dashboard using the following credentials:</p>" +
                    "<p><b>Email:</b> " + toEmail + "</p>" +
                    "<p><b>Temporary Password:</b> " + temporaryPassword + "</p>" +
                    "<p>Please change your password after your first login for security reasons.</p>" +
                    "<p>Best regards,<br>The FixZone Team</p>";

            helper.setText(content, true);
            mailSender.send(message);
            log.info("Email SENT successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("ERROR: Email failed to send to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}
