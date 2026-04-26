package com.example.conges.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Test Controller for Email Configuration
 * Endpoints to verify SMTP configuration and send test emails
 */
@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class EmailTestController {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${app.notification.from.name}")
    private String fromName;

    /**
     * ✅ Check SMTP Configuration
     * GET /api/test/smtp-config
     */
    @GetMapping("/smtp-config")
    public ResponseEntity<?> checkSmtpConfig() {
        Map<String, Object> response = new HashMap<>();
        try {
            // Verify mail sender is configured
            if (mailSender != null) {
                response.put("status", "✅ SUCCESS");
                response.put("message", "SMTP Configuration is valid");
                response.put("mailHost", "${MAIL_HOST}");
                response.put("mailPort", "${MAIL_PORT}");
                response.put("mailUsername", maskEmail(mailUsername));
                response.put("fromName", fromName);
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "❌ FAILED");
                response.put("message", "JavaMailSender not configured");
                return ResponseEntity.status(400).body(response);
            }
        } catch (Exception e) {
            response.put("status", "❌ ERROR");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 📧 Send Test Email
     * POST /api/test/send-email
     * Body: { "to": "test@example.com", "subject": "Test", "message": "Test message" }
     */
    @PostMapping("/send-email")
    public ResponseEntity<?> sendTestEmail(@RequestBody EmailTestRequest request) {
        Map<String, Object> response = new HashMap<>();

        // Validate input
        if (request.getTo() == null || request.getTo().isEmpty()) {
            response.put("status", "❌ FAILED");
            response.put("message", "Email 'to' is required");
            return ResponseEntity.status(400).body(response);
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailUsername);
            message.setTo(request.getTo());
            message.setSubject(request.getSubject() != null ? request.getSubject() : "Test Email - Gestion des Congés");
            message.setText(request.getMessage() != null ? request.getMessage() : "Ceci est un email de test du système Gestion des Congés");

            // Send email
            mailSender.send(message);

            response.put("status", "✅ SUCCESS");
            response.put("message", "Email sent successfully");
            response.put("recipient", request.getTo());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "❌ FAILED");
            response.put("message", "Error sending email: " + e.getMessage());
            response.put("errorDetails", e.getClass().getName());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 🔍 Advanced SMTP Test
     * POST /api/test/smtp-verify
     * Tests SMTP connectivity without sending
     */
    @PostMapping("/smtp-verify")
    public ResponseEntity<?> verifySmtpConnection() {
        Map<String, Object> response = new HashMap<>();
        try {
            // Try to send a minimal test
            SimpleMailMessage testMessage = new SimpleMailMessage();
            testMessage.setFrom(mailUsername);
            testMessage.setTo(mailUsername); // Send to self
            testMessage.setSubject("[SYSTEM TEST] SMTP Verification");
            testMessage.setText("SMTP Configuration Test - " + System.currentTimeMillis());

            mailSender.send(testMessage);

            response.put("status", "✅ VERIFIED");
            response.put("message", "SMTP connection successful");
            response.put("host", "${MAIL_HOST}");
            response.put("port", "${MAIL_PORT}");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (org.springframework.mail.MailAuthenticationException e) {
            response.put("status", "❌ AUTH_FAILED");
            response.put("message", "SMTP Authentication failed - Check credentials");
            response.put("hint", "Verify App Password is correct and 2FA is enabled on Gmail");
            return ResponseEntity.status(401).body(response);

        } catch (org.springframework.mail.MailSendException e) {
            response.put("status", "❌ SEND_FAILED");
            response.put("message", "Failed to connect to SMTP server");
            response.put("error", e.getMessage());
            response.put("hint", "Check host, port, and network connectivity");
            return ResponseEntity.status(500).body(response);

        } catch (Exception e) {
            response.put("status", "❌ ERROR");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Helper: Mask email for security
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 3) return "***";
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return email;
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    /**
     * Request DTO for Email Test
     */
    public static class EmailTestRequest {
        private String to;
        private String subject;
        private String message;

        public EmailTestRequest() {}

        public EmailTestRequest(String to, String subject, String message) {
            this.to = to;
            this.subject = subject;
            this.message = message;
        }

        // Getters & Setters
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
