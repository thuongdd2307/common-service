# API Generate Số Sequence với ZooKeeper

## Tổng Quan

API này cung cấp khả năng generate số sequence duy nhất sử dụng ZooKeeper và AtomicLong. Mỗi keyname sẽ có một counter riêng biệt, đảm bảo tính duy nhất trong hệ thống phân tán.

## Kiến Trúc

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Client App    │───▶│  Common Service │───▶│   ZooKeeper     │
│                 │    │                 │    │                 │
│ - Generate ID   │    │ - AtomicLong    │    │ - Persistent    │
│ - Get Current   │    │ - Cache         │    │ - Distributed   │
│ - Reset Value   │    │ - Sync Logic    │    │ - Coordination  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Đặc Điểm

- **High Performance**: Sử dụng AtomicLong local cache
- **Distributed Safe**: ZooKeeper đảm bảo consistency
- **Auto Sync**: Tự động đồng bộ với ZooKeeper
- **Multiple Keys**: Hỗ trợ nhiều keyname khác nhau
- **Fault Tolerant**: Xử lý lỗi và retry logic

## API Endpoints

### 1. Generate Số Sequence

#### POST `/api/sequences/generate`

**Request Body:**
```json
{
  "keyName": "ORDER_ID"
}
```

**Response:**
```json
{
  "keyName": "ORDER_ID",
  "currentValue": 100,
  "nextValue": 101,
  "status": "SUCCESS",
  "message": "Sequence generated successfully"
}
```

#### POST `/api/sequences/generate/{keyName}`

**URL:** `/api/sequences/generate/ORDER_ID`

**Response:**
```json
{
  "keyName": "ORDER_ID",
  "currentValue": 101,
  "nextValue": 102,
  "status": "SUCCESS",
  "message": "Sequence generated successfully"
}
```

### 2. Lấy Giá Trị Hiện Tại

#### GET `/api/sequences/{keyName}`

**URL:** `/api/sequences/ORDER_ID`

**Response:**
```json
{
  "keyName": "ORDER_ID",
  "currentValue": 102,
  "nextValue": null,
  "status": "SUCCESS",
  "message": "Current value retrieved"
}
```

### 3. Reset Sequence

#### PUT `/api/sequences/reset`

**Request Body:**
```json
{
  "keyName": "ORDER_ID",
  "resetValue": 1000
}
```

**Response:**
```json
{
  "keyName": "ORDER_ID",
  "currentValue": 1000,
  "nextValue": null,
  "status": "SUCCESS",
  "message": "Sequence reset successfully"
}
```

#### PUT `/api/sequences/reset/{keyName}/{value}`

**URL:** `/api/sequences/reset/ORDER_ID/2000`

**Response:**
```json
{
  "keyName": "ORDER_ID",
  "currentValue": 2000,
  "nextValue": null,
  "status": "SUCCESS",
  "message": "Sequence reset successfully"
}
```

### 4. Lấy Tất Cả Sequences

#### GET `/api/sequences`

**Response:**
```json
{
  "keys": ["ORDER_ID", "INVOICE_ID", "USER_ID"],
  "sequences": {
    "ORDER_ID": 102,
    "INVOICE_ID": 55,
    "USER_ID": 1001
  },
  "totalCount": 3,
  "status": "SUCCESS",
  "message": "Sequences retrieved successfully"
}
```

### 5. Xóa Sequence

#### DELETE `/api/sequences/{keyName}`

**URL:** `/api/sequences/ORDER_ID`

**Response:**
```json
{
  "keyName": "ORDER_ID",
  "currentValue": null,
  "nextValue": null,
  "status": "SUCCESS",
  "message": "Sequence deleted successfully"
}
```

### 6. Health Check

#### GET `/api/sequences/health`

**Response (Healthy):**
```json
{
  "status": "UP",
  "zookeeper": "CONNECTED",
  "totalKeys": 3,
  "message": "ZooKeeper sequence generator is healthy"
}
```

**Response (Unhealthy):**
```json
{
  "status": "DOWN",
  "zookeeper": "DISCONNECTED",
  "error": "Connection refused",
  "message": "ZooKeeper sequence generator is unhealthy"
}
```

