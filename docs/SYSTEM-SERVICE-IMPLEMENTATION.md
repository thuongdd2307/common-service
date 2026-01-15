# Implementation Guide - System Service

## 📦 Dependencies (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>

    <groupId>com.hddt</groupId>
    <artifactId>system-service</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- Common Service -->
        <dependency>
            <groupId>com.hddt.common</groupId>
            <artifactId>common-service</artifactId>
            <version>1.0.0</version>
        </dependency>

        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- MySQL Driver -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- MapStruct -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>1.5.5.Final</version>
        </dependency>

        <!-- Flyway Migration -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-mysql</artifactId>
        </dependency>

        <!-- Apache POI (Excel Export) -->
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>5.2.5</version>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

## ⚙️ Configuration (application.yml)

```yaml
spring:
  application:
    name: system-service

  datasource:
    url: jdbc:mysql://localhost:3306/hddt_system?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Ho_Chi_Minh
    username: root
    password: ${DB_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true
        use_sql_comments: true

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

  redis:
    host: localhost
    port: 6379
    password: ${REDIS_PASSWORD:}
    database: 0
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 2

# Common Service Configuration
security:
  jwt:
    enabled: true
    secret: ${JWT_SECRET:THIS_IS_A_32_BYTE_SECRET_KEY_FOR_JWT_2026}
    issuer: system-service
    expiration-seconds: 3600
    refresh-expiration-seconds: 604800

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

# Application Configuration
app:
  security:
    max-failed-login-attempts: 5
    account-lock-duration-minutes: 30
    password-min-length: 8
    
  audit:
    enabled: true
    async: true
    include-request-body: true
    include-response-body: false
    
  token:
    cleanup-enabled: true
    cleanup-cron: "0 0 2 * * ?" # 2 AM daily

server:
  port: 8082

logging:
  level:
    root: INFO
    com.hddt.system: DEBUG
  file:
    name: logs/system-service.log
```

## 🏗️ Core Implementation

### 1. Entity Classes

#### User.java

```java
package com.hddt.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "sys_user")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(unique = true, nullable = false, length = 100)
    private String email;
    
    @Column(name = "full_name", length = 100)
    private String fullName;
    
    @Column(length = 20)
    private String phone;
    
    @Column(name = "avatar_url")
    private String avatarUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserStatus status = UserStatus.ACTIVE;
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;
    
    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;
    
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "sys_user_role",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_by")
    private Long updatedBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

#### Role.java

```java
package com.hddt.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "sys_role")
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String code;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserStatus status = UserStatus.ACTIVE;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "sys_role_permission",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_by")
    private Long updatedBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

#### AuditLog.java

```java
package com.hddt.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_audit_log")
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "trace_id", length = 100)
    private String traceId;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(length = 50)
    private String username;
    
    @Column(nullable = false, length = 100)
    private String action;
    
    @Column(length = 100)
    private String resource;
    
    @Column(length = 10)
    private String method;
    
    @Column(name = "request_url", columnDefinition = "TEXT")
    private String requestUrl;
    
    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;
    
    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;
    
    @Column(name = "response_status")
    private Integer responseStatus;
    
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;
    
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "execution_time")
    private Integer executionTime;
    
    @Column(length = 20)
    private String status;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

### 2. Repository Interfaces

```java
package com.hddt.system.repository;

import com.hddt.system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.username = :username AND u.deletedAt IS NULL")
    Optional<User> findByUsernameWithRoles(String username);
}
```

```java
package com.hddt.system.repository;

import com.hddt.system.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);
    
    Page<AuditLog> findByAction(String action, Pageable pageable);
    
    Page<AuditLog> findByCreatedAtBetween(
        LocalDateTime startDate, 
        LocalDateTime endDate, 
        Pageable pageable
    );
    
    Page<AuditLog> findByUserIdAndCreatedAtBetween(
        Long userId,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable
    );
}
```

### 3. Service Implementation

#### AuthService.java

```java
package com.hddt.system.service;

