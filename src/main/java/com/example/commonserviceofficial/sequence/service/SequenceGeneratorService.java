package com.example.commonserviceofficial.sequence.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service để generate số sequence duy nhất sử dụng ZooKeeper và AtomicLong
 * Mỗi keyname sẽ có một counter riêng biệt
 */
@Slf4j
@Service
public class SequenceGeneratorService {

    @Value("${zookeeper.connection-string:localhost:2181}")
    private String zookeeperConnectionString;

    @Value("${zookeeper.session-timeout:60000}")
    private int sessionTimeout;

    @Value("${zookeeper.connection-timeout:15000}")
    private int connectionTimeout;

    @Value("${sequence.zookeeper.base-path:/sequences}")
    private String basePath;

    private CuratorFramework curatorFramework;
    
    // Cache các AtomicLong cho từng keyname
    private final ConcurrentHashMap<String, AtomicLong> sequenceCounters = new ConcurrentHashMap<>();
    
    // Cache giá trị hiện tại từ ZooKeeper
    private final ConcurrentHashMap<String, Long> zookeeperValues = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            // Khởi tạo Curator Framework
            curatorFramework = CuratorFrameworkFactory.builder()
                    .connectString(zookeeperConnectionString)
                    .sessionTimeoutMs(sessionTimeout)
                    .connectionTimeoutMs(connectionTimeout)
                    .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                    .build();

            curatorFramework.start();
            
            // Đợi kết nối
            curatorFramework.blockUntilConnected();
            
            // Tạo base path nếu chưa tồn tại
            if (curatorFramework.checkExists().forPath(basePath) == null) {
                curatorFramework.create()
                        .creatingParentsIfNeeded()
                        .forPath(basePath);
            }
            
