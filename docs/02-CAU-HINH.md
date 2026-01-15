# Cấu Hình - Common Service

## File Cấu Hình Mẫu

File `application.yml` mẫu với đầy đủ các tùy chọn:

```yaml
spring:
  application:
    name: hddt-common-service

  main:
    web-application-type: reactive
    allow-bean-definition-overriding: true
    banner-mode: "off"

  jackson:
    serialization:
      write-dates-as-timestamps: false
    default-property-inclusion: non_null

# ===================== SECURITY =====================
security:
  jwt:
    enabled: true
    
    # Secret key phải >= 32 bytes (256 bits) theo RFC 7518
    secret: "THIS_IS_A_32_BYTE_SECRET_KEY_FOR_JWT_2026"
    
    issuer: common-service
    
    # Thời gian hết hạn (giây)
    expiration-seconds: 3600          # 1 giờ
    refresh-expiration-seconds: 86400 # 1 ngày
    
    header:
      authorization: Authorization
      prefix: Bearer
    
    claim:
      user-id: user_id
      username: username
      roles: role_codes

  # ===== Phân quyền dựa trên permission =====
  permission:
    enabled: true
    role-prefix: ROLE_
    permission-prefix: PERM_

# ===================== GATEWAY =====================
gateway:
  security:
    enabled: true
    
    # Headers được truyền xuống downstream services
    headers:
      trace-id: X-Trace-Id
      user-id: X-User-Id
      username: X-Username
      roles: X-Roles
    
    # Tùy chọn: Chặn routes tại gateway level
    rules:
      enabled: false
      definitions:
        - path: /admin/**
          roles: [ADMIN]
        - path: /internal/**
          roles: [SYSTEM]

# ===================== LOGGING =====================
logging:
  level:
    root: INFO
    com.example.commonserviceofficial: DEBUG
    org.springframework.security: INFO
    org.springframework.cloud.gateway: INFO
  
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId}]"
  
  file:
    name: logs/common-service.log

# ===================== COMMON LOGGING =====================
common:
  logging:
    enabled: true
    
    request:
      enabled: true
      include-headers: true
      include-body: false
      max-body-size: 2048
    
    response:
      enabled: true
      include-body: false
    
    trace:
      enabled: true
      header-name: X-Trace-Id

# ===================== MANAGEMENT =====================
management:
  endpoints:
    web:
      exposure:
        include: health,info
  
  endpoint:
    health:
      show-details: never
```

## Chi Tiết Các Cấu Hình

### 1. Security - JWT Configuration

#### `security.jwt.enabled`
- **Mặc định**: `true`
- **Mô tả**: Bật/tắt JWT authentication
- **Giá trị**: `true` | `false`

#### `security.jwt.secret`
- **Bắt buộc**: Có
- **Mô tả**: Secret key để ký JWT token
- **Yêu cầu**: Phải >= 32 bytes (256 bits) theo chuẩn RFC 7518
- **Ví dụ**: `"THIS_IS_A_32_BYTE_SECRET_KEY_FOR_JWT_2026"`

#### `security.jwt.issuer`
- **Mặc định**: Không có
- **Mô tả**: Issuer của JWT token (claim `iss`)
- **Ví dụ**: `"common-service"`

#### `security.jwt.expiration-seconds`
- **Mặc định**: `3600` (1 giờ)
- **Mô tả**: Thời gian hết hạn của access token (giây)

#### `security.jwt.refresh-expiration-seconds`
- **Mặc định**: `86400` (1 ngày)
- **Mô tả**: Thời gian hết hạn của refresh token (giây)

#### `security.jwt.header.authorization`
- **Mặc định**: `"Authorization"`
- **Mô tả**: Tên header chứa JWT token

#### `security.jwt.header.prefix`
- **Mặc định**: `"Bearer"`
- **Mô tả**: Prefix của token trong header

#### `security.jwt.claim.*`
- **Mô tả**: Tên các claim trong JWT token
- **user-id**: Claim chứa user ID
- **username**: Claim chứa username
- **roles**: Claim chứa danh sách roles

### 2. Gateway Configuration

#### `gateway.security.enabled`
- **Mặc định**: `false`
- **Mô tả**: Bật/tắt JWT Gateway Filter
- **Lưu ý**: Chỉ hoạt động khi có Spring Cloud Gateway trong classpath

#### `gateway.security.headers.*`
- **Mô tả**: Các headers được truyền xuống downstream services
- **trace-id**: Header chứa trace ID
- **user-id**: Header chứa user ID
- **username**: Header chứa username
- **roles**: Header chứa danh sách roles (phân cách bằng dấu phẩy)

