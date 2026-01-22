package com.example.commonserviceofficial.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO cho gửi email
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {
    
    @NotEmpty(message = "Danh sách người nhận không được để trống")
    private List<@Email(message = "Email không hợp lệ") String> to;
    
    private List<@Email(message = "Email CC không hợp lệ") String> cc;
    
    private List<@Email(message = "Email BCC không hợp lệ") String> bcc;
    
    @NotBlank(message = "Tiêu đề email không được để trống")
    private String subject;
    
    private String textContent;  // Plain text content
    
    private String htmlContent;  // HTML content
    
    private String templateName; // Template name (nếu sử dụng template)
    
    private Map<String, Object> templateVariables; // Variables cho template
    
    private List<EmailAttachment> attachments; // File đính kèm
    
    private EmailPriority priority = EmailPriority.NORMAL; // Độ ưu tiên
    
    private String replyTo; // Reply-to address
    
    private String fromName; // Tên người gửi
    
    private Boolean trackOpening = false; // Theo dõi mở email
    
    private Boolean trackClicking = false; // Theo dõi click link
    
    private String category; // Phân loại email (marketing, transactional, etc.)
    
    private Map<String, String> customHeaders; // Custom headers
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailAttachment {
        private String fileName;
        private String contentType;
        private byte[] content;
        private String contentId; // Cho inline attachments
        private Boolean inline = false;
    }
    
    public enum EmailPriority {
        LOW(5),
        NORMAL(3),
        HIGH(1);
        
        private final int value;
        
        EmailPriority(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
}