package com.example.commonserviceofficial.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO cho SMS service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsResponse {
    
    private String messageId;
    private String batchId; // ID cho batch SMS
    private String status;
    private String message;
    private LocalDateTime sentAt;
    private List<String> phoneNumbers;
    private String content;
    private Integer totalSms; // Số lượng SMS (có thể > 1 nếu tin nhắn dài)
    private Double cost; // Chi phí gửi
    private String currency;
    private Integer retryCount;
    private String errorDetails;
    private Long processingTimeMs;
    private Map<String, SmsDeliveryStatus> deliveryStatus; // Trạng thái gửi từng số
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmsDeliveryStatus {
        private String phoneNumber;
        private String status; // SENT, DELIVERED, FAILED, PENDING
        private String errorCode;
        private String errorMessage;
        private LocalDateTime deliveredAt;
        private Integer parts; // Số phần của SMS
    }
    
    public static SmsResponse success(String messageId, String batchId, List<String> phoneNumbers, String content) {
        return SmsResponse.builder()
                .messageId(messageId)
                .batchId(batchId)
                .status("SUCCESS")
                .message("SMS sent successfully")
                .sentAt(LocalDateTime.now())
                .phoneNumbers(phoneNumbers)
                .content(content)
                .totalSms(phoneNumbers.size())
                .retryCount(0)
                .build();
    }
    
    public static SmsResponse queued(String messageId, String batchId, List<String> phoneNumbers, String content) {
        return SmsResponse.builder()
                .messageId(messageId)
                .batchId(batchId)
                .status("QUEUED")
                .message("SMS queued for sending")
                .sentAt(LocalDateTime.now())
                .phoneNumbers(phoneNumbers)
                .content(content)
                .totalSms(phoneNumbers.size())
                .retryCount(0)
                .build();
    }
    
    public static SmsResponse failed(String messageId, String batchId, List<String> phoneNumbers, String content, String error) {
        return SmsResponse.builder()
                .messageId(messageId)
                .batchId(batchId)
                .status("FAILED")
                .message("Failed to send SMS")
                .sentAt(LocalDateTime.now())
                .phoneNumbers(phoneNumbers)
                .content(content)
                .errorDetails(error)
                .build();
    }
}