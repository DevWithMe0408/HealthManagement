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
| common | — | Shared event POJOs + web/security utilities (library module) |

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

### Synchronous Inter-Service Calls
Besides the async RabbitMQ events, services also make direct HTTP calls to each other. `user-service` calls `health-data-service` via `HealthDataClient` (`RestTemplate`, base URL from `app.services.health-data.url`, default `http://localhost:8085`). These calls bypass the gateway, so they don't carry a JWT — keep that in mind for any auth assumptions on internal endpoints.

### JWT Flow
1. Client logs in via `POST /api/auth/login` → user-service returns access token (1hr) + refresh token (7 days)
2. Client sends `Authorization: Bearer <token>` on all subsequent requests
3. Gateway's `JwtFilter` validates the token and injects user context headers for downstream services

### Event-Driven Communication (RabbitMQ)
- **Exchange**: `user.events.exchange` (TopicExchange, durable)
- **user-service publishes**: `user.event.created`, `user.profile.updated`
- **health-data-service consumes**: binds queues `health-data.user-created.queue` and `health-data.user-profile-updated.queue` to the same exchange/routing keys
- Shared event classes (`UserCreatedEvent`, `UserProfileUpdatedEvent`) live in the `common` module (which also holds shared web/security helpers — `spring-webmvc`, `spring-security-web`, servlet API)

### ML Integration (Model 1 PBF)
`health-data-service` integrates an external ML service to predict Percent Body Fat (PBF).
- **Client**: `Model1PbfClient` (Spring `RestClient`) does `POST {app.ml.pbf-url}/v1/predict/pbf`. Config: `app.ml.pbf-url` (`ML_PBF_URL`, default `http://localhost:8000`) and `app.ml.timeout-ms` (`ML_PBF_TIMEOUT_MS`, default 3000). The ML service itself is a separate (Python) process, not a Maven module here.
- **Two PBF sources**: PBF snapshots are persisted with a `method` column on `CalculatedMetricSnapshot` — `FORMULA` (Navy formula) vs `MODEL_1` (AI). When the ML call fails, the code falls back to the formula value. Body classification respects the user's `pbf_method` preference.
- **Input schema change (breaking)**: the `IndicatorType` enum renamed `WAIST` → `ABDOMEN` and added `THIGH`. Submit DTOs use `abdomen`/`thigh`; clients still sending `waist`/`WAIST` must be updated. `THIGH` is an input for Model 1 only — it does not feed Navy/BMI/WHR.

### Database
- `user_db`: user accounts, auth credentials, refresh tokens
- `health_db`: health metrics, indicator configs, units, calculated snapshots
- Both use `ddl-auto: update` — Hibernate manages schema automatically
- Both databases and the MySQL user are created by `docker-compose.yml`
- **Manual migrations**: `ddl-auto: update` only adds columns/tables — it does NOT rewrite enum values stored as `STRING` in existing rows. Data migrations (e.g. `WAIST` → `ABDOMEN`, backfilling `THIGH` configs and PBF `method`) must be run by hand. See `health-data-service/src/main/resources/db/model1_pbf_migration.sql` (take a backup first).

## Key Configuration

Each service has `src/main/resources/application.yml`. The root `pom.xml` manages shared versions:
- Spring Boot: 3.2.3 | Spring Cloud: 2023.0.0 | Java: 21
- JWT: JJWT 0.12.5 | Lombok: 1.18.30 | MapStruct: 1.5.5.Final | uuid-creator: 5.3.7

**Gateway routes** are declared in `api-gateway/src/main/resources/application.yml`.

**RabbitMQ wiring** (exchange/queue/binding beans) is in:
- `user-service/.../config/UserRabbitMQConfig.java` (producer side)
- `health-data-service/.../config/HealthDataRabbitMQConfig.java` (consumer side)

## Development Notes

- The `docker-compose.yml` currently only defines infrastructure (MySQL, RabbitMQ). Service containers are commented out — use `spring-boot:run` for local development.
- Redis is referenced in `application.yml` files (`localhost:6379`) for caching but may not be in `docker-compose.yml`; add it if needed.
- `show-sql: true` is enabled in both data services — SQL queries appear in logs during development.
- MapStruct processors require Lombok to run first; the root `pom.xml` annotation processor order handles this.
