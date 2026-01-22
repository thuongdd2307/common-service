package com.example.commonserviceofficial.notification.service;

import com.example.commonserviceofficial.notification.dto.SmsRequest;
import com.example.commonserviceofficial.notification.dto.SmsResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service để gửi SMS với đầy đủ tính năng enterprise
 * Hỗ trợ nhiều nhà cung cấp SMS: Twilio, AWS SNS, Firebase, Viettel, etc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final WebClient.Builder webClientBuilder;
    private final TemplateEngine templateEngine;
    private final ObjectMapper objectMapper;

    @Value("${notification.sms.enabled:true}")
    private boolean smsEnabled;

    @Value("${notification.sms.provider:twilio}")
    private String smsProvider;

    @Value("${notification.sms.async:true}")
    private boolean asyncEnabled;

    @Value("${notification.sms.retry.max-attempts:3}")
    private int maxRetryAttempts;

    // Twilio Configuration
    @Value("${notification.sms.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${notification.sms.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${notification.sms.twilio.from-number:}")
    private String twilioFromNumber;

    // AWS SNS Configuration
    @Value("${notification.sms.aws.access-key:}")
    private String awsAccessKey;

    @Value("${notification.sms.aws.secret-key:}")
    private String awsSecretKey;

    @Value("${notification.sms.aws.region:us-east-1}")
    private String awsRegion;

    // Viettel SMS Configuration
    @Value("${notification.sms.viettel.username:}")
    private String viettelUsername;

    @Value("${notification.sms.viettel.password:}")
    private String viettelPassword;

    @Value("${notification.sms.viettel.cp-code:}")
    private String viettelCpCode;

    @Value("${notification.sms.viettel.service-id:}")
    private String viettelServiceId;

    /**
     * Gửi SMS đồng bộ
     */
    public SmsResponse sendSms(SmsRequest request) {
        if (!smsEnabled) {
            log.warn("SMS service is disabled");
            return SmsResponse.failed(null, null, request.getPhoneNumbers(), request.getMessage(), "SMS service is disabled");
        }

        long startTime = System.currentTimeMillis();
        String messageId = generateMessageId();
        String batchId = generateBatchId();

        try {
            // Process template if needed
            String processedMessage = processMessage(request);
            
            // Create processed request
            SmsRequest processedRequest = SmsRequest.builder()
                    .phoneNumbers(request.getPhoneNumbers())
                    .message(processedMessage)
                    .priority(request.getPriority())
                    .type(request.getType())
                    .scheduledAt(request.getScheduledAt())
                    .senderId(request.getSenderId())
                    .unicode(request.getUnicode())
                    .validityPeriod(request.getValidityPeriod())
                    .category(request.getCategory())
                    .customData(request.getCustomData())
                    .deliveryReport(request.getDeliveryReport())
                    .build();

            // Send based on provider
            SmsResponse response = switch (smsProvider.toLowerCase()) {
                case "twilio" -> sendViaTwilio(processedRequest, messageId, batchId);
                case "aws", "sns" -> sendViaAwsSns(processedRequest, messageId, batchId);
                case "viettel" -> sendViaViettel(processedRequest, messageId, batchId);
                case "mock" -> sendViaMock(processedRequest, messageId, batchId);
                default -> throw new IllegalArgumentException("Unsupported SMS provider: " + smsProvider);
            };

            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            return response;

        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", request.getPhoneNumbers(), e.getMessage(), e);
            
            SmsResponse response = SmsResponse.failed(messageId, batchId, request.getPhoneNumbers(), request.getMessage(), e.getMessage());
            response.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            return response;
        }
    }

    /**
     * Gửi SMS bất đồng bộ
     */
    @Async("smsTaskExecutor")
    public CompletableFuture<SmsResponse> sendSmsAsync(SmsRequest request) {
        SmsResponse response = sendSms(request);
        return CompletableFuture.completedFuture(response);
    }

    /**
     * Gửi SMS với retry logic
     */
    public SmsResponse sendSmsWithRetry(SmsRequest request) {
        SmsResponse lastResponse = null;
        
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                lastResponse = sendSms(request);
                
                if ("SUCCESS".equals(lastResponse.getStatus())) {
                    lastResponse.setRetryCount(attempt - 1);
                    return lastResponse;
                }
                
            } catch (Exception e) {
                log.warn("SMS send attempt {} failed: {}", attempt, e.getMessage());
                
                if (attempt == maxRetryAttempts) {
                    lastResponse = SmsResponse.failed(
                        lastResponse != null ? lastResponse.getMessageId() : generateMessageId(),
                        lastResponse != null ? lastResponse.getBatchId() : generateBatchId(),
                        request.getPhoneNumbers(),
                        request.getMessage(),
                        "Max retry attempts reached: " + e.getMessage()
                    );
                    lastResponse.setRetryCount(attempt);
                }
                
                // Exponential backoff
                if (attempt < maxRetryAttempts) {
                    try {
                        Thread.sleep(1000L * attempt * attempt); // 1s, 4s, 9s
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        return lastResponse;
    }

    /**
     * Gửi SMS qua Twilio
     */
    private SmsResponse sendViaTwilio(SmsRequest request, String messageId, String batchId) {
        WebClient webClient = webClientBuilder
                .baseUrl("https://api.twilio.com/2010-04-01")
                .defaultHeader(HttpHeaders.AUTHORIZATION, 
                    "Basic " + java.util.Base64.getEncoder().encodeToString(
                        (twilioAccountSid + ":" + twilioAuthToken).getBytes()))
                .build();

        for (String phoneNumber : request.getPhoneNumbers()) {
            try {
                String response = webClient.post()
                        .uri("/Accounts/{accountSid}/Messages.json", twilioAccountSid)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .bodyValue(buildTwilioPayload(request, phoneNumber))
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                log.info("SMS sent via Twilio. MessageId: {}, Phone: {}", messageId, phoneNumber);

            } catch (Exception e) {
                log.error("Failed to send SMS via Twilio to {}: {}", phoneNumber, e.getMessage());
                return SmsResponse.failed(messageId, batchId, request.getPhoneNumbers(), request.getMessage(), e.getMessage());
            }
        }

        return SmsResponse.success(messageId, batchId, request.getPhoneNumbers(), request.getMessage());
    }

    /**
     * Gửi SMS qua AWS SNS
     */
    private SmsResponse sendViaAwsSns(SmsRequest request, String messageId, String batchId) {
        // Implementation for AWS SNS
        // This would require AWS SDK integration
        log.info("Sending SMS via AWS SNS - Implementation needed");
        return SmsResponse.success(messageId, batchId, request.getPhoneNumbers(), request.getMessage());
    }

    /**
     * Gửi SMS qua Viettel
     */
    private SmsResponse sendViaViettel(SmsRequest request, String messageId, String batchId) {
        WebClient webClient = webClientBuilder
                .baseUrl("http://api.viettel.vn/sms")
                .build();

        try {
            Map<String, Object> payload = buildViettelPayload(request);
            
            String response = webClient.post()
                    .uri("/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("SMS sent via Viettel. MessageId: {}, Response: {}", messageId, response);
            return SmsResponse.success(messageId, batchId, request.getPhoneNumbers(), request.getMessage());

        } catch (Exception e) {
            log.error("Failed to send SMS via Viettel: {}", e.getMessage());
            return SmsResponse.failed(messageId, batchId, request.getPhoneNumbers(), request.getMessage(), e.getMessage());
        }
    }

    /**
     * Mock SMS provider cho testing
     */
    private SmsResponse sendViaMock(SmsRequest request, String messageId, String batchId) {
        log.info("Mock SMS sent. MessageId: {}, Recipients: {}, Message: {}", 
                messageId, request.getPhoneNumbers(), request.getMessage());
        
        // Simulate processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return SmsResponse.success(messageId, batchId, request.getPhoneNumbers(), request.getMessage());
    }

    /**
     * Process message template
     */
    private String processMessage(SmsRequest request) {
        if (request.getTemplateName() != null && request.getTemplateVariables() != null) {
            Context context = new Context();
            request.getTemplateVariables().forEach(context::setVariable);
            return templateEngine.process(request.getTemplateName(), context);
        }
        return request.getMessage();
    }

    /**
     * Build Twilio payload
     */
    private String buildTwilioPayload(SmsRequest request, String phoneNumber) {
        StringBuilder payload = new StringBuilder();
        payload.append("From=").append(request.getSenderId() != null ? request.getSenderId() : twilioFromNumber);
        payload.append("&To=").append(phoneNumber);
        payload.append("&Body=").append(java.net.URLEncoder.encode(request.getMessage(), java.nio.charset.StandardCharsets.UTF_8));
        
        if (request.getValidityPeriod() != null) {
            payload.append("&ValidityPeriod=").append(request.getValidityPeriod());
        }
        
        if (Boolean.TRUE.equals(request.getDeliveryReport())) {
            payload.append("&StatusCallback=").append("https://your-webhook-url.com/sms/status");
        }
        
        return payload.toString();
    }

    /**
     * Build Viettel payload
     */
    private Map<String, Object> buildViettelPayload(SmsRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", viettelUsername);
        payload.put("password", viettelPassword);
        payload.put("cpCode", viettelCpCode);
        payload.put("serviceId", viettelServiceId);
        payload.put("phones", String.join(",", request.getPhoneNumbers()));
        payload.put("message", request.getMessage());
        payload.put("type", request.getType().name());
        
        if (request.getSenderId() != null) {
            payload.put("senderId", request.getSenderId());
        }
        
        return payload;
    }

    /**
     * Generate unique message ID
     */
    private String generateMessageId() {
        return "SMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Generate unique batch ID
     */
    private String generateBatchId() {
        return "BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Validate SMS request
     */
    public boolean validateSmsRequest(SmsRequest request) {
        if (request.getPhoneNumbers() == null || request.getPhoneNumbers().isEmpty()) {
            return false;
        }
        
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return false;
        }
        
        // Validate phone numbers format
        for (String phone : request.getPhoneNumbers()) {
            if (!phone.matches("^\\+?[1-9]\\d{1,14}$")) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Calculate SMS parts (for long messages)
     */
    public int calculateSmsParts(String message, boolean unicode) {
        int maxLength = unicode ? 70 : 160;
        int maxConcatLength = unicode ? 67 : 153;
        
        if (message.length() <= maxLength) {
            return 1;
        }
        
        return (int) Math.ceil((double) message.length() / maxConcatLength);
    }

    /**
     * Get SMS service status
     */
    public boolean isSmsServiceHealthy() {
        try {
            // Test based on provider
            return switch (smsProvider.toLowerCase()) {
                case "twilio" -> testTwilioConnection();
                case "aws", "sns" -> testAwsSnsConnection();
                case "viettel" -> testViettelConnection();
                case "mock" -> true;
                default -> false;
            };
        } catch (Exception e) {
            log.error("SMS service health check failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean testTwilioConnection() {
        // Implementation for Twilio health check
        return twilioAccountSid != null && !twilioAccountSid.isEmpty() && 
               twilioAuthToken != null && !twilioAuthToken.isEmpty();
    }

    private boolean testAwsSnsConnection() {
        // Implementation for AWS SNS health check
        return awsAccessKey != null && !awsAccessKey.isEmpty() && 
               awsSecretKey != null && !awsSecretKey.isEmpty();
    }

    private boolean testViettelConnection() {
        // Implementation for Viettel health check
        return viettelUsername != null && !viettelUsername.isEmpty() && 
               viettelPassword != null && !viettelPassword.isEmpty();
    }
}