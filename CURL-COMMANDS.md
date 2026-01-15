# CURL Commands - Common Service API Testing

## 🚀 Test Endpoints (Public - Không cần authentication)

### 1. Hello Endpoint

```bash
curl -X GET http://localhost:8081/api/test/hello
```

**Expected Response:**
```json
{
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "data": {
    "message": "Common Service is running!",
    "timestamp": "2026-01-14T17:30:00",
    "status": "OK"
  },
  "errorCode": null,
  "message": "SUCCESS"
}
```

### 2. Health Check

```bash
curl -X GET http://localhost:8081/api/test/health
```

**Expected Response:**
```json
{
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "data": "Service is healthy!",
  "errorCode": null,
  "message": "SUCCESS"
}
```

---

## 🏥 Actuator Endpoints

### 1. Health Check

```bash
curl -X GET http://localhost:8081/actuator/health
```

**Expected Response:**
```json
{
  "status": "UP"
}
```

### 2. Application Info

```bash
curl -X GET http://localhost:8081/actuator/info
```

---

## 🔐 JWT Authentication Testing

### Tạo JWT Token (Cần implement trong microservice thực tế)

Để test JWT, bạn cần tạo một AuthController. Đây là ví dụ:

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password123"
  }'
```

**Expected Response:**
```json
{
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 3600
  },
  "errorCode": null,
  "message": "SUCCESS"
}
```

### Test Protected Endpoint - Với Token

```bash
# Thay YOUR_JWT_TOKEN bằng token thực tế
curl -X GET http://localhost:8081/api/protected/resource \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Test Protected Endpoint - Không có Token (Sẽ trả về 401)

```bash
curl -X GET http://localhost:8081/api/protected/resource
```

---

## 📊 Test với Headers

### Test với Custom Headers

```bash
curl -X GET http://localhost:8081/api/test/hello \
  -H "X-Custom-Header: test-value" \
  -H "X-Request-ID: 12345"
```

### Test với Trace ID từ Client

```bash
curl -X GET http://localhost:8081/api/test/hello \
  -H "X-Trace-Id: my-custom-trace-id-123"
```

---

## 🧪 Test Error Handling

### Test Business Exception (Cần endpoint tương ứng)

```bash
curl -X GET http://localhost:8081/api/test/error
```

**Expected Response:**
```json
{
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "data": null,
  "errorCode": "BUSINESS_ERROR",
  "message": "This is a test error"
}
```

---

## 🔄 Test với PowerShell (Windows)

### Basic GET Request

```powershell
Invoke-WebRequest -Uri "http://localhost:8081/api/test/hello" -Method GET
```

### GET Request với Response Body

```powershell
$response = Invoke-RestMethod -Uri "http://localhost:8081/api/test/hello" -Method GET
$response | ConvertTo-Json -Depth 10
```

### POST Request với JSON Body

```powershell
$body = @{
    username = "admin"
    password = "password123"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" `
  -Method POST `
  -ContentType "application/json" `
  -Body $body
```

### Request với Authorization Header

```powershell
$token = "YOUR_JWT_TOKEN"
$headers = @{
    "Authorization" = "Bearer $token"
}

Invoke-RestMethod -Uri "http://localhost:8081/api/protected/resource" `
  -Method GET `
  -Headers $headers
```

---

## 📝 Test Response Format

Tất cả response đều có format chuẩn:

```json
{
  "traceId": "UUID",           // Trace ID tự động
  "data": {},                  // Dữ liệu response
  "errorCode": null,           // Mã lỗi (null nếu thành công)
  "message": "SUCCESS"         // Message
}
```

### Success Response

```json
{
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "data": {
    "id": 1,
    "name": "Product A"
  },
  "errorCode": null,
  "message": "SUCCESS"
}
```

### Error Response

```json
{
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "data": null,
  "errorCode": "PRODUCT_NOT_FOUND",
  "message": "Không tìm thấy sản phẩm"
}
```

---

## 🎯 Quick Test Script

### Bash Script (Linux/Mac)

```bash
#!/bin/bash

BASE_URL="http://localhost:8081"

echo "Testing Common Service..."
echo ""

echo "1. Testing Hello Endpoint..."
curl -s $BASE_URL/api/test/hello | jq '.'
echo ""

echo "2. Testing Health Endpoint..."
curl -s $BASE_URL/api/test/health | jq '.'
echo ""

echo "3. Testing Actuator Health..."
curl -s $BASE_URL/actuator/health | jq '.'
echo ""

echo "All tests completed!"
```

### PowerShell Script (Windows)

```powershell
# test-api.ps1
$baseUrl = "http://localhost:8081"

Write-Host "Testing Common Service..." -ForegroundColor Green
Write-Host ""

Write-Host "1. Testing Hello Endpoint..." -ForegroundColor Yellow
$response1 = Invoke-RestMethod -Uri "$baseUrl/api/test/hello"
$response1 | ConvertTo-Json -Depth 10
Write-Host ""

Write-Host "2. Testing Health Endpoint..." -ForegroundColor Yellow
$response2 = Invoke-RestMethod -Uri "$baseUrl/api/test/health"
$response2 | ConvertTo-Json -Depth 10
Write-Host ""

Write-Host "3. Testing Actuator Health..." -ForegroundColor Yellow
$response3 = Invoke-RestMethod -Uri "$baseUrl/actuator/health"
$response3 | ConvertTo-Json -Depth 10
Write-Host ""

Write-Host "All tests completed!" -ForegroundColor Green
```

**Chạy script:**
```powershell
.\test-api.ps1
```

---

## 🔍 Debug & Troubleshooting

### Check if Server is Running

```bash
curl -I http://localhost:8081/api/test/hello
```

### Verbose Output

```bash
curl -v http://localhost:8081/api/test/hello
```

### Save Response to File

```bash
curl -o response.json http://localhost:8081/api/test/hello
```

### Show Response Headers

```bash
curl -i http://localhost:8081/api/test/hello
```

### Test with Timeout

```bash
curl --max-time 5 http://localhost:8081/api/test/hello
```

---

## 📦 Import vào Postman

1. Mở Postman
2. Click **Import** button
3. Chọn file `postman/Common-Service-API.postman_collection.json`
4. Collection sẽ được import với tất cả các request đã cấu hình sẵn

---

## 🎨 Pretty Print JSON Response

### Với jq (Linux/Mac)

```bash
curl -s http://localhost:8081/api/test/hello | jq '.'
```

### Với Python

```bash
curl -s http://localhost:8081/api/test/hello | python -m json.tool
```

### Với PowerShell

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/test/hello" | ConvertTo-Json -Depth 10
```

---

## ⚡ Performance Testing

### Apache Bench

```bash
ab -n 1000 -c 10 http://localhost:8081/api/test/hello
```

### wrk (Load Testing)

```bash
wrk -t4 -c100 -d30s http://localhost:8081/api/test/hello
```

---

## 📌 Notes

- Tất cả endpoints `/api/test/**` và `/actuator/**` đều public (không cần authentication)
- Các endpoints khác sẽ yêu cầu JWT token trong header `Authorization: Bearer <token>`
- Mỗi response đều có `traceId` để tracking
- Response format chuẩn: `BaseResponse<T>`
