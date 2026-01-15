# Hướng Dẫn Sử Dụng - Common Service

## 1. JWT Token Provider

### Tạo Access Token

```java
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    public String login(String username, String password) {
        // Xác thực user...
        
        List<String> roleCodes = Arrays.asList("ADMIN", "USER");
        
        // Tạo access token
        String token = jwtTokenProvider.generateToken(username, roleCodes);
        
        return token;
    }
}
```

### Tạo Token Với Custom Claims

```java
public String loginWithCustomClaims(String username, List<String> roles) {
    Map<String, Object> additionalClaims = new HashMap<>();
    additionalClaims.put("department", "IT");
    additionalClaims.put("employee_id", "EMP001");
    
    String token = jwtTokenProvider.generateToken(username, roles, additionalClaims);
    
    return token;
}
```

### Tạo Refresh Token

```java
public String createRefreshToken(String username) {
    String refreshToken = jwtTokenProvider.generateRefreshToken(username);
    return refreshToken;
}
```

### Validate Token

```java
public boolean isValidToken(String token) {
    return jwtTokenProvider.validateToken(token);
}
```

### Parse Token Claims

```java
public JwtClaims getUserInfo(String token) {
    if (!jwtTokenProvider.validateToken(token)) {
        throw new BusinessException("INVALID_TOKEN", "Token không hợp lệ");
    }
    
    JwtClaims claims = jwtTokenProvider.parseToken(token);
    String username = claims.getUsername();
    List<String> roles = claims.getRoleCodes();
    
    return claims;
}
```

### Kiểm Tra Token Expiration

```java
public boolean isTokenExpired(String token) {
    return jwtTokenProvider.isTokenExpired(token);
}

public Date getExpirationDate(String token) {
    return jwtTokenProvider.getExpirationDateFromToken(token);
}
```

## 2. Security Annotations

### @HasRole - Kiểm Tra Role

```java
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    @GetMapping("/users")
    @HasRole(role = "ADMIN")
    public List<User> getAllUsers() {
        // Chỉ user có role ADMIN mới truy cập được
        return userService.findAll();
    }
    
    @DeleteMapping("/users/{id}")
    @HasRole(role = "SUPER_ADMIN")
    public void deleteUser(@PathVariable Long id) {
        // Chỉ SUPER_ADMIN mới xóa được user
        userService.delete(id);
    }
}
```

### @HasPermission - Kiểm Tra Permission

```java
@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    
    @GetMapping
    @HasPermission("READ_DOCUMENT")
    public List<Document> getDocuments() {
        // Cần permission PERM_READ_DOCUMENT
        return documentService.findAll();
    }
    
    @PostMapping
    @HasPermission("CREATE_DOCUMENT")
    public Document createDocument(@RequestBody Document doc) {
        // Cần permission PERM_CREATE_DOCUMENT
        return documentService.save(doc);
    }
    
    @DeleteMapping("/{id}")
    @HasPermission("DELETE_DOCUMENT")
    public void deleteDocument(@PathVariable Long id) {
        // Cần permission PERM_DELETE_DOCUMENT
        documentService.delete(id);
    }
}
```

### Sử Dụng Spring Security @PreAuthorize

Ngoài custom annotations, bạn vẫn có thể dùng `@PreAuthorize`:

```java
@RestController
@RequestMapping("/api/reports")
public class ReportController {
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public List<Report> getReports() {
        return reportService.findAll();
    }
    
    @GetMapping("/sensitive")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('PERM_VIEW_SENSITIVE')")
    public List<Report> getSensitiveReports() {
        return reportService.findSensitive();
    }
}
```

## 3. Base Response

### Trả Về Response Thành Công

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @GetMapping("/{id}")
    public BaseResponse<Product> getProduct(@PathVariable Long id) {
        Product product = productService.findById(id);
        return BaseResponse.ok(product);
    }
    
    @GetMapping
    public BaseResponse<List<Product>> getAllProducts() {
        List<Product> products = productService.findAll();
        return BaseResponse.ok(products);
    }
    
    @PostMapping
    public BaseResponse<Product> createProduct(@RequestBody Product product) {
        Product saved = productService.save(product);
        return BaseResponse.ok(saved);
    }
}
```

### Trả Về Response Lỗi

```java
@GetMapping("/{id}")
public BaseResponse<Product> getProduct(@PathVariable Long id) {
    Product product = productService.findById(id);
    
    if (product == null) {
        return BaseResponse.error("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm");
    }
    
    return BaseResponse.ok(product);
}
```

### Response Format

Response thành công:
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

Response lỗi:
```json
{
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "data": null,
  "errorCode": "PRODUCT_NOT_FOUND",
  "message": "Không tìm thấy sản phẩm"
}
```

## 4. Exception Handling

### Throw BusinessException

```java
@Service
public class OrderService {
    
