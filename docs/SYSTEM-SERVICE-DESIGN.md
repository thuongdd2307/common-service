# Thiết Kế System Service - Authentication & Authorization

## 📋 Tổng Quan

**System Service** là microservice quản lý:
- 🔐 Đăng nhập / Đăng xuất
- 👥 Quản lý người dùng
- 🔑 Phân quyền (Role & Permission)
- 📊 Lịch sử hoạt động (Audit Log)
- 🔄 Refresh Token
- 🚫 Token Blacklist

## 🏗️ Kiến Trúc Hệ Thống

```
┌─────────────────────────────────────────────────────────────┐
│                        API Gateway                           │
│                    (Spring Cloud Gateway)                    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      System Service                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Auth Module  │  │ User Module  │  │ Audit Module │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│  ┌──────────────┐  ┌──────────────┐                        │
│  │ Role Module  │  │ Permission   │                        │
│  └──────────────┘  └──────────────┘                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                         Database                             │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────────┐         │
│  │Users │ │Roles │ │Perms │ │Audit │ │Blacklist │         │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────────┘         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Redis Cache                             │
│  - JWT Token Cache                                           │
│  - Token Blacklist                                           │
│  - User Session                                              │
└─────────────────────────────────────────────────────────────┘
```

## 📊 Database Schema

### 1. Bảng Users (sys_user)

```sql
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    full_name VARCHAR(100),
    phone VARCHAR(20),
    avatar_url VARCHAR(255),
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, LOCKED
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(50),
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_status (status)
);
```

### 2. Bảng Roles (sys_role)

```sql
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    INDEX idx_code (code),
    INDEX idx_status (status)
);
```

### 3. Bảng Permissions (sys_permission)

```sql
CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    resource VARCHAR(100), -- API endpoint hoặc resource
    action VARCHAR(50), -- CREATE, READ, UPDATE, DELETE
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    INDEX idx_code (code),
    INDEX idx_resource (resource)
);
```

### 4. Bảng User-Role (sys_user_role)

```sql
CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
);
```

### 5. Bảng Role-Permission (sys_role_permission)

```sql
CREATE TABLE sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES sys_permission(id) ON DELETE CASCADE,
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    INDEX idx_role_id (role_id),
    INDEX idx_permission_id (permission_id)
);
```

### 6. Bảng Audit Log (sys_audit_log)

```sql
CREATE TABLE sys_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trace_id VARCHAR(100), -- Trace ID từ Common Service
    user_id BIGINT,
    username VARCHAR(50),
    action VARCHAR(100) NOT NULL, -- LOGIN, LOGOUT, CREATE_USER, UPDATE_ROLE, etc.
    resource VARCHAR(100), -- API endpoint hoặc resource
    method VARCHAR(10), -- GET, POST, PUT, DELETE
    request_url TEXT,
    request_params TEXT, -- JSON
    request_body TEXT, -- JSON
    response_status INT,
    response_body TEXT, -- JSON
    ip_address VARCHAR(50),
    user_agent TEXT,
    execution_time INT, -- milliseconds
    status VARCHAR(20), -- SUCCESS, FAILED
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at),
    INDEX idx_trace_id (trace_id)
);
```

### 7. Bảng Token Blacklist (sys_token_blacklist)

```sql
CREATE TABLE sys_token_blacklist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token VARCHAR(500) UNIQUE NOT NULL,
    user_id BIGINT,
    reason VARCHAR(100), -- LOGOUT, FORCE_LOGOUT, EXPIRED
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_token (token),
    INDEX idx_expires_at (expires_at)
);
```

### 8. Bảng Refresh Token (sys_refresh_token)

```sql
CREATE TABLE sys_refresh_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token VARCHAR(500) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    access_token VARCHAR(500),
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at)
);
```

## 🔧 Cấu Trúc Project

