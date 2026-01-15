# Common Service - Thư Viện Chung Cho Microservices

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)]()
[![Version](https://img.shields.io/badge/version-1.0.0-blue)]()
[![Java](https://img.shields.io/badge/Java-17-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-green)]()

Thư viện Spring Boot tái sử dụng cung cấp các chức năng chung cho hệ sinh thái microservices, bao gồm JWT authentication, logging, tracing, và exception handling.

## ✨ Tính Năng

- 🔐 **JWT Authentication & Authorization**: Xác thực và phân quyền dựa trên JWT token
- 🌐 **Gateway Integration**: Tích hợp với Spring Cloud Gateway
- 📝 **Distributed Tracing**: Trace ID tự động cho mọi request
- 📊 **Request/Response Logging**: Ghi log chi tiết với trace ID
- ⚠️ **Exception Handling**: Xử lý exception tập trung với response format chuẩn
- 🎯 **Security Annotations**: `@HasRole`, `@HasPermission` để kiểm tra quyền
- 🚀 **Auto-Configuration**: Tự động cấu hình, chỉ cần thêm dependency

## 📦 Cài Đặt

### Maven

```xml
<dependency>
    <groupId>com.hddt.common</groupId>
    <artifactId>common-service</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Build Từ Source

```bash
git clone <repository-url>
cd common-service-official
mvn clean install
```

## 🚀 Quick Start

### 1. Thêm Dependency

Thêm dependency vào `pom.xml` của microservice

### 2. Cấu Hình

Tạo file `application.yml`:

```yaml
security:
  jwt:
    enabled: true
    secret: "YOUR_SECRET_KEY_MUST_BE_AT_LEAST_32_BYTES"
    issuer: your-service-name
    expiration-seconds: 3600

common:
  logging:
    enabled: true
```

### 3. Sử Dụng

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @GetMapping("/{id}")
    public BaseResponse<Product> getProduct(@PathVariable Long id) {
        Product product = productService.findById(id);
        return BaseResponse.ok(product);
    }
    
    @PostMapping
    @HasRole(role = "ADMIN")
    public BaseResponse<Product> createProduct(@RequestBody Product product) {
        Product saved = productService.save(product);
        return BaseResponse.ok(saved);
    }
}
```

## 📚 Tài Liệu

- [01 - Tổng Quan Dự Án](docs/01-TONG-QUAN-DU-AN.md)
- [02 - Cấu Hình](docs/02-CAU-HINH.md)
- [03 - Hướng Dẫn Sử Dụng](docs/03-HUONG-DAN-SU-DUNG.md)
- [04 - Tích Hợp Vào Microservice](docs/04-TICH-HOP-VAO-MICROSERVICE.md)
- [05 - Chuyên Sâu Technical](docs/05-CHUYEN-SAU-TECHNICAL.md)

## 🏗️ Kiến Trúc

```
common-service/
├── autoconfigure/      # Auto-configuration classes
├── contract/           # API contracts (BaseResponse, HeaderConstant)
├── exception/          # Exception handling
├── gateway/            # Gateway filters
├── logging/            # Logging & tracing
├── properties/         # Configuration properties
└── security/           # JWT & security components
```

## 🔑 Các Thành Phần Chính

### JWT Token Provider

```java
@Autowired
private JwtTokenProvider jwtTokenProvider;

String token = jwtTokenProvider.generateToken(username, roles);
boolean valid = jwtTokenProvider.validateToken(token);
JwtClaims claims = jwtTokenProvider.parseToken(token);
```

### Base Response

```java
// Success
return BaseResponse.ok(data);

// Error
return BaseResponse.error("ERROR_CODE", "Error message");
```

### Security Annotations

```java
@HasRole(role = "ADMIN")
public void adminOnly() { }

@HasPermission("CREATE_USER")
public void createUser() { }
```

### Trace ID

```java
String traceId = TraceIdUtil.getOrCreate();
```

## 🔧 Cấu Hình Nâng Cao

### API Gateway

```yaml
gateway:
  security:
    enabled: true
    headers:
      trace-id: X-Trace-Id
      user-id: X-User-Id
      username: X-Username
      roles: X-Roles
```

### Logging

```yaml
common:
  logging:
    request:
      enabled: true
      include-headers: true
      include-body: false
    response:
      enabled: true
    trace:
      enabled: true
```

## 🧪 Testing

```java
@SpringBootTest
class JwtTokenProviderTest {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Test
    void testGenerateToken() {
        String token = tokenProvider.generateToken("user", List.of("USER"));
        assertTrue(tokenProvider.validateToken(token));
    }
}
```

## 📊 Response Format

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

## 🔒 Security

- JWT token với HS256 algorithm
- Secret key >= 256 bits (32 bytes)
- Token expiration & refresh token support
- Role-based & Permission-based authorization
- Spring Security integration

## 📈 Performance

- Optional dependencies để tránh conflict
- Conditional bean creation
- Async logging support
- Token caching (recommended)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📝 License

This project is licensed under the MIT License.

## 👥 Authors

- HDDT Team

## 📞 Support

For support, email [email] or create an issue in the repository.

## 🗺️ Roadmap

- [ ] Rate Limiting
- [ ] OAuth2 Integration
- [ ] API Key Authentication
- [ ] Audit Logging
- [ ] Multi-tenancy Support

## ⚙️ Requirements

- Java 17+
- Spring Boot 3.4.1+
- Maven 3.6+

## 🎯 Use Cases

### 1. Authentication Service

Tạo và validate JWT tokens cho user authentication

### 2. API Gateway

Parse JWT và truyền user info xuống downstream services

### 3. Microservices

Nhận user info từ headers, validate permissions, log requests

## 📦 Build Info

```
Group ID: com.hddt.common
Artifact ID: common-service
Version: 1.0.0
Packaging: JAR
```

## 🌟 Features Highlight

✅ Zero configuration - Works out of the box  
✅ Spring Boot Auto-Configuration  
✅ Reactive (WebFlux) support  
✅ Production-ready  
✅ Well documented  
✅ Easy to integrate  

---

Made with ❤️ by HDDT Team