            log.info("ZooKeeper Sequence Generator initialized successfully. Connection: {}", zookeeperConnectionString);
            
        } catch (Exception e) {
            log.error("Failed to initialize ZooKeeper connection", e);
            throw new RuntimeException("Cannot initialize ZooKeeper sequence generator", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (curatorFramework != null) {
            curatorFramework.close();
            log.info("ZooKeeper connection closed");
        }
    }

    /**
     * Generate số sequence tiếp theo cho keyname
     * 
     * @param keyName tên key để phân biệt các sequence khác nhau
     * @return số sequence tiếp theo
     */
    public long generateNext(String keyName) {
        if (keyName == null || keyName.trim().isEmpty()) {
            throw new IllegalArgumentException("KeyName cannot be null or empty");
        }

        try {
            // Lấy hoặc tạo AtomicLong cho keyname này
            AtomicLong counter = sequenceCounters.computeIfAbsent(keyName, k -> {
                long currentValue = getCurrentValueFromZooKeeper(k);
                return new AtomicLong(currentValue);
            });

            // Tăng giá trị local
            long nextValue = counter.incrementAndGet();
            
            // Đồng bộ với ZooKeeper mỗi 10 lần hoặc khi cần thiết
            if (nextValue % 10 == 0 || shouldSyncWithZooKeeper(keyName, nextValue)) {
                syncWithZooKeeper(keyName, nextValue);
            }

            log.debug("Generated sequence for key '{}': {}", keyName, nextValue);
            return nextValue;

        } catch (Exception e) {
            log.error("Failed to generate sequence for key: {}", keyName, e);
            throw new RuntimeException("Cannot generate sequence for key: " + keyName, e);
        }
    }

    /**
     * Lấy giá trị hiện tại của sequence cho keyname
     * 
     * @param keyName tên key
     * @return giá trị hiện tại
     */
    public long getCurrentValue(String keyName) {
        if (keyName == null || keyName.trim().isEmpty()) {
            throw new IllegalArgumentException("KeyName cannot be null or empty");
        }

        AtomicLong counter = sequenceCounters.get(keyName);
        if (counter != null) {
            return counter.get();
        }

        // Nếu chưa có trong cache, lấy từ ZooKeeper
        return getCurrentValueFromZooKeeper(keyName);
    }

    /**
     * Reset sequence về giá trị cụ thể
     * 
     * @param keyName tên key
     * @param value giá trị mới
     */
    public void resetSequence(String keyName, long value) {
        if (keyName == null || keyName.trim().isEmpty()) {
            throw new IllegalArgumentException("KeyName cannot be null or empty");
        }

        try {
            // Cập nhật local cache
            sequenceCounters.put(keyName, new AtomicLong(value));
            
            // Cập nhật ZooKeeper
            syncWithZooKeeper(keyName, value);
            
            log.info("Reset sequence for key '{}' to value: {}", keyName, value);

        } catch (Exception e) {
            log.error("Failed to reset sequence for key: {}", keyName, e);
            throw new RuntimeException("Cannot reset sequence for key: " + keyName, e);
        }
    }

    /**
     * Lấy giá trị hiện tại từ ZooKeeper
     */
    private long getCurrentValueFromZooKeeper(String keyName) {
        try {
            String path = basePath + "/" + keyName;
            
            if (curatorFramework.checkExists().forPath(path) == null) {
                // Tạo node mới với giá trị 0
                curatorFramework.create()
                        .creatingParentsIfNeeded()
                        .forPath(path, "0".getBytes());
                zookeeperValues.put(keyName, 0L);
                return 0L;
            }

            byte[] data = curatorFramework.getData().forPath(path);
            long value = Long.parseLong(new String(data));
            zookeeperValues.put(keyName, value);
            
            return value;

        } catch (Exception e) {
            log.error("Failed to get current value from ZooKeeper for key: {}", keyName, e);
            return 0L;
        }
    }

    /**
     * Đồng bộ giá trị với ZooKeeper
     */
    private void syncWithZooKeeper(String keyName, long value) {
        try {
            String path = basePath + "/" + keyName;
            
            if (curatorFramework.checkExists().forPath(path) == null) {
                curatorFramework.create()
                        .creatingParentsIfNeeded()
                        .forPath(path, String.valueOf(value).getBytes());
            } else {
                curatorFramework.setData()
                        .forPath(path, String.valueOf(value).getBytes());
            }
            
            zookeeperValues.put(keyName, value);
            log.debug("Synced sequence for key '{}' with ZooKeeper: {}", keyName, value);

        } catch (Exception e) {
            log.error("Failed to sync with ZooKeeper for key: {}", keyName, e);
        }
    }

    /**
     * Kiểm tra có cần đồng bộ với ZooKeeper không
     */
    private boolean shouldSyncWithZooKeeper(String keyName, long currentValue) {
        Long zkValue = zookeeperValues.get(keyName);
        if (zkValue == null) {
            return true;
        }
        
        // Đồng bộ nếu chênh lệch quá 50
        return (currentValue - zkValue) >= 50;
    }

    /**
     * Lấy danh sách tất cả các key đang có
     */
    public java.util.Set<String> getAllKeys() {
        try {
            return new java.util.HashSet<>(curatorFramework.getChildren().forPath(basePath));
        } catch (Exception e) {
            log.error("Failed to get all keys from ZooKeeper", e);
            return java.util.Collections.emptySet();
        }
    }

    /**
     * Xóa một sequence key
     */
    public void deleteSequence(String keyName) {
        if (keyName == null || keyName.trim().isEmpty()) {
            throw new IllegalArgumentException("KeyName cannot be null or empty");
        }

        try {
            String path = basePath + "/" + keyName;
            
            if (curatorFramework.checkExists().forPath(path) != null) {
                curatorFramework.delete().forPath(path);
            }
            
            // Xóa khỏi cache
            sequenceCounters.remove(keyName);
            zookeeperValues.remove(keyName);
            
            log.info("Deleted sequence for key: {}", keyName);

        } catch (Exception e) {
            log.error("Failed to delete sequence for key: {}", keyName, e);
            throw new RuntimeException("Cannot delete sequence for key: " + keyName, e);
        }
    }
}