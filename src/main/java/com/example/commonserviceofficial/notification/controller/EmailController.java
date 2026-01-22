package com.example.commonserviceofficial.notification.controller;

import com.example.commonserviceofficial.notification.dto.EmailRequest;
import com.example.commonserviceofficial.notification.dto.EmailResponse;
import com.example.commonserviceofficial.notification.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller cho Email Service
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    /**
     * Gửi email đồng bộ
     * 
     * POST /api/notifications/email/send
     */
    @PostMapping("/send")
    public ResponseEntity<EmailResponse> sendEmail(@Valid @RequestBody EmailRequest request) {
        try {
            if (!emailService.validateEmailRequest(request)) {
                EmailResponse response = EmailResponse.failed(null, request.getTo(), request.getSubject(), "Invalid email request");
                return ResponseEntity.badRequest().body(response);
            }

            EmailResponse response = emailService.sendEmail(request);
            
            if ("SUCCESS".equals(response.getStatus())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage(), e);
            EmailResponse response = EmailResponse.failed(null, request.getTo(), request.getSubject(), e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Gửi email bất đồng bộ
     * 
     * POST /api/notifications/email/send-async
     */
    @PostMapping("/send-async")
    public ResponseEntity<Map<String, Object>> sendEmailAsync(@Valid @RequestBody EmailRequest request) {
        try {
            if (!emailService.validateEmailRequest(request)) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "ERROR");
                response.put("message", "Invalid email request");
                return ResponseEntity.badRequest().body(response);
            }

            CompletableFuture<EmailResponse> future = emailService.sendEmailAsync(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "QUEUED");
            response.put("message", "Email queued for async processing");
            response.put("recipients", request.getTo());
            response.put("subject", request.getSubject());
            
            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            log.error("Failed to queue email: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Gửi email với retry logic
     * 
     * POST /api/notifications/email/send-with-retry
     */
    @PostMapping("/send-with-retry")
    public ResponseEntity<EmailResponse> sendEmailWithRetry(@Valid @RequestBody EmailRequest request) {
        try {
            if (!emailService.validateEmailRequest(request)) {
                EmailResponse response = EmailResponse.failed(null, request.getTo(), request.getSubject(), "Invalid email request");
                return ResponseEntity.badRequest().body(response);
            }

            EmailResponse response = emailService.sendEmailWithRetry(request);
            
            if ("SUCCESS".equals(response.getStatus())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Failed to send email with retry: {}", e.getMessage(), e);
            EmailResponse response = EmailResponse.failed(null, request.getTo(), request.getSubject(), e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Gửi email đơn giản (chỉ cần to, subject, message)
     * 
     * POST /api/notifications/email/send-simple
     */
    @PostMapping("/send-simple")
    public ResponseEntity<EmailResponse> sendSimpleEmail(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String message,
            @RequestParam(required = false) String type) {
        try {
            EmailRequest.EmailRequestBuilder builder = EmailRequest.builder()
                    .to(java.util.List.of(to))
                    .subject(subject);

            if ("html".equalsIgnoreCase(type)) {
                builder.htmlContent(message);
            } else {
                builder.textContent(message);
            }

            EmailRequest request = builder.build();
            EmailResponse response = emailService.sendEmail(request);
            
            if ("SUCCESS".equals(response.getStatus())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Failed to send simple email: {}", e.getMessage(), e);
            EmailResponse response = EmailResponse.failed(null, java.util.List.of(to), subject, e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Health check cho email service
     * 
     * GET /api/notifications/email/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            boolean isHealthy = emailService.isEmailServiceHealthy();
            
            health.put("status", isHealthy ? "UP" : "DOWN");
            health.put("service", "email");
            health.put("message", isHealthy ? "Email service is healthy" : "Email service is unhealthy");
            health.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("service", "email");
            health.put("error", e.getMessage());
            health.put("message", "Email service health check failed");
            health.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(503).body(health);
        }
    }

    /**
     * Test email template
     * 
     * POST /api/notifications/email/test-template
     */
    @PostMapping("/test-template")
    public ResponseEntity<EmailResponse> testEmailTemplate(
            @RequestParam String to,
            @RequestParam String templateName,
            @RequestBody(required = false) Map<String, Object> variables) {
        try {
            EmailRequest request = EmailRequest.builder()
                    .to(java.util.List.of(to))
                    .subject("Test Template: " + templateName)
                    .templateName(templateName)
                    .templateVariables(variables != null ? variables : new HashMap<>())
                    .build();

            EmailResponse response = emailService.sendEmail(request);
            
            if ("SUCCESS".equals(response.getStatus())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Failed to test email template: {}", e.getMessage(), e);
            EmailResponse response = EmailResponse.failed(null, java.util.List.of(to), "Test Template: " + templateName, e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Validate email request
     * 
     * POST /api/notifications/email/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateEmail(@RequestBody EmailRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean isValid = emailService.validateEmailRequest(request);
            
            result.put("valid", isValid);
            result.put("message", isValid ? "Email request is valid" : "Email request is invalid");
            
            if (!isValid) {
                result.put("errors", getValidationErrors(request));
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("valid", false);
            result.put("message", "Validation failed");
            result.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Get validation errors
     */
    private java.util.List<String> getValidationErrors(EmailRequest request) {
        java.util.List<String> errors = new java.util.ArrayList<>();
        
        if (request.getTo() == null || request.getTo().isEmpty()) {
            errors.add("Recipients list is required");
        }
        
        if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
            errors.add("Subject is required");
        }
        
        if (request.getTextContent() == null && request.getHtmlContent() == null && request.getTemplateName() == null) {
            errors.add("Email content is required (text, html, or template)");
        }
        
        return errors;
    }
}