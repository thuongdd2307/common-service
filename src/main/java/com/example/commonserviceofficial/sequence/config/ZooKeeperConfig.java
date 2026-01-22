package com.example.commonserviceofficial.sequence.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties cho ZooKeeper
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "zookeeper")
public class ZooKeeperConfig {
    
    /**
     * ZooKeeper connection string (host:port)
     * Default: localhost:2181
     */
    private String connectionString = "localhost:2181";
    
    /**
     * Session timeout in milliseconds
     * Default: 60000 (60 seconds)
     */
    private int sessionTimeout = 60000;
    
    /**
     * Connection timeout in milliseconds
     * Default: 15000 (15 seconds)
     */
    private int connectionTimeout = 15000;
    
    /**
     * Base path for sequences in ZooKeeper
     * Default: /sequences
     */
    private String basePath = "/sequences";
    
    /**
     * Retry policy settings
     */
    private RetryPolicy retryPolicy = new RetryPolicy();
    
    @Data
    public static class RetryPolicy {
        /**
         * Base sleep time between retries in milliseconds
         * Default: 1000 (1 second)
         */
        private int baseSleepTimeMs = 1000;
        
        /**
         * Maximum number of retries
         * Default: 3
         */
        private int maxRetries = 3;
        
        /**
         * Maximum sleep time between retries in milliseconds
         * Default: 30000 (30 seconds)
         */
        private int maxSleepMs = 30000;
    }
}