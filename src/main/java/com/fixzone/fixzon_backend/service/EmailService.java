package com.fixzone.fixzon_backend.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

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
        System.out.println("EMAIL SERVICE INITIALIZED");
        System.out.println("   > SMTP Host: smtp-relay.brevo.com");
        System.out.println("   > Login ID: " + fromEmail);
        System.out.println("   > Sender Email: " + senderEmail);
        if (senderEmail.contains("example.com")) {
            System.err.println("WARNING: Sender Email is still set to placeholder! Check your .env file.");
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
            System.out.println("✅ Email SENT successfully to: " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ ERROR: Email failed to send!");
            e.printStackTrace(); // This will print the exact reason (e.g. Auth failure) to your console
        }
    }
}
