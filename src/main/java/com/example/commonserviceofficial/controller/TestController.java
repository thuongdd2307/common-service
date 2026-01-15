package com.example.commonserviceofficial.controller;

import com.example.commonserviceofficial.contract.BaseResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {
    
    @GetMapping("/hello")
    public BaseResponse<Map<String, Object>> hello() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Common Service is running!");
        data.put("timestamp", LocalDateTime.now().toString());
        data.put("status", "OK");
        
        return BaseResponse.ok(data);
    }
    
    @GetMapping("/actuator")
    public BaseResponse<String> health() {
        return BaseResponse.ok("Service is healthy!");
    }
}
