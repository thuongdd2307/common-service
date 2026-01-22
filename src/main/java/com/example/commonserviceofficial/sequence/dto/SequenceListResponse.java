package com.example.commonserviceofficial.sequence.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO cho danh sách sequences
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SequenceListResponse {
    
    private List<String> keys;
    private Map<String, Long> sequences;
    private int totalCount;
    private String status;
    private String message;
    
    public static SequenceListResponse success(List<String> keys, Map<String, Long> sequences) {
        return new SequenceListResponse(
            keys, 
            sequences, 
            keys.size(), 
            "SUCCESS", 
            "Sequences retrieved successfully"
        );
    }
    
    public static SequenceListResponse error(String message) {
        return new SequenceListResponse(null, null, 0, "ERROR", message);
    }
}