## Cấu Hình

### application.yml

```yaml
# ZooKeeper Configuration
zookeeper:
  connection-string: localhost:2181
  session-timeout: 60000
  connection-timeout: 15000
  base-path: /sequences
  retry-policy:
    base-sleep-time-ms: 1000
    max-retries: 3
    max-sleep-ms: 30000

# Sequence Generator Configuration
sequence:
  zookeeper:
    base-path: /sequences
    sync-interval: 10    # Sync mỗi 10 lần generate
    sync-threshold: 50   # Sync khi chênh lệch >= 50
```

### Environment Variables

```bash
# ZooKeeper Connection
export ZOOKEEPER_CONNECTION_STRING=zk1:2181,zk2:2181,zk3:2181
export ZOOKEEPER_SESSION_TIMEOUT=60000
export ZOOKEEPER_CONNECTION_TIMEOUT=15000
export ZOOKEEPER_BASE_PATH=/hddt/sequences
```

## Cách Sử Dụng

### 1. Cài Đặt ZooKeeper

**Docker:**
```bash
docker run -d --name zookeeper \
  -p 2181:2181 \
  -e ALLOW_ANONYMOUS_LOGIN=yes \
  bitnami/zookeeper:latest
```

**Manual:**
```bash
# Download ZooKeeper
wget https://downloads.apache.org/zookeeper/zookeeper-3.9.1/apache-zookeeper-3.9.1-bin.tar.gz
tar -xzf apache-zookeeper-3.9.1-bin.tar.gz

# Start ZooKeeper
cd apache-zookeeper-3.9.1-bin
./bin/zkServer.sh start
```

### 2. Test API

**Generate ORDER_ID:**
```bash
curl -X POST http://localhost:8081/api/sequences/generate/ORDER_ID
```

**Get Current Value:**
```bash
curl -X GET http://localhost:8081/api/sequences/ORDER_ID
```

**Reset to 1000:**
```bash
curl -X PUT http://localhost:8081/api/sequences/reset/ORDER_ID/1000
```

**List All:**
```bash
curl -X GET http://localhost:8081/api/sequences
```

### 3. Java Client Example

```java
@Service
public class OrderService {
    
    @Autowired
    private SequenceGeneratorService sequenceGenerator;
    
    public String createOrder() {
        // Generate ORDER_ID
        long orderId = sequenceGenerator.generateNext("ORDER_ID");
        
        // Tạo order với ID duy nhất
        Order order = new Order();
        order.setId("ORD-" + String.format("%08d", orderId));
        
        return order.getId(); // ORD-00000101
    }
    
    public String createInvoice() {
        // Generate INVOICE_ID
        long invoiceId = sequenceGenerator.generateNext("INVOICE_ID");
        
        return "INV-" + String.format("%06d", invoiceId); // INV-000055
    }
}
```

## Performance

### Benchmark Results

- **Local Generate**: ~100,000 ops/sec
- **With ZooKeeper Sync**: ~10,000 ops/sec
- **Memory Usage**: ~50MB for 10,000 keys
- **ZooKeeper Storage**: ~1KB per key

### Optimization

1. **Batch Sync**: Sync mỗi 10-50 lần thay vì mỗi lần
2. **Local Cache**: Sử dụng AtomicLong để giảm ZooKeeper calls
3. **Connection Pool**: Reuse ZooKeeper connections
4. **Async Sync**: Đồng bộ không đồng bộ để không block

## Error Handling

### Common Errors

**1. ZooKeeper Connection Failed:**
```json
{
  "keyName": "ORDER_ID",
  "status": "ERROR",
  "message": "Cannot connect to ZooKeeper: Connection refused"
}
```

**2. Invalid Key Name:**
```json
{
  "keyName": "",
  "status": "ERROR",
  "message": "KeyName cannot be null or empty"
}
```

**3. Reset Value Invalid:**
```json
{
  "keyName": "ORDER_ID",
  "status": "ERROR",
  "message": "Reset value is required"
}
```

### Retry Logic

