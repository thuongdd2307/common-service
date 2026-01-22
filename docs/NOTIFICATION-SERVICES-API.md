# Notification Services API - Email & SMS

## Tổng Quan

Hệ thống Notification Services cung cấp khả năng gửi email và SMS với đầy đủ tính năng enterprise:

- **Email Service**: Gửi email với HTML, attachments, templates
- **SMS Service**: Gửi SMS qua nhiều nhà cung cấp (Twilio, AWS SNS, Viettel)
- **Template Engine**: Hỗ trợ Thymeleaf templates
- **Async Processing**: Xử lý bất đồng bộ với thread pools
- **Retry Logic**: Tự động retry khi gặp lỗi
- **Multi-Provider**: Hỗ trợ nhiều nhà cung cấp SMS

## Kiến Trúc

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Client App    │───▶│ Notification    │───▶│   Providers     │
│                 │    │   Services      │    │                 │
│ - Send Email    │    │                 │    │ - SMTP Server   │
│ - Send SMS      │    │ - EmailService  │    │ - Twilio        │
│ - Templates     │    │ - SmsService    │    │ - AWS SNS       │
│ - Async Queue   │    │ - Templates     │    │ - Viettel SMS   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Email Service API

### 1. Gửi Email Đồng Bộ

#### POST `/api/notifications/email/send`

**Request Body:**
```json
{
  "to": ["user@example.com", "admin@example.com"],
  "cc": ["manager@example.com"],
  "bcc": ["audit@example.com"],
  "subject": "Chào mừng đến với HDDT",
  "textContent": "Nội dung text thuần",
  "htmlContent": "<h1>Nội dung HTML</h1><p>Chào mừng bạn!</p>",
  "templateName": "welcome",
  "templateVariables": {
    "userName": "Nguyễn Văn A",
    "userEmail": "user@example.com",
    "registrationDate": "2026-01-15",
    "customerId": "CUST001",
    "activationUrl": "https://hddt.com/activate?token=abc123"
  },
  "attachments": [
    {
      "fileName": "document.pdf",
      "contentType": "application/pdf",
      "content": "base64-encoded-content",
      "inline": false
    }
  ],
  "priority": "HIGH",
  "replyTo": "noreply@hddt.com",
  "fromName": "HDDT System",
  "trackOpening": true,
  "trackClicking": true,
  "category": "welcome",
  "customHeaders": {
    "X-Campaign-ID": "WELCOME-2026"
  }
}
```

**Response:**
```json
{
  "messageId": "550e8400-e29b-41d4-a716-446655440000@hddt.com",
  "status": "SUCCESS",
  "message": "Email sent successfully",
  "sentAt": "2026-01-15T10:30:00",
  "recipients": ["user@example.com", "admin@example.com"],
  "subject": "Chào mừng đến với HDDT",
  "retryCount": 0,
  "processingTimeMs": 1250
}
```

### 2. Gửi Email Bất Đồng Bộ

#### POST `/api/notifications/email/send-async`

**Response:**
```json
{
  "status": "QUEUED",
  "message": "Email queued for async processing",
  "recipients": ["user@example.com"],
  "subject": "Chào mừng đến với HDDT"
}
```

### 3. Gửi Email Đơn Giản

#### POST `/api/notifications/email/send-simple`

**Parameters:**
- `to`: Email người nhận
- `subject`: Tiêu đề
- `message`: Nội dung
- `type`: `text` hoặc `html` (optional)

```bash
curl -X POST "http://localhost:8081/api/notifications/email/send-simple" \
  -d "to=user@example.com" \
  -d "subject=Test Email" \
  -d "message=Hello World" \
  -d "type=text"
```

### 4. Test Email Template

#### POST `/api/notifications/email/test-template`

**Parameters:**
- `to`: Email người nhận
- `templateName`: Tên template

**Request Body (Variables):**
```json
{
  "userName": "Test User",
  "otpCode": "123456",
  "validityMinutes": 5,
  "createdAt": "2026-01-15 10:30:00"
}
```

## SMS Service API

### 1. Gửi SMS Đồng Bộ

#### POST `/api/notifications/sms/send`

**Request Body:**
```json
{
  "phoneNumbers": ["+84901234567", "+84987654321"],
  "message": "Chào mừng bạn đến với HDDT!",
  "templateName": "welcome",
  "templateVariables": {
    "userName": "Nguyễn Văn A",
    "customerId": "CUST001"
  },
  "priority": "HIGH",
  "type": "TRANSACTIONAL",
  "scheduledAt": "2026-01-15T15:00:00",
  "senderId": "HDDT",
  "unicode": true,
  "validityPeriod": 60,
  "category": "welcome",
  "customData": {
    "campaignId": "WELCOME-2026"
  },
  "deliveryReport": true
}
```

