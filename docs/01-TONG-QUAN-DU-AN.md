# Tổng Quan Dự Án - Common Service

## Giới Thiệu

**Common Service** là một thư viện Spring Boot tái sử dụng, cung cấp các chức năng chung cho các microservices trong hệ thống, bao gồm:

- **JWT Authentication & Authorization**: Xác thực và phân quyền dựa trên JWT token
- **Gateway Filter**: Bộ lọc cho Spring Cloud Gateway để xử lý JWT và truyền thông tin user
- **Logging & Tracing**: Ghi log request/response và trace ID cho distributed tracing
- **Exception Handling**: Xử lý exception tập trung với response format chuẩn
- **Security Annotations**: Các annotation để kiểm tra role và permission

## Thông Tin Dự Án

- **Group ID**: `com.hddt.common`
- **Artifact ID**: `common-service`
- **Version**: `1.0.0`
- **Java Version**: 17
- **Spring Boot Version**: 3.4.1
- **Packaging**: JAR (thư viện)

## Công Nghệ Sử Dụng

### Core Dependencies

- **Spring Boot Autoconfigure**: Tự động cấu hình các bean
- **Spring Security Core**: Bảo mật cơ bản
- **Spring WebFlux**: Reactive web framework
- **Spring Security Web & Config**: Cấu hình bảo mật web

### JWT & Security

- **JJWT (0.11.5)**: Thư viện xử lý JWT token
  - `jjwt-api`: API chính
  - `jjwt-impl`: Implementation
  - `jjwt-jackson`: JSON processing

### Logging

- **SLF4J**: Logging facade
- **Logstash Logback Encoder (7.4)**: Format log dạng JSON cho ELK stack

### Utilities

- **Spring AOP**: Aspect-Oriented Programming cho annotations
- **Jakarta Validation**: Validation API
- **Lombok**: Giảm boilerplate code

## Cấu Trúc Package

```
com.example.commonserviceofficial/
├── autoconfigure/          # Auto-configuration classes
│   ├── CommonAutoConfiguration
│   ├── GatewayAutoConfiguration
│   ├── LoggingAutoConfiguration
│   └── SecurityAutoConfiguration
├── contract/               # API contracts
│   ├── BaseResponse
│   └── HeaderConstant
├── exception/              # Exception handling
│   ├── BusinessException
│   └── GlobalExceptionAdvice
├── gateway/                # Gateway filters
│   └── JwtGatewayFilter
├── logging/                # Logging components
│   ├── filter/
│   │   └── RequestLoggingFilter
│   ├── util/
│   │   └── TraceIdUtil
│   └── TraceIdFilter
├── properties/             # Configuration properties
│   ├── JwtProperties
│   └── LoggingProperties
└── security/               # Security components
    ├── annotation/
    │   ├── HasPermission
    │   └── HasRole
    ├── jwt/
    │   └── JwtConstants
    ├── JwtAuthenticationFilter
    ├── JwtClaims
    ├── JwtTokenProvider
    └── RoleAuthorityMapper
```

## Tính Năng Chính

### 1. JWT Authentication
- Tạo và validate JWT token
- Hỗ trợ access token và refresh token
- Tự động parse claims từ token
- Tích hợp với Spring Security

### 2. Gateway Integration
- Filter JWT tại gateway layer
- Truyền thông tin user qua headers xuống downstream services
- Hỗ trợ Spring Cloud Gateway

### 3. Distributed Tracing
- Tự động tạo trace ID cho mỗi request
- Ghi trace ID vào MDC (Mapped Diagnostic Context)
- Truyền trace ID qua headers

### 4. Request/Response Logging
- Log thông tin request (method, URI, headers)
- Log response status và thời gian xử lý
- Có thể bật/tắt qua configuration

### 5. Exception Handling
- Xử lý BusinessException với error code
- Xử lý generic Exception
- Response format chuẩn với trace ID

### 6. Security Annotations
- `@HasRole`: Kiểm tra role của user
- `@HasPermission`: Kiểm tra permission của user

## Auto-Configuration

Thư viện sử dụng Spring Boot Auto-Configuration để tự động cấu hình các bean khi được thêm vào classpath. Các configuration được khai báo trong:

```
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

Bao gồm:
- `CommonAutoConfiguration`: Cấu hình chung
- `LoggingAutoConfiguration`: Cấu hình logging
- `SecurityAutoConfiguration`: Cấu hình security
- `GatewayAutoConfiguration`: Cấu hình gateway (conditional)

## Cách Sử Dụng

### 1. Thêm Dependency

```xml
<dependency>
    <groupId>com.hddt.common</groupId>
    <artifactId>common-service</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Cấu Hình application.yml

Xem chi tiết trong file `02-CAU-HINH.md`

### 3. Sử Dụng Trong Code

Xem chi tiết trong file `03-HUONG-DAN-SU-DUNG.md`

## Lưu Ý

- Thư viện này được thiết kế để sử dụng với **Spring WebFlux** (reactive)
- Một số dependency được đánh dấu `optional=true` để tránh conflict khi integrate
- Gateway features chỉ hoạt động khi có Spring Cloud Gateway trong classpath
- Tất cả các tính năng đều có thể bật/tắt qua configuration