#### `gateway.security.rules.enabled`
- **Mặc định**: `false`
- **Mô tả**: Bật/tắt kiểm tra authorization tại gateway level

#### `gateway.security.rules.definitions`
- **Mô tả**: Danh sách rules để kiểm tra authorization
- **Cấu trúc**:
  ```yaml
  - path: /admin/**      # Path pattern
    roles: [ADMIN]       # Danh sách roles được phép
  ```

### 3. Common Logging Configuration

#### `common.logging.enabled`
- **Mặc định**: `true`
- **Mô tả**: Bật/tắt toàn bộ logging features

#### `common.logging.request.enabled`
- **Mặc định**: `true`
- **Mô tả**: Bật/tắt logging request

#### `common.logging.request.include-headers`
- **Mặc định**: `true`
- **Mô tả**: Có log headers của request không

#### `common.logging.request.include-body`
- **Mặc định**: `false`
- **Mô tả**: Có log body của request không
- **Lưu ý**: Nên tắt trong production vì ảnh hưởng performance

#### `common.logging.request.max-body-size`
- **Mặc định**: `2048`
- **Mô tả**: Kích thước tối đa của body được log (bytes)

#### `common.logging.response.enabled`
- **Mặc định**: `true`
- **Mô tả**: Bật/tắt logging response

#### `common.logging.response.include-body`
- **Mặc định**: `false`
- **Mô tả**: Có log body của response không

#### `common.logging.trace.enabled`
- **Mặc định**: `true`
- **Mô tả**: Bật/tắt trace ID generation

#### `common.logging.trace.header-name`
- **Mặc định**: `"X-Trace-Id"`
- **Mô tả**: Tên header chứa trace ID

### 4. Logging Configuration

#### `logging.pattern.level`
- **Mô tả**: Pattern cho log level, bao gồm trace ID từ MDC
- **Ví dụ**: `"%5p [${spring.application.name:},%X{traceId}]"`

#### `logging.file.name`
- **Mô tả**: Đường dẫn file log
- **Ví dụ**: `"logs/common-service.log"`

## Các Tình Huống Cấu Hình

### Tình Huống 1: Service Thông Thường (Không phải Gateway)

```yaml
security:
  jwt:
    enabled: true
    secret: "YOUR_SECRET_KEY_HERE_32_BYTES_MIN"
    issuer: my-service

gateway:
  security:
    enabled: false  # Tắt gateway features

common:
  logging:
    enabled: true
```

### Tình Huống 2: API Gateway

```yaml
security:
  jwt:
    enabled: true
    secret: "YOUR_SECRET_KEY_HERE_32_BYTES_MIN"
    issuer: api-gateway

gateway:
  security:
    enabled: true  # Bật gateway features
    headers:
      trace-id: X-Trace-Id
      user-id: X-User-Id
      username: X-Username
      roles: X-Roles

common:
  logging:
    enabled: true
```

### Tình Huống 3: Development Environment

```yaml
security:
  jwt:
    enabled: true
    secret: "DEV_SECRET_KEY_32_BYTES_MINIMUM"
    expiration-seconds: 86400  # 1 ngày cho dev

common:
  logging:
    enabled: true
    request:
      include-headers: true
      include-body: true  # Bật để debug
    response:
      include-body: true  # Bật để debug

logging:
  level:
    com.example.commonserviceofficial: DEBUG
```

### Tình Huống 4: Production Environment

```yaml
security:
  jwt:
    enabled: true
    secret: "${JWT_SECRET}"  # Lấy từ environment variable
    expiration-seconds: 3600  # 1 giờ

common:
  logging:
    enabled: true
    request:
      include-body: false  # Tắt vì performance
    response:
      include-body: false  # Tắt vì performance

logging:
  level:
    root: WARN
    com.example.commonserviceofficial: INFO
```

## Environment Variables

Nên sử dụng environment variables cho các giá trị nhạy cảm:

```yaml
security:
  jwt:
    secret: ${JWT_SECRET:default-secret-for-dev}
    issuer: ${JWT_ISSUER:common-service}
```

Khi deploy:
```bash
export JWT_SECRET="your-production-secret-key-here"
export JWT_ISSUER="production-service"
```

## Conditional Configuration

Các bean sẽ được tạo dựa trên configuration:

| Bean | Điều kiện |
|------|-----------|
| `JwtTokenProvider` | `security.jwt.enabled=true` |
| `JwtAuthenticationFilter` | `security.jwt.enabled=true` |
| `JwtGatewayFilter` | `gateway.security.enabled=true` + Spring Cloud Gateway trong classpath |
| `TraceIdFilter` | `common.logging.trace.enabled=true` |
| `RequestLoggingFilter` | `common.logging.request.enabled=true` |