```
system-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/hddt/system/
│   │   │       ├── SystemServiceApplication.java
│   │   │       ├── config/
│   │   │       │   ├── SecurityConfig.java
│   │   │       │   ├── RedisConfig.java
│   │   │       │   └── AuditConfig.java
│   │   │       ├── controller/
│   │   │       │   ├── AuthController.java
│   │   │       │   ├── UserController.java
│   │   │       │   ├── RoleController.java
│   │   │       │   ├── PermissionController.java
│   │   │       │   └── AuditLogController.java
│   │   │       ├── service/
│   │   │       │   ├── AuthService.java
│   │   │       │   ├── UserService.java
│   │   │       │   ├── RoleService.java
│   │   │       │   ├── PermissionService.java
│   │   │       │   ├── AuditLogService.java
│   │   │       │   └── TokenBlacklistService.java
│   │   │       ├── repository/
│   │   │       │   ├── UserRepository.java
│   │   │       │   ├── RoleRepository.java
│   │   │       │   ├── PermissionRepository.java
│   │   │       │   ├── AuditLogRepository.java
│   │   │       │   ├── TokenBlacklistRepository.java
│   │   │       │   └── RefreshTokenRepository.java
│   │   │       ├── entity/
│   │   │       │   ├── User.java
│   │   │       │   ├── Role.java
│   │   │       │   ├── Permission.java
│   │   │       │   ├── UserRole.java
│   │   │       │   ├── RolePermission.java
│   │   │       │   ├── AuditLog.java
│   │   │       │   ├── TokenBlacklist.java
│   │   │       │   └── RefreshToken.java
│   │   │       ├── dto/
│   │   │       │   ├── request/
│   │   │       │   │   ├── LoginRequest.java
│   │   │       │   │   ├── RegisterRequest.java
│   │   │       │   │   ├── RefreshTokenRequest.java
│   │   │       │   │   ├── CreateUserRequest.java
│   │   │       │   │   ├── UpdateUserRequest.java
│   │   │       │   │   ├── CreateRoleRequest.java
│   │   │       │   │   └── AssignRoleRequest.java
│   │   │       │   └── response/
│   │   │       │       ├── LoginResponse.java
│   │   │       │       ├── UserResponse.java
│   │   │       │       ├── RoleResponse.java
│   │   │       │       └── AuditLogResponse.java
│   │   │       ├── aspect/
│   │   │       │   └── AuditLogAspect.java
│   │   │       ├── annotation/
│   │   │       │   └── AuditLog.java
│   │   │       └── enums/
│   │   │           ├── UserStatus.java
│   │   │           ├── AuditAction.java
│   │   │           └── TokenRevokeReason.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/
│   │           └── migration/
│   │               ├── V1__create_user_tables.sql
│   │               ├── V2__create_role_tables.sql
│   │               ├── V3__create_audit_tables.sql
│   │               └── V4__insert_initial_data.sql
│   └── test/
└── pom.xml
```

## 📝 API Endpoints

### 1. Authentication APIs

#### POST /api/auth/login
Đăng nhập

**Request:**
```json
{
  "username": "admin",
  "password": "password123"
}
```

**Response:**
```json
{
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com",
      "fullName": "Administrator",
      "roles": ["ADMIN", "USER"]
    }
  },
  "errorCode": null,
  "message": "SUCCESS"
}
```

#### POST /api/auth/logout
Đăng xuất

**Request Header:**
```
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "data": "Đăng xuất thành công",
  "errorCode": null,
  "message": "SUCCESS"
}
```

#### POST /api/auth/refresh
Refresh token

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**
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

#### POST /api/auth/register
Đăng ký tài khoản mới

**Request:**
```json
{
  "username": "newuser",
  "password": "password123",
  "email": "newuser@example.com",
  "fullName": "New User",
  "phone": "0123456789"
}
```

### 2. User Management APIs

#### GET /api/users
Lấy danh sách users (có phân trang)

**Query Params:**
- page: số trang (default: 0)
- size: số record/trang (default: 20)
- sort: sắp xếp (default: id,desc)
- search: tìm kiếm theo username, email, fullName

**Response:**
```json
{
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "data": {
    "content": [
      {
        "id": 1,
        "username": "admin",
        "email": "admin@example.com",
        "fullName": "Administrator",
        "status": "ACTIVE",
        "roles": ["ADMIN"],
        "createdAt": "2026-01-14T10:00:00"
      }
    ],
    "totalElements": 100,
    "totalPages": 5,
    "currentPage": 0,
    "pageSize": 20
  },
  "errorCode": null,
  "message": "SUCCESS"
}
```

#### GET /api/users/{id}
Lấy thông tin user theo ID

#### POST /api/users
Tạo user mới (Yêu cầu role ADMIN)

#### PUT /api/users/{id}
Cập nhật thông tin user

#### DELETE /api/users/{id}
Xóa user (soft delete)

#### POST /api/users/{id}/roles
Gán role cho user

**Request:**
```json
{
  "roleIds": [1, 2, 3]
}
```

#### DELETE /api/users/{id}/roles/{roleId}
Xóa role của user

