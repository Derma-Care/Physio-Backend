package com.pharmacyManagement.service.impl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;   // error

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final String fromAddress;
    private final String clinicLoginUrl;

    public EmailService(JavaMailSender mailSender, Environment env) {
        this.mailSender = mailSender;
        this.fromAddress = env.getProperty(
                "notification.default-from-email",
                "no-reply@glowkart.com"
        );
        this.clinicLoginUrl = env.getProperty(
                "notification.clinic-login-url",
                "http://52.66.144.177:9090/supplier-Dashboard"
        );
    }

    public void sendEmail(String to, Map<String, String> data) {
        try {
            if (to == null || to.isBlank()) {
                logger.warn("Email not sent: recipient address is blank");
                return;
            }

            String subject = data.getOrDefault("subject", "CCMS Notification");

            // Determine emoji based on subject
            String emoji = "";
            if (subject.contains("Verified")) emoji = "🎉";
            else if (subject.contains("Pending")) emoji = "⏳";
            else if (subject.contains("Review")) emoji = "🔍";
            else if (subject.contains("Rejected")) emoji = "❌";
            else if (subject.contains("OTP")) emoji = "🔒";

            String subjectWithEmoji = emoji.isEmpty() ? subject : emoji + " " + subject;

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(to);
            helper.setFrom(fromAddress);
            helper.setSubject(subjectWithEmoji);

            // Select template based on email type
            if (subject.contains("OTP")) {
                helper.setText(buildOtpMessageBody(data, emoji), true);
            } else if (subject.contains("Rejected")) {
                helper.setText(buildRejectionMessageBody(data, emoji), true);
            } else {
                helper.setText(buildMessageBody(data, emoji), true);
            }

            mailSender.send(mimeMessage);
            logger.info("Email sent successfully to {}", to);

        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    // Standard notification emails
    private String buildMessageBody(Map<String, String> data, String emoji) {
        String bodyMessage = data.getOrDefault("message", "");
        String otpType = data.get("otpType");

        String username = data.get("username");
        String password = data.get("password");
        String payoutUsername = data.get("payoutUsername");
        String payoutPassword = data.get("payoutPassword");

        String greetingEmoji = "👋";

        StringBuilder body = new StringBuilder();

        body.append("""
                <html>
                <body style="font-family: Arial, sans-serif; font-size: 15px; color: #333; background:#f9f9f9; padding:20px;">
                <div style="
                    max-width:600px;
                    margin:0 auto;
                    border:1px solid #ccc;
                    padding:20px;
                    border-radius:8px;
                    background:#ffffff;
                ">
                """);

        // Greeting
        body.append(String.format("<p>%s Hello,</p>", greetingEmoji));

        // Main message
        if (!emoji.isEmpty()) {
            body.append("<p>").append(emoji).append(" ")
                .append(bodyMessage.replace("\n", "<br>"))
                .append("</p>");
        } else {
            body.append("<p>").append(bodyMessage.replace("\n", "<br>")).append("</p>");
        }

        // Login button
        if (username != null && password != null) {
            body.append(String.format("""
                    <p style="text-align:center; margin-top:30px;">
                        <a href="%s" style="
                            background-color:#D2025B;
                            color:#ffffff;
                            padding:12px 24px;
                            text-decoration:none;
                            font-weight:bold;
                            border-radius:6px;
                            display:inline-block;
                            font-size:16px;">
                            Login to Clinic Portal
                        </a>
                    </p>
                    """, clinicLoginUrl));
        }

        // Clinic login credentials
        if (username != null && password != null) {
            body.append(String.format("""
                    <h3 style="color:#D2025B;">Supplier Login Credentials</h3>
                    <p style="background:#f4f4f4; padding:10px; border-radius:4px;">
                        <b>Username:</b> %s<br>
                        <b>Password:</b> %s
                    </p>
                    """, username, password));
        }

        // Payout credentials
        if (payoutUsername != null && payoutPassword != null) {
            body.append(String.format("""
                    <h3 style="color:#D2025B;">Payout Login Credentials</h3>
                    <p style="background:#f4f4f4; padding:10px; border-radius:4px;">
                        <b>Payout Username:</b> %s<br>
                        <b>Payout Password:</b> %s
                    </p>
                    """, payoutUsername, payoutPassword));
        }

        // Closing
        body.append("""
                <p>🙏 Thank you,<br><b>CCMS Team</b></p>
                </div>
                </body>
                </html>
                """);

        return body.toString();
    }

    // OTP emails with highlighted OTP
    private String buildOtpMessageBody(Map<String, String> data, String emoji) {

        String bodyMessage = data.getOrDefault("message", "");
        String otpType = data.get("otpType");

        // Extract OTP (4–6 digits)
        String otp = "";
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("\\b\\d{4,6}\\b").matcher(bodyMessage);
        if (matcher.find()) {
            otp = matcher.group();
        }

        String otpTitle;
        String otpDescription;

        if ("PAYOUT_PASSWORD_RESET".equals(otpType)) {
            otpTitle = "Payout Password Reset OTP";
            otpDescription = "Your OTP for resetting your Payout password:";
        } else {
            otpTitle = "Clinic Login Password Reset OTP";
            otpDescription = "Your OTP for resetting your Clinic Login password:";
        }

        return """
            <html>
            <body style="font-family: Arial, sans-serif; font-size:15px; color:#333; background:#f9f9f9; padding:20px;">
                <div style="
                    max-width:600px;
                    margin:0 auto;
                    background:#ffffff;
                    border:1px solid #ccc;
                    border-radius:8px;
                    padding:25px;
                ">

                    <p>👋 Hello,</p>

                    <h3 style="color:#D2025B; margin-bottom:10px;">
                        %s
                    </h3>

                    <p style="margin-top:5px;">
                        🔒 %s
                    </p>

                    <p style="text-align:center; margin:25px 0;">
                        <span style="
                            background:#f4f4f4;
                            color:#D2025B;
                            font-size:28px;
                            font-weight:bold;
                            letter-spacing:3px;
                            padding:12px 26px;
                            border-radius:8px;
                            display:inline-block;
                        ">
                            %s
                        </span>
                    </p>

                    <p>This OTP is valid for <b>10 minutes</b>.</p>

                    <p style="margin-top:30px;">
                        🙏 Thank you,<br>
                        <b>CCMS Team</b>
                    </p>

                </div>
            </body>
            </html>
            """.formatted(otpTitle, otpDescription, otp);
    }


    // Rejection emails
    private String buildRejectionMessageBody(Map<String, String> data, String emoji) {
        String bodyMessage = data.getOrDefault("message", "");
        String rejectionReason = data.getOrDefault("reason", "Not specified");

        String greetingEmoji = "👋";
        StringBuilder body = new StringBuilder();

        body.append("""
                <html>
                <body style="font-family: Arial, sans-serif; font-size: 15px; color: #333; background:#f9f9f9; padding:20px;">
                <div style="
                    max-width:600px;
                    margin:0 auto;
                    border:1px solid #ccc;
                    padding:20px;
                    border-radius:8px;
                    background:#ffffff;
                ">
                """);

        body.append(String.format("<p>%s Hello,</p>", greetingEmoji));

        if (!emoji.isEmpty()) {
            body.append("<p>").append(emoji).append(" ")
                .append(bodyMessage.replace("\n", "<br>"))
                .append("</p>");
        } else {
            body.append("<p>").append(bodyMessage.replace("\n", "<br>")).append("</p>");
        }

        // Highlight the rejection reason
        body.append(String.format("""
                <p style="background:#fdecea; color:#611a15; padding:10px; border-radius:4px;">
                    <b>Reason for rejection:</b> %s
                </p>
                """, rejectionReason));

        // Closing
        body.append("""
                <p>🙏 Thank you,<br><b>CCMS Team</b></p>
                </div>
                </body>
                </html>
                """);

        return body.toString();
    }
}
