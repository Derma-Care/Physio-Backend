package com.AdminService.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

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
                "http://52.66.144.177:3000/clinic-admin"
        );
    }

    // ================= SEND EMAIL =================
    public void sendEmail(String to, Map<String, String> data) {
        try {
            if (to == null || to.isBlank()) {
                logger.warn("Email not sent: recipient address is blank");
                return;
            }

            String subject = data.getOrDefault("subject", "CCMS Notification");

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

            // Template selection
            if (subject.contains("OTP")) {
                helper.setText(buildOtpMessageBody(data), true);
            } else if (subject.contains("Rejected")) {
                helper.setText(buildRejectionMessageBody(data), true);
            } else {
                helper.setText(buildMessageBody(data), true);
            }

            mailSender.send(mimeMessage);
            logger.info("Email sent successfully to {}", to);

        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    // ================= COMMON TEMPLATE (DOCTOR + CLINIC) =================
    private String buildMessageBody(Map<String, String> data) {

        String bodyMessage = data.getOrDefault("message", "");
        String username = data.get("username");
        String password = data.get("password");

        StringBuilder body = new StringBuilder();

        body.append("""
            <html>
            <body style="margin:0; padding:0; font-family: Arial, sans-serif; background:#f5f7fa;">
            
            <div style="
                max-width:600px;
                margin:30px auto;
                background:#ffffff;
                border-radius:10px;
                border:1px solid #e0e0e0;
                overflow:hidden;
            ">
            
            <!-- Header -->
            <div style="background:linear-gradient(135deg, #0f2027, #203a43, #2c5364); padding:18px; text-align:center;">
                <h2 style="color:#ffffff; margin:0;">CCMS Notification</h2>
            </div>

            <!-- Body -->
            <div style="padding:20px; color:#333;">
                <p>👋 Hello,</p>
        """);

        // Message
        body.append("<p style='line-height:1.6;'>")
            .append(bodyMessage.replace("\n", "<br>"))
            .append("</p>");

        // Credentials Section
        if (username != null && password != null) {
            body.append(String.format("""
                <div style="background:#e8f0fe; padding:15px; border-radius:6px; margin-top:15px;">
                    <h3 style="margin-top:0; color:#1a73e8;">Login Credentials</h3>
                    <p><b>Username:</b> %s</p>
                    <p><b>Password:</b> %s</p>
                </div>
            """, username, password));

            body.append(String.format("""
                <div style="text-align:center; margin-top:20px;">
                    <a href="%s"
                       style="background:#28a745; color:white; padding:10px 22px; 
                              border-radius:6px; text-decoration:none; font-weight:bold;">
                       Login Now
                    </a>
                </div>
            """, clinicLoginUrl));
        }

        body.append("""
            </div>

            <div style="background:#f1f3f6; padding:12px; text-align:center; font-size:12px; color:#777;">
                © 2026 CCMS. All rights reserved.
            </div>

            </div>
            </body>
            </html>
        """);

        return body.toString();
    }

    // ================= OTP TEMPLATE =================
    private String buildOtpMessageBody(Map<String, String> data) {

        String bodyMessage = data.getOrDefault("message", "");

        return """
            <html>
            <body style="font-family: Arial, sans-serif; background:#f5f7fa; padding:20px;">
                <div style="max-width:600px; margin:auto; background:#ffffff; padding:20px; border-radius:10px;">
                    <h3 style="color:#0f2027;">🔒 OTP Verification</h3>
                    <p>%s</p>
                    <p style="font-size:28px; font-weight:bold; color:#28a745;">%s</p>
                    <p>This OTP is valid for <b>10 minutes</b>.</p>
                </div>
            </body>
            </html>
        """.formatted(bodyMessage, bodyMessage.replaceAll("\\D+", ""));
    }

    // ================= REJECTION TEMPLATE =================
    private String buildRejectionMessageBody(Map<String, String> data) {

        String bodyMessage = data.getOrDefault("message", "");
        String reason = data.getOrDefault("reason", "Not specified");

        return """
            <html>
            <body style="font-family: Arial, sans-serif; background:#f5f7fa; padding:20px;">
                <div style="max-width:600px; margin:auto; background:#ffffff; padding:20px; border-radius:10px;">
                    <h3 style="color:#d32f2f;">❌ Rejected</h3>
                    <p>%s</p>
                    <p style="background:#fdecea; padding:10px; border-radius:5px;">
                        <b>Reason:</b> %s
                    </p>
                </div>
            </body>
            </html>
        """.formatted(bodyMessage, reason);
    }
}