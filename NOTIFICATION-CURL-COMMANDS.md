# CURL Commands - Notification Services API

## 🚀 Test Commands cho Email & SMS Services

### Email Service Tests

#### 1. Health Check

```bash
# Kiểm tra trạng thái Email service
curl -X GET http://localhost:8081/api/notifications/email/health
```

#### 2. Send Simple Email

```bash
# Gửi email text đơn giản
curl -X POST "http://localhost:8081/api/notifications/email/send-simple" \
  -d "to=test@example.com" \
  -d "subject=Test Email" \
  -d "message=Hello from HDDT System!" \
  -d "type=text"

# Gửi email HTML đơn giản
curl -X POST "http://localhost:8081/api/notifications/email/send-simple" \
  -d "to=test@example.com" \
  -d "subject=HTML Test Email" \
  -d "message=<h1>Hello</h1><p>This is <b>HTML</b> email!</p>" \
  -d "type=html"
```

#### 3. Send Full Email

```bash
# Gửi email đầy đủ tính năng
curl -X POST http://localhost:8081/api/notifications/email/send \
  -H "Content-Type: application/json" \
  -d '{
    "to": ["user@example.com", "admin@example.com"],
    "cc": ["manager@example.com"],
    "subject": "Welcome to HDDT System",
    "textContent": "Welcome to our system!",
    "htmlContent": "<h1>Welcome!</h1><p>Thank you for joining us.</p>",
    "priority": "HIGH",
    "fromName": "HDDT Support",
    "trackOpening": true,
    "category": "welcome"
  }'
```

#### 4. Send Email with Template

```bash
# Test welcome template
curl -X POST "http://localhost:8081/api/notifications/email/test-template" \
  -H "Content-Type: application/json" \
  -d "to=test@example.com&templateName=welcome" \
  --data-raw '{
    "userName": "Nguyễn Văn A",
    "userEmail": "test@example.com",
    "registrationDate": "2026-01-22",
    "customerId": "CUST001",
    "activationUrl": "https://hddt.com/activate?token=abc123"
  }'

# Test OTP template
curl -X POST "http://localhost:8081/api/notifications/email/test-template" \
  -H "Content-Type: application/json" \
  -d "to=test@example.com&templateName=otp" \
  --data-raw '{
    "userName": "Nguyễn Văn A",
    "otpCode": "123456",
    "validityMinutes": 5,
    "createdAt": "2026-01-22 10:30:00"
  }'
```

#### 5. Send Async Email

```bash
# Gửi email bất đồng bộ
curl -X POST http://localhost:8081/api/notifications/email/send-async \
  -H "Content-Type: application/json" \
  -d '{
    "to": ["user1@example.com", "user2@example.com"],
    "subject": "Bulk Email Test",
    "textContent": "This is a bulk email sent asynchronously.",
    "category": "bulk"
  }'
```

#### 6. Send Email with Retry

```bash
# Gửi email với retry logic
curl -X POST http://localhost:8081/api/notifications/email/send-with-retry \
  -H "Content-Type: application/json" \
  -d '{
    "to": ["important@example.com"],
    "subject": "Important Email with Retry",
    "htmlContent": "<h2>Important Notice</h2><p>This email will be retried if failed.</p>",
    "priority": "HIGH"
  }'
```

#### 7. Validate Email Request

```bash
# Validate email request
curl -X POST http://localhost:8081/api/notifications/email/validate \
  -H "Content-Type: application/json" \
  -d '{
    "to": ["valid@example.com"],
    "subject": "Test Subject",
    "textContent": "Test content"
  }'

# Test invalid email request
curl -X POST http://localhost:8081/api/notifications/email/validate \
  -H "Content-Type: application/json" \
  -d '{
    "to": [],
    "subject": "",
    "textContent": ""
  }'
```

### SMS Service Tests

#### 1. Health Check

```bash
# Kiểm tra trạng thái SMS service
curl -X GET http://localhost:8081/api/notifications/sms/health
```

#### 2. Send Simple SMS

```bash
# Gửi SMS đơn giản
curl -X POST "http://localhost:8081/api/notifications/sms/send-simple" \
  -d "phoneNumber=+84901234567" \
  -d "message=Hello from HDDT System!" \
  -d "type=TRANSACTIONAL"

# Gửi SMS promotional
curl -X POST "http://localhost:8081/api/notifications/sms/send-simple" \
  -d "phoneNumber=+84987654321" \
  -d "message=Special offer! 50% discount today only." \
  -d "type=PROMOTIONAL"
```

#### 3. Send OTP SMS

```bash
# Gửi SMS OTP
curl -X POST "http://localhost:8081/api/notifications/sms/send-otp" \
  -d "phoneNumber=+84901234567" \
  -d "otpCode=123456" \
  -d "validityMinutes=5"

# Gửi OTP với thời gian hiệu lực khác
curl -X POST "http://localhost:8081/api/notifications/sms/send-otp" \
  -d "phoneNumber=+84987654321" \
  -d "otpCode=789012" \
  -d "validityMinutes=10"
```

#### 4. Send Full SMS

