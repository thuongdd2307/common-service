package com.example.commonserviceofficial.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Request DTO cho gửi SMS
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsRequest {
    
    @NotEmpty(message = "Danh sách số điện thoại không được để trống")
    private List<@Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Số điện thoại không hợp lệ") String> phoneNumbers;
    
    @NotBlank(message = "Nội dung SMS không được để trống")
    private String message;
    
    private String templateName; // Template name (nếu sử dụng template)
    
    private Map<String, Object> templateVariables; // Variables cho template
    
    private SmsPriority priority = SmsPriority.NORMAL; // Độ ưu tiên
    
    private SmsType type = SmsType.TRANSACTIONAL; // Loại SMS
    
    private LocalDateTime scheduledAt; // Thời gian gửi (nếu schedule)
    
    private String senderId; // Sender ID/Brand name
    
    private Boolean unicode = false; // Hỗ trợ Unicode
    
    private Integer validityPeriod; // Thời gian hiệu lực (phút)
    
    private String category; // Phân loại SMS (otp, marketing, notification, etc.)
    
    private Map<String, String> customData; // Custom data
    
    private Boolean deliveryReport = true; // Yêu cầu delivery report
    
    public enum SmsPriority {
        LOW(3),
        NORMAL(2),
        HIGH(1);
        
        private final int value;
        
        SmsPriority(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    public enum SmsType {
        TRANSACTIONAL,  // SMS giao dịch (OTP, thông báo)
        PROMOTIONAL,    // SMS quảng cáo
        NOTIFICATION    // SMS thông báo
    }
}