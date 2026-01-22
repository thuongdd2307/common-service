package com.example.commonserviceofficial.sequence.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho sequence generation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SequenceResponse {
    
    private String keyName;
    private Long currentValue;
    private Long nextValue;
    private String status;
    private String message;
    
    public static SequenceResponse success(String keyName, Long nextValue) {
        return new SequenceResponse(keyName, nextValue - 1, nextValue, "SUCCESS", "Sequence generated successfully");
    }
    
    public static SequenceResponse current(String keyName, Long currentValue) {
        return new SequenceResponse(keyName, currentValue, null, "SUCCESS", "Current value retrieved");
    }
    
    public static SequenceResponse reset(String keyName, Long resetValue) {
        return new SequenceResponse(keyName, resetValue, null, "SUCCESS", "Sequence reset successfully");
    }
    
    public static SequenceResponse error(String keyName, String message) {
        return new SequenceResponse(keyName, null, null, "ERROR", message);
    }
}