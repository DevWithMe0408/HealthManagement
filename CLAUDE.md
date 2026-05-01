# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Quy tắc làm việc
- Tôi là người code chính. Không tự động viết code mới hay sửa code mà chưa được tôi đồng ý.
- Khi tôi đưa code, hãy review, đánh giá, giải thích rõ ràng trước khi đề xuất thay đổi.
- Chỉ áp dụng thay đổi sau khi tôi xác nhận.

## Ngôn ngữ
- Giải thích, review, phân tích: tiếng Việt
- Code comment, commit message, tên biến: tiếng Anh

## Project Overview

Spring Boot microservices-based Health Management System. Uses Spring Cloud for service discovery, an API Gateway for routing and JWT auth, RabbitMQ for async events, and MySQL for persistence.

## Services and Ports

| Service | Port | Role |
|---|---|---|
| api-gateway | 8080 | Entry point, JWT validation, routing |
| user-service | 8081 | Auth, user profiles, JWT issuance |
| health-data-service | 8085 | Health metrics and indicators |
| discovery-server | 8761 | Eureka service registry |
| nutrition-service | — | Minimal stub |
| notification-service | — | Minimal stub |
| common | — | Shared event POJOs (library module) |

## Build Commands

```bash
# Build all modules
./mvnw clean install

# Build a single service (skip tests)
./mvnw clean package -DskipTests -pl user-service

# Run all tests
./mvnw test

# Run tests for a specific module
./mvnw test -pl health-data-service

# Run a single test class
./mvnw test -pl user-service -Dtest=UserServiceApplicationTests
```

## Running the Project

**Start infrastructure first (MySQL + RabbitMQ):**
```bash
docker-compose up -d
```

**Start services in order** (each in a separate terminal):
```bash
./mvnw spring-boot:run -pl discovery-server   # Start first
./mvnw spring-boot:run -pl user-service
./mvnw spring-boot:run -pl health-data-service
./mvnw spring-boot:run -pl api-gateway        # Start last
```

Infrastructure URLs:
- Eureka dashboard: http://localhost:8761
- RabbitMQ management UI: http://localhost:15672 (guest/guest)
- MySQL: localhost:3306 (user: `userdb_user` / `userdb_password`)

## Architecture

### Request Flow
All external HTTP calls go through the **API Gateway** (port 8080). The gateway validates JWTs, adds `X-User-Id` / `X-Username` / `X-Roles` headers, and routes to the appropriate service via Eureka load balancing (`lb://service-name`).

Public endpoints (no JWT required): `/api/auth/login`, `/api/auth/register`, `/api/auth/refresh`, `/api/public/**`, `/actuator/**`, Swagger endpoints.

CORS is configured to allow `http://localhost:5173` (React frontend).

### JWT Flow
1. Client logs in via `POST /api/auth/login` → user-service returns access token (1hr) + refresh token (7 days)
2. Client sends `Authorization: Bearer <token>` on all subsequent requests
3. Gateway's `JwtFilter` validates the token and injects user context headers for downstream services

### Event-Driven Communication (RabbitMQ)
- **Exchange**: `user.events.exchange` (TopicExchange, durable)
- **user-service publishes**: `user.event.created`, `user.profile.updated`
- **health-data-service consumes**: binds queues `health-data.user-created.queue` and `health-data.user-profile-updated.queue` to the same exchange/routing keys
- Shared event classes (`UserCreatedEvent`, `UserProfileUpdatedEvent`) live in the `common-events` module

### Database
- `user_db`: user accounts, auth credentials, refresh tokens
- `health_db`: health metrics, indicator configs, units, calculated snapshots
- Both use `ddl-auto: update` — Hibernate manages schema automatically
- Both databases and the MySQL user are created by `docker-compose.yml`

## Key Configuration

Each service has `src/main/resources/application.yml`. The root `pom.xml` manages shared versions:
- Spring Boot: 3.4.3 | Spring Cloud: 2023.0.0 | Java: 17
- JWT: JJWT 0.12.5 | Lombok: 1.18.30 | MapStruct: 1.5.5.Final

**Gateway routes** are declared in `api-gateway/src/main/resources/application.yml`.

**RabbitMQ wiring** (exchange/queue/binding beans) is in:
- `user-service/.../config/UserRabbitMQConfig.java` (producer side)
- `health-data-service/.../config/HealthDataRabbitMQConfig.java` (consumer side)

## Development Notes

- The `docker-compose.yml` currently only defines infrastructure (MySQL, RabbitMQ). Service containers are commented out — use `spring-boot:run` for local development.
- Redis is referenced in `application.yml` files (`localhost:6379`) for caching but may not be in `docker-compose.yml`; add it if needed.
- `show-sql: true` is enabled in both data services — SQL queries appear in logs during development.
- MapStruct processors require Lombok to run first; the root `pom.xml` annotation processor order handles this.
