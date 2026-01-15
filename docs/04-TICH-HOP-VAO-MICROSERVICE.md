# Tích Hợp Common Service Vào Microservice

## Kết Quả Build

✅ **BUILD SUCCESS**

Common Service đã được build và install thành công vào Maven local repository:

```
Location: C:\Users\admin\.m2\repository\com\hddt\common\common-service\1.0.0\
File: common-service-1.0.0.jar
```

## Cách Tích Hợp Vào Microservice Khác

### Bước 1: Thêm Dependency

Trong file `pom.xml` của microservice, thêm dependency:

```xml
<dependencies>
    <!-- Common Service -->
    <dependency>
        <groupId>com.hddt.common</groupId>
        <artifactId>common-service</artifactId>
        <version>1.0.0</version>
    </dependency>
    
    <!-- Các dependencies khác... -->
</dependencies>
```

### Bước 2: Cấu Hình application.yml

Thêm cấu hình JWT và logging vào `application.yml`:

```yaml
spring:
  application:
    name: your-service-name
  
  main:
    web-application-type: reactive

# JWT Configuration
security:
  jwt:
    enabled: true
    secret: "YOUR_SECRET_KEY_MUST_BE_AT_LEAST_32_BYTES_LONG_HERE"
    issuer: your-service-name
    expiration-seconds: 3600
    refresh-expiration-seconds: 86400

# Logging Configuration
common:
  logging:
    enabled: true
    request:
      enabled: true
      include-headers: true
      include-body: false
    response:
      enabled: true
      include-body: false
    trace:
      enabled: true
      header-name: X-Trace-Id

logging:
  level:
    root: INFO
    com.yourcompany: DEBUG
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId}]"
```

### Bước 3: Tạo Controller Sử Dụng Common Service

```java
package com.yourcompany.yourservice.controller;

import com.example.commonserviceofficial.contract.BaseResponse;
import com.example.commonserviceofficial.exception.BusinessException;
import com.example.commonserviceofficial.security.annotation.HasRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    
    @GetMapping
    public BaseResponse<List<Product>> getAllProducts() {
        List<Product> products = productService.findAll();
        return BaseResponse.ok(products);
    }
    
    @GetMapping("/{id}")
    public BaseResponse<Product> getProduct(@PathVariable Long id) {
        Product product = productService.findById(id)
            .orElseThrow(() -> new BusinessException(
                "PRODUCT_NOT_FOUND", 
                "Không tìm thấy sản phẩm"
            ));
        return BaseResponse.ok(product);
    }
    
    @PostMapping
    @HasRole(role = "ADMIN")
    public BaseResponse<Product> createProduct(@RequestBody Product product) {
        Product saved = productService.save(product);
        return BaseResponse.ok(saved);
    }
    
    @DeleteMapping("/{id}")
    @HasRole(role = "ADMIN")
    public BaseResponse<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return BaseResponse.ok();
    }
}
```

### Bước 4: Tạo Authentication Service

```java
package com.yourcompany.yourservice.service;

import com.example.commonserviceofficial.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    
    public LoginResponse login(LoginRequest request) {
        // Validate credentials
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new BusinessException(
                "USER_NOT_FOUND", 
                "Tên đăng nhập hoặc mật khẩu không đúng"
            ));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(
                "INVALID_CREDENTIALS", 
                "Tên đăng nhập hoặc mật khẩu không đúng"
            );
        }
        
        // Generate tokens
        List<String> roleCodes = user.getRoles().stream()
            .map(Role::getCode)
            .toList();
        
        String accessToken = jwtTokenProvider.generateToken(
            user.getUsername(), 
            roleCodes
        );
        
        String refreshToken = jwtTokenProvider.generateRefreshToken(
            user.getUsername()
        );
        
        return new LoginResponse(accessToken, refreshToken);
    }
    
    public TokenResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(
                "INVALID_REFRESH_TOKEN", 
                "Refresh token không hợp lệ"
            );
        }
        
        JwtClaims claims = jwtTokenProvider.parseToken(refreshToken);
        User user = userRepository.findByUsername(claims.getUsername())
            .orElseThrow(() -> new BusinessException(
                "USER_NOT_FOUND", 
                "Người dùng không tồn tại"
            ));
        
        List<String> roleCodes = user.getRoles().stream()
            .map(Role::getCode)
            .toList();
        
        String newAccessToken = jwtTokenProvider.generateToken(
            user.getUsername(), 
            roleCodes
        );
        
        return new TokenResponse(newAccessToken, refreshToken);
    }
}
```

## Các Loại Microservice

### 1. API Gateway Service

Nếu service của bạn là API Gateway, cấu hình thêm:

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

Gateway sẽ tự động:
- Parse JWT token từ request
- Validate token
- Truyền thông tin user qua headers xuống downstream services

### 2. Downstream Service (Service Thông Thường)

