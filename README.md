# BondHub Backend - Microservices Architecture

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Microservices](#microservices)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Gateway Routes](#api-gateway-routes)
- [Development Guidelines](#development-guidelines)
- [Monitoring & Health Checks](#monitoring--health-checks)
- [Troubleshooting](#troubleshooting)

---

## 🎯 Overview

BondHub is a modern microservices-based backend system built with Spring Boot and Spring Cloud. The system follows cloud-native patterns including service discovery, centralized configuration, API gateway, and distributed tracing.

**Key Features:**

- Microservices architecture with Spring Cloud
- Service discovery with Netflix Eureka
- Centralized configuration management
- API Gateway with routing and load balancing
- Circuit breakers and resilience patterns
- PostgreSQL and MongoDB databases
- Redis caching
- RabbitMQ message queue
- Distributed tracing with Zipkin

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        API Gateway (8080)                    │
│        Routes, Load Balancing, Circuit Breakers             │
└───────────┬─────────────────────────────────────────────────┘
            │
            ├─────────────┬──────────────┬──────────────┐
            │             │              │              │
    ┌───────▼──────┐ ┌───▼───────┐ ┌───▼────────┐ ┌───▼─────────┐
    │ User Service │ │Auth Service│ │Message Svc │ │Notification │
    │   (8081)     │ │   (8084)   │ │   (8082)   │ │ Svc (8083)  │
    └──────┬───────┘ └─────┬──────┘ └──────┬─────┘ └──────┬──────┘
           │               │               │              │
           │               │               │              │
    ┌──────▼───────┐ ┌─────▼──────┐ ┌──────▼─────┐ ┌─────▼──────┐
    │ PostgreSQL   │ │PostgreSQL  │ │  MongoDB   │ │  MongoDB   │
    │  bondhub_    │ │ bondhub_   │ │  bondhub_  │ │ bondhub_   │
    │   users      │ │  users     │ │  messages  │ │ messages   │
    └──────────────┘ └────────────┘ └────────────┘ └────────────┘

    ┌────────────────────────────────────────────────────────────┐
    │          Service Discovery (Eureka Server - 8761)          │
    └────────────────────────────────────────────────────────────┘

    ┌────────────────────────────────────────────────────────────┐
    │        Config Server - Centralized Config (8888)           │
    └────────────────────────────────────────────────────────────┘

    ┌────────────────────────────────────────────────────────────┐
    │  Shared Infrastructure: Redis, RabbitMQ, Zipkin            │
    └────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Technology Stack

### Core Framework

- **Java**: 21
- **Spring Boot**: 3.5.9
- **Spring Cloud**: 2025.0.1

### Microservices Components

- **Service Discovery**: Netflix Eureka
- **API Gateway**: Spring Cloud Gateway (WebFlux)
- **Config Server**: Spring Cloud Config
- **Circuit Breaker**: Resilience4j
- **Load Balancing**: Spring Cloud LoadBalancer

### Databases

- **PostgreSQL**: 16 (User & Auth Services)
- **MongoDB**: 7.0 (Message & Notification Services)
- **Redis**: 7 (Caching & Sessions)

### Messaging & Tracing

- **RabbitMQ**: 3.13 (Message Queue)
- **Zipkin**: Latest (Distributed Tracing)

### Build & Package

- **Maven**: Multi-module project
- **Docker**: Containerization (docker-compose.yml)

---

## 📁 Project Structure

```
backend/
├── api-gateway/              # API Gateway (Port: 8080)
│   ├── src/
│   └── pom.xml
│
├── discovery-server/         # Eureka Server (Port: 8761)
│   ├── src/
│   └── pom.xml
│
├── config-server/            # Config Server (Port: 8888)
│   ├── src/
│   │   └── main/resources/config/
│   │       ├── api-gateway.yml
│   │       ├── user-service.yml
│   │       ├── auth-service.yml
│   │       ├── message-service.yml
│   │       └── notification-service.yml
│   └── pom.xml
│
├── user-service/             # User Management (Port: 8081)
│   ├── src/
│   └── pom.xml
│
├── auth-service/             # Authentication (Port: 8084)
│   ├── src/
│   └── pom.xml
│
├── message-service/          # Messaging (Port: 8082)
│   ├── src/
│   └── pom.xml
│
├── notification-service/     # Notifications (Port: 8083)
│   ├── src/
│   └── pom.xml
│
├── common/                   # Shared utilities and DTOs
│   ├── src/
│   └── pom.xml
│
├── docker-compose.yml        # Infrastructure setup
├── pom.xml                   # Parent POM
├── .env                      # Environment variables
└── logs/                     # Application logs
```

---

## 🔧 Microservices

### 1. **API Gateway** (Port: 8080)

**Purpose**: Single entry point for all client requests

- Routes requests to appropriate microservices
- Load balancing with Eureka service discovery
- Circuit breaker integration
- CORS configuration
- Request/response filtering

**Routes**:

- `/user/**` → User Service
- `/auth/**` → Auth Service
- `/message/**` → Message Service
- `/notification/**` → Notification Service

**Key Features**:

- StripPrefix filter removes first path segment
- Circuit breakers with fallback handlers
- Global CORS support
- Actuator endpoints: `/actuator/gateway/routes`

---

### 2. **Discovery Server** (Port: 8761)

**Purpose**: Service registry and discovery

- Netflix Eureka Server
- All services register on startup
- Enables dynamic service discovery
- Health checking and monitoring

**Access**: `http://localhost:8761`

---

### 3. **Config Server** (Port: 8888)

**Purpose**: Centralized configuration management

- Stores configuration for all services
- Supports refresh without restart (`/actuator/refresh`)
- Environment-specific configurations
- File-based configuration storage

**Config Files**:

- Located in: `config-server/src/main/resources/config/`
- Each service has its own `.yml` file

---

### 4. **User Service** (Port: 8081)

**Purpose**: User profile and account management

- **Database**: PostgreSQL (`bondhub_users`)
- **Tech**: Spring Data JPA, Hibernate
- User CRUD operations
- Profile management
- User search and filtering

**API Endpoints** (via Gateway):

- `GET /user` - List users
- `GET /user/{id}` - Get user by ID
- `POST /user` - Create user
- `PUT /user/{id}` - Update user
- `DELETE /user/{id}` - Delete user

---

### 5. **Auth Service** (Port: 8084)

**Purpose**: Authentication and authorization

- **Database**: PostgreSQL (`bondhub_users`)
- **Tech**: Spring Security, JWT
- User authentication
- Token generation and validation
- Password management
- Session management with Redis

**API Endpoints** (via Gateway):

- `POST /auth/register` - User registration
- `POST /auth/login` - User login
- `POST /auth/logout` - User logout
- `POST /auth/refresh` - Refresh token
- `GET /auth/validate` - Validate token

---

### 6. **Message Service** (Port: 8082)

**Purpose**: Chat and messaging functionality

- **Database**: MongoDB (`bondhub_messages`)
- **Tech**: Spring Data MongoDB
- Real-time messaging
- Message history
- Conversation management
- RabbitMQ integration for async messaging

**API Endpoints** (via Gateway):

- `GET /message` - Get messages
- `POST /message` - Send message
- `GET /message/conversation/{id}` - Get conversation
- `DELETE /message/{id}` - Delete message

---

### 7. **Notification Service** (Port: 8083)

**Purpose**: Push notifications and alerts

- **Database**: MongoDB (`bondhub_messages`)
- **Tech**: Spring Data MongoDB
- Push notifications
- Email notifications
- SMS notifications (future)
- Notification preferences

**API Endpoints** (via Gateway):

- `GET /notification` - Get notifications
- `POST /notification` - Create notification
- `PUT /notification/{id}/read` - Mark as read
- `DELETE /notification/{id}` - Delete notification

---

### 8. **Common Module**

**Purpose**: Shared code across services

- Common DTOs
- Utility classes
- Exception handlers
- Constants
- Base configurations

---

## 📋 Prerequisites

### Required Software

- **Java JDK**: 21 or higher
- **Maven**: 3.8+ or use included wrapper (`mvnw`)
- **Docker & Docker Compose**: For infrastructure
- **Git**: Version control

### Infrastructure Services (via Docker)

- PostgreSQL 16
- MongoDB 7.0
- Redis 7
- RabbitMQ 3.13
- Zipkin (optional)

---

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd backend
```

### 2. Configure Environment Variables

Create/edit `.env` file in the project root:

```env
# PostgreSQL
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_DB=bondhub_users
DB_PORT=5432

# MongoDB
MONGO_INITDB_ROOT_USERNAME=root
MONGO_INITDB_ROOT_PASSWORD=root
MONGO_INITDB_DATABASE=bondhub_messages
MONGODB_PORT=27017

# Redis
REDIS_PASSWORD=redis123
REDIS_PORT=6379

# RabbitMQ
RABBITMQ_DEFAULT_USER=admin
RABBITMQ_DEFAULT_PASS=admin123
```

### 3. Start Infrastructure Services

```bash
docker-compose up -d postgres mongodb redis rabbitmq zipkin
```

Verify services are running:

```bash
docker-compose ps
```

### 4. Build the Project

```bash
# Build all modules
./mvnw clean install

# Or on Windows
mvnw.cmd clean install
```

### 5. Start Services in Order

**Step 1: Start Config Server**

```bash
cd config-server
../mvnw spring-boot:run
```

Wait until it's fully started (check logs for "Started ConfigServerApplication")

**Step 2: Start Discovery Server**

```bash
cd discovery-server
../mvnw spring-boot:run
```

Access Eureka Dashboard: `http://localhost:8761`

**Step 3: Start Business Services**
Open separate terminals for each:

```bash
# User Service
cd user-service
../mvnw spring-boot:run

# Auth Service
cd auth-service
../mvnw spring-boot:run

# Message Service
cd message-service
../mvnw spring-boot:run

# Notification Service
cd notification-service
../mvnw spring-boot:run
```

**Step 4: Start API Gateway**

```bash
cd api-gateway
../mvnw spring-boot:run
```

### 6. Verify Deployment

**Check Eureka Dashboard**: `http://localhost:8761`

- All services should be registered

**Check API Gateway**: `http://localhost:8080/actuator/health`

- Should return status "UP"

**View Gateway Routes**: `http://localhost:8080/actuator/gateway/routes`

---

## ⚙️ Configuration

### Service Ports

| Service              | Port |
| -------------------- | ---- |
| API Gateway          | 8080 |
| User Service         | 8081 |
| Message Service      | 8082 |
| Notification Service | 8083 |
| Auth Service         | 8084 |
| Discovery Server     | 8761 |
| Config Server        | 8888 |

### Database Configuration

**PostgreSQL (User & Auth Services)**:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bondhub_users
    username: postgres
    password: postgres
```

**MongoDB (Message & Notification Services)**:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://root:root@127.0.0.1:27017/bondhub_messages?authSource=admin
```

### Actuator Endpoints

All services expose actuator endpoints at `/actuator`:

- `/actuator/health` - Health check
- `/actuator/info` - Application info
- `/actuator/metrics` - Metrics
- `/actuator/prometheus` - Prometheus metrics
- `/actuator/refresh` - Refresh configuration

---

## 🌐 API Gateway Routes

### Route Configuration

All client requests go through the API Gateway at `http://localhost:8080`

| Route Pattern      | Target Service       | Actual Path After Strip |
| ------------------ | -------------------- | ----------------------- |
| `/user/**`         | User Service         | `/**`                   |
| `/auth/**`         | Auth Service         | `/**`                   |
| `/message/**`      | Message Service      | `/**`                   |
| `/notification/**` | Notification Service | `/**`                   |

**Example**:

- Client calls: `http://localhost:8080/user/123`
- Gateway forwards to: `http://user-service/123`
- The `/user` prefix is stripped by the `StripPrefix=1` filter

### Circuit Breaker

Each route has a circuit breaker configured:

- **Failure Threshold**: 50%
- **Sliding Window**: 10 calls
- **Wait Duration**: 30 seconds
- **Fallback**: Returns error response when service is down

---

## 💻 Development Guidelines

### 1. Adding a New Service

**Step 1**: Create module

```bash
mkdir new-service
cd new-service
```

**Step 2**: Create `pom.xml` with parent reference

```xml
<parent>
    <groupId>com.bondhub</groupId>
    <artifactId>bond-hub-api</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</parent>
```

**Step 3**: Add to parent `pom.xml`

```xml
<modules>
    <module>new-service</module>
</modules>
```

**Step 4**: Create configuration in `config-server/src/main/resources/config/new-service.yml`

**Step 5**: Register route in `api-gateway.yml`

### 2. Adding New Endpoints

All controller endpoints should use proper REST conventions:

```java
@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping
    public ResponseEntity<List<User>> getUsers() {
        // Implementation
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        // Implementation
    }
}
```

### 3. Database Migrations

**PostgreSQL**: Use Liquibase or Flyway for versioned migrations
**MongoDB**: Indexes are auto-created with `auto-index-creation: true`

### 4. Logging

All services use Logback with consistent format:

```java
private static final Logger log = LoggerFactory.getLogger(YourClass.class);

log.info("Processing request: {}", requestId);
log.debug("Details: {}", details);
log.error("Error occurred", exception);
```

Logs are written to:

- Console (stdout)
- File: `logs/{service-name}.log`

### 5. Exception Handling

Use centralized exception handling:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
        ResourceNotFoundException ex) {
        // Return appropriate error response
    }
}
```

---

## 📊 Monitoring & Health Checks

### Eureka Dashboard

- URL: `http://localhost:8761`
- View all registered services
- Check service health and instances

### Actuator Health Checks

Each service exposes health information:

```bash
# API Gateway health
curl http://localhost:8080/actuator/health

# User Service health
curl http://localhost:8081/actuator/health
```

### Gateway Routes

View all configured routes:

```bash
curl http://localhost:8080/actuator/gateway/routes
```

### Distributed Tracing

- Zipkin UI: `http://localhost:9411`
- Trace requests across services
- Identify bottlenecks

### Prometheus Metrics

All services expose Prometheus metrics:

```bash
curl http://localhost:8080/actuator/prometheus
```

---

## 🔧 Troubleshooting

### Common Issues

#### 1. Service Not Registering with Eureka

**Symptoms**: Service doesn't appear in Eureka dashboard

**Solutions**:

- Ensure Config Server is running first
- Check `eureka.client.service-url.defaultZone` in config
- Verify network connectivity: `curl http://localhost:8761/eureka/apps`
- Check service logs for registration errors

#### 2. Cannot Access Endpoint Through Gateway

**Symptoms**: 404 or 503 errors

**Solutions**:

- Check service is registered: `http://localhost:8761`
- Verify route configuration: `http://localhost:8080/actuator/gateway/routes`
- Ensure service path matches gateway configuration
- Check circuit breaker status

#### 3. MongoDB Authentication Failed

**Symptoms**: `MongoSecurityException: Exception authenticating`

**Solution**:

- Add `?authSource=admin` to MongoDB URI
- Correct format: `mongodb://root:root@127.0.0.1:27017/bondhub_messages?authSource=admin`

#### 4. Configuration Not Loading

**Symptoms**: Service uses default values instead of Config Server values

**Solutions**:

- Ensure Config Server is running before starting services
- Check Config Server URL in `application.yml` or bootstrap properties
- Verify config file exists in Config Server
- Check Config Server logs

#### 5. Database Connection Issues

**Symptoms**: `Cannot acquire connection` or timeout errors

**Solutions**:

- Verify Docker containers are running: `docker-compose ps`
- Check database credentials in configuration
- Ensure ports are not blocked by firewall
- Test connection: `psql -h localhost -U postgres -d bondhub_users`

### Viewing Logs

```bash
# View service logs
tail -f logs/user-service.log

# View Docker logs
docker-compose logs postgres
docker-compose logs mongodb
```

### Refreshing Configuration

To reload configuration without restarting:

```bash
curl -X POST http://localhost:8081/actuator/refresh
```

---

## 📚 Additional Resources

### Documentation

- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)
- [Netflix Eureka](https://github.com/Netflix/eureka/wiki)
- [Resilience4j](https://resilience4j.readme.io/)

### API Testing

Use tools like:

- **Postman**: Import collection for API testing
- **curl**: Command-line testing
- **HTTPie**: User-friendly HTTP client

### Development Tools

- **IntelliJ IDEA**: Recommended IDE
- **VS Code**: With Spring Boot extension
- **Docker Desktop**: Container management

---

## 👥 Contributing

### Branch Strategy

- `main`: Production-ready code
- `develop`: Integration branch
- `feature/*`: New features
- `bugfix/*`: Bug fixes

### Code Style

- Follow Java naming conventions
- Use meaningful variable names
- Add JavaDoc for public methods
- Keep methods focused and small

### Testing

- Write unit tests for business logic
- Integration tests for API endpoints
- Minimum 80% code coverage

---

## 📝 License

[Add your license information here]

---

## 📞 Contact & Support

For questions or issues:

- Create an issue in the repository
- Contact the development team
- Check the troubleshooting section

---

**Last Updated**: January 2026
**Version**: 0.0.1-SNAPSHOT
