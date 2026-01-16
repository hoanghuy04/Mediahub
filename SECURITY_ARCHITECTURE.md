# BondHub Security Architecture Documentation

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Components](#components)
4. [Authentication Flow](#authentication-flow)
5. [Authorization Flow](#authorization-flow)
6. [Security Configuration](#security-configuration)
7. [Public Paths Management](#public-paths-management)
8. [Testing Security](#testing-security)
9. [Best Practices](#best-practices)

---

## Overview

BondHub implements a **centralized security architecture** using JWT (JSON Web Tokens) with gateway-level authentication and service-level authorization. This approach offloads authentication to the API Gateway while enabling fine-grained authorization in individual services.

### Key Features
- ✅ Centralized JWT authentication at API Gateway
- ✅ Stateless authentication (no sessions)
- ✅ Role-based access control (RBAC)
- ✅ Method-level security with `@PreAuthorize`
- ✅ User context propagation via HTTP headers
- ✅ Centralized public paths configuration

---

## Architecture

```
┌─────────┐         ┌─────────────┐         ┌──────────────┐
│ Client  │────────▶│ API Gateway │────────▶│ Auth Service │
└─────────┘         └─────────────┘         └──────────────┘
                          │
                          │ JWT Validation
                          │ + Header Injection
                          ▼
                    ┌──────────────┐
                    │ User Service │
                    │ Other Services│
                    └──────────────┘
                          │
                    SecurityContextFilter
                          │
                    @PreAuthorize
```

### Flow Layers

1. **Gateway Layer**: JWT validation, public path filtering
2. **Service Layer**: Security context creation, authorization
3. **Application Layer**: Method-level security, business logic

---

## Components

### 1. Common Module Components

#### `JwtUtil` (common/utils/JwtUtil.java)
- **Purpose**: JWT token generation, validation, and parsing
- **Key Methods**:
  - `generateAccessToken(userId, email, roles)` - Creates access token
  - `generateRefreshToken(userId)` - Creates refresh token
  - `validateToken(token)` - Validates token signature and expiration
  - `extractUserId(token)`, `extractEmail(token)`, `extractRoles(token)` - Extract claims

#### `JwtProperties` (common/config/JwtProperties.java)
- **Purpose**: JWT configuration binding
- **Properties**:
  - `jwt.secret` - Secret key for signing tokens
  - `jwt.access-token-expiration` - Access token lifetime (default: 1 hour)
  - `jwt.refresh-token-expiration` - Refresh token lifetime (default: 7 days)

#### `Role` (common/enums/Role.java)
- **Purpose**: Type-safe role enumeration
- **Values**: `USER`, `ADMIN`

#### `SecurityPaths` (common/config/SecurityPaths.java)
- **Purpose**: Centralized public paths configuration
- **Lists**:
  - `PUBLIC_PATHS` - Paths for gateway (with `/api` prefix)
  - `SERVICE_PUBLIC_PATHS` - Paths for services (after rewrite)
  - `INTERNAL_PATHS` - Internal-only paths

#### `SecurityContextFilter` (common/security/SecurityContextFilter.java)
- **Purpose**: Extract user info from headers and populate Spring Security context
- **Condition**: Only loads in servlet-based applications
- **Headers Read**: `X-User-Id`, `X-User-Email`, `X-User-Roles`

#### `UserPrincipal` (common/security/UserPrincipal.java)
- **Purpose**: Represents authenticated user in security context
- **Implements**: `UserDetails`
- **Fields**: `userId`, `email`, `authorities`

#### `CommonSecurityConfig` (common/config/CommonSecurityConfig.java)
- **Purpose**: Spring Security configuration for services
- **Features**:
  - Enables method security (`@PreAuthorize`, `@Secured`)
  - Configures stateless session management
  - Permits public paths
  - Adds SecurityContextFilter

---

### 2. API Gateway Components

#### `JwtAuthenticationFilter` (api-gateway/filter/JwtAuthenticationFilter.java)
- **Purpose**: Global filter for JWT validation at gateway level
- **Order**: -100 (runs early in filter chain)
- **Process**:
  1. Check if path is public → skip authentication
  2. Extract JWT from `Authorization: Bearer <token>` header
  3. Validate token
  4. Extract user info (userId, email, roles)
  5. Add headers to downstream request:
     - `X-User-Id`
     - `X-User-Email`
     - `X-User-Roles`
  6. Forward request to service

#### `GatewayConfig` (api-gateway/config/GatewayConfig.java)
- **Purpose**: CORS configuration
- **Features**:
  - Allows configured origins
  - Exposes `Authorization` and custom headers
  - Permits all HTTP methods

---

### 3. Auth Service Components

#### `AuthenticationService` (auth-service/service/AuthenticationService.java)
- **Purpose**: Handle authentication operations
- **Methods**:
  - `login(LoginRequest)` - Authenticate user by phone number
  - `register(RegisterRequest)` - Create new user account
  - `refreshToken(RefreshTokenRequest)` - Generate new access token
  - `validateToken(token)` - Check token validity

#### `AuthController` (auth-service/controller/AuthController.java)
- **Endpoints**:
  - `POST /auth/login` - User login
  - `POST /auth/register` - User registration
  - `POST /auth/refresh` - Token refresh
  - `GET /auth/validate` - Token validation

#### `Account` (auth-service/model/Account.java)
- **Fields**:
  - `id`, `phoneNumber`, `password`, `email`
  - `roles: Set<Role>` - User roles
  - `enabled: Boolean` - Account status

---

## Authentication Flow

### 1. User Registration
```
Client                  Gateway                Auth Service
  │                       │                         │
  │ POST /api/auth/register                        │
  ├──────────────────────▶│                         │
  │                       │ Rewrite: /auth/register │
  │                       ├────────────────────────▶│
  │                       │                         │
  │                       │                    Create Account
  │                       │                    Hash Password
  │                       │                    Set Role: USER
  │                       │                    Generate Tokens
  │                       │                         │
  │                       │     TokenResponse       │
  │                       │◀────────────────────────┤
  │   TokenResponse       │                         │
  │◀──────────────────────┤                         │
  │                       │                         │
```

### 2. User Login
```
Client                  Gateway                Auth Service
  │                       │                         │
  │ POST /api/auth/login                           │
  │ { phoneNumber, password }                      │
  ├──────────────────────▶│                         │
  │                       │ Rewrite: /auth/login    │
  │                       ├────────────────────────▶│
  │                       │                         │
  │                       │                   Find Account
  │                       │                   Verify Password
  │                       │                   Check Status
  │                       │                   Generate Tokens
  │                       │                         │
  │                       │     TokenResponse       │
  │                       │◀────────────────────────┤
  │   TokenResponse       │                         │
  │◀──────────────────────┤                         │
  │                       │                         │
  │ Store tokens          │                         │
  │                       │                         │
```

### 3. Token Structure

**Access Token Claims:**
```json
{
  "userId": "507f1f77bcf86cd799439011",
  "email": "user@example.com",
  "roles": ["USER"],
  "type": "access",
  "iat": 1673456789,
  "exp": 1673460389
}
```

**Refresh Token Claims:**
```json
{
  "type": "refresh",
  "sub": "507f1f77bcf86cd799439011",
  "iat": 1673456789,
  "exp": 1674061589
}
```

---

## Authorization Flow

### Protected Endpoint Request

```
Client          Gateway              Service
  │               │                     │
  │ GET /api/users/profile             │
  │ Authorization: Bearer <JWT>        │
  ├──────────────▶│                     │
  │               │                     │
  │          JWT Filter                │
  │          ├─ Validate Token          │
  │          ├─ Extract Claims          │
  │          └─ Add Headers             │
  │               │                     │
  │               │ GET /users/profile  │
  │               │ X-User-Id: 507...   │
  │               │ X-User-Email: user@ │
  │               │ X-User-Roles: USER  │
  │               ├────────────────────▶│
  │               │                     │
  │               │            SecurityContextFilter
  │               │            ├─ Read Headers
  │               │            ├─ Create UserPrincipal
  │               │            └─ Set SecurityContext
  │               │                     │
  │               │            @PreAuthorize Check
  │               │            ├─ isAuthenticated()
  │               │            └─ hasRole('USER')
  │               │                     │
  │               │              Business Logic
  │               │                     │
  │               │     Response        │
  │               │◀────────────────────┤
  │   Response    │                     │
  │◀──────────────┤                     │
  │               │                     │
```

### @PreAuthorize Examples

```java
// 1. Any authenticated user
@PreAuthorize("isAuthenticated()")
public UserResponse getProfile() { }

// 2. Specific role required
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(String userId) { }

// 3. Multiple roles (OR)
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public void moderateContent() { }

// 4. Path variable check
@PreAuthorize("#userId == authentication.principal.id")
public UserResponse getUser(String userId) { }

// 5. Complex expression
@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
public void updateUser(String userId, UpdateRequest req) { }
```

---

## Security Configuration

### Gateway Configuration (config-server/config/api-gateway.yml)

```yaml
spring:
  cloud:
    gateway:
      routes:
        # Public route - no JWT required
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/auth/**
          filters:
            - RewritePath=/api/auth/(?<segment>.*), /auth/${segment}
        
        # Protected route - JWT required
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
          filters:
            - RewritePath=/api/users/(?<segment>.*), /users/${segment}

jwt:
  secret: ${JWT_SECRET}
  access-token-expiration: 3600000
  refresh-token-expiration: 604800000
```

### Service Configuration (user-service/application.yml)

```yaml
# Enable Security Context Filter
bondhub:
  security:
    gateway-auth:
      enabled: true
```

---

## Public Paths Management

### How It Works

The system maintains two lists of public paths:

1. **Gateway Level** (`PUBLIC_PATHS`):
   - Paths with `/api` prefix as seen by clients
   - Used by `JwtAuthenticationFilter`
   - Example: `/api/auth/login`

2. **Service Level** (`SERVICE_PUBLIC_PATHS`):
   - Paths after gateway rewrite
   - Used by `CommonSecurityConfig`
   - Example: `/auth/login`

### Path Transformation

```
Client Request:     /api/users/test/security/public
                            │
                            ▼
Gateway Filter:     Check PUBLIC_PATHS
                    ✓ Found → Skip JWT validation
                            │
                            ▼
Gateway Rewrite:    /api/users → /users
                            │
                            ▼
Service Receives:   /users/test/security/public
                            │
                            ▼
Service Filter:     Check SERVICE_PUBLIC_PATHS
                    ✓ Found → Permit without authentication
                            │
                            ▼
Controller:         Handle request
```

### Adding New Public Paths

Edit `SecurityPaths.java`:

```java
public static final List<String> PUBLIC_PATHS = List.of(
    "/api/auth/login",
    "/api/your-new-public-endpoint"  // Add gateway-level path
);

public static final List<String> SERVICE_PUBLIC_PATHS = List.of(
    "/auth/login",
    "/your-new-public-endpoint"     // Add service-level path
);
```

---

## Testing Security

### Test Endpoints

The system includes comprehensive security test endpoints in user-service:

| Endpoint | Auth Required | Role | Description |
|----------|--------------|------|-------------|
| `GET /api/users/test/security/public` | ❌ | - | Public endpoint |
| `GET /api/users/test/security/authenticated` | ✅ | Any | Any authenticated user |
| `GET /api/users/test/security/admin-only` | ✅ | ADMIN | Admin only |
| `GET /api/users/test/security/user-or-admin` | ✅ | USER/ADMIN | Either role |
| `GET /api/users/test/security/whoami` | ✅ | Any | Current user info |
| `GET /api/users/test/security/users/{id}/profile` | ✅ | Own ID | Path variable check |
| `DELETE /api/users/test/security/users/{id}` | ✅ | ADMIN or Own | Complex auth |
| `GET /api/users/test/security/headers` | ✅ | Any | View headers |

### Postman Collection

Use `BondHub_Auth_API.postman_collection.json` for testing:

1. **Run Registration/Login** → Tokens auto-saved
2. **Test Authenticated Endpoints** → Uses saved token
3. **Test Role-Based Access** → Create admin user for admin tests
4. **Test Public Endpoints** → Remove Authorization header

---

## Best Practices

### 1. JWT Secret Management

**❌ Don't:**
```yaml
jwt:
  secret: "my-secret-key"  # Hardcoded in config
```

**✅ Do:**
```yaml
jwt:
  secret: ${JWT_SECRET}  # From environment variable
```

Set via:
- Environment variable: `export JWT_SECRET=<secure-random-string>`
- Config server encrypted properties
- Vault/Secrets manager

### 2. Token Expiration

- **Access Token**: Short-lived (1 hour recommended)
- **Refresh Token**: Longer (7 days - 30 days)
- Never store tokens in localStorage (XSS risk)
- Use httpOnly cookies or secure storage

### 3. Password Security

- **Always hash passwords** with BCrypt
- **Minimum password strength** requirements
- Consider password rotation policies
- Never log or transmit passwords

### 4. Role Management

```java
// ✅ Use enum for type safety
Set<Role> roles = Set.of(Role.USER);

// ❌ Avoid string literals
Set<String> roles = Set.of("USER");  // Type-unsafe
```

### 5. Method Security

```java
// ✅ Specific and clear
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(String id) { }

// ❌ Too permissive
@PreAuthorize("isAuthenticated()")
public void deleteUser(String id) { }  // Any user can delete!
```

### 6. Error Handling

- **Don't leak information** in error messages
- Return generic "Unauthorized" or "Forbidden"
- Log detailed errors server-side
- Use proper HTTP status codes (401, 403)

### 7. CORS Configuration

```yaml
cors:
  allowed-origins: 
    - https://app.bondhub.com      # ✅ Specific origins
    # - "*"                         # ❌ Too permissive
```

### 8. HTTPS in Production

- **Always use HTTPS** in production
- Configure SSL/TLS at gateway level
- Redirect HTTP to HTTPS
- Use HSTS headers

---

## Security Checklist

### Development
- [ ] JWT secret is externalized
- [ ] Passwords are hashed with BCrypt
- [ ] Public paths are properly configured
- [ ] @PreAuthorize annotations are in place
- [ ] SecurityContextFilter is enabled for services

### Testing
- [ ] Test with valid tokens
- [ ] Test with expired tokens
- [ ] Test with invalid tokens
- [ ] Test without tokens
- [ ] Test role-based access
- [ ] Test public endpoints

### Production
- [ ] HTTPS enabled
- [ ] Strong JWT secret (256+ bits)
- [ ] Short token expiration
- [ ] Rate limiting configured
- [ ] Intrusion detection enabled
- [ ] Audit logging in place
- [ ] Secrets stored securely
- [ ] CORS properly restricted

---

## Troubleshooting

### Common Issues

**1. 401 Unauthorized on public endpoints**
- Check if path is in both `PUBLIC_PATHS` and `SERVICE_PUBLIC_PATHS`
- Verify gateway rewrite rules match service paths

**2. 403 Forbidden for authenticated users**
- Check `@PreAuthorize` expression
- Verify user has required role
- Check SecurityContextFilter is enabled

**3. SecurityContext is empty**
- Verify headers are being set by gateway
- Check SecurityContextFilter is loaded
- Ensure `bondhub.security.gateway-auth.enabled=true`

**4. Token validation fails**
- Check JWT secret matches across services
- Verify token hasn't expired
- Check token format (Bearer <token>)

---

## Summary

The BondHub security architecture provides:

1. **Centralized Authentication**: Gateway validates JWT once
2. **Distributed Authorization**: Services enforce their own rules
3. **Stateless Design**: No session storage required
4. **Type Safety**: Enum-based roles, UserPrincipal
5. **Flexibility**: Easy to add new public paths or roles
6. **Testability**: Comprehensive test endpoints included

This design balances security, performance, and developer experience while maintaining scalability for microservices.