```bash
# Gửi SMS đầy đủ tính năng
curl -X POST http://localhost:8081/api/notifications/sms/send \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumbers": ["+84901234567", "+84987654321"],
    "message": "Chào mừng bạn đến với hệ thống HDDT!",
    "priority": "HIGH",
    "type": "TRANSACTIONAL",
    "senderId": "HDDT",
    "unicode": true,
    "validityPeriod": 60,
    "category": "welcome",
    "deliveryReport": true
  }'
```

#### 5. Send SMS with Template

```bash
# Test welcome SMS template
curl -X POST "http://localhost:8081/api/notifications/sms/test-template" \
  -H "Content-Type: application/json" \
  -d "phoneNumber=+84901234567&templateName=welcome" \
  --data-raw '{
    "userName": "Nguyễn Văn A",
    "customerId": "CUST001"
  }'

# Test OTP SMS template
curl -X POST "http://localhost:8081/api/notifications/sms/test-template" \
  -H "Content-Type: application/json" \
  -d "phoneNumber=+84901234567&templateName=otp" \
  --data-raw '{
    "otpCode": "123456",
    "validityMinutes": 5
  }'
```

#### 6. Send Async SMS

```bash
# Gửi SMS bất đồng bộ
curl -X POST http://localhost:8081/api/notifications/sms/send-async \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumbers": ["+84901111111", "+84902222222", "+84903333333"],
    "message": "Bulk SMS notification sent asynchronously.",
    "type": "NOTIFICATION",
    "category": "bulk"
  }'
```

#### 7. Calculate SMS Parts

```bash
# Tính số phần SMS cho tin nhắn ngắn
curl -X POST "http://localhost:8081/api/notifications/sms/calculate-parts" \
  -d "message=Hello World" \
  -d "unicode=false"

# Tính số phần SMS cho tin nhắn dài
curl -X POST "http://localhost:8081/api/notifications/sms/calculate-parts" \
  -d "message=Đây là một tin nhắn rất dài có thể sẽ được chia thành nhiều phần khi gửi qua SMS. Chúng tôi cần kiểm tra xem tin nhắn này sẽ được chia thành bao nhiều phần." \
  -d "unicode=true"

# Tính SMS parts cho tin nhắn tiếng Anh dài
curl -X POST "http://localhost:8081/api/notifications/sms/calculate-parts" \
  -d "message=This is a very long message that might be split into multiple parts when sent via SMS. We need to check how many parts this message will be divided into when sending through the SMS gateway." \
  -d "unicode=false"
```

#### 8. Validate SMS Request

```bash
# Validate SMS request hợp lệ
curl -X POST http://localhost:8081/api/notifications/sms/validate \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumbers": ["+84901234567"],
    "message": "Valid SMS message"
  }'

# Test invalid SMS request
curl -X POST http://localhost:8081/api/notifications/sms/validate \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumbers": ["invalid-phone"],
    "message": ""
  }'
```

## 🧪 Test Scenarios

### Scenario 1: Welcome Flow

```bash
echo "=== Welcome Flow Test ==="

# 1. Send welcome email
echo "1. Sending welcome email..."
curl -s -X POST "http://localhost:8081/api/notifications/email/test-template" \
  -H "Content-Type: application/json" \
  -d "to=newuser@example.com&templateName=welcome" \
  --data-raw '{
    "userName": "New User",
    "userEmail": "newuser@example.com",
    "registrationDate": "2026-01-22",
    "customerId": "CUST001",
    "activationUrl": "https://hddt.com/activate?token=xyz789"
  }' | jq

# 2. Send welcome SMS
echo "2. Sending welcome SMS..."
curl -s -X POST "http://localhost:8081/api/notifications/sms/test-template" \
  -H "Content-Type: application/json" \
  -d "phoneNumber=+84901234567&templateName=welcome" \
  --data-raw '{
    "userName": "New User",
    "customerId": "CUST001"
  }' | jq
```

### Scenario 2: OTP Flow

```bash
echo "=== OTP Flow Test ==="

# 1. Send OTP email
echo "1. Sending OTP email..."
curl -s -X POST "http://localhost:8081/api/notifications/email/test-template" \
  -H "Content-Type: application/json" \
  -d "to=user@example.com&templateName=otp" \
  --data-raw '{
    "userName": "Test User",
    "otpCode": "567890",
    "validityMinutes": 5,
    "createdAt": "2026-01-22 15:30:00"
  }' | jq

# 2. Send OTP SMS
echo "2. Sending OTP SMS..."
curl -s -X POST "http://localhost:8081/api/notifications/sms/send-otp" \
  -d "phoneNumber=+84901234567" \
  -d "otpCode=567890" \
  -d "validityMinutes=5" | jq
```

### Scenario 3: Bulk Notifications