**Response:**
```json
{
  "messageId": "SMS-A1B2C3D4",
  "batchId": "BATCH-E5F6G7H8",
  "status": "SUCCESS",
  "message": "SMS sent successfully",
  "sentAt": "2026-01-15T10:30:00",
  "phoneNumbers": ["+84901234567", "+84987654321"],
  "content": "Chào mừng bạn đến với HDDT!",
  "totalSms": 2,
  "cost": 0.05,
  "currency": "USD",
  "retryCount": 0,
  "processingTimeMs": 850
}
```

### 2. Gửi SMS OTP

#### POST `/api/notifications/sms/send-otp`

**Parameters:**
- `phoneNumber`: Số điện thoại
- `otpCode`: Mã OTP
- `validityMinutes`: Thời gian hiệu lực (phút)

```bash
curl -X POST "http://localhost:8081/api/notifications/sms/send-otp" \
  -d "phoneNumber=+84901234567" \
  -d "otpCode=123456" \
  -d "validityMinutes=5"
```

### 3. Tính Toán Số Phần SMS

#### POST `/api/notifications/sms/calculate-parts`

**Parameters:**
- `message`: Nội dung SMS
- `unicode`: `true` hoặc `false`

**Response:**
```json
{
  "message": "Đây là tin nhắn dài có thể chia thành nhiều phần...",
  "messageLength": 180,
  "unicode": true,
  "parts": 3,
  "maxLengthPerPart": 67
}
```

## Configuration

### Email Configuration

```yaml
# Spring Mail
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password
    from-name: HDDT System
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

# Notification Settings
notification:
  email:
    enabled: true
    async: true
    retry:
      max-attempts: 3
```

### SMS Configuration

```yaml
notification:
  sms:
    enabled: true
    provider: twilio  # twilio, aws, viettel, mock
    async: true
    retry:
      max-attempts: 3
    
    # Twilio Settings
    twilio:
      account-sid: ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
      auth-token: your-auth-token
      from-number: +1234567890
    
    # AWS SNS Settings
    aws:
      access-key: AKIAIOSFODNN7EXAMPLE
      secret-key: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
      region: us-east-1
    
    # Viettel SMS Settings
    viettel:
      username: your-username
      password: your-password
      cp-code: your-cp-code
      service-id: your-service-id
```

### Environment Variables

```bash
# Email Settings
export MAIL_HOST=smtp.gmail.com
export MAIL_USERNAME=your-email@gmail.com
export MAIL_PASSWORD=your-app-password

# SMS Settings
export SMS_PROVIDER=twilio
export TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
export TWILIO_AUTH_TOKEN=your-auth-token
export TWILIO_FROM_NUMBER=+1234567890

# Viettel SMS
export VIETTEL_SMS_USERNAME=your-username
export VIETTEL_SMS_PASSWORD=your-password
```

## Templates

### Email Templates (Thymeleaf)

**Location:** `src/main/resources/templates/email/`

**welcome.html:**
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Chào mừng</title>
</head>
<body>
    <h1>Chào mừng <span th:text="${userName}">User</span>!</h1>
    <p>Email: <span th:text="${userEmail}">user@example.com</span></p>
    <a th:href="${activationUrl}">Kích hoạt tài khoản</a>
</body>
</html>
```

**otp.html:**
```html
<div class="otp-code" th:text="${otpCode}">123456</div>
<p>Hiệu lực: <span th:text="${validityMinutes}">5</span> phút</p>
```

### SMS Templates (Text)

**Location:** `src/main/resources/templates/sms/`

**otp.txt:**
```
Ma OTP cua ban la: [(${otpCode})]. Ma co hieu luc trong [(${validityMinutes})] phut. - HDDT
```

**welcome.txt:**
```
Chao mung [(${userName})] den voi HDDT! Ma khach hang: [(${customerId})]. - HDDT
```

## Health Checks

### Email Health Check

#### GET `/api/notifications/email/health`

**Response:**
```json
{
  "status": "UP",
  "service": "email",
  "message": "Email service is healthy",
  "timestamp": "2026-01-15T10:30:00"
}
```

### SMS Health Check

#### GET `/api/notifications/sms/health`

**Response:**
```json
{
  "status": "UP",
  "service": "sms",
  "message": "SMS service is healthy",
  "timestamp": "2026-01-15T10:30:00"
}
```

## Error Handling

### Common Error Responses

**Invalid Request:**
```json
{
  "status": "FAILED",
  "message": "Invalid email request",
  "errorDetails": "Recipients list is required"
}
```

**Service Unavailable:**
```json
{
  "status": "FAILED",
  "message": "Failed to send email",
  "errorDetails": "SMTP server connection timeout"
}
```

**Retry Exhausted:**
```json
{
  "status": "FAILED",
  "message": "Max retry attempts reached",
  "retryCount": 3,
  "errorDetails": "Connection refused"
}
```

## Best Practices

### 1. Email Best Practices

```java
// ✓ Sử dụng templates
EmailRequest request = EmailRequest.builder()
    .to(List.of("user@example.com"))
    .subject("Chào mừng")
    .templateName("welcome")
    .templateVariables(Map.of("userName", "John"))
    .build();

