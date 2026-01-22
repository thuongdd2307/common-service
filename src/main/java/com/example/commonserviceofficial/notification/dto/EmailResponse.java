package com.example.commonserviceofficial.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO cho email service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailResponse {
    
    private String messageId;
    private String status;
    private String message;
    private LocalDateTime sentAt;
    private List<String> recipients;
    private String subject;
    private Integer retryCount;
    private String errorDetails;
    private Long processingTimeMs;
    
    public static EmailResponse success(String messageId, List<String> recipients, String subject) {
        return EmailResponse.builder()
                .messageId(messageId)
                .status("SUCCESS")
                .message("Email sent successfully")
                .sentAt(LocalDateTime.now())
                .recipients(recipients)
                .subject(subject)
                .retryCount(0)
                .build();
    }
    
    public static EmailResponse queued(String messageId, List<String> recipients, String subject) {
        return EmailResponse.builder()
                .messageId(messageId)
                .status("QUEUED")
                .message("Email queued for sending")
                .sentAt(LocalDateTime.now())
                .recipients(recipients)
                .subject(subject)
                .retryCount(0)
                .build();
    }
    
    public static EmailResponse failed(String messageId, List<String> recipients, String subject, String error) {
        return EmailResponse.builder()
                .messageId(messageId)
                .status("FAILED")
                .message("Failed to send email")
                .sentAt(LocalDateTime.now())
                .recipients(recipients)
                .subject(subject)
                .errorDetails(error)
                .build();
    }
}