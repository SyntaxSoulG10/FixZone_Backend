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

            String sender = (senderEmail != null && !senderEmail.isEmpty() && !senderEmail.contains("example.com")) ? senderEmail : fromEmail;
            helper.setFrom(sender, "FixZone Team");
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

    public void sendVerificationOtpEmail(String toEmail, String fullName, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            String sender = (senderEmail != null && !senderEmail.isEmpty() && !senderEmail.contains("example.com")) ? senderEmail : fromEmail;
            helper.setFrom(sender, "FixZone Team");
            helper.setTo(toEmail);
            helper.setSubject("FixZone - Your Verification Code");

            String content = "<h1>Verify Your Email, " + fullName + "!</h1>" +
                    "<p>Thank you for registering with FixZone.</p>" +
                    "<p>Your 5-digit verification code is:</p>" +
                    "<h2 style=\"letter-spacing: 5px; color: #E84E0F;\">" + otp + "</h2>" +
                    "<p>This code will expire in 10 minutes.</p>" +
                    "<p>Best regards,<br>The FixZone Team</p>";

            helper.setText(content, true);
            mailSender.send(message);
            log.info("OTP Email SENT successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("ERROR: OTP Email failed to send to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            String sender = (senderEmail != null && !senderEmail.isEmpty() && !senderEmail.contains("example.com")) ? senderEmail : fromEmail;
            helper.setFrom(sender, "FixZone Security");
            helper.setTo(toEmail);
            helper.setSubject("FixZone - Password Recovery");

            String content = "<h1>FixZone Password Reset</h1>" +
                    "<p>We received a request to reset your password.</p>" +
                    "<p>Click the link below to set a new password. This link is valid for 15 minutes.</p>" +
                    "<p><a href=\"" + resetLink + "\" style=\"display:inline-block;padding:10px 20px;color:white;background-color:#4F46E5;text-decoration:none;border-radius:5px;\">Reset My Password</a></p>" +
                    "<p>If you did not request this, please ignore this email.</p>" +
                    "<p>Best regards,<br>The FixZone Team</p>";

            helper.setText(content, true);
            mailSender.send(message);
            log.info("Password Reset Email SENT successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("ERROR: Password reset email failed to send to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send reset email");
        }
    }

    public void sendBookingConfirmationEmail(String toEmail, String customerName, String packageName, String serviceCenterName, String bookingDate, String bookingTime, java.math.BigDecimal amount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            String sender = (senderEmail != null && !senderEmail.isEmpty() && !senderEmail.contains("example.com")) ? senderEmail : fromEmail;
            helper.setFrom(sender, "FixZone Booking");
            helper.setTo(toEmail);
            helper.setSubject("FixZone - Booking Payment Confirmation");

            String content = "<h1>Booking Confirmed, " + customerName + "!</h1>" +
                    "<p>Thank you for choosing FixZone. Your service booking payment has been successfully received.</p>" +
                    "<div style=\"background-color:#F3F4F6;padding:15px;border-radius:8px;margin:15px 0;\">" +
                    "<p><b>Service Package:</b> " + packageName + "</p>" +
                    "<p><b>Service Center:</b> " + serviceCenterName + "</p>" +
                    "<p><b>Booking Date:</b> " + bookingDate + "</p>" +
                    "<p><b>Time Slot:</b> " + bookingTime + "</p>" +
                    "<p><b>Initial Fee Paid:</b> LKR " + amount + "</p>" +
                    "</div>" +
                    "<p>Please arrive 10 minutes prior to your scheduled time slot.</p>" +
                    "<p>Best regards,<br>The FixZone Team</p>";

            helper.setText(content, true);
            mailSender.send(message);
            log.info("Booking Confirmation Email SENT successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("ERROR: Booking confirmation email failed to send to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    public void sendBookingCancellationEmail(String toEmail, String customerName, String bookingDate, java.math.BigDecimal refundAmount, java.math.BigDecimal penaltyAmount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            String sender = (senderEmail != null && !senderEmail.isEmpty() && !senderEmail.contains("example.com")) ? senderEmail : fromEmail;
            helper.setFrom(sender, "FixZone Booking");
            helper.setTo(toEmail);
            helper.setSubject("FixZone - Booking Cancellation Confirmation");

            java.math.BigDecimal penalty = penaltyAmount != null ? penaltyAmount : java.math.BigDecimal.ZERO;
            java.math.BigDecimal refund = refundAmount != null ? refundAmount : java.math.BigDecimal.ZERO;

            String penaltyFormatted = String.format("LKR %,.2f", penalty);
            String refundFormatted = String.format("LKR %,.2f", refund);

            String content = "<h1>Booking Cancelled, " + customerName + "!</h1>" +
                    "<p>Your booking scheduled for <b>" + bookingDate + "</b> has been cancelled.</p>" +
                    "<div style=\"background-color:#F8FAFC;padding:16px;border-radius:8px;border:1px solid #E2E8F0;margin:15px 0;\">" +
                    "<p style=\"margin:6px 0;\"><b>Cancellation Penalty (5%):</b> <span style=\"color:#EF4444;font-weight:bold;\">" + penaltyFormatted + "</span></p>" +
                    "<p style=\"margin:6px 0;\"><b>Refund Amount:</b> <span style=\"color:#10B981;font-weight:bold;\">" + refundFormatted + "</span></p>" +
                    "</div>" +
                    "<p>Refunds usually process back to your card within 5-10 business days.</p>" +
                    "<p>Best regards,<br>The FixZone Team</p>";

            helper.setText(content, true);
            mailSender.send(message);
            log.info("Booking Cancellation Email SENT successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("ERROR: Booking cancellation email failed to send to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}