// ✓ Async cho bulk emails
emailService.sendEmailAsync(request);

// ✓ Retry cho emails quan trọng
emailService.sendEmailWithRetry(request);
```

### 2. SMS Best Practices

```java
// ✓ Validate phone numbers
if (!smsService.validateSmsRequest(request)) {
    throw new IllegalArgumentException("Invalid SMS request");
}

// ✓ Calculate parts for long messages
int parts = smsService.calculateSmsParts(message, true);
if (parts > 3) {
    // Consider splitting or shortening message
}

// ✓ Use appropriate SMS type
SmsRequest request = SmsRequest.builder()
    .phoneNumbers(List.of("+84901234567"))
    .message("Your OTP: 123456")
    .type(SmsRequest.SmsType.TRANSACTIONAL)
    .priority(SmsRequest.SmsPriority.HIGH)
    .build();
```

### 3. Template Best Practices

```html
<!-- ✓ Responsive email template -->
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  @media only screen and (max-width: 600px) {
    .container { width: 100% !important; }
  }
</style>

<!-- ✓ Fallback for variables -->
<span th:text="${userName ?: 'Valued Customer'}">User</span>

<!-- ✓ Safe URLs -->
<a th:href="${activationUrl}" th:if="${activationUrl}">Activate</a>
```

## Monitoring & Metrics

### Key Metrics

- **Email Success Rate**: Tỷ lệ email gửi thành công
- **SMS Delivery Rate**: Tỷ lệ SMS được gửi
- **Processing Time**: Thời gian xử lý trung bình
- **Queue Size**: Số lượng messages đang chờ
- **Error Rate**: Tỷ lệ lỗi theo loại

### Logging

```java
// Email logs
log.info("Email sent successfully. MessageId: {}, Recipients: {}", messageId, recipients);
log.error("Failed to send email to {}: {}", recipients, error);

// SMS logs  
log.info("SMS sent via {}. MessageId: {}, Phone: {}", provider, messageId, phoneNumber);
log.warn("SMS send attempt {} failed: {}", attempt, error);
```

## Security

### 1. Email Security

- **SMTP Authentication**: Luôn sử dụng authenticated SMTP
- **TLS/SSL**: Bật STARTTLS cho SMTP connections
- **Rate Limiting**: Giới hạn số email per minute/hour
- **Content Filtering**: Validate và sanitize email content

### 2. SMS Security

- **API Keys**: Bảo mật API keys của SMS providers
- **Phone Validation**: Validate định dạng số điện thoại
- **Rate Limiting**: Giới hạn SMS per phone number
- **Content Filtering**: Kiểm tra nội dung SMS

### 3. Template Security

- **XSS Prevention**: Escape user input trong templates
- **Path Traversal**: Validate template names
- **Access Control**: Kiểm soát quyền truy cập templates

## Troubleshooting

### Email Issues

**Problem:** "Authentication failed"
```yaml
# Solution: Check SMTP credentials
spring:
  mail:
    username: correct-email@gmail.com
    password: correct-app-password  # Not account password!
```

**Problem:** "Connection timeout"
```yaml
# Solution: Adjust timeouts
spring:
  mail:
    properties:
      mail:
        smtp:
          connectiontimeout: 10000
          timeout: 10000
```

### SMS Issues

**Problem:** "Invalid phone number"
```java
// Solution: Validate format
if (!phoneNumber.matches("^\\+?[1-9]\\d{1,14}$")) {
    throw new IllegalArgumentException("Invalid phone format");
}
```

**Problem:** "Provider authentication failed"
```bash
# Solution: Check provider credentials
export TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
export TWILIO_AUTH_TOKEN=your-correct-token
```

---

**Tác giả:** HDDT Development Team  
**Phiên bản:** 1.0.0  
**Cập nhật:** 2026-01-22