package com.fixzone.fixzon_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${brevo.api-key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${mail.sender:}")
    private String senderEmail;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public String getSenderEmail() {
        if (senderEmail != null && !senderEmail.trim().isEmpty()) {
            return senderEmail.trim();
        }
        if (fromEmail != null && !fromEmail.trim().isEmpty()) {
            return fromEmail.trim();
        }
        return "info@fixzone.com";
    }

    @jakarta.annotation.PostConstruct
    public void debugConfig() {
        log.info("EMAIL SERVICE INITIALIZED");
        log.info("   > Login ID: {}", fromEmail);
        log.info("   > Host: {}", mailHost);
        log.info("   > Verified Sender Email: {}", getSenderEmail());
    }

    /**
     * Sends an HTML email.
     * In cloud hosting environments like Render, outbound SMTP ports (587, 465, 25) are blocked by default.
     * When Brevo is used, this method uses Brevo's HTTPS REST API (Port 443) which is never blocked by cloud firewalls.
     */
    public void sendEmail(String toEmail, String recipientName, String subject, String htmlContent) {
        boolean isBrevoKey = mailPassword != null && (mailPassword.startsWith("xsmtpsib-") || mailPassword.startsWith("xkeysib-"));

        // If using Brevo key or running on cloud where SMTP is blocked, try HTTPS REST API first
        if (isBrevoKey || (mailHost != null && mailHost.contains("brevo"))) {
            boolean sentViaApi = sendViaBrevoHttpApi(toEmail, recipientName, subject, htmlContent);
            if (sentViaApi) {
                return;
            }
            log.warn("Brevo HTTPS API attempt failed or skipped, falling back to SMTP...");
        }

        // Fallback to standard JavaMail SMTP
        sendViaSmtp(toEmail, subject, htmlContent);
    }

    private boolean sendViaBrevoHttpApi(String toEmail, String recipientName, String subject, String htmlContent) {
        String effectiveApiKey = (brevoApiKey != null && !brevoApiKey.isBlank()) 
                ? brevoApiKey.trim() 
                : (mailPassword != null ? mailPassword.trim() : "");

        if (effectiveApiKey.isEmpty()) {
            return false;
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            Map<String, Object> payload = new HashMap<>();
            
            Map<String, String> sender = new HashMap<>();
            sender.put("name", "FixZone Team");
            sender.put("email", getSenderEmail());
            payload.put("sender", sender);

            Map<String, String> to = new HashMap<>();
            to.put("email", toEmail);
            if (recipientName != null && !recipientName.isBlank()) {
                to.put("name", recipientName);
            }
            payload.put("to", List.of(to));

            payload.put("subject", subject);
            payload.put("htmlContent", htmlContent);

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("accept", "application/json")
                    .header("api-key", effectiveApiKey)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Email SENT successfully via Brevo HTTPS REST API to: {}", toEmail);
                return true;
            } else {
                log.error("Brevo HTTPS API returned status {}: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("Brevo HTTPS API exception for {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    private void sendViaSmtp(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(getSenderEmail(), "FixZone Team");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email SENT successfully via SMTP to: {}", toEmail);
        } catch (Exception e) {
            log.error("SMTP send failed to {}: {}", toEmail, e.getMessage(), e);
            // If SMTP timed out and we haven't tried Brevo HTTP yet, try as ultimate safety net
            if (mailPassword != null && !mailPassword.trim().isEmpty()) {
                log.info("Attempting emergency fallback to Brevo HTTPS API for: {}", toEmail);
                sendViaBrevoHttpApi(toEmail, null, subject, htmlContent);
            }
        }
    }

    @Async
    public void sendManagerCredentialsEmail(String toEmail, String fullName, String password, String centerName, String companyName) {
        String loginUrl = (frontendUrl != null ? frontendUrl : "http://localhost:3000") + "/login";

        String content = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;\">" +
                "<div style=\"text-align: center; margin-bottom: 20px;\">" +
                "<h2 style=\"color: #ea580c; margin: 0;\">FixZone Manager Access</h2>" +
                "<p style=\"color: #64748b; font-size: 14px; margin-top: 4px;\">Automotive Service Center Management</p>" +
                "</div>" +
                "<p>Hello <b>" + fullName + "</b>,</p>" +
                "<p>You have been assigned as a <b>Service Center Manager</b>" + 
                (centerName != null ? (" for the <b>" + centerName + "</b> branch") : "") +
                (companyName != null ? (" at <b>" + companyName + "</b>.") : ".") + "</p>" +
                "<p>Use the following credentials to sign in to your dashboard. Your account will automatically activate upon your first login:</p>" +
                "<div style=\"background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin: 20px 0;\">" +
                "<p style=\"margin: 6px 0;\"><strong>Email (Login ID):</strong> <span style=\"color: #0f172a; font-weight: 600;\">" + toEmail + "</span></p>" +
                "<p style=\"margin: 10px 0 6px 0;\"><strong>Temporary Password:</strong></p>" +
                "<p style=\"margin: 4px 0;\"><span style=\"color: #ea580c; font-weight: bold; font-family: 'Courier New', Courier, monospace; font-size: 18px; letter-spacing: 1.5px; background: #ffffff; padding: 6px 12px; border-radius: 6px; border: 1px dashed #fdba74; display: inline-block;\">" + password + "</span></p>" +
                "</div>" +
                "<div style=\"text-align: center; margin: 25px 0;\">" +
                "<a href=\"" + loginUrl + "\" style=\"background-color: #ea580c; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;\">Go to FixZone Login</a>" +
                "</div>" +
                "<p style=\"color: #64748b; font-size: 13px;\">For security, you can change your password at any time from your profile settings after logging in.</p>" +
                "<hr style=\"border: none; border-top: 1px solid #f1f5f9; margin: 20px 0;\" />" +
                "<p style=\"color: #94a3b8; font-size: 12px; text-align: center;\">FixZone Platform</p>" +
                "</div>";

        sendEmail(toEmail, fullName, "Your FixZone Manager Account Credentials", content);
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String fullName, String temporaryPassword) {
        String content = "<h1>Welcome to FixZone, " + fullName + "!</h1>" +
                "<p>Your manager account has been created successfully.</p>" +
                "<p>You can now log in to the dashboard using the following credentials:</p>" +
                "<p><b>Email:</b> " + toEmail + "</p>" +
                "<p><b>Temporary Password:</b> " + temporaryPassword + "</p>" +
                "<p>Please change your password after your first login for security reasons.</p>" +
                "<p>Best regards,<br>The FixZone Team</p>";

        sendEmail(toEmail, fullName, "Welcome to FixZone - Your Manager Account is Ready", content);
    }

    @Async
    public void sendVerificationOtpEmail(String toEmail, String fullName, String otp) {
        String content = "<h1>Verify Your Email, " + fullName + "!</h1>" +
                "<p>Thank you for registering with FixZone.</p>" +
                "<p>Your 5-digit verification code is:</p>" +
                "<h2 style=\"letter-spacing: 5px; color: #E84E0F;\">" + otp + "</h2>" +
                "<p>This code will expire in 10 minutes.</p>" +
                "<p>Best regards,<br>The FixZone Team</p>";

        sendEmail(toEmail, fullName, "FixZone - Your Verification Code", content);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String content = "<h1>FixZone Password Reset</h1>" +
                "<p>We received a request to reset your password.</p>" +
                "<p>Click the link below to set a new password. This link is valid for 15 minutes.</p>" +
                "<p><a href=\"" + resetLink + "\" style=\"display:inline-block;padding:10px 20px;color:white;background-color:#4F46E5;text-decoration:none;border-radius:5px;\">Reset My Password</a></p>" +
                "<p>If you did not request this, please ignore this email.</p>" +
                "<p>Best regards,<br>The FixZone Team</p>";

        sendEmail(toEmail, null, "FixZone - Password Recovery", content);
    }
}