```bash
echo "=== Bulk Notifications Test ==="

# 1. Bulk email async
echo "1. Sending bulk emails..."
curl -s -X POST http://localhost:8081/api/notifications/email/send-async \
  -H "Content-Type: application/json" \
  -d '{
    "to": ["user1@example.com", "user2@example.com", "user3@example.com"],
    "subject": "System Maintenance Notice",
    "htmlContent": "<h2>Maintenance Notice</h2><p>System will be down for maintenance on Sunday.</p>",
    "category": "maintenance"
  }' | jq

# 2. Bulk SMS async
echo "2. Sending bulk SMS..."
curl -s -X POST http://localhost:8081/api/notifications/sms/send-async \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumbers": ["+84901111111", "+84902222222", "+84903333333"],
    "message": "System maintenance scheduled for Sunday 2AM-4AM. Service may be temporarily unavailable.",
    "type": "NOTIFICATION",
    "category": "maintenance"
  }' | jq
```

### Scenario 4: Error Handling

```bash
echo "=== Error Handling Test ==="

# 1. Invalid email
echo "1. Testing invalid email..."
curl -s -X POST http://localhost:8081/api/notifications/email/send \
  -H "Content-Type: application/json" \
  -d '{
    "to": [],
    "subject": "",
    "textContent": ""
  }' | jq

# 2. Invalid SMS
echo "2. Testing invalid SMS..."
curl -s -X POST http://localhost:8081/api/notifications/sms/send \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumbers": ["invalid-phone"],
    "message": ""
  }' | jq
```

### Scenario 5: Performance Test

```bash
echo "=== Performance Test ==="

# Send multiple emails quickly
echo "Sending 10 emails..."
for i in {1..10}; do
  curl -s -X POST "http://localhost:8081/api/notifications/email/send-simple" \
    -d "to=perf-test-$i@example.com" \
    -d "subject=Performance Test $i" \
    -d "message=This is performance test email number $i" \
    -d "type=text" > /dev/null &
done
wait
echo "All emails sent!"

# Send multiple SMS quickly
echo "Sending 10 SMS..."
for i in {1..10}; do
  curl -s -X POST "http://localhost:8081/api/notifications/sms/send-simple" \
    -d "phoneNumber=+8490123456$i" \
    -d "message=Performance test SMS number $i" \
    -d "type=TRANSACTIONAL" > /dev/null &
done
wait
echo "All SMS sent!"
```

## 📊 Expected Responses

### Successful Email Response
```json
{
  "messageId": "550e8400-e29b-41d4-a716-446655440000@hddt.com",
  "status": "SUCCESS",
  "message": "Email sent successfully",
  "sentAt": "2026-01-22T10:30:00",
  "recipients": ["user@example.com"],
  "subject": "Test Email",
  "retryCount": 0,
  "processingTimeMs": 1250
}
```

### Successful SMS Response
```json
{
  "messageId": "SMS-A1B2C3D4",
  "batchId": "BATCH-E5F6G7H8",
  "status": "SUCCESS",
  "message": "SMS sent successfully",
  "sentAt": "2026-01-22T10:30:00",
  "phoneNumbers": ["+84901234567"],
  "content": "Hello from HDDT!",
  "totalSms": 1,
  "retryCount": 0,
  "processingTimeMs": 850
}
```

### Health Check Response (Healthy)
```json
{
  "status": "UP",
  "service": "email",
  "message": "Email service is healthy",
  "timestamp": "2026-01-22T10:30:00"
}
```

### Error Response
```json
{
  "status": "FAILED",
  "message": "Invalid email request",
  "errorDetails": "Recipients list is required"
}
```

## 🔧 Quick Test Script

```bash
#!/bin/bash

echo "=== Notification Services API Test ==="

# Health checks
echo "1. Health Checks:"
echo "Email Health:"
curl -s -X GET http://localhost:8081/api/notifications/email/health | jq
echo "SMS Health:"
curl -s -X GET http://localhost:8081/api/notifications/sms/health | jq

# Simple tests
echo -e "\n2. Simple Tests:"
echo "Simple Email:"
curl -s -X POST "http://localhost:8081/api/notifications/email/send-simple" \
  -d "to=test@example.com" \
  -d "subject=API Test" \
  -d "message=Hello from API test!" \
  -d "type=text" | jq

echo "Simple SMS:"
curl -s -X POST "http://localhost:8081/api/notifications/sms/send-simple" \
  -d "phoneNumber=+84901234567" \
  -d "message=Hello from SMS API test!" \
  -d "type=TRANSACTIONAL" | jq

# Template tests
echo -e "\n3. Template Tests:"
echo "OTP Email:"
curl -s -X POST "http://localhost:8081/api/notifications/email/test-template" \
  -H "Content-Type: application/json" \
  -d "to=test@example.com&templateName=otp" \
  --data-raw '{
    "userName": "Test User",
    "otpCode": "123456",
    "validityMinutes": 5,
    "createdAt": "2026-01-22 10:30:00"
  }' | jq

echo "OTP SMS:"
curl -s -X POST "http://localhost:8081/api/notifications/sms/send-otp" \
  -d "phoneNumber=+84901234567" \
  -d "otpCode=123456" \
  -d "validityMinutes=5" | jq

echo -e "\n=== Test Completed ==="
```

Lưu script trên thành `test-notification-api.sh` và chạy:
```bash
chmod +x test-notification-api.sh
./test-notification-api.sh
```