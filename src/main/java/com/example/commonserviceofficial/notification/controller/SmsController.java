package com.example.commonserviceofficial.notification.controller;

import com.example.commonserviceofficial.notification.dto.SmsRequest;
import com.example.commonserviceofficial.notification.dto.SmsResponse;
import com.example.commonserviceofficial.notification.service.SmsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller cho SMS Service
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;

    /**
     * Gửi SMS đồng bộ
     * 
     * POST /api/notifications/sms/send
     */
    @PostMapping("/send")
    public ResponseEntity<SmsResponse> sendSms(@Valid @RequestBody SmsRequest request) {
        try {
            if (!smsService.validateSmsRequest(request)) {
                SmsResponse response = SmsResponse.failed(null, null, request.getPhoneNumbers(), request.getMessage(), "Invalid SMS request");
                return ResponseEntity.badRequest().body(response);
            }

            SmsResponse response = smsService.sendSms(request);
            
            if ("SUCCESS".equals(response.getStatus())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Failed to send SMS: {}", e.getMessage(), e);
            SmsResponse response = SmsResponse.failed(null, null, request.getPhoneNumbers(), request.getMessage(), e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Gửi SMS bất đồng bộ
     * 
     * POST /api/notifications/sms/send-async
     */
    @PostMapping("/send-async")
    public ResponseEntity<Map<String, Object>> sendSmsAsync(@Valid @RequestBody SmsRequest request) {
        try {
            if (!smsService.validateSmsRequest(request)) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "ERROR");
                response.put("message", "Invalid SMS request");
                return ResponseEntity.badRequest().body(response);
            }

            CompletableFuture<SmsResponse> future = smsService.sendSmsAsync(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "QUEUED");
            response.put("message", "SMS queued for async processing");
            response.put("phoneNumbers", request.getPhoneNumbers());
            response.put("content", request.getMessage());
            
            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            log.error("Failed to queue SMS: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Gửi SMS với retry logic
     * 
     * POST /api/notifications/sms/send-with-retry
     */
    @PostMapping("/send-with-retry")
    public ResponseEntity<SmsResponse> sendSmsWithRetry(@Valid @RequestBody SmsRequest request) {
        try {
            if (!smsService.validateSmsRequest(request)) {
                SmsResponse response = SmsResponse.failed(null, null, request.getPhoneNumbers(), request.getMessage(), "Invalid SMS request");
                return ResponseEntity.badRequest().body(response);
            }

            SmsResponse response = smsService.sendSmsWithRetry(request);
            
            if ("SUCCESS".equals(response.getStatus())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Failed to send SMS with retry: {}", e.getMessage(), e);
            SmsResponse response = SmsResponse.failed(null, null, request.getPhoneNumbers(), request.getMessage(), e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Gửi SMS đơn giản (chỉ cần phone, message)
     * 
     * POST /api/notifications/sms/send-simple
     */
    @PostMapping("/send-simple")
    public ResponseEntity<SmsResponse> sendSimpleSms(
            @RequestParam String phoneNumber,
            @RequestParam String message,
            @RequestParam(required = false, defaultValue = "TRANSACTIONAL") String type) {
        try {
            SmsRequest request = SmsRequest.builder()
                    .phoneNumbers(java.util.List.of(phoneNumber))
                    .message(message)
                    .type(SmsRequest.SmsType.valueOf(type.toUpperCase()))
                    .build();

            SmsResponse response = smsService.sendSms(request);
            
            if ("SUCCESS".equals(response.getStatus())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Failed to send simple SMS: {}", e.getMessage(), e);
            SmsResponse response = SmsResponse.failed(null, null, java.util.List.of(phoneNumber), message, e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Gửi OTP SMS
     * 
     * POST /api/notifications/sms/send-otp
     */
    @PostMapping("/send-otp")
    public ResponseEntity<SmsResponse> sendOtpSms(
            @RequestParam String phoneNumber,
            @RequestParam String otpCode,
            @RequestParam(required = false, defaultValue = "5") Integer validityMinutes) {
        try {
            String message = String.format("Mã OTP của bạn là: %s. Mã có hiệu lực trong %d phút. Không chia sẻ mã này với ai.", 
                    otpCode, validityMinutes);

            SmsRequest request = SmsRequest.builder()
                    .phoneNumbers(java.util.List.of(phoneNumber))
                    .message(message)
                    .type(SmsRequest.SmsType.TRANSACTIONAL)
                    .priority(SmsRequest.SmsPriority.HIGH)
                    .category("otp")
                    .validityPeriod(validityMinutes)
                    .build();

            SmsResponse response = smsService.sendSms(request);
            
            if ("SUCCESS".equals(response.getStatus())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Failed to send OTP SMS: {}", e.getMessage(), e);
            SmsResponse response = SmsResponse.failed(null, null, java.util.List.of(phoneNumber), "OTP SMS", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Health check cho SMS service
     * 
     * GET /api/notifications/sms/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            boolean isHealthy = smsService.isSmsServiceHealthy();
            
            health.put("status", isHealthy ? "UP" : "DOWN");
            health.put("service", "sms");
            health.put("message", isHealthy ? "SMS service is healthy" : "SMS service is unhealthy");
            health.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("service", "sms");
            health.put("error", e.getMessage());
            health.put("message", "SMS service health check failed");
            health.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.status(503).body(health);
        }
    }

    /**
     * Calculate SMS parts
     * 
     * POST /api/notifications/sms/calculate-parts
     */
    @PostMapping("/calculate-parts")
    public ResponseEntity<Map<String, Object>> calculateSmsParts(
            @RequestParam String message,
            @RequestParam(required = false, defaultValue = "false") Boolean unicode) {
        try {
            int parts = smsService.calculateSmsParts(message, unicode);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", message);
            result.put("messageLength", message.length());
            result.put("unicode", unicode);
            result.put("parts", parts);
            result.put("maxLengthPerPart", unicode ? (parts > 1 ? 67 : 70) : (parts > 1 ? 153 : 160));
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Test SMS template
     * 
     * POST /api/notifications/sms/test-template
     */
    @PostMapping("/test-template")
    public ResponseEntity<SmsResponse> testSmsTemplate(
            @RequestParam String phoneNumber,
            @RequestParam String templateName,
            @RequestBody(required = false) Map<String, Object> variables) {
        try {
            SmsRequest request = SmsRequest.builder()
                    .phoneNumbers(java.util.List.of(phoneNumber))
                    .message("Template test message")
                    .templateName(templateName)
                    .templateVariables(variables != null ? variables : new HashMap<>())
                    .type(SmsRequest.SmsType.TRANSACTIONAL)
                    .category("test")
                    .build();

            SmsResponse response = smsService.sendSms(request);
            
            if ("SUCCESS".equals(response.getStatus())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Failed to test SMS template: {}", e.getMessage(), e);
            SmsResponse response = SmsResponse.failed(null, null, java.util.List.of(phoneNumber), "Template test", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Validate SMS request
     * 
     * POST /api/notifications/sms/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateSms(@RequestBody SmsRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean isValid = smsService.validateSmsRequest(request);
            
            result.put("valid", isValid);
            result.put("message", isValid ? "SMS request is valid" : "SMS request is invalid");
            
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
    private java.util.List<String> getValidationErrors(SmsRequest request) {
        java.util.List<String> errors = new java.util.ArrayList<>();
        
        if (request.getPhoneNumbers() == null || request.getPhoneNumbers().isEmpty()) {
            errors.add("Phone numbers list is required");
        } else {
            for (String phone : request.getPhoneNumbers()) {
                if (!phone.matches("^\\+?[1-9]\\d{1,14}$")) {
                    errors.add("Invalid phone number format: " + phone);
                }
            }
        }
        
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            errors.add("Message content is required");
        }
        
        return errors;
    }
}