Downstream service nhận thông tin user từ headers:

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @PostMapping
    public BaseResponse<Order> createOrder(
            @RequestHeader(HeaderConstant.USER_ID) String userId,
            @RequestHeader(HeaderConstant.USERNAME) String username,
            @RequestBody OrderRequest request) {
        
        Order order = orderService.create(userId, request);
        return BaseResponse.ok(order);
    }
}
```

Cấu hình:

```yaml
security:
  jwt:
    enabled: false  # Tắt JWT validation vì đã validate ở Gateway

gateway:
  security:
    enabled: false  # Không phải Gateway

common:
  logging:
    enabled: true  # Vẫn bật logging
```

### 3. Authentication Service

Service xử lý login/register cần JWT để tạo token:

```yaml
security:
  jwt:
    enabled: true
    secret: "YOUR_SECRET_KEY_HERE"
    issuer: auth-service

gateway:
  security:
    enabled: false

common:
  logging:
    enabled: true
```

## Kiểm Tra Tích Hợp

### 1. Kiểm Tra Auto-Configuration

Khi start service, bạn sẽ thấy log:

```
INFO  --- Auto-configured JwtTokenProvider
INFO  --- Auto-configured JwtAuthenticationFilter
INFO  --- Auto-configured TraceIdFilter
INFO  --- Auto-configured RequestLoggingFilter
```

### 2. Test API Với JWT

```bash
# Login để lấy token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password"
  }'

# Response:
# {
#   "traceId": "550e8400-e29b-41d4-a716-446655440000",
#   "data": {
#     "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#     "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
#   },
#   "errorCode": null,
#   "message": "SUCCESS"
# }

# Sử dụng token để gọi API
curl -X GET http://localhost:8080/api/products \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### 3. Kiểm Tra Trace ID

Mọi response đều có trace ID:

```json
{
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "data": [...],
  "errorCode": null,
  "message": "SUCCESS"
}
```

Log cũng có trace ID:

```
INFO [your-service,550e8400-e29b-41d4-a716-446655440000] --- Processing request
```

## Troubleshooting

### Lỗi: "Secret key must be at least 256 bits"

**Nguyên nhân**: JWT secret key quá ngắn

**Giải pháp**: Đảm bảo secret key >= 32 ký tự

```yaml
security:
  jwt:
    secret: "THIS_IS_A_VALID_32_BYTE_SECRET_KEY_2026"
```

### Lỗi: "Bean of type JwtTokenProvider could not be found"

**Nguyên nhân**: Auto-configuration không được kích hoạt

**Giải pháp**: Kiểm tra:
1. Dependency đã được thêm đúng chưa
2. `security.jwt.enabled=true` trong config
3. Spring Boot version tương thích (3.4.1)

### Lỗi: "Unauthorized" khi gọi API

**Nguyên nhân**: Token không hợp lệ hoặc thiếu

**Giải pháp**:
1. Kiểm tra token có được gửi trong header không
2. Format: `Authorization: Bearer <token>`
3. Token chưa hết hạn
4. Secret key giống nhau giữa các services

### Lỗi: "ConditionalOnClass did not match"

**Nguyên nhân**: Thiếu dependency

**Giải pháp**: Thêm Spring WebFlux:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

## Best Practices Cho Microservices

### 1. Shared Secret Key

Tất cả services trong hệ thống nên dùng chung một JWT secret key:

```bash
# Environment variable
export JWT_SECRET="your-shared-secret-key-here"
```

```yaml
security:
  jwt:
    secret: ${JWT_SECRET}
```

### 2. Service-to-Service Communication

Khi service A gọi service B, truyền JWT token:

```java
@Service
public class OrderService {
    
    private final WebClient webClient;
    
    public Product getProduct(Long productId, String jwtToken) {
        return webClient.get()
            .uri("http://product-service/api/products/{id}", productId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
            .retrieve()
            .bodyToMono(BaseResponse.class)
            .map(response -> (Product) response.getData())
            .block();
    }
}
```

### 3. Centralized Configuration

Sử dụng Spring Cloud Config để quản lý config tập trung:

```yaml
# config-server/application.yml
security:
  jwt:
    secret: ${JWT_SECRET}
    issuer: hddt-system
    expiration-seconds: 3600

common:
  logging:
    enabled: true
```

### 4. Health Check Endpoint

Thêm health check để monitoring:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

## Kết Luận

Common Service đã sẵn sàng để tích hợp vào các microservices trong hệ sinh thái của bạn. Các tính năng chính:

✅ JWT Authentication & Authorization  
✅ Distributed Tracing với Trace ID  
✅ Request/Response Logging  
✅ Exception Handling chuẩn  
✅ Gateway Integration  
✅ Security Annotations  

Chỉ cần thêm dependency và cấu hình, tất cả tính năng sẽ tự động hoạt động nhờ Spring Boot Auto-Configuration!
