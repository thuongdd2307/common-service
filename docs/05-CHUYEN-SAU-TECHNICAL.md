# Chuyên Sâu Technical - Common Service

## Kiến Trúc Tổng Quan

### Auto-Configuration Flow

```
Application Start
    ↓
Spring Boot scans META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
    ↓
Load Auto-Configuration Classes:
    ├── CommonAutoConfiguration
    ├── LoggingAutoConfiguration  
    ├── SecurityAutoConfiguration
    └── GatewayAutoConfiguration
    ↓
Check @ConditionalOnProperty, @ConditionalOnClass
    ↓
Create Beans if conditions match
    ↓
Application Ready
```

### Request Flow

#### 1. Normal Service Request Flow

```
Client Request
    ↓
TraceIdFilter (tạo trace ID)
    ↓
RequestLoggingFilter (log request)
    ↓
JwtAuthenticationFilter (parse JWT, set SecurityContext)
    ↓
Controller (xử lý business logic)
    ↓
GlobalExceptionAdvice (nếu có exception)
    ↓
BaseResponse (format response)
    ↓
Client Response (với trace ID header)
```

#### 2. Gateway Request Flow

```
Client Request → Gateway
    ↓
JwtGatewayFilter (parse JWT)
    ↓
Add headers: X-User-Id, X-Username, X-Roles
    ↓
Route to Downstream Service
    ↓
Downstream Service reads headers
    ↓
Process & Response
    ↓
Gateway → Client
```

## Chi Tiết Implementation

### 1. JWT Token Provider

#### Key Generation

```java
// Secret key phải >= 256 bits (32 bytes)
private Key key = Keys.hmacShaKeyFor(secret.getBytes());
```

#### Token Structure

```
Header:
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload:
{
  "sub": "username",
  "iss": "common-service",
  "iat": 1705219200,
  "exp": 1705222800,
  "role_codes": ["ADMIN", "USER"]
}

Signature:
HMACSHA256(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret
)
```

#### Token Validation Process

```java
public boolean validateToken(String token) {
    try {
        Jwts.parserBuilder()
            .setSigningKey(key)           // 1. Verify signature
            .requireIssuer(issuer)        // 2. Check issuer
            .build()
            .parseClaimsJws(token);       // 3. Parse & validate
        return true;
    } catch (Exception e) {
        return false;
    }
}
```

### 2. Security Filter Chain

#### Filter Order

```
SecurityWebFiltersOrder:
  -100: FIRST
  ...
  -1: JwtAuthenticationFilter (custom)
  0: AUTHENTICATION
  100: AUTHORIZATION
  ...
  1000: LAST
```

#### Authentication Flow

```java
// 1. Extract token from header
String authHeader = request.getHeaders().getFirst("Authorization");
String token = authHeader.substring(7); // Remove "Bearer "

// 2. Validate & parse token
JwtClaims claims = tokenProvider.parseToken(token);

// 3. Create Authentication object
UsernamePasswordAuthenticationToken auth = 
    new UsernamePasswordAuthenticationToken(
        claims.getUsername(),
        null,
        RoleAuthorityMapper.map(claims.getRoleCodes())
    );

// 4. Set SecurityContext
SecurityContext context = new SecurityContextImpl(auth);
return chain.filter(exchange)
    .contextWrite(ctx -> ctx.put(SecurityContext.class, context));
```

### 3. Role & Authority Mapping

#### Spring Security Authority Format

```
Role: ROLE_ADMIN
Permission: PERM_CREATE_USER
```

#### Mapping Logic

```java
public static List<GrantedAuthority> map(List<String> roleCodes) {
    return roleCodes.stream()
        .map(code -> new SimpleGrantedAuthority("ROLE_" + code))
        .toList();
}

// Input: ["ADMIN", "USER"]
// Output: [ROLE_ADMIN, ROLE_USER]
```

#### Authorization Check

```java
@HasRole(role = "ADMIN")
// Internally: @PreAuthorize("hasRole('ADMIN')")
// Checks: SecurityContext contains ROLE_ADMIN

@HasPermission("CREATE_USER")
// Internally: @PreAuthorize("hasAuthority('PERM_CREATE_USER')")
// Checks: SecurityContext contains PERM_CREATE_USER
```