    public Order createOrder(OrderRequest request) {
        // Validate
        if (request.getQuantity() <= 0) {
            throw new BusinessException(
                "INVALID_QUANTITY", 
                "Số lượng phải lớn hơn 0"
            );
        }
        
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new BusinessException(
                "PRODUCT_NOT_FOUND",
                "Không tìm thấy sản phẩm"
            ));
        
        if (product.getStock() < request.getQuantity()) {
            throw new BusinessException(
                "INSUFFICIENT_STOCK",
                "Không đủ hàng trong kho"
            );
        }
        
        // Create order...
        return order;
    }
}
```

### Global Exception Handler

`GlobalExceptionAdvice` sẽ tự động bắt và xử lý exceptions:

```java
// BusinessException sẽ trả về HTTP 400 với error code và message
throw new BusinessException("INVALID_INPUT", "Dữ liệu không hợp lệ");

// Response:
// Status: 400 Bad Request
// Body:
// {
//   "traceId": "...",
//   "data": null,
//   "errorCode": "INVALID_INPUT",
//   "message": "Dữ liệu không hợp lệ"
// }

// Generic Exception sẽ trả về HTTP 500
throw new RuntimeException("Something went wrong");

// Response:
// Status: 500 Internal Server Error
// Body:
// {
//   "traceId": "...",
//   "data": null,
//   "errorCode": "INTERNAL_ERROR",
//   "message": "Internal server error"
// }
```

## 5. Trace ID

### Lấy Trace ID Trong Code

```java
@Service
public class PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    
    public void processPayment(Payment payment) {
        String traceId = TraceIdUtil.getOrCreate();
        
        logger.info("Processing payment with trace ID: {}", traceId);
        
        // Business logic...
        
        // Trace ID sẽ tự động có trong log
        logger.info("Payment processed successfully");
    }
}
```

### Trace ID Trong Response

Mọi response đều tự động có trace ID:

```java
@GetMapping("/status")
public BaseResponse<String> getStatus() {
    // Trace ID tự động được thêm vào response
    return BaseResponse.ok("Service is running");
}

// Response:
// {
//   "traceId": "550e8400-e29b-41d4-a716-446655440000",
//   "data": "Service is running",
//   "errorCode": null,
//   "message": "SUCCESS"
// }
```

### Trace ID Trong Log

Log pattern đã được cấu hình để hiển thị trace ID:

```
2026-01-14 10:30:45.123  INFO [my-service,550e8400-e29b-41d4-a716-446655440000] --- Processing payment
2026-01-14 10:30:45.456  INFO [my-service,550e8400-e29b-41d4-a716-446655440000] --- Payment processed successfully
```

## 6. Gateway Integration

### Đọc User Info Từ Headers (Downstream Service)

Khi request đi qua Gateway, thông tin user được truyền qua headers:

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @PostMapping
    public BaseResponse<Order> createOrder(
            @RequestHeader(HeaderConstant.USER_ID) String userId,
            @RequestHeader(HeaderConstant.USERNAME) String username,
            @RequestHeader(HeaderConstant.ROLE_CODES) String roles,
            @RequestBody OrderRequest request) {
        
        logger.info("User {} (ID: {}) creating order", username, userId);
        logger.info("User roles: {}", roles);
        
        Order order = orderService.create(userId, request);
        return BaseResponse.ok(order);
    }
}
```

### Sử Dụng Optional Headers

```java
@GetMapping
public BaseResponse<List<Order>> getOrders(
        @RequestHeader(value = HeaderConstant.USER_ID, required = false) String userId) {
    
    if (userId != null) {
        // Request từ Gateway, có thông tin user
        return BaseResponse.ok(orderService.findByUserId(userId));
    } else {
        // Request trực tiếp, không có user info
        return BaseResponse.ok(orderService.findAll());
    }
}
```

## 7. Logging

### Request/Response Logging

Request logging tự động hoạt động, log format:

```
[TRACE=550e8400-e29b-41d4-a716-446655440000] GET /api/products/1 → 200 OK (45ms)
[TRACE=550e8400-e29b-41d4-a716-446655440000] POST /api/orders → 201 CREATED (123ms)
```

