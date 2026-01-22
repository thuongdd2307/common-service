# CURL Commands - Sequence Generator API

## 🚀 Test Commands cho Sequence Generator API

### 1. Health Check

```bash
# Kiểm tra trạng thái ZooKeeper connection
curl -X GET http://localhost:8081/api/sequences/health
```

### 2. Generate Sequence Numbers

#### Generate với Request Body
```bash
# Generate ORDER_ID
curl -X POST http://localhost:8081/api/sequences/generate \
  -H "Content-Type: application/json" \
  -d '{"keyName": "ORDER_ID"}'

# Generate INVOICE_ID
curl -X POST http://localhost:8081/api/sequences/generate \
  -H "Content-Type: application/json" \
  -d '{"keyName": "INVOICE_ID"}'

# Generate USER_ID
curl -X POST http://localhost:8081/api/sequences/generate \
  -H "Content-Type: application/json" \
  -d '{"keyName": "USER_ID"}'
```

#### Generate với URL Parameter
```bash
# Generate ORDER_ID (cách 2)
curl -X POST http://localhost:8081/api/sequences/generate/ORDER_ID

# Generate INVOICE_ID (cách 2)
curl -X POST http://localhost:8081/api/sequences/generate/INVOICE_ID

# Generate PRODUCT_ID
curl -X POST http://localhost:8081/api/sequences/generate/PRODUCT_ID

# Generate CUSTOMER_ID
curl -X POST http://localhost:8081/api/sequences/generate/CUSTOMER_ID
```

### 3. Get Current Values

```bash
# Lấy giá trị hiện tại của ORDER_ID
curl -X GET http://localhost:8081/api/sequences/ORDER_ID

# Lấy giá trị hiện tại của INVOICE_ID
curl -X GET http://localhost:8081/api/sequences/INVOICE_ID

# Lấy giá trị hiện tại của USER_ID
curl -X GET http://localhost:8081/api/sequences/USER_ID
```

### 4. Reset Sequences

#### Reset với Request Body
```bash
# Reset ORDER_ID về 1000
curl -X PUT http://localhost:8081/api/sequences/reset \
  -H "Content-Type: application/json" \
  -d '{"keyName": "ORDER_ID", "resetValue": 1000}'

# Reset INVOICE_ID về 5000
curl -X PUT http://localhost:8081/api/sequences/reset \
  -H "Content-Type: application/json" \
  -d '{"keyName": "INVOICE_ID", "resetValue": 5000}'

# Reset USER_ID về 10000
curl -X PUT http://localhost:8081/api/sequences/reset \
  -H "Content-Type: application/json" \
  -d '{"keyName": "USER_ID", "resetValue": 10000}'
```

#### Reset với URL Parameter
```bash
# Reset ORDER_ID về 2000 (cách 2)
curl -X PUT http://localhost:8081/api/sequences/reset/ORDER_ID/2000

# Reset INVOICE_ID về 6000 (cách 2)
curl -X PUT http://localhost:8081/api/sequences/reset/INVOICE_ID/6000

# Reset PRODUCT_ID về 100
curl -X PUT http://localhost:8081/api/sequences/reset/PRODUCT_ID/100
```

### 5. List All Sequences

```bash
# Lấy danh sách tất cả sequences
curl -X GET http://localhost:8081/api/sequences
```

### 6. Delete Sequences

```bash
# Xóa ORDER_ID sequence
curl -X DELETE http://localhost:8081/api/sequences/ORDER_ID

# Xóa INVOICE_ID sequence
curl -X DELETE http://localhost:8081/api/sequences/INVOICE_ID

# Xóa USER_ID sequence
curl -X DELETE http://localhost:8081/api/sequences/USER_ID
```

## 🧪 Test Scenarios

### Scenario 1: Tạo và Test Basic Flow

```bash
# 1. Check health
curl -X GET http://localhost:8081/api/sequences/health

# 2. Generate first ORDER_ID
curl -X POST http://localhost:8081/api/sequences/generate/ORDER_ID

# 3. Generate more ORDER_IDs
curl -X POST http://localhost:8081/api/sequences/generate/ORDER_ID
curl -X POST http://localhost:8081/api/sequences/generate/ORDER_ID
curl -X POST http://localhost:8081/api/sequences/generate/ORDER_ID

# 4. Check current value
curl -X GET http://localhost:8081/api/sequences/ORDER_ID

# 5. List all sequences
curl -X GET http://localhost:8081/api/sequences
```

### Scenario 2: Multiple Keys Test

```bash
# Tạo nhiều loại sequence
curl -X POST http://localhost:8081/api/sequences/generate/ORDER_ID
curl -X POST http://localhost:8081/api/sequences/generate/INVOICE_ID
curl -X POST http://localhost:8081/api/sequences/generate/USER_ID
curl -X POST http://localhost:8081/api/sequences/generate/PRODUCT_ID
curl -X POST http://localhost:8081/api/sequences/generate/CUSTOMER_ID

# Generate thêm cho mỗi key
for i in {1..5}; do
  curl -X POST http://localhost:8081/api/sequences/generate/ORDER_ID
  curl -X POST http://localhost:8081/api/sequences/generate/INVOICE_ID
  curl -X POST http://localhost:8081/api/sequences/generate/USER_ID
done

# Check tất cả
curl -X GET http://localhost:8081/api/sequences
```