### 4. Trace ID & MDC

#### MDC (Mapped Diagnostic Context)

MDC là ThreadLocal map để lưu context data cho logging:

```java
// Set trace ID
MDC.put("traceId", "550e8400-e29b-41d4-a716-446655440000");

// Get trace ID
String traceId = MDC.get("traceId");

// Clear (important!)
MDC.clear();
```

#### Reactive Context Propagation

Với WebFlux (reactive), MDC không work vì non-blocking. Giải pháp:

```java
@Override
public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String traceId = UUID.randomUUID().toString();
    MDC.put("traceId", traceId);
    
    return chain.filter(exchange)
        .doFinally(signalType -> MDC.clear()); // Clean up
}
```

#### Logback Pattern

```xml
<pattern>
  %d{yyyy-MM-dd HH:mm:ss.SSS} %5p [${spring.application.name:},%X{traceId}] --- %m%n
</pattern>
```

Output:
```
2026-01-14 10:30:45.123  INFO [my-service,550e8400-e29b-41d4-a716-446655440000] --- Processing request
```

### 5. Exception Handling

#### Exception Hierarchy

```
Throwable
  └── Exception
      ├── RuntimeException
      │   └── BusinessException (custom)
      └── Other exceptions
```

#### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionAdvice {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<?>> handle(BusinessException ex) {
        // Known business error → 400 Bad Request
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new BaseResponse<>(
                TraceIdUtil.getOrCreate(),
                null,
                ex.getErrorCode(),
                ex.getMessage()
            ));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<?>> handle(Exception ex) {
        // Unknown error → 500 Internal Server Error
        // Don't expose internal error details
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new BaseResponse<>(
                TraceIdUtil.getOrCreate(),
                null,
                "INTERNAL_ERROR",
                "Internal server error"
            ));
    }
}
```

### 6. Gateway Filter

#### Filter Priority

```java
@Override
public int getOrder() {
    return -1; // Run before other filters
}
```

#### Header Propagation

```java
// Parse JWT at Gateway
JwtClaims claims = tokenProvider.parseToken(token);

// Add headers for downstream
ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
    .header("X-User-Id", claims.getUsername())
    .header("X-Username", claims.getUsername())
    .header("X-Roles", String.join(",", claims.getRoleCodes()))
    .build();

// Continue with mutated request
return chain.filter(exchange.mutate().request(mutatedRequest).build());
```

## Performance Considerations

### 1. JWT Validation

**Problem**: Validate JWT mỗi request → CPU intensive

**Solution**: 
- Cache validated tokens (Redis)
- Short expiration time
- Use refresh token

```java
@Service
public class CachedJwtValidator {
    
    private final RedisTemplate<String, Boolean> redis;
    private final JwtTokenProvider provider;
    
    public boolean validateToken(String token) {
        // Check cache first
        Boolean cached = redis.opsForValue().get("jwt:valid:" + token);
        if (cached != null) {
            return cached;
        }
        
        // Validate
        boolean valid = provider.validateToken(token);
        
        // Cache result (TTL = token expiration)
        if (valid) {
            long ttl = provider.getExpirationDateFromToken(token).getTime() 
                     - System.currentTimeMillis();
            redis.opsForValue().set("jwt:valid:" + token, true, ttl, TimeUnit.MILLISECONDS);
        }
        
        return valid;
    }
}
```

### 2. Logging

**Problem**: Log body → memory & performance issue

**Solution**: 
- Disable body logging in production
- Limit body size
- Use async logging

```yaml
common:
  logging:
    request:
      include-body: false  # Disable in production
      max-body-size: 2048  # Limit size
```

### 3. MDC in Reactive

**Problem**: MDC doesn't work well with reactive

**Solution**: Use Reactor Context

```java
return chain.filter(exchange)
    .contextWrite(Context.of("traceId", traceId));
