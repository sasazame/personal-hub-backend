package com.zametech.todoapp.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.email.provider", havingValue = "brevo")
public class BrevoEmailService implements EmailService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${brevo.api-key}")
    private String apiKey;

    @Value("${brevo.api-url:https://api.brevo.com/v3}")
    private String apiUrl;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.email.from:noreply@personalhub.com}")
    private String fromEmail;

    @Value("${app.email.from-name:Personal Hub}")
    private String fromName;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            Map<String, Object> emailData = new HashMap<>();
            
            // Sender
            Map<String, String> sender = new HashMap<>();
            sender.put("email", fromEmail);
            sender.put("name", fromName);
            emailData.put("sender", sender);
            
            // Recipients
            Map<String, String> recipient = new HashMap<>();
            recipient.put("email", toEmail);
            emailData.put("to", List.of(recipient));
            
            // Email content
            emailData.put("subject", "Password Reset Request - Personal Hub");
            emailData.put("htmlContent", buildPasswordResetEmailContent(resetLink));
            
            String jsonBody = objectMapper.writeValueAsString(emailData);
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
            
            String url = apiUrl + "/smtp/email";
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Password reset email sent successfully to: {} via Brevo API", toEmail);
                if (response.getBody() != null) {
                    try {
                        Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), Map.class);
                        String messageId = (String) responseBody.get("messageId");
                        if (messageId != null) {
                            log.info("Brevo Message ID: {}", messageId);
                        }
                    } catch (Exception e) {
                        log.debug("Could not parse Brevo response body: {}", response.getBody());
                    }
                }
            } else {
                log.error("Failed to send email via Brevo. Status: {}, Body: {}", 
                    response.getStatusCode(), response.getBody());
                throw new RuntimeException("Failed to send password reset email");
            }
            
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
    
    private String buildPasswordResetEmailContent(String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        background-color: #f5f5f5;
                        margin: 0;
                        padding: 0;
                    }
                    .container {
                        max-width: 600px;
                        margin: 40px auto;
                        background-color: #ffffff;
                        border-radius: 10px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        overflow: hidden;
                    }
                    .header {
                        background: linear-gradient(135deg, #0B5CFF 0%, #007AFF 100%);
                        color: white;
                        padding: 40px 20px;
                        text-align: center;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 28px;
                        font-weight: 600;
                    }
                    .content {
                        padding: 40px 30px;
                    }
                    .content h2 {
                        color: #333;
                        margin-top: 0;
                    }
                    .button {
                        display: inline-block;
                        padding: 14px 32px;
                        background: linear-gradient(135deg, #0B5CFF 0%, #007AFF 100%);
                        color: white !important;
                        text-decoration: none;
                        border-radius: 30px;
                        font-weight: 600;
                        margin: 20px 0;
                        transition: transform 0.2s, box-shadow 0.2s;
                    }
                    .button:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 4px 12px rgba(11, 92, 255, 0.3);
                        color: white !important;
                    }
                    .button:visited {
                        color: white !important;
                    }
                    .link-box {
                        background-color: #f8f9fa;
                        border: 1px solid #e9ecef;
                        border-radius: 5px;
                        padding: 15px;
                        margin: 20px 0;
                        word-break: break-all;
                        font-family: monospace;
                        font-size: 14px;
                    }
                    .warning {
                        background-color: #fff3cd;
                        border-left: 4px solid #ffc107;
                        padding: 15px;
                        margin: 20px 0;
                        border-radius: 4px;
                    }
                    .footer {
                        background-color: #f8f9fa;
                        padding: 30px;
                        text-align: center;
                        font-size: 14px;
                        color: #6c757d;
                    }
                    .footer p {
                        margin: 5px 0;
                    }
                    @media only screen and (max-width: 600px) {
                        .container {
                            margin: 0;
                            border-radius: 0;
                        }
                        .content {
                            padding: 30px 20px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Personal Hub</h1>
                    </div>
                    <div class="content">
                        <h2>Password Reset Request</h2>
                        <p>Hello,</p>
                        <p>We received a request to reset the password for your Personal Hub account.</p>
                        <p style="text-align: center;">
                            <a href="RESET_LINK_PLACEHOLDER" class="button">Reset My Password</a>
                        </p>
                        <p>If the button above doesn't work, copy and paste this link into your browser:</p>
                        <div class="link-box">RESET_LINK_PLACEHOLDER</div>
                        <div class="warning">
                            <strong>⏰ Important:</strong> This link will expire in 1 hour for security reasons.
                        </div>
                        <p>If you didn't request this password reset, you can safely ignore this email. Your password won't be changed.</p>
                    </div>
                    <div class="footer">
                        <p><strong>Personal Hub Team</strong></p>
                        <p>This is an automated message, please do not reply to this email.</p>
                        <p>If you need help, please contact our support team.</p>
                    </div>
                </div>
            </body>
            </html>
            """.replace("RESET_LINK_PLACEHOLDER", resetLink);
    }
}