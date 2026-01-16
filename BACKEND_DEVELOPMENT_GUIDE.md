# Backend Development Guide - BondHub API

## Table of Contents
1. [Overview](#overview)
2. [Technology Stack](#technology-stack)
3. [Project Structure](#project-structure)
4. [Architecture & Design Patterns](#architecture--design-patterns)
5. [Package Organization](#package-organization)
6. [Coding Standards](#coding-standards)
7. [Common Module Usage](#common-module-usage)
8. [Error Handling](#error-handling)
9. [Logging Practices](#logging-practices)
10. [Database Conventions](#database-conventions)
11. [API Design Guidelines](#api-design-guidelines)
12. [Development Workflow](#development-workflow)

---

## Overview

This guide establishes the architectural patterns, coding standards, and best practices for the BondHub backend microservices. All developers must follow these guidelines to ensure consistency, maintainability, and scalability across the application.

**Based on:** `auth-service` reference implementation

---

## Technology Stack

### Core Technologies
- **Java**: Version 21
- **Spring Boot**: 3.5.9
- **Spring Cloud**: 2025.0.1
- **Maven**: 3.9.8+

### Key Libraries
- **Lombok**: 1.18.36 - Reduces boilerplate code
- **MapStruct**: 1.6.3 - Type-safe bean mapping
- **MongoDB**: NoSQL database
- **Spring Security**: Authentication & authorization
- **SpringDoc OpenAPI**: API documentation (2.8.13)
- **SLF4J + Logback**: Logging framework

### Microservices Infrastructure
- **Spring Cloud Config**: Centralized configuration management
- **Netflix Eureka**: Service discovery and registration
- **Spring Cloud Gateway**: API Gateway with routing and filtering
- **Resilience4j**: Circuit breaker, rate limiting, retry patterns
- **Spring Cloud LoadBalancer**: Client-side load balancing

---

## Microservices Architecture

### Overview

BondHub follows a **microservices architecture** with the following infrastructure components:

```
┌─────────────────┐
│   API Gateway   │ ← Single entry point (Port 8080)
└────────┬────────┘
         │
    ┌────┴────────────────────┐
    │                         │
┌───┴──────────┐    ┌─────────┴──────┐
│ Discovery    │    │  Config Server │
│ Server       │    │  (Port 8888)   │
│ (Eureka)     │    └────────────────┘
│ (Port 8761)  │
└───┬──────────┘
    │
    │  (Service Registration & Discovery)
    │
    ├─────────────┬──────────────┬──────────────┬──────────────┐
    │             │              │              │              │
┌───┴──────┐ ┌───┴──────┐ ┌────┴─────┐ ┌──────┴──────┐ ┌─────┴──────┐
│  Auth    │ │  User    │ │ Message  │ │Notification │ │   Other    │
│ Service  │ │ Service  │ │ Service  │ │  Service    │ │  Services  │
└──────────┘ └──────────┘ └──────────┘ └─────────────┘ └────────────┘
```

### Core Infrastructure Components

#### 1. Discovery Server (Eureka)

**Purpose**: Service registry for dynamic service discovery

**Configuration**:
```java
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
```

**Key Features**:
- Automatic service registration
- Health monitoring
- Load balancing support
- Fault tolerance

**Port**: 8761  
**URL**: http://localhost:8761

#### 2. Config Server

**Purpose**: Centralized configuration management across all microservices

**Configuration**:
```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

**Structure**:
```
config-server/
└── src/main/resources/
    └── config/
        ├── api-gateway.yml
        ├── auth-service.yml
        ├── user-service.yml
        ├── message-service.yml
        └── notification-service.yml
```

**Port**: 8888  
**Configuration Source**: Local filesystem (`classpath:/config/`)

#### 3. API Gateway

**Purpose**: Single entry point for all client requests with routing, filtering, and cross-cutting concerns

**Configuration**:
```java
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
```

**Features**:
- Dynamic routing to microservices
- Load balancing
- Circuit breaker integration
- Request/response filtering
- Authentication/Authorization
- Rate limiting

**Port**: 8080  
**Gateway Routes**: Configured via Config Server

---

## Service Configuration Standards

### 1. Application Configuration (application.yml)

Every microservice must have minimal local configuration:

```yaml
spring:
  application:
    name: service-name  # Must match config file name
  config:
    import: optional:configserver:http://localhost:8888
  cloud:
    config:
      uri: http://localhost:8888
      fail-fast: true
      retry:
        max-attempts: 6
        initial-interval: 1000
        max-interval: 2000
        multiplier: 1.1
```

### 2. Centralized Configuration (Config Server)

Service-specific configuration in `config-server/src/main/resources/config/{service-name}.yml`:

```yaml
server:
  port: 8081  # Unique port for each service

spring:
  application:
    name: auth-service
  
  # Database configuration
  data:
    mongodb:
      host: localhost
      port: 27017
      database: bondhub_auth
      authentication-database: admin
      username: ${MONGO_USERNAME:admin}
      password: ${MONGO_PASSWORD:123456}

# Eureka Client Configuration
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
    hostname: localhost

# API Documentation
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true

# Actuator endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

### 3. Service Ports Convention

| Service | Port | Description |
|---------|------|-------------|
| **Discovery Server** | 8761 | Eureka service registry |
| **Config Server** | 8888 | Centralized configuration |
| **API Gateway** | 8080 | Main entry point |
| **Auth Service** | 8081 | Authentication & authorization |
| **User Service** | 8082 | User management |
| **Message Service** | 8083 | Messaging functionality |
| **Notification Service** | 8084 | Notifications |

> **Rule**: Services start from port 8081 and increment by 1

---

## Service Discovery & Registration

### Registering a Service with Eureka

#### Step 1: Add Eureka Client Dependency

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

#### Step 2: Enable Discovery Client (Optional - Auto-enabled)

```java
@SpringBootApplication
public class YourServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourServiceApplication.class, args);
    }
}
```

#### Step 3: Configure Eureka Client

In Config Server (`config/{service-name}.yml`):

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true
    hostname: localhost
    lease-renewal-interval-in-seconds: 10
    lease-expiration-duration-in-seconds: 20
```

### Inter-Service Communication with OpenFeign

BondHub uses **OpenFeign** for inter-service communication. Feign is a declarative HTTP client that makes calling other microservices easy and type-safe.

#### Step 1: Add OpenFeign Dependency

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

#### Step 2: Enable Feign Clients

Add `@EnableFeignClients` to your main application class:

```java
@SpringBootApplication
@EnableFeignClients
public class YourServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourServiceApplication.class, args);
    }
}
```

#### Step 3: Create Feign Client Interface

Create a Feign client interface in a `client` package:

```java
package com.bondhub.messageservice.client;

import com.bondhub.common.dto.ApiResponse;
import com.bondhub.messageservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "user-service",           // Service name in Eureka
    path = "/users",                 // Base path for this service
    fallback = UserServiceFallback.class  // Fallback implementation
)
public interface UserServiceClient {
    
    @GetMapping("/{id}")
    ApiResponse<UserDTO> getUserById(@PathVariable("id") String id);
    
    @GetMapping("/email/{email}")
    ApiResponse<UserDTO> getUserByEmail(@PathVariable("email") String email);
}
```

#### Step 4: Implement Fallback (Optional but Recommended)

Create a fallback class for resilience:

```java
package com.bondhub.messageservice.client;

import com.bondhub.common.dto.ApiResponse;
import com.bondhub.messageservice.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserServiceFallback implements UserServiceClient {
    
    @Override
    public ApiResponse<UserDTO> getUserById(String id) {
        log.warn("Fallback: getUserById called for id: {}", id);
        UserDTO fallbackUser = UserDTO.builder()
            .id(id)
            .name("Unknown User")
            .build();
        return ApiResponse.success(fallbackUser);
    }
    
    @Override
    public ApiResponse<UserDTO> getUserByEmail(String email) {
        log.warn("Fallback: getUserByEmail called for email: {}", email);
        return ApiResponse.success(null);
    }
}
```

#### Step 5: Use Feign Client in Service

Inject and use the Feign client in your service:

```java
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MessageServiceImpl implements MessageService {
    
    UserServiceClient userServiceClient;  // Feign client injected automatically
    MessageRepository messageRepository;
    
    public ApiResponse<MessageResponse> sendMessage(MessageCreateRequest request) {
        log.info("Sending message from user: {}", request.senderId());
        
        // Call user-service via Feign
        ApiResponse<UserDTO> senderResponse = userServiceClient.getUserById(request.senderId());
        
        if (senderResponse.code() != 1000) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        
        // Business logic continues...
        Message message = messageRepository.save(newMessage);
        return ApiResponse.success(messageMapper.toResponse(message));
    }
}
```

### OpenFeign Configuration

Configure Feign in your service's config file:

```yaml
# In config-server/src/main/resources/config/your-service.yml
feign:
  client:
    config:
      default:
        connect-timeout: 5000
        read-timeout: 5000
        logger-level: BASIC
      user-service:
        connect-timeout: 3000
        read-timeout: 3000
        logger-level: FULL
  circuitbreaker:
    enabled: true
    alphanumeric-ids:
      enabled: true
  compression:
    request:
      enabled: true
      mime-types: application/json
      min-request-size: 2048
    response:
      enabled: true

# Enable logging for Feign
logging:
  level:
    com.bondhub.yourservice.client: DEBUG
```

### Package Structure for Feign Clients

Add a `client` package to your service structure:

```
your-service/
├── src/main/java/com/bondhub/yourservice/
│   ├── client/                     # Feign clients ⭐ NEW
│   │   ├── UserServiceClient.java
│   │   ├── UserServiceFallback.java
│   │   ├── AuthServiceClient.java
│   │   └── AuthServiceFallback.java
│   ├── config/
│   ├── controller/
│   ├── dto/
│   ├── mapper/
│   ├── model/
│   ├── repository/
│   └── service/
```

### Alternative: RestTemplate (Not Recommended)

If you need to use RestTemplate for specific cases:

```java
@Configuration
public class RestTemplateConfig {
    
    @Bean
    @LoadBalanced  // Enable client-side load balancing
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

**Usage**:
```java
@Service
@RequiredArgsConstructor
public class UserServiceClient {
    
    private final RestTemplate restTemplate;
    
    public UserDTO getUserById(String userId) {
        // Use service name instead of host:port
        String url = "http://user-service/api/users/" + userId;
        return restTemplate.getForObject(url, UserDTO.class);
    }
}
```

### Alternative: WebClient (For Reactive Services)

For reactive microservices using WebFlux:

```java
@Configuration
public class WebClientConfig {
    
    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
```

**Usage**:
```java
@Service
@RequiredArgsConstructor
public class UserServiceClient {
    
    private final WebClient.Builder webClientBuilder;
    
    public Mono<UserDTO> getUserById(String userId) {
        return webClientBuilder.build()
            .get()
            .uri("http://user-service/api/users/{id}", userId)
            .retrieve()
            .bodyToMono(UserDTO.class);
    }
}
```


---

## API Gateway Routing

### Route Configuration

Routes are configured in `config-server/src/main/resources/config/api-gateway.yml`:

```yaml
spring:
  cloud:
    gateway:
      routes:
        # Auth Service Routes
        - id: auth-service
          uri: lb://auth-service  # lb = load balanced
          predicates:
            - Path=/api/auth/**
          filters:
            - RewritePath=/api/auth/(?<segment>.*), /${segment}
            
        # User Service Routes
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
          filters:
            - RewritePath=/api/users/(?<segment>.*), /${segment}
            
        # Message Service Routes
        - id: message-service
          uri: lb://message-service
          predicates:
            - Path=/api/messages/**
          filters:
            - RewritePath=/api/messages/(?<segment>.*), /${segment}
      
      # Global CORS configuration
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origins: "*"
            allowed-methods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowed-headers: "*"
            allow-credentials: true
```

### Gateway Filters

#### Built-in Filters
- **RewritePath**: Modify request path before forwarding
- **AddRequestHeader**: Add headers to requests
- **AddResponseHeader**: Add headers to responses
- **CircuitBreaker**: Integrate with Resilience4j
- **RateLimiter**: Apply rate limiting

#### Custom Global Filter Example

```java
@Component
@Slf4j
public class LoggingGlobalFilter implements GlobalFilter, Ordered {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("Request: {} {}", 
            exchange.getRequest().getMethod(), 
            exchange.getRequest().getURI());
        
        return chain.filter(exchange)
            .then(Mono.fromRunnable(() -> {
                log.info("Response: {}", 
                    exchange.getResponse().getStatusCode());
            }));
    }
    
    @Override
    public int getOrder() {
        return -1; // High priority
    }
}
```

---

## Resilience Patterns

### Circuit Breaker with Resilience4j

#### Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      user-service:
        register-health-indicator: true
        sliding-window-size: 10
        minimum-number-of-calls: 5
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
        wait-duration-in-open-state: 10s
        failure-rate-threshold: 50
        event-consumer-buffer-size: 10
        
  retry:
    instances:
      user-service:
        max-attempts: 3
        wait-duration: 1000ms
        
  timelimiter:
    instances:
      user-service:
        timeout-duration: 5s
```

#### Usage

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {
    
    private final RestTemplate restTemplate;
    private final CircuitBreakerFactory circuitBreakerFactory;
    
    public UserDTO getUserById(String userId) {
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("user-service");
        
        return circuitBreaker.run(
            () -> restTemplate.getForObject(
                "http://user-service/api/users/" + userId, 
                UserDTO.class
            ),
            throwable -> getFallbackUser(userId)  // Fallback
        );
    }
    
    private UserDTO getFallbackUser(String userId) {
        log.warn("Fallback triggered for user: {}", userId);
        return UserDTO.builder()
            .id(userId)
            .name("Unknown User")
            .build();
    }
}
```

---

## Inter-Service Communication

### Best Practices with OpenFeign

#### 1. Use Service Names in @FeignClient

❌ **BAD**:
```java
@FeignClient(name = "user-service", url = "http://localhost:8082")
public interface UserServiceClient {
    // ...
}
```

✅ **GOOD**:
```java
@FeignClient(
    name = "user-service",       // Service discovery via Eureka
    path = "/users",             // Base path
    fallback = UserServiceFallback.class
)
public interface UserServiceClient {
    // ...
}
```

#### 2. Always Implement Fallbacks

Every Feign client should have a fallback for resilience:

```java
@FeignClient(
    name = "user-service",
    path = "/users",
    fallback = UserServiceFallback.class
)
public interface UserServiceClient {
    @GetMapping("/{id}")
    ApiResponse<UserDTO> getUserById(@PathVariable("id") String id);
}

@Component
@Slf4j
public class UserServiceFallback implements UserServiceClient {
    @Override
    public ApiResponse<UserDTO> getUserById(String id) {
        log.warn("Fallback triggered for getUserById: {}", id);
        return ApiResponse.error(5001, "User service unavailable", null);
    }
}
```

#### 3. Use Proper Path Mappings

Match the target service's controller mapping exactly:

```java
// If UserController has @RequestMapping("/users")
@FeignClient(
    name = "user-service",
    path = "/users"  // Match the controller path
)
public interface UserServiceClient {
    
    @GetMapping("/{id}")  // Calls: GET /users/{id}
    ApiResponse<UserDTO> getUserById(@PathVariable String id);
    
    @GetMapping("/email/{email}")  // Calls: GET /users/email/{email}
    ApiResponse<UserDTO> getUserByEmail(@PathVariable String email);
}
```

#### 4. Handle ApiResponse Wrapper

Since all services return `ApiResponse<T>`, handle it properly:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageServiceImpl implements MessageService {
    
    UserServiceClient userServiceClient;
    
    public void validateUser(String userId) {
        ApiResponse<UserDTO> response = userServiceClient.getUserById(userId);
        
        // Check response code
        if (response.code() != 1000) {
            log.error("User validation failed: {}", response.message());
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        
        UserDTO user = response.data();
        // Use user data...
    }
}
```

#### 5. Configure Timeouts and Retries

```yaml
feign:
  client:
    config:
      default:
        connect-timeout: 5000
        read-timeout: 5000
      user-service:
        connect-timeout: 3000
        read-timeout: 3000
        
resilience4j:
  retry:
    instances:
      user-service:
        max-attempts: 3
        wait-duration: 500ms
        retry-exceptions:
          - feign.FeignException
```

#### 6. Use Appropriate HTTP Methods

```java
@FeignClient(name = "user-service", path = "/users")
public interface UserServiceClient {
    
    @PostMapping
    ApiResponse<UserResponse> createUser(@RequestBody UserCreateRequest request);
    
    @GetMapping("/{id}")
    ApiResponse<UserResponse> getUserById(@PathVariable String id);
    
    @PutMapping("/{id}")
    ApiResponse<UserResponse> updateUser(
        @PathVariable String id,
        @RequestBody UserUpdateRequest request
    );
    
    @DeleteMapping("/{id}")
    ApiResponse<Void> deleteUser(@PathVariable String id);
}
```

### Communication Patterns

| Pattern | Use Case | Implementation | Example |
|---------|----------|----------------|---------|
| **Synchronous (Feign)** | Immediate response needed | OpenFeign client | Get user details during message creation |
| **Async Messaging** | Fire-and-forget operations | Message queue (future) | Send email notification |
| **Event-Driven** | Multiple services need to react | Event bus (future) | User registration event |

### Error Handling in Feign Clients

#### Custom Error Decoder

```java
@Configuration
public class FeignConfig {
    
    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }
}

public class CustomErrorDecoder implements ErrorDecoder {
    
    private final ErrorDecoder defaultDecoder = new Default();
    
    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) {
            return new AppException(ErrorCode.USER_NOT_FOUND);
        }
        if (response.status() == 503) {
            return new AppException(ErrorCode.SERVICE_UNAVAILABLE);
        }
        return defaultDecoder.decode(methodKey, response);
    }
}
```

### Logging Feign Requests

Enable detailed logging for debugging:

```yaml
logging:
  level:
    com.bondhub.yourservice.client: DEBUG

feign:
  client:
    config:
      default:
        logger-level: FULL  # NONE, BASIC, HEADERS, FULL
```

**Logger levels**:
- **NONE**: No logging
- **BASIC**: Log request method, URL, response status, and execution time
- **HEADERS**: Log basic info plus request and response headers
- **FULL**: Log headers, body, and metadata for both request and response


---

## Environment Configuration

### Development Environment

**Start Order**:
1. **Config Server** (Port 8888)
2. **Discovery Server** (Port 8761)
3. **API Gateway** (Port 8080)
4. **Microservices** (Ports 8081+)

**Quick Start Script** (PowerShell):
```powershell
# Set Java 21
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"

# Start infrastructure in order
cd config-server
Start-Process mvn "spring-boot:run"

Start-Sleep 10
cd ../discovery-server
Start-Process mvn "spring-boot:run"

Start-Sleep 15
cd ../api-gateway
Start-Process mvn "spring-boot:run"

# Start services
cd ../auth-service
Start-Process mvn "spring-boot:run"

cd ../user-service
Start-Process mvn "spring-boot:run"
```

### Environment Variables

Use environment variables for sensitive configuration:

```yaml
spring:
  data:
    mongodb:
      username: ${MONGO_USERNAME:admin}
      password: ${MONGO_PASSWORD:default}
      
jwt:
  secret: ${JWT_SECRET:your-secret-key}
```

---

## Monitoring & Health Checks

### Actuator Endpoints

Enable actuators in every service:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env
  endpoint:
    health:
      show-details: always
  health:
    circuitbreakers:
      enabled: true
```

### Health Check Endpoints

| Service | Health Endpoint |
|---------|----------------|
| Discovery Server | http://localhost:8761/actuator/health |
| Config Server | http://localhost:8888/actuator/health |
| API Gateway | http://localhost:8080/actuator/health |
| Auth Service | http://localhost:8081/actuator/health |

### Eureka Dashboard

Access the Eureka dashboard to view registered services:
- **URL**: http://localhost:8761
- Shows all registered services, health status, and instances

---

## Project Structure

### Module Organization

```
backend/
├── common/                    # Shared utilities and components
├── api-gateway/              # API Gateway service
├── discovery-server/         # Eureka service registry
├── config-server/           # Centralized configuration
├── auth-service/            # Authentication service ⭐ REFERENCE
├── user-service/            # User management service
├── message-service/         # Messaging service
├── notification-service/    # Notification service
└── pom.xml                  # Parent POM
```

### Service Structure (using auth-service as example)

```
auth-service/
├── src/main/java/com/bondhub/authservice/
│   ├── client/                    # Feign clients (if calling other services)
│   │   ├── UserServiceClient.java
│   │   └── UserServiceFallback.java
│   ├── config/                    # Configuration classes
│   │   └── SecurityConfig.java
│   ├── controller/                # REST Controllers
│   │   └── AccountController.java
│   ├── dto/                       # Data Transfer Objects
│   │   ├── account/
│   │   │   ├── request/          # Request DTOs
│   │   │   │   ├── AccountCreateRequest.java
│   │   │   │   └── AccountUpdateRequest.java
│   │   │   └── response/         # Response DTOs
│   │   │       └── AccountResponse.java
│   ├── mapper/                    # MapStruct mappers
│   │   └── AccountMapper.java
│   ├── model/                     # Domain entities
│   │   └── Account.java
│   ├── repository/                # Data access layer
│   │   └── AccountRepository.java
│   ├── service/                   # Business logic layer
│   │   └── account/
│   │       ├── AccountService.java      # Interface
│   │       └── AccountServiceImpl.java  # Implementation
│   └── AuthServiceApplication.java  # Main application class
├── src/main/resources/
│   └── application.yml           # Application configuration
└── pom.xml
```

---

## Architecture & Design Patterns

### Layered Architecture

We follow a **strict 4-layer architecture**:

```
Controller Layer (REST API)
    ↓
Service Layer (Business Logic)
    ↓
Repository Layer (Data Access)
    ↓
Database
```

#### Layer Responsibilities

| Layer | Responsibility | Example |
|-------|---------------|---------|
| **Controller** | Handle HTTP requests/responses, validation | `AccountController` |
| **Service** | Business logic, orchestration, transactions | `AccountServiceImpl` |
| **Repository** | Database operations | `AccountRepository` |
| **Model** | Domain entities | `Account` |

### Design Patterns Used

1. **Interface-Implementation Pattern**: All services must have an interface
2. **Repository Pattern**: Spring Data repositories for data access
3. **DTO Pattern**: Separate request/response objects from domain models
4. **Mapper Pattern**: Use MapStruct for entity-DTO conversion
5. **Dependency Injection**: Constructor-based injection with Lombok

---

## Package Organization

### Package Naming Convention

```
com.bondhub.<service-name>.<package>
```

### Required Packages for Each Service

| Package | Purpose | Mandatory |
|---------|---------|-----------|
| `client` | Feign client interfaces and fallbacks | ✅ |
| `config` | Configuration classes (Security, Swagger, etc.) | ✅ |
| `controller` | REST controllers | ✅ |
| `dto` | Data transfer objects (request/response) | ✅ |
| `mapper` | MapStruct mappers | ✅ |
| `model` | Domain entities | ✅ |
| `repository` | Data repositories | ✅ |
| `service` | Business logic (interface + impl) | ✅ |
| `exception` | Custom exceptions (if needed) | ⚠️ Optional |

---

## Coding Standards

### 1. Class-Level Annotations

#### Controllers
```java
@RestController
@RequestMapping("/resource-name")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ResourceController {
    // ...
}
```

#### Services
```java
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ResourceServiceImpl implements ResourceService {
    // ...
}
```

#### Entities (MongoDB)
```java
@Document("collection_name")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Resource extends BaseModel {
    @EqualsAndHashCode.Include
    @MongoId
    String id;
    // fields...
}
```

#### Repositories
```java
@Repository
public interface ResourceRepository extends MongoRepository<Resource, String> {
    // Custom query methods
}
```

#### Mappers (MapStruct)
```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ResourceMapper {
    ResourceResponse toResponse(Resource entity);
    Resource toEntity(ResourceCreateRequest request);
    
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(@MappingTarget Resource entity, ResourceUpdateRequest request);
}
```

### 2. Naming Conventions

| Component | Convention | Example |
|-----------|-----------|---------|
| **Controller** | `<Entity>Controller` | `AccountController` |
| **Service Interface** | `<Entity>Service` | `AccountService` |
| **Service Impl** | `<Entity>ServiceImpl` | `AccountServiceImpl` |
| **Repository** | `<Entity>Repository` | `AccountRepository` |
| **Entity/Model** | `<EntityName>` | `Account` |
| **DTO Request** | `<Entity><Action>Request` | `AccountCreateRequest` |
| **DTO Response** | `<Entity>Response` | `AccountResponse` |
| **Mapper** | `<Entity>Mapper` | `AccountMapper` |

### 3. Method Naming

| Operation | Pattern | Example |
|-----------|---------|---------|
| Create | `create<Entity>` | `createAccount` |
| Read One | `get<Entity>By<Field>` | `getAccountById` |
| Read All | `getAll<Entities>` | `getAllAccounts` |
| Update | `update<Entity>` | `updateAccount` |
| Delete | `delete<Entity>` | `deleteAccount` |
| Exists | `existsBy<Field>` | `existsByEmail` |

### 4. DTOs (Data Transfer Objects)

#### Request DTOs
Use **Java Records** for immutability:

```java
public record AccountCreateRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password,
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
    String phoneNumber
) {
}
```

#### Response DTOs
Also use **Java Records**:

```java
public record AccountResponse(
    String id,
    String email,
    String phoneNumber,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    // Do NOT include sensitive data like passwords
}
```

### 5. Dependency Injection

**Always use constructor injection** with `@RequiredArgsConstructor`:

```java
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountServiceImpl implements AccountService {
    
    // Lombok generates constructor automatically
    AccountRepository accountRepository;
    AccountMapper accountMapper;
    PasswordEncoder passwordEncoder;
    
    // No need for @Autowired or manual constructors
}
```

---

## Common Module Usage

### ApiResponse Standard

**All API responses MUST use the common `ApiResponse` wrapper**:

```java
public record ApiResponse<T>(
    int code, 
    String message, 
    T data, 
    Map<String, String> errors
) {
    // Factory methods
    public static <T> ApiResponse<T> success(T data);
    public static <T> ApiResponse<T> successWithoutData();
    public static <T> ApiResponse<T> error(int code, String message, Map<String, String> errors);
}
```

#### Usage Examples

**Success with data:**
```java
return ApiResponse.success(accountMapper.toResponse(savedAccount));
```

**Success without data:**
```java
return ApiResponse.successWithoutData();
```

**Error response:**
```java
return ApiResponse.error(2003, "Account not found", null);
```

### Standard HTTP Status Codes

| Operation | Success Code | Error Codes |
|-----------|-------------|-------------|
| CREATE | 201 CREATED | 400 BAD_REQUEST, 409 CONFLICT |
| GET | 200 OK | 404 NOT_FOUND |
| UPDATE | 200 OK | 400 BAD_REQUEST, 404 NOT_FOUND |
| DELETE | 200 OK | 404 NOT_FOUND |

---

## Error Handling

### 1. Using ErrorCode Enum

All errors must be defined in the **common module's `ErrorCode` enum**:

```java
@Getter
public enum ErrorCode {
    // System errors (9xxx)
    SYS_UNCATEGORIZED(HttpStatus.INTERNAL_SERVER_ERROR, 9999, "Unknown system error occurred"),
    
    // Authentication errors (1xxx)
    AUTH_UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, 1001, "Authentication failed"),
    AUTH_UNAUTHORIZED(HttpStatus.FORBIDDEN, 1002, "Insufficient permissions"),
    
    // Account errors (2xxx)
    ACC_EMAIL_ALREADY_USED(HttpStatus.CONFLICT, 2001, "Email already linked with another account"),
    ACC_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, 2003, "User account not found"),
    
    // Add service-specific errors here
    ;
    
    private final HttpStatus httpStatus;
    private final int code;
    private final String message;
}
```

### 2. Error Code Ranges

| Range | Category | Example |
|-------|----------|---------|
| 1xxx | Authentication/Authorization | `AUTH_UNAUTHENTICATED` |
| 2xxx | User/Account errors | `ACC_EMAIL_ALREADY_USED` |
| 21xx | Role/Permission errors | `ROLE_NOT_FOUND` |
| 22xx | Validation errors | `VALIDATION_ERROR` |
| 9xxx | System errors | `SYS_UNCATEGORIZED` |

### 3. Throwing Exceptions

```java
if (accountRepository.existsByEmail(email)) {
    throw new AppException(ErrorCode.ACC_EMAIL_ALREADY_USED);
}
```

### 4. Validation

Use **Bean Validation (JSR-380)** annotations:

```java
@PostMapping
public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
    @Valid @RequestBody AccountCreateRequest request) {
    // ...
}
```

---

## Logging Practices

### 1. Logging Levels

| Level | Usage | Example |
|-------|-------|---------|
| **INFO** | Normal operations | `log.info("Creating account with email: {}", email)` |
| **WARN** | Expected but unusual | `log.warn("Account not found: {}", id)` |
| **ERROR** | Unexpected errors | `log.error("Failed to save account", exception)` |
| **DEBUG** | Detailed debugging | `log.debug("Account data: {}", account)` |

### 2. Logging Standards

#### Service Layer
```java
@Override
public ApiResponse<AccountResponse> createAccount(AccountCreateRequest request) {
    log.info("Creating new account with email: {}", request.email());
    
    // Business logic...
    
    log.info("Account created successfully with id: {}", savedAccount.getId());
    return ApiResponse.success(accountMapper.toResponse(savedAccount));
}
```

#### Controller Layer
```java
@PostMapping
public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
        @Valid @RequestBody AccountCreateRequest request) {
    log.info("REST request to create account with email: {}", request.email());
    ApiResponse<AccountResponse> response = accountService.createAccount(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

### 3. What to Log

✅ **DO LOG:**
- Method entry with key parameters
- Successful operations
- Warnings for expected error conditions
- Errors with stack traces

❌ **DON'T LOG:**
- Sensitive data (passwords, tokens, etc.)
- Complete request/response bodies
- Inside loops (use summary logging)

---

## Database Conventions

### 1. MongoDB Entity Pattern

```java
@Document("collection_name")  // Use lowercase, plural
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Entity extends BaseModel {
    
    @EqualsAndHashCode.Include
    @MongoId
    String id;
    
    // Other fields...
}
```

### 2. Collection Naming

- Use **lowercase**
- Use **plural** form
- Examples: `accounts`, `users`, `roles`

### 3. Repository Query Methods

Follow Spring Data naming conventions:

```java
@Repository
public interface AccountRepository extends MongoRepository<Account, String> {
    
    // Find operations
    Optional<Account> findByEmail(String email);
    Optional<Account> findByPhoneNumber(String phoneNumber);
    List<Account> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // Existence checks
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    
    // Count operations
    long countByRole(String role);
    
    // Delete operations
    void deleteByEmail(String email);
}
```

---

## API Design Guidelines

### 1. RESTful Endpoint Patterns

| Operation | HTTP Method | Endpoint | Response Code |
|-----------|------------|----------|---------------|
| Create | POST | `/resources` | 201 CREATED |
| Get One | GET | `/resources/{id}` | 200 OK |
| Get All | GET | `/resources` | 200 OK |
| Update | PUT | `/resources/{id}` | 200 OK |
| Delete | DELETE | `/resources/{id}` | 200 OK |
| Custom Query | GET | `/resources/email/{email}` | 200 OK |
| Exists Check | GET | `/resources/exists/email/{email}` | 200 OK |

### 2. Controller Pattern

```java
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AccountController {
    
    AccountService accountService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody AccountCreateRequest request) {
        log.info("REST request to create account");
        ApiResponse<AccountResponse> response = accountService.createAccount(request);
        
        if (response.code() == 1000) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountById(
            @PathVariable String id) {
        log.info("REST request to get account by id: {}", id);
        ApiResponse<AccountResponse> response = accountService.getAccountById(id);
        
        if (response.code() == 1000) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    // ... other endpoints
}
```

### 3. Path Variables vs Query Parameters

**Use Path Variables** for:
- Resource identifiers: `/accounts/{id}`
- Hierarchical resources: `/users/{userId}/posts/{postId}`

**Use Query Parameters** for:
- Filtering: `/accounts?role=admin`
- Pagination: `/accounts?page=0&size=10`
- Sorting: `/accounts?sort=createdAt,desc`

---

## Development Workflow

### 1. Setting Up a New Service

Follow these steps when creating a new microservice:

#### Step 1: Create Service Module
```bash
cd backend
# Add module to parent pom.xml
```

#### Step 2: Create Package Structure
```
src/main/java/com/bondhub/<service-name>/
├── config/
├── controller/
├── dto/
│   └── <entity>/
│       ├── request/
│       └── response/
├── mapper/
├── model/
├── repository/
└── service/
    └── <entity>/
```

#### Step 3: Add Dependencies to pom.xml

```xml
<dependencies>
    <!-- Common module -->
    <dependency>
        <groupId>com.bondhub</groupId>
        <artifactId>common</artifactId>
    </dependency>
    
    <!-- Spring Boot Starter Web or WebFlux -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Database (MongoDB or PostgreSQL) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
    
    <!-- Spring Cloud Config -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-config</artifactId>
    </dependency>
    
    <!-- Eureka Client -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <scope>provided</scope>
    </dependency>
    
    <!-- MapStruct -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
    </dependency>
</dependencies>
```

#### Step 4: Configure Maven Compiler Plugin

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>${org.lombok.version}</version>
                    </path>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>${org.mapstruct.version}</version>
                    </path>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok-mapstruct-binding</artifactId>
                        <version>0.2.0</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 2. Creating a New Feature

Follow this order when implementing a new feature:

1. **Model** - Define the entity
2. **Repository** - Create data access layer
3. **DTOs** - Create request and response objects
4. **Mapper** - Create MapStruct mapper
5. **Service Interface** - Define business operations
6. **Service Implementation** - Implement business logic
7. **Controller** - Expose REST endpoints
8. **Add Error Codes** - Add to common ErrorCode enum if needed

### 3. Build & Run

#### Set Java Home for Maven
```powershell
# Windows PowerShell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
mvn clean compile
```

#### Run Service
```powershell
mvn spring-boot:run
```

#### Build All Services
```bash
cd backend
mvn clean install -DskipTests
```

---

## Best Practices Summary

### ✅ DO

1. **Follow the layered architecture** strictly
2. **Use constructor injection** with `@RequiredArgsConstructor`
3. **Use MapStruct** for all entity-DTO conversions
4. **Use `ApiResponse<T>`** for all API responses
5. **Define error codes** in common module
6. **Log at appropriate levels** (INFO for operations, WARN for expected errors)
7. **Use Java Records** for DTOs
8. **Validate input** with `@Valid` and Bean Validation
9. **Use interface-implementation** pattern for services
10. **Follow naming conventions** consistently

### ❌ DON'T

1. **Don't skip service interfaces** - Always create interface + implementation
2. **Don't expose entities directly** - Use DTOs
3. **Don't log sensitive data** - Passwords, tokens, etc.
4. **Don't use field injection** - Use constructor injection
5. **Don't hard-code error messages** - Use ErrorCode enum
6. **Don't mix business logic** in controllers
7. **Don't return entities** from service layer - Return DTOs
8. **Don't handle exceptions** in controllers - Use global exception handler
9. **Don't skip validation** - Always validate input
10. **Don't create custom response wrappers** - Use `ApiResponse<T>`

---

## Quick Reference Checklist

When creating a new feature, verify:

- [ ] Entity extends `BaseModel` (if using MongoDB)
- [ ] Repository interface created with `@Repository`
- [ ] Request DTOs are Java Records with validation
- [ ] Response DTOs are Java Records
- [ ] MapStruct mapper with `componentModel = "spring"`
- [ ] Service interface created
- [ ] Service implementation with `@Service` and logs
- [ ] Controller with proper HTTP methods and status codes
- [ ] All methods return `ApiResponse<T>`
- [ ] Error codes added to common module if needed
- [ ] Maven compiler plugin configured for Lombok + MapStruct

---

## Questions or Issues?

If you encounter any issues or have questions about these standards:

1. Review the **auth-service** as the reference implementation
2. Check this guide for patterns and examples
3. Ask the team lead or senior developer
4. Update this guide if you find improvements

**Last Updated**: 2026-01-15
**Version**: 1.0
**Reference Service**: auth-service