import com.example.commonserviceofficial.contract.BaseResponse;
import com.example.commonserviceofficial.exception.BusinessException;
import com.example.commonserviceofficial.security.JwtTokenProvider;
import com.hddt.system.dto.request.LoginRequest;
import com.hddt.system.dto.response.LoginResponse;
import com.hddt.system.entity.User;
import com.hddt.system.enums.UserStatus;
import com.hddt.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;
    
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress) {
        // Find user
        User user = userRepository.findByUsernameWithRoles(request.getUsername())
            .orElseThrow(() -> new BusinessException(
                "INVALID_CREDENTIALS",
                "Tên đăng nhập hoặc mật khẩu không đúng"
            ));
        
        // Check if account is locked
        if (user.getLockedUntil() != null && 
            user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(
                "ACCOUNT_LOCKED",
                "Tài khoản đã bị khóa. Vui lòng thử lại sau."
            );
        }
        
        // Check account status
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(
                "ACCOUNT_INACTIVE",
                "Tài khoản không hoạt động"
            );
        }
        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedLogin(user);
            throw new BusinessException(
                "INVALID_CREDENTIALS",
                "Tên đăng nhập hoặc mật khẩu không đúng"
            );
        }
        
        // Reset failed attempts
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(ipAddress);
        userRepository.save(user);
        
        // Generate tokens
        List<String> roleCodes = user.getRoles().stream()
            .map(role -> role.getCode())
            .collect(Collectors.toList());
        
        String accessToken = jwtTokenProvider.generateToken(
            user.getUsername(),
            roleCodes
        );
        
        String refreshToken = jwtTokenProvider.generateRefreshToken(
            user.getUsername()
        );
        
        // Save refresh token
        refreshTokenService.saveRefreshToken(
            refreshToken,
            user.getId(),
            accessToken
        );
        
        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(3600)
            .userId(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .roles(roleCodes)
            .build();
    }
    
    @Transactional
    public void logout(String accessToken) {
        // Add token to blacklist
        tokenBlacklistService.blacklistToken(accessToken, "LOGOUT");
        
        // Revoke refresh token
        refreshTokenService.revokeByAccessToken(accessToken);
    }
    
    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        
        if (attempts >= 5) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            log.warn("Account locked due to too many failed attempts: {}", 
                user.getUsername());
        }
        
        userRepository.save(user);
    }
}
```

### 4. Controller Implementation

#### AuthController.java

```java
package com.hddt.system.controller;

import com.example.commonserviceofficial.contract.BaseResponse;
import com.hddt.system.annotation.AuditLog;
import com.hddt.system.dto.request.LoginRequest;
import com.hddt.system.dto.request.RefreshTokenRequest;
import com.hddt.system.dto.response.LoginResponse;
import com.hddt.system.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/login")
    @AuditLog(action = "LOGIN", resource = "/api/auth/login")
    public BaseResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = getClientIp(httpRequest);
        LoginResponse response = authService.login(request, ipAddress);
        
        return BaseResponse.ok(response);
    }
    
    @PostMapping("/logout")
    @AuditLog(action = "LOGOUT", resource = "/api/auth/logout")
    public BaseResponse<String> logout(
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.substring(7); // Remove "Bearer "
        authService.logout(token);
        
        return BaseResponse.ok("Đăng xuất thành công");
    }
    
    @PostMapping("/refresh")
    public BaseResponse<LoginResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        
        LoginResponse response = authService.refreshToken(request.getRefreshToken());
        return BaseResponse.ok(response);
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
```

### 5. Audit Log Aspect

```java
package com.hddt.system.aspect;

import com.example.commonserviceofficial.logging.util.TraceIdUtil;
import com.hddt.system.annotation.AuditLog;
import com.hddt.system.entity.AuditLog as AuditLogEntity;
import com.hddt.system.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {
    
    private final AuditLogService auditLogService;
    
    @Around("@annotation(auditLog)")
    public Object logAudit(ProceedingJoinPoint joinPoint, AuditLog auditLog) 
            throws Throwable {
        
        long startTime = System.currentTimeMillis();
        HttpServletRequest request = getCurrentRequest();
        
        AuditLogEntity log = new AuditLogEntity();
        log.setTraceId(TraceIdUtil.getOrCreate());
        log.setAction(auditLog.action());
        log.setResource(auditLog.resource());
        log.setMethod(request.getMethod());
        log.setRequestUrl(request.getRequestURL().toString());
        log.setIpAddress(getClientIp(request));
        log.setUserAgent(request.getHeader("User-Agent"));
        
        // Get current user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            log.setUsername(auth.getName());
        }
        
        try {
            Object result = joinPoint.proceed();
            
            log.setStatus("SUCCESS");
            log.setResponseStatus(200);
            log.setExecutionTime((int)(System.currentTimeMillis() - startTime));
            
            auditLogService.saveAsync(log);
            
            return result;
            
        } catch (Exception e) {
            log.setStatus("FAILED");
            log.setErrorMessage(e.getMessage());
            log.setExecutionTime((int)(System.currentTimeMillis() - startTime));
            
            auditLogService.saveAsync(log);
            
            throw e;
        }
    }
    
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes.getRequest();
    }
    
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
```

Bạn có muốn tôi tiếp tục với phần còn lại không? (DTO classes, scheduled jobs, Redis caching, etc.)