### Custom Logging Với Trace ID

```java
@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    public void sendEmail(String to, String subject) {
        // Trace ID tự động có trong MDC
        logger.info("Sending email to: {}", to);
        
        try {
            emailClient.send(to, subject);
            logger.info("Email sent successfully");
        } catch (Exception e) {
            logger.error("Failed to send email", e);
        }
    }
}
```

## 8. Testing

### Test JWT Token Provider

```java
@SpringBootTest
class JwtTokenProviderTest {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Test
    void testGenerateAndValidateToken() {
        String username = "testuser";
        List<String> roles = Arrays.asList("USER", "ADMIN");
        
        // Generate token
        String token = tokenProvider.generateToken(username, roles);
        assertNotNull(token);
        
        // Validate token
        assertTrue(tokenProvider.validateToken(token));
        
        // Parse token
        JwtClaims claims = tokenProvider.parseToken(token);
        assertEquals(username, claims.getUsername());
        assertEquals(roles, claims.getRoleCodes());
    }
    
    @Test
    void testInvalidToken() {
        String invalidToken = "invalid.token.here";
        assertFalse(tokenProvider.validateToken(invalidToken));
    }
}
```

### Test Controller Với Security

```java
@WebFluxTest(ProductController.class)
@Import(SecurityAutoConfiguration.class)
class ProductControllerTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Test
    void testGetProductWithValidToken() {
        String token = tokenProvider.generateToken("testuser", List.of("USER"));
        
        webTestClient.get()
            .uri("/api/products/1")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.data.id").isEqualTo(1);
    }
    
    @Test
    void testGetProductWithoutToken() {
        webTestClient.get()
            .uri("/api/products/1")
            .exchange()
            .expectStatus().isUnauthorized();
    }
}
```

## 9. Best Practices

### 1. Luôn Validate Input

```java
@PostMapping
public BaseResponse<Product> createProduct(@Valid @RequestBody ProductRequest request) {
    if (request.getPrice() < 0) {
        throw new BusinessException("INVALID_PRICE", "Giá không được âm");
    }
    
    Product product = productService.create(request);
    return BaseResponse.ok(product);
}
```

### 2. Sử Dụng Meaningful Error Codes

```java
// Good
throw new BusinessException("PRODUCT_OUT_OF_STOCK", "Sản phẩm đã hết hàng");
throw new BusinessException("PAYMENT_FAILED", "Thanh toán thất bại");

// Bad
throw new BusinessException("ERROR", "Có lỗi xảy ra");
throw new BusinessException("ERR001", "Error");
```

### 3. Log Đầy Đủ Thông Tin

```java
@Service
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    public Order createOrder(OrderRequest request) {
        logger.info("Creating order for user: {}, product: {}", 
            request.getUserId(), request.getProductId());
        
        try {
            Order order = processOrder(request);
            logger.info("Order created successfully: {}", order.getId());
            return order;
        } catch (Exception e) {
            logger.error("Failed to create order", e);
            throw new BusinessException("ORDER_CREATION_FAILED", "Không thể tạo đơn hàng");
        }
    }
}
```

### 4. Refresh Token Flow

```java
@PostMapping("/refresh")
public BaseResponse<TokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
    String refreshToken = request.getRefreshToken();
    
    // Validate refresh token
    if (!tokenProvider.validateToken(refreshToken)) {
        throw new BusinessException("INVALID_REFRESH_TOKEN", "Refresh token không hợp lệ");
    }
    
    // Parse username from refresh token
    JwtClaims claims = tokenProvider.parseToken(refreshToken);
    String username = claims.getUsername();
    
    // Get user roles from database
    List<String> roles = userService.getRolesByUsername(username);
    
    // Generate new access token
    String newAccessToken = tokenProvider.generateToken(username, roles);
    
    TokenResponse response = new TokenResponse(newAccessToken, refreshToken);
    return BaseResponse.ok(response);
}
```

### 5. Logout (Token Blacklist)

```java
@Service
public class TokenBlacklistService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider tokenProvider;
    
    public void blacklistToken(String token) {
        Date expiration = tokenProvider.getExpirationDateFromToken(token);
        long ttl = expiration.getTime() - System.currentTimeMillis();
        
        if (ttl > 0) {
            redisTemplate.opsForValue().set(
                "blacklist:" + token, 
                "true", 
                ttl, 
                TimeUnit.MILLISECONDS
            );
        }
    }
    
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(
            redisTemplate.hasKey("blacklist:" + token)
        );
    }
}
```
