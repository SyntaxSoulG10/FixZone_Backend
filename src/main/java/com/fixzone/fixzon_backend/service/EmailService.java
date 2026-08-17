package com.fixzone.fixzon_backend.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${mail.sender:${MAIL_SENDER:dinithi1625403@gmail.com}}")
    private String senderEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public String getSenderEmail() {
        if (senderEmail != null && !senderEmail.trim().isEmpty() && !senderEmail.contains("example.com")) {
            return senderEmail.trim();
        }
        return "dinithi1625403@gmail.com";
    }

    @jakarta.annotation.PostConstruct
    public void debugConfig() {
        log.info("EMAIL SERVICE INITIALIZED");
        log.info("   > Login ID: {}", fromEmail);
        log.info("   > Verified Sender Email: {}", getSenderEmail());
    }

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Async
    public void sendManagerCredentialsEmail(String toEmail, String fullName, String password, String centerName, String companyName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(getSenderEmail(), "FixZone Team");
            helper.setTo(toEmail);
            helper.setSubject("Your FixZone Manager Account Credentials");

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

            helper.setText(content, true);
            mailSender.send(message);
            log.info("Manager credentials email SENT successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("ERROR: Manager credentials email failed to send to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String fullName, String temporaryPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(getSenderEmail(), "FixZone Team");
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

    @Async
    public void sendVerificationOtpEmail(String toEmail, String fullName, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(getSenderEmail(), "FixZone Team");
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

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        sendPasswordResetEmail(toEmail, resetLink, null);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetLink, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(getSenderEmail(), "FixZone Security");
            helper.setTo(toEmail);
            helper.setSubject("FixZone - Password Recovery Code");

            String otpSection = (otpCode != null && !otpCode.trim().isEmpty())
                    ? "<div style=\"background-color: #f4f5f7; padding: 15px; border-radius: 8px; text-align: center; margin: 20px 0;\">" +
                      "<p style=\"margin: 0 0 5px 0; color: #555; font-size: 14px;\">Mobile App Verification Code:</p>" +
                      "<span style=\"font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #4F46E5;\">" + otpCode + "</span>" +
                      "<p style=\"margin: 5px 0 0 0; color: #888; font-size: 12px;\">Valid for 15 minutes</p>" +
                      "</div>"
                    : "";

            String content = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 10px;\">" +
                    "<h2 style=\"color: #4F46E5; text-align: center;\">FixZone Password Recovery</h2>" +
                    "<p>We received a request to reset the password for your account.</p>" +
                    otpSection +
                    "<p>Or if you are on a Web browser, click the link below to reset your password:</p>" +
                    "<p style=\"text-align: center;\"><a href=\"" + resetLink + "\" style=\"display:inline-block;padding:12px 24px;color:white;background-color:#4F46E5;text-decoration:none;border-radius:6px;font-weight:bold;\">Reset My Password</a></p>" +
                    "<p style=\"color: #777; font-size: 13px; margin-top: 25px;\">If you did not request a password reset, please ignore this email.</p>" +
                    "<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\">" +
                    "<p style=\"color: #aaa; font-size: 12px; text-align: center;\">The FixZone Security Team</p>" +
                    "</div>";

            helper.setText(content, true);
            mailSender.send(message);
            log.info("Password Reset Email SENT successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("ERROR: Password reset email failed to send to {}: {}", toEmail, e.getMessage(), e);
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