- **Connection Retry**: 3 lần với exponential backoff
- **Operation Retry**: Tự động retry khi ZooKeeper timeout
- **Fallback**: Sử dụng local cache khi ZooKeeper unavailable

## Monitoring

### Metrics

- `sequence.generate.count` - Số lần generate
- `sequence.sync.count` - Số lần sync với ZooKeeper
- `sequence.error.count` - Số lỗi xảy ra
- `zookeeper.connection.status` - Trạng thái kết nối

### Logs

```
2026-01-15 11:30:00 [main] INFO  [trace-123] SequenceGeneratorService - ZooKeeper initialized: localhost:2181
2026-01-15 11:30:01 [http-1] INFO  [trace-124] SequenceGeneratorService - Generated sequence for key 'ORDER_ID': 101
2026-01-15 11:30:02 [http-2] DEBUG [trace-125] SequenceGeneratorService - Synced sequence for key 'ORDER_ID' with ZooKeeper: 110
```

## Security

### Authentication

API này không yêu cầu authentication mặc định. Để bật security:

```yaml
security:
  jwt:
    enabled: true
```

### Authorization

Có thể thêm role-based access:

```java
@PreAuthorize("hasRole('ADMIN')")
@PutMapping("/reset")
public ResponseEntity<SequenceResponse> resetSequence(...)
```

### Network Security

- Sử dụng ZooKeeper với authentication
- Encrypt ZooKeeper communication
- Firewall rules cho ZooKeeper ports

## Best Practices

### 1. Key Naming Convention

```
ORDER_ID        ✓ Good
INVOICE_ID      ✓ Good
USER_ID         ✓ Good

order-id        ✗ Avoid hyphens
OrderId         ✗ Avoid camelCase
order_id_2024   ✗ Avoid dates in key
```

### 2. Reset Strategy

```java
// ✓ Reset to reasonable value
sequenceGenerator.resetSequence("ORDER_ID", 10000);

// ✗ Don't reset to 0 in production
sequenceGenerator.resetSequence("ORDER_ID", 0);
```

### 3. Error Handling

```java
try {
    long id = sequenceGenerator.generateNext("ORDER_ID");
    return "ORD-" + String.format("%08d", id);
} catch (Exception e) {
    // Fallback to UUID or timestamp
    return "ORD-" + UUID.randomUUID().toString().substring(0, 8);
}
```

### 4. Monitoring

```java
@EventListener
public void onSequenceGenerated(SequenceGeneratedEvent event) {
    meterRegistry.counter("sequence.generate", "key", event.getKeyName()).increment();
}
```

## Troubleshooting

### 1. ZooKeeper Connection Issues

**Problem:** `Connection refused`

**Solution:**
```bash
# Check ZooKeeper status
./bin/zkServer.sh status

# Check network connectivity
telnet localhost 2181

# Check logs
tail -f logs/zookeeper.out
```

### 2. Performance Issues

**Problem:** Slow sequence generation

**Solution:**
- Tăng `sync-interval` trong config
- Giảm `sync-threshold`
- Sử dụng ZooKeeper cluster
- Optimize network latency

### 3. Memory Leaks

**Problem:** Memory usage tăng liên tục

**Solution:**
- Cleanup unused keys định kỳ
- Set TTL cho sequences
- Monitor cache size

## Migration

### From Database Sequences

```sql
-- Export current sequences
SELECT sequence_name, current_value FROM sequences;

-- Import to ZooKeeper via API
curl -X PUT http://localhost:8081/api/sequences/reset \
  -H "Content-Type: application/json" \
  -d '{"keyName": "ORDER_ID", "resetValue": 12345}'
```

### From Redis Counters

```bash
# Export from Redis
redis-cli --scan --pattern "counter:*" | while read key; do
  value=$(redis-cli get $key)
  keyname=$(echo $key | sed 's/counter://')
  curl -X PUT http://localhost:8081/api/sequences/reset \
    -H "Content-Type: application/json" \
    -d "{\"keyName\": \"$keyname\", \"resetValue\": $value}"
done
```

---

**Tác giả:** HDDT Development Team  
**Phiên bản:** 1.0.0  
**Cập nhật:** 2026-01-15