### Scenario 3: Reset và Recovery Test

```bash
# 1. Generate một số sequences
curl -X POST http://localhost:8081/api/sequences/generate/TEST_ID
curl -X POST http://localhost:8081/api/sequences/generate/TEST_ID
curl -X POST http://localhost:8081/api/sequences/generate/TEST_ID

# 2. Check current value
curl -X GET http://localhost:8081/api/sequences/TEST_ID

# 3. Reset về giá trị cao hơn
curl -X PUT http://localhost:8081/api/sequences/reset/TEST_ID/1000

# 4. Generate tiếp
curl -X POST http://localhost:8081/api/sequences/generate/TEST_ID
curl -X POST http://localhost:8081/api/sequences/generate/TEST_ID

# 5. Verify
curl -X GET http://localhost:8081/api/sequences/TEST_ID
```

### Scenario 4: Performance Test

```bash
# Generate 100 sequences nhanh
for i in {1..100}; do
  curl -s -X POST http://localhost:8081/api/sequences/generate/PERF_TEST > /dev/null
  echo "Generated $i"
done

# Check final value
curl -X GET http://localhost:8081/api/sequences/PERF_TEST
```

### Scenario 5: Error Handling Test

```bash
# Test với key name rỗng (sẽ lỗi)
curl -X POST http://localhost:8081/api/sequences/generate \
  -H "Content-Type: application/json" \
  -d '{"keyName": ""}'

# Test reset không có resetValue (sẽ lỗi)
curl -X PUT http://localhost:8081/api/sequences/reset \
  -H "Content-Type: application/json" \
  -d '{"keyName": "TEST_ID"}'

# Test get key không tồn tại
curl -X GET http://localhost:8081/api/sequences/NON_EXISTENT_KEY
```

## 📊 Expected Responses

### Successful Generate Response
```json
{
  "keyName": "ORDER_ID",
  "currentValue": 0,
  "nextValue": 1,
  "status": "SUCCESS",
  "message": "Sequence generated successfully"
}
```

### Current Value Response
```json
{
  "keyName": "ORDER_ID",
  "currentValue": 5,
  "nextValue": null,
  "status": "SUCCESS",
  "message": "Current value retrieved"
}
```

### Reset Response
```json
{
  "keyName": "ORDER_ID",
  "currentValue": 1000,
  "nextValue": null,
  "status": "SUCCESS",
  "message": "Sequence reset successfully"
}
```

### List All Response
```json
{
  "keys": ["ORDER_ID", "INVOICE_ID", "USER_ID"],
  "sequences": {
    "ORDER_ID": 5,
    "INVOICE_ID": 3,
    "USER_ID": 10
  },
  "totalCount": 3,
  "status": "SUCCESS",
  "message": "Sequences retrieved successfully"
}
```

### Health Check Response (Healthy)
```json
{
  "status": "UP",
  "zookeeper": "CONNECTED",
  "totalKeys": 3,
  "message": "ZooKeeper sequence generator is healthy"
}
```

### Error Response
```json
{
  "keyName": "ORDER_ID",
  "currentValue": null,
  "nextValue": null,
  "status": "ERROR",
  "message": "KeyName cannot be null or empty"
}
```

## 🔧 Troubleshooting Commands

### Check ZooKeeper Connection
```bash
# Test ZooKeeper directly
echo ruok | nc localhost 2181

# Check ZooKeeper status
docker exec zookeeper zkServer.sh status
```

### Monitor Logs
```bash
# Follow application logs
tail -f logs/common-service.log

# Filter sequence-related logs
tail -f logs/common-service.log | grep -i sequence
```

### Performance Monitoring
```bash
# Monitor response times
time curl -X POST http://localhost:8081/api/sequences/generate/PERF_TEST

# Batch performance test
time for i in {1..100}; do
  curl -s -X POST http://localhost:8081/api/sequences/generate/BATCH_TEST > /dev/null
done
```

## 🚀 Quick Start Script

```bash
#!/bin/bash

echo "=== Sequence Generator API Test ==="

# 1. Health check
echo "1. Health Check:"
curl -s -X GET http://localhost:8081/api/sequences/health | jq

# 2. Generate sequences
echo -e "\n2. Generate Sequences:"
curl -s -X POST http://localhost:8081/api/sequences/generate/ORDER_ID | jq
curl -s -X POST http://localhost:8081/api/sequences/generate/INVOICE_ID | jq
curl -s -X POST http://localhost:8081/api/sequences/generate/USER_ID | jq

# 3. Generate more
echo -e "\n3. Generate More:"
for i in {1..3}; do
  curl -s -X POST http://localhost:8081/api/sequences/generate/ORDER_ID | jq .nextValue
done

# 4. List all
echo -e "\n4. List All Sequences:"
curl -s -X GET http://localhost:8081/api/sequences | jq

# 5. Reset test
echo -e "\n5. Reset Test:"
curl -s -X PUT http://localhost:8081/api/sequences/reset/ORDER_ID/1000 | jq

# 6. Generate after reset
echo -e "\n6. Generate After Reset:"
curl -s -X POST http://localhost:8081/api/sequences/generate/ORDER_ID | jq

echo -e "\n=== Test Completed ==="
```

Lưu script trên thành `test-sequence-api.sh` và chạy:
```bash
chmod +x test-sequence-api.sh
./test-sequence-api.sh
```