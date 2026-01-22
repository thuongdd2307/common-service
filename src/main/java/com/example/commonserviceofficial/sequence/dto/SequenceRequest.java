package com.example.commonserviceofficial.sequence.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO cho sequence generation
 */
@Data
public class SequenceRequest {
    
    @NotBlank(message = "Key name không được để trống")
    private String keyName;
    
    private Long resetValue; // Optional: để reset sequence về giá trị cụ thể
}