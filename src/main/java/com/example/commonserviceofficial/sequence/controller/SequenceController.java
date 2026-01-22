package com.example.commonserviceofficial.sequence.controller;

import com.example.commonserviceofficial.sequence.dto.SequenceListResponse;
import com.example.commonserviceofficial.sequence.dto.SequenceRequest;
import com.example.commonserviceofficial.sequence.dto.SequenceResponse;
import com.example.commonserviceofficial.sequence.service.SequenceGeneratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST Controller cho Sequence Generation API
 */
@Slf4j
@RestController
@RequestMapping("/api/sequences")
@RequiredArgsConstructor
public class SequenceController {

    private final SequenceGeneratorService sequenceGeneratorService;

    /**
     * Generate số sequence tiếp theo cho keyname
     * 
     * POST /api/sequences/generate
     * Body: {"keyName": "ORDER_ID"}
     */
    @PostMapping("/generate")
    public ResponseEntity<SequenceResponse> generateNext(@Valid @RequestBody SequenceRequest request) {
        try {
            long nextValue = sequenceGeneratorService.generateNext(request.getKeyName());
            
            SequenceResponse response = SequenceResponse.success(request.getKeyName(), nextValue);
            
            log.info("Generated sequence for key '{}': {}", request.getKeyName(), nextValue);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to generate sequence for key: {}", request.getKeyName(), e);
            SequenceResponse response = SequenceResponse.error(request.getKeyName(), e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Generate số sequence cho keyname qua URL parameter
     * 
     * POST /api/sequences/generate/{keyName}
     */
    @PostMapping("/generate/{keyName}")
    public ResponseEntity<SequenceResponse> generateNextByPath(@PathVariable String keyName) {
        try {
            long nextValue = sequenceGeneratorService.generateNext(keyName);
            
            SequenceResponse response = SequenceResponse.success(keyName, nextValue);
            
            log.info("Generated sequence for key '{}': {}", keyName, nextValue);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to generate sequence for key: {}", keyName, e);
            SequenceResponse response = SequenceResponse.error(keyName, e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy giá trị hiện tại của sequence
     * 
     * GET /api/sequences/{keyName}
     */
    @GetMapping("/{keyName}")
    public ResponseEntity<SequenceResponse> getCurrentValue(@PathVariable String keyName) {
        try {
            long currentValue = sequenceGeneratorService.getCurrentValue(keyName);
            
            SequenceResponse response = SequenceResponse.current(keyName, currentValue);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get current value for key: {}", keyName, e);
            SequenceResponse response = SequenceResponse.error(keyName, e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Reset sequence về giá trị cụ thể
     * 
     * PUT /api/sequences/reset
     * Body: {"keyName": "ORDER_ID", "resetValue": 1000}
     */
    @PutMapping("/reset")
    public ResponseEntity<SequenceResponse> resetSequence(@Valid @RequestBody SequenceRequest request) {
        try {
            if (request.getResetValue() == null) {
                throw new IllegalArgumentException("Reset value is required");
            }
            
            sequenceGeneratorService.resetSequence(request.getKeyName(), request.getResetValue());
            
            SequenceResponse response = SequenceResponse.reset(request.getKeyName(), request.getResetValue());
            
            log.info("Reset sequence for key '{}' to value: {}", request.getKeyName(), request.getResetValue());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to reset sequence for key: {}", request.getKeyName(), e);
            SequenceResponse response = SequenceResponse.error(request.getKeyName(), e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Reset sequence qua URL parameter
     * 
     * PUT /api/sequences/reset/{keyName}/{value}
     */
    @PutMapping("/reset/{keyName}/{value}")
    public ResponseEntity<SequenceResponse> resetSequenceByPath(
            @PathVariable String keyName, 
            @PathVariable Long value) {
        try {
            sequenceGeneratorService.resetSequence(keyName, value);
            
            SequenceResponse response = SequenceResponse.reset(keyName, value);
            
            log.info("Reset sequence for key '{}' to value: {}", keyName, value);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to reset sequence for key: {}", keyName, e);
            SequenceResponse response = SequenceResponse.error(keyName, e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy danh sách tất cả sequences
     * 
     * GET /api/sequences
     */
    @GetMapping
    public ResponseEntity<SequenceListResponse> getAllSequences() {
        try {
            Set<String> keys = sequenceGeneratorService.getAllKeys();
            List<String> keyList = keys.stream().sorted().toList();
            
            Map<String, Long> sequences = new HashMap<>();
            for (String key : keyList) {
                try {
                    long value = sequenceGeneratorService.getCurrentValue(key);
                    sequences.put(key, value);
                } catch (Exception e) {
                    log.warn("Failed to get value for key: {}", key, e);
                    sequences.put(key, -1L); // Đánh dấu lỗi
                }
            }
            
            SequenceListResponse response = SequenceListResponse.success(keyList, sequences);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get all sequences", e);
            SequenceListResponse response = SequenceListResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Xóa một sequence
     * 
     * DELETE /api/sequences/{keyName}
     */
    @DeleteMapping("/{keyName}")
    public ResponseEntity<SequenceResponse> deleteSequence(@PathVariable String keyName) {
        try {
            sequenceGeneratorService.deleteSequence(keyName);
            
            SequenceResponse response = new SequenceResponse(
                keyName, null, null, "SUCCESS", "Sequence deleted successfully"
            );
            
            log.info("Deleted sequence for key: {}", keyName);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to delete sequence for key: {}", keyName, e);
            SequenceResponse response = SequenceResponse.error(keyName, e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Health check cho ZooKeeper connection
     * 
     * GET /api/sequences/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Test bằng cách lấy danh sách keys
            Set<String> keys = sequenceGeneratorService.getAllKeys();
            
            health.put("status", "UP");
            health.put("zookeeper", "CONNECTED");
            health.put("totalKeys", keys.size());
            health.put("message", "ZooKeeper sequence generator is healthy");
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("zookeeper", "DISCONNECTED");
            health.put("error", e.getMessage());
            health.put("message", "ZooKeeper sequence generator is unhealthy");
            
            return ResponseEntity.status(503).body(health);
        }
    }
}