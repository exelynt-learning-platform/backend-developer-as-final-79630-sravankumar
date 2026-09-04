# Resource Booking System API

A production-grade, secure RESTful Resource and Meeting Room Booking backend built with **Java 17+**, **Spring Boot 3.3.5**, **Spring Security 6**, **JJWT**, **PostgreSQL**, **Hibernate**, **Jakarta Bean Validation**, and **Springdoc OpenAPI**.

---

## Table of Contents
1. [Project Overview](#project-overview)
2. [Features](#features)
3. [Architecture & Package Structure](#architecture--package-structure)
4. [Technology Stack](#technology-stack)
5. [Prerequisites](#prerequisites)
6. [PostgreSQL Database Setup](#postgresql-database-setup)
7. [Environment Variables & Configuration](#environment-variables--configuration)
8. [Building and Running the Application](#building-and-running-the-application)
9. [Seed Data & Default Credentials](#seed-data--default-credentials)
10. [Authentication & Registration Flow](#authentication--registration-flow)
11. [API Endpoints & Authorization Matrix](#api-endpoints--authorization-matrix)
12. [Reservation Business Rules & Concurrency](#reservation-business-rules--concurrency)
13. [Swagger / OpenAPI Documentation](#swagger--openapi-documentation)
14. [Automated Testing & Test Isolation](#automated-testing--test-isolation)
15. [Postman Collection](#postman-collection)

---

## Project Overview
This project provides a robust, scalable enterprise backend for managing and reserving resources (such as meeting rooms and conference halls). It enforces strict industrial backend design practices:
- **Stateless JWT Security**: Passwords hashed with BCrypt, role-based endpoint protection with Spring Security 6.
- **DTO-Based API Layer**: Entities are never leaked to API clients.
- **Concurrency & Double-Booking Prevention**: Pessimistic locking serialized transactions prevent race-condition booking conflicts.
- **Dynamic Specification Filtering**: Dynamic query filtering using Spring Data JPA Specifications.
- **Server-Side Price Calculation**: Room reservation prices calculated server-side based on exact time duration and hourly rates using `BigDecimal`.

---

## Features
- **User Registration & Login**: Users and Admins can register with their desired role (`USER` or `ADMIN`), followed by JWT login.
- **Role-Based Access Control (RBAC)**: Fine-grained permissions differentiating standard users from system administrators.
- **Resource Management**: Administrative CRUD operations on rooms with capacity, hourly price, and availability controls.
- **Ownership Isolation**: Users can only access, modify, or cancel their own reservations.
- **Reservation Conflict Prevention**: Prevents double-booking overlapping time ranges. Cancelled reservations release the slot automatically.
- **Dynamic Filtering, Pagination & Sorting**: Pagination bounds checking (max size 100), sort field whitelisting to prevent SQL/property injection, and multi-criteria specifications.
- **Unified Global Exception Handling**: Centralized `@RestControllerAdvice` converting all validation errors and business exceptions into structured `ApiErrorResponse` JSON.
- **Automatic Idempotent Seeding**: Seeds `admin`, `user1`, exactly 20 meeting rooms (`Room 101` to `Room 120`), and sample reservations on first launch.

---

## Architecture & Package Structure
The application adopts a clean, layered monolithic architecture:

```
src/main/java/com/example/resourcebooking
├── config                     # OpenAPI, Spring Security 6, CORS, JPA Auditing configuration
├── controller                 # REST API Controllers (HTTP transport layer only)
├── dto
│   ├── auth                   # LoginRequest, LoginResponse, RegisterRequest
│   ├── common                 # ApiErrorResponse, PageResponse
│   ├── reservation            # ReservationCreateRequest, ReservationUpdateRequest, ReservationResponse
│   ├── resource               # ResourceCreateRequest, ResourceUpdateRequest, ResourceResponse
│   └── user                   # UserResponse (password excluded)
├── entity                     # JPA Entities (User, Resource, Reservation, BaseAuditableEntity)
├── enums                      # Role, ResourceType, ReservationStatus
├── exception                  # GlobalExceptionHandler and custom domain exceptions
├── mapper                     # Entity <-> DTO mappers
├── repository                 # Spring Data JPA repositories with query methods & locks
├── security                   # JwtService, JwtAuthenticationFilter, UserPrincipal, CustomUserDetailsService
├── seed                       # DataSeeder CommandLineRunner for idempotent startup data
├── service
│   ├── impl                   # Business logic and transaction boundaries
│   ├── AuthService.java
│   ├── ReservationService.java
│   ├── ResourceService.java
│   └── UserService.java
├── specification              # Spring Data JPA dynamic filtering specifications
└── ResourceBookingApplication.java
```

---

## Technology Stack
| Technology | Version / Specification | Purpose |
|---|---|---|
| **Java** | 17+ (Tested on Java 21 LTS) | Modern LTS platform |
| **Spring Boot** | 3.3.5 | Core application framework |
| **Spring Security** | 6 | Stateless authentication & RBAC |
| **JJWT (io.jsonwebtoken)** | 0.12.6 | Maintained RFC 7519 JWT implementation |
| **PostgreSQL** | PostgreSQL 14+ | Primary relational database |
| **Spring Data JPA & Hibernate** | 6.x | Persistence layer & auditing |
| **Jakarta Bean Validation** | 3.0 | Declarative input validation |
| **Springdoc OpenAPI** | 2.6.0 | Interactive Swagger UI documentation |
| **JUnit 5, MockMvc** | Latest | Unit and integration test suite |
| **H2 Database** | Test Scope Only | Isolated, fast in-memory PostgreSQL mode testing |

---

## Prerequisites
- **Java Development Kit (JDK)**: Java 17 or Java 21 installed.
- **Maven**: Maven 3.8+ (or use the included Maven wrapper `mvnw` / `mvnw.cmd`).
- **PostgreSQL**: PostgreSQL 14+ installed and running locally on port 5432.

---

## PostgreSQL Database Setup
1. Open your PostgreSQL command line (`psql`) or graphical client (pgAdmin / DBeaver).
2. Create the target database:
   ```sql
   CREATE DATABASE resource_booking_db;
   ```
3. Ensure user privileges are granted (default user is usually `postgres`).

---

## Environment Variables & Configuration
The application strictly uses `src/main/resources/application.properties` (no YAML). All sensitive and deployment-specific values can be overridden via environment variables:

| Property Name | Environment Variable | Default Value | Description |
|---|---|---|---|
| `spring.datasource.url` | `DB_URL` | `jdbc:postgresql://localhost:5432/resource_booking_db` | PostgreSQL connection URL |
| `spring.datasource.username` | `DB_USERNAME` | `postgres` | Database username |
| `spring.datasource.password` | `DB_PASSWORD` | `postgres` | Database password |
| `jwt.secret` | `JWT_SECRET` | `404E6352...` *(256-bit dev default)* | HMAC-SHA256 signing secret key |
| `jwt.expiration` | `JWT_EXPIRATION` | `3600000` *(1 hour)* | JWT lifespan in milliseconds |
| `app.cors.allowed-origins` | `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Whitelisted frontend CORS origins |

> [!CAUTION]
> For production deployments, always provide strong, unique secrets via `JWT_SECRET` and `DB_PASSWORD`. Never commit production credentials to source control.

---

## Building and Running the Application

### 1. Build and Run Tests
Using Maven or the included Maven wrapper:
```bash
# Windows
.\mvnw.cmd clean test

# Linux / macOS
./mvnw clean test
```

### 2. Package the Application
```bash
# Windows
.\mvnw.cmd clean package

# Linux / macOS
./mvnw clean package
```
This generates the standalone executable JAR in `target/resource-booking-0.0.1-SNAPSHOT.jar`.

### 3. Run the Application
```bash
# Via Maven
.\mvnw.cmd spring-boot:run

# Or run the packaged JAR directly:
java -jar target/resource-booking-0.0.1-SNAPSHOT.jar
```
The application will start on `http://localhost:8080`.

---

## Seed Data & Default Credentials
On initial startup, `DataSeeder` automatically verifies existing records and seeds the following deterministic development data:

### Seed Users
| Role | Username | Password | Email | Note |
|---|---|---|---|---|
| **ADMIN** | `admin` | `Admin@123` | `admin@example.com` | Full administrative access |
| **USER** | `user1` | `User@123` | `user1@example.com` | Standard booking user |

*All passwords are encrypted with BCrypt before storage.*

### Seed Resources
Exactly 20 realistic conference rooms are seeded (`Room 101` through `Room 120`):
- Capacities vary realistically: 2, 4, 6, 8, 10 persons.
- Hourly rates vary: `500.00`, `750.00`, `1000.00`, `1250.00`, `1500.00` (stored as `BigDecimal`).
- Availability defaults to `true`.

---

## Authentication & Registration Flow

### 1. User / Admin Registration
Users can register with their desired role (`USER` or `ADMIN`):
- **Endpoint**: `POST /auth/register`
- **Request Body**:
  ```json
  {
    "username": "johndoe",
    "email": "johndoe@example.com",
    "password": "Password@123",
    "role": "USER"
  }
  ```
- **Response**: `201 Created` with user details (passwords are never returned).

### 2. Login & JWT Issuance
- **Endpoint**: `POST /auth/login`
- **Request Body**:
  ```json
  {
    "username": "johndoe",
    "password": "Password@123"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "username": "johndoe",
    "role": "USER"
  }
  ```

### 3. Authenticated Requests
Include the token in subsequent HTTP headers:
```http
Authorization: Bearer <your_jwt_token>
```

---

## API Endpoints & Authorization Matrix

| Method | Endpoint | Description | ADMIN | USER | Public |
|---|---|---|:---:|:---:|:---:|
| `POST` | `/auth/register` | Register new user or admin | Yes | Yes | Yes |
| `POST` | `/auth/login` | Authenticate and obtain JWT | Yes | Yes | Yes |
| `GET` | `/api/resources` | List all resources (paginated & sorted) | Yes | Yes | Yes |
| `GET` | `/api/resources/{id}` | Get resource details by ID | Yes | Yes | Yes |
| `POST` | `/api/admin/resources` | Create a new resource | Yes | No (403) | No (401) |
| `PUT` | `/api/admin/resources/{id}` | Update resource details | Yes | No (403) | No (401) |
| `DELETE` | `/api/admin/resources/{id}` | Delete a resource | Yes | No (403) | No (401) |
| `POST` | `/api/reservations` | Book a room (user from JWT) | Yes | Yes | No (401) |
| `GET` | `/api/reservations/my` | View own reservations | Yes | Yes | No (401) |
| `GET` | `/api/reservations/{id}` | View own reservation by ID | Yes | Owner only | No (401) |
| `PUT` | `/api/reservations/{id}` | Update own reservation | Yes | Owner only | No (401) |
| `DELETE` | `/api/reservations/{id}` | Cancel own reservation | Yes | Owner only | No (401) |
| `GET` | `/api/admin/reservations` | View all reservations (dynamic filter) | Yes | No (403) | No (401) |
| `GET` | `/api/admin/reservations/{id}` | View any reservation by ID | Yes | No (403) | No (401) |
| `PUT` | `/api/admin/reservations/{id}` | Admin update reservation | Yes | No (403) | No (401) |
| `DELETE` | `/api/admin/reservations/{id}` | Admin delete reservation | Yes | No (403) | No (401) |

---

## Reservation Business Rules & Concurrency

1. **Rule 1 — Start Time in Future**: `startTime` must be in the future. Past times return `400 Bad Request` ("Reservation start time cannot be in the past").
2. **Rule 2 — Chronological Range**: `endTime` must be strictly after `startTime`. Violations return `400 Bad Request` ("End time must be after start time").
3. **Rule 3 — Resource Availability**: If `resource.available == false`, booking returns `409 Conflict` ("Resource is currently unavailable for booking").
4. **Rule 4 — Overlapping Bookings & Concurrency Protection**:
   - Active reservations (`PENDING` or `CONFIRMED`) cannot overlap for the same resource.
   - An overlap occurs when: `existing.startTime < newEndTime AND existing.endTime > newStartTime`.
   - Rejections return `409 Conflict` ("Resource is already booked for the selected time range").
   - `CANCELLED` reservations are excluded and never block re-booking.
   - **Concurrency Strategy**: Handled via `@Transactional` with PostgreSQL pessimistic locking (`findByIdWithLock` using `LockModeType.PESSIMISTIC_WRITE`) on the target resource row to eliminate race conditions under concurrent requests.
5. **Rule 5 — Server-Calculated Pricing**: The client cannot specify the price. Price is computed strictly server-side:
   $$\text{Price} = \text{PricePerHour} \times \text{Duration in Hours}$$
   Calculated with high-precision `BigDecimal` and scaled with `RoundingMode.HALF_UP`.
6. **Rule 6 — Trusted User Identity**: The client request body cannot contain `userId`. The authenticated user is derived securely from the validated JWT token.
7. **Rule 7 — Ownership Security**: Accessing another user's reservation (`GET /api/reservations/{id}`) returns `403 Forbidden`.
8. **Rule 8 — Status Transitions**:
   - Allowed transitions: `PENDING` $\rightarrow$ `CONFIRMED`, `PENDING` $\rightarrow$ `CANCELLED`, `CONFIRMED` $\rightarrow$ `CANCELLED`.
   - `CANCELLED` reservations cannot be re-opened (`CANCELLED` $\rightarrow$ `CONFIRMED` is rejected).

---

## Swagger / OpenAPI Documentation
When the application is running, visit:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

To test secured endpoints in Swagger UI:
1. Call `POST /auth/login` with `admin` / `Admin@123` or `user1` / `User@123`.
2. Copy the returned `token`.
3. Click the green **Authorize** button at the top of Swagger UI.
4. Paste the token into the value field and click **Authorize**.

---

## Automated Testing & Test Isolation
The project includes a comprehensive suite of 32 unit and integration tests covering:
- Authentication & JWT generation / validation (`AuthControllerTest`, `JwtServiceTest`).
- Role authorization and 403 Forbidden protection (`ResourceControllerTest`, `ReservationOwnershipAndSecurityTest`).
- Reservation business rules, price calculations, and overlap detection (`ReservationServiceAndConflictTest`).
- Pagination bounds (max 100 limit), safe sorting validation, and dynamic JPA Specification filtering (`PaginationSortingAndFilteringTest`).

### Test Database Isolation
Tests run using an isolated in-memory database configured in PostgreSQL-compatibility mode via `src/test/resources/application.properties`. This ensures that:
- `mvn clean test` and `mvn clean package` succeed deterministically on any machine or CI/CD runner without requiring Docker or a running PostgreSQL server with hardcoded passwords.
- Production and local development runs use PostgreSQL exclusively.

---

## Postman Collection
A complete Postman collection is included in the project root:
- File: [`postman_collection.json`](postman_collection.json)
- Import it into Postman to instantly test all endpoints. Login requests automatically save the bearer tokens into collection variables (`authToken` and `adminToken`) for subsequent requests.