### 3. Role Management APIs

#### GET /api/roles
Lấy danh sách roles

#### GET /api/roles/{id}
Lấy thông tin role theo ID

#### POST /api/roles
Tạo role mới

**Request:**
```json
{
  "code": "MANAGER",
  "name": "Manager",
  "description": "Manager role",
  "permissionIds": [1, 2, 3]
}
```

#### PUT /api/roles/{id}
Cập nhật role

#### DELETE /api/roles/{id}
Xóa role

#### POST /api/roles/{id}/permissions
Gán permissions cho role

### 4. Permission Management APIs

#### GET /api/permissions
Lấy danh sách permissions

#### GET /api/permissions/{id}
Lấy thông tin permission theo ID

#### POST /api/permissions
Tạo permission mới

**Request:**
```json
{
  "code": "CREATE_USER",
  "name": "Create User",
  "resource": "/api/users",
  "action": "CREATE",
  "description": "Permission to create new user"
}
```

### 5. Audit Log APIs

#### GET /api/audit-logs
Lấy lịch sử hoạt động

**Query Params:**
- userId: lọc theo user
- action: lọc theo action
- startDate: từ ngày
- endDate: đến ngày
- page, size, sort

**Response:**
```json
{
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "data": {
    "content": [
      {
        "id": 1,
        "traceId": "84bc63ad-3ed4-4270-8648-deae7be079f4",
        "userId": 1,
        "username": "admin",
        "action": "LOGIN",
        "resource": "/api/auth/login",
        "method": "POST",
        "ipAddress": "192.168.1.100",
        "status": "SUCCESS",
        "executionTime": 150,
        "createdAt": "2026-01-14T10:00:00"
      }
    ],
    "totalElements": 1000,
    "totalPages": 50,
    "currentPage": 0
  },
  "errorCode": null,
  "message": "SUCCESS"
}
```

#### GET /api/audit-logs/{id}
Lấy chi tiết audit log

#### GET /api/audit-logs/user/{userId}
Lấy lịch sử hoạt động của user

#### GET /api/audit-logs/export
Export audit logs ra Excel/CSV

## 🔐 Security Flow

### Login Flow

```
1. User gửi username/password
2. System validate credentials
3. Check user status (ACTIVE/LOCKED)
4. Check failed login attempts
5. Generate JWT access token & refresh token
6. Save refresh token vào DB
7. Log audit: LOGIN action
8. Return tokens + user info
```

### Logout Flow

```
1. User gửi access token
2. System validate token
3. Add token vào blacklist
4. Revoke refresh token
5. Log audit: LOGOUT action
6. Clear Redis cache
7. Return success
```

### Request Authorization Flow

```
1. Request đến với JWT token
2. JwtAuthenticationFilter validate token
3. Check token trong blacklist
4. Parse user info từ token
5. Load user roles & permissions
6. Check @HasRole hoặc @HasPermission
7. Allow/Deny request
8. Log audit (nếu cần)
```

## 📊 Audit Logging Strategy

### Automatic Audit với AOP

```java
@AuditLog(action = "CREATE_USER", resource = "/api/users")
@PostMapping
public BaseResponse<UserResponse> createUser(@RequestBody CreateUserRequest request) {
    // Business logic
}
```

### Audit Log Aspect sẽ tự động:
- Capture request info (URL, method, params, body)
- Capture user info (từ SecurityContext)
- Capture response info (status, body)
- Calculate execution time
- Save vào database
- Async processing để không ảnh hưởng performance

## 🚀 Performance Optimization

### 1. Redis Caching

```yaml
Cache Strategy:
- User info: TTL 30 minutes
- User roles: TTL 1 hour
- User permissions: TTL 1 hour
- Token validation: TTL = token expiration
- Blacklist: TTL = token expiration
```

### 2. Database Indexing

- Index trên username, email (unique)
- Index trên user_id, role_id trong audit_log
- Index trên created_at trong audit_log
- Composite index cho queries phức tạp

### 3. Async Processing

- Audit logging: async
- Email notification: async
- Token cleanup: scheduled job

## 📈 Monitoring & Metrics

### Key Metrics

- Login success/failure rate
- Average login time
- Active users count
- Token generation rate
- Audit log volume
- Failed authentication attempts

### Alerts

- Too many failed login attempts
- Unusual login patterns
- High error rate
- Database connection issues

Bạn muốn tôi tiếp tục với phần implementation code không?