```

## Security Best Practices

### 1. Secret Key Management

**❌ Bad**:
```yaml
security:
  jwt:
    secret: "my-secret"  # Hardcoded, too short
```

**✅ Good**:
```yaml
security:
  jwt:
    secret: ${JWT_SECRET}  # From environment variable
```

```bash
# Generate strong secret
openssl rand -base64 32
# Output: 8xK9mP2nQ5rT7vW0yZ3aB6cD8eF1gH4j

export JWT_SECRET="8xK9mP2nQ5rT7vW0yZ3aB6cD8eF1gH4j"
```

### 2. Token Expiration

```yaml
security:
  jwt:
    expiration-seconds: 900          # 15 minutes (short)
    refresh-expiration-seconds: 604800  # 7 days
```

### 3. HTTPS Only

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_PASSWORD}
```

### 4. CORS Configuration

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsWebFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://yourdomain.com"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsWebFilter(source);
    }
}
```

## Testing Strategies

### 1. Unit Test JWT Provider

```java
@Test
void testTokenGeneration() {
    JwtTokenProvider provider = new JwtTokenProvider("test-secret-key-32-bytes-long");
    
    String token = provider.generateToken("testuser", List.of("USER"));
    
    assertNotNull(token);
    assertTrue(provider.validateToken(token));
    
    JwtClaims claims = provider.parseToken(token);
    assertEquals("testuser", claims.getUsername());
    assertEquals(List.of("USER"), claims.getRoleCodes());
}
```

### 2. Integration Test with Security

```java
@WebFluxTest
@Import(SecurityAutoConfiguration.class)
class SecuredControllerTest {
    
    @Autowired
    WebTestClient webClient;
    
    @Autowired
    JwtTokenProvider tokenProvider;
    
    @Test
    void testSecuredEndpoint() {
        String token = tokenProvider.generateToken("admin", List.of("ADMIN"));
        
        webClient.get()
            .uri("/api/admin/users")
            .header("Authorization", "Bearer " + token)
            .exchange()
            .expectStatus().isOk();
    }
    
    @Test
    void testUnauthorized() {
        webClient.get()
            .uri("/api/admin/users")
            .exchange()
            .expectStatus().isUnauthorized();
    }
}
```

### 3. Test Exception Handling

```java
@Test
void testBusinessException() {
    webClient.get()
        .uri("/api/products/999")
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.errorCode").isEqualTo("PRODUCT_NOT_FOUND")
        .jsonPath("$.traceId").exists();
}
```

## Monitoring & Observability

### 1. Metrics

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 2. Distributed Tracing

Tích hợp với Zipkin/Jaeger:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

### 3. Logging to ELK

```xml
<!-- logback-spring.xml -->
<appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
    <destination>localhost:5000</destination>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>traceId</includeMdcKeyName>
    </encoder>
</appender>
```

## Troubleshooting Guide

### Issue 1: "Invalid JWT signature"

**Cause**: Secret key mismatch between services

**Solution**: Ensure all services use same secret key

### Issue 2: "Token expired"

**Cause**: Token TTL quá ngắn hoặc clock skew

**Solution**: 
- Tăng expiration time
- Sync clocks (NTP)
- Add clock skew tolerance

```java
Jwts.parserBuilder()
    .setAllowedClockSkewSeconds(60) // 1 minute tolerance
    .build();
```

### Issue 3: "MDC is empty in logs"

**Cause**: MDC cleared before logging

**Solution**: Use `doFinally` to clear MDC

```java
return chain.filter(exchange)
    .doFinally(signalType -> MDC.clear());
```

## Roadmap & Future Enhancements

### Planned Features

1. **Rate Limiting**: Giới hạn số request per user
2. **API Key Authentication**: Hỗ trợ API key cho external clients
3. **OAuth2 Integration**: Tích hợp OAuth2/OIDC
4. **Audit Logging**: Log tất cả actions của user
5. **Multi-tenancy**: Hỗ trợ nhiều tenant

### Version 2.0.0 (Planned)

- Spring Boot 3.5.x
- Java 21
- Virtual Threads support
- Enhanced observability
- GraphQL support
