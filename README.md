# API Gateway

## Overview

API Gateway is the single entry point for the Marketplace microservices architecture.

It is built with Spring Cloud Gateway and Spring WebFlux. The Gateway routes incoming HTTP requests to the appropriate backend services and validates JWT access tokens before allowing access to protected endpoints.

## Architecture

The current architecture consists of:

- **API Gateway** — request routing and JWT authentication.
- **Auth Service** — user registration, login, token generation and refresh.
- **User Service** — user profile and payment card management.

The Gateway communicates with backend services through internal Docker Compose networking. Only the API Gateway exposes a port to the host machine.

## Technologies

- Java 21
- Spring Boot 3.4
- Spring Cloud Gateway
- Spring WebFlux
- Spring Security
- OAuth2 Resource Server
- JWT (RS256)
- JWKS (JSON Web Key Set)
- Maven
- Docker
- Docker Compose
- JUnit 5
- Mockito
- MockWebServer

## API Routes

| Route | Destination | Description |
|---|---|---|
| `/api/auth/**` | Auth Service | Authentication and registration |
| `/api/users/**` | User Service | User management |
| `/api/cards/**` | User Service | Payment card management |
| `/actuator/health` | API Gateway | Health check |

### Public Endpoints

The following endpoints do not require authentication:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET /actuator/health`

All other endpoints require a valid JWT access token.

## Architecture

The Marketplace application follows a microservices architecture.

The current deployment consists of the following services:

- **API Gateway** — the single external entry point, responsible for request routing and JWT authentication.
- **Auth Service** — handles user registration, authentication, credential management and JWT generation.
- **User Service** — manages user profiles and payment cards.
- **Auth PostgreSQL** — stores authentication credentials.
- **User PostgreSQL** — stores user profiles and payment card data.
- **Redis** — used by the User Service for caching.

All services communicate through an internal Docker Compose network. Only the API Gateway exposes a port to the host machine (`8080`).

### JWT Authentication Flow

1. A client sends an authentication request to the API Gateway.
2. The Gateway forwards the request to the Auth Service.
3. The Auth Service generates JWT access and refresh tokens.
4. For protected requests, the Gateway retrieves the public key from the Auth Service JWKS endpoint.
5. The Gateway validates the JWT signature, expiration and token type.
6. If authentication succeeds, the request is forwarded to the appropriate backend service.
7. The backend service performs its own authorization checks.

### Persistent Storage and Secrets

- PostgreSQL data is stored in external Docker volumes.
- RSA private and public keys are provided to the Auth Service through Docker Secrets.
- The Gateway uses the public key exposed by the Auth Service through JWKS.
- Database credentials and service configuration are supplied through environment variables.

### Error Handling

| HTTP Status | Description |
|---|---|
| `400 Bad Request` | Invalid request data |
| `401 Unauthorized` | Missing, invalid, expired or non-access JWT |
| `403 Forbidden` | Authenticated user does not have permission to access the resource |

The Gateway provides custom JSON responses for its authentication and authorization errors.

Role-based access control is handled by the downstream services.

## Configuration

The application uses environment variables for configuration.

| Variable | Description |
|---|---|
| `SERVER_PORT` | API Gateway server port |
| `AUTH_JWKS_URI` | Auth Service JWKS endpoint |
| `AUTH_SERVICE_URI` | Internal Auth Service URL |
| `USER_SERVICE_URI` | Internal User Service URL |

For local development, the application supports the `local` Spring profile. Docker Compose uses the Docker configuration.

## Running the Application

### Prerequisites

- Java 21
- Maven
- Docker
- Docker Compose

### Start with Docker Compose

1. Configure the required environment variables in the `.env` file.
2. Navigate to the project root directory.
3. Start the application:

```bash
docker compose up --build -d
```

Check the running containers:

```bash
docker compose ps
```

### Health Check

After startup, check the Gateway health endpoint:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:

```json
{
  "status": "UP"
}
```

## Running Tests

Run all tests:

```bash
mvn clean test
```

Run a specific test class:

```bash
mvn -Dtest=SecurityConfigTest test
```

The test suite covers:

- JWT token type validation.
- JWT signature and JWKS validation.
- Security configuration.
- Public endpoint matching.
- Gateway route configuration.
- Custom authentication error handling.
- Custom access denied error handling.

## Project Structure

```text
src
├── main
│   ├── java
│   │   └── com.innowise.apigateway
│   │       ├── config
│   │       ├── security
│   │       └── dto
│   └── resources
│       ├── application.yml
│       ├── application-local.yml
│       └── application-docker.yml
└── test
    └── java
        └── com.innowise.apigateway
            ├── config
            └── security
```

## Notes

- The API Gateway is responsible for request routing and JWT authentication.
- User roles and resource-level authorization are managed by the corresponding backend services.
- Backend services communicate through the internal Docker network.
- Only the API Gateway is exposed externally.

