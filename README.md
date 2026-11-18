# API Gateway

## Overview

API Gateway is the single entry point to the Marketplace microservices. It is built with Spring Cloud Gateway and Spring WebFlux.

The Gateway routes requests to backend services and validates JWT access tokens before forwarding protected requests.

## Architecture

The current architecture includes:

- **API Gateway** — request routing, JWT validation and rate limiting.
- **Auth Service** — registration, login and token management.
- **User Service** — user profiles and payment cards.
- **Redis** — rate limiting and caching.
- **PostgreSQL** — persistent data storage.

## Routes

| Path | Service |
|---|---|
| `/api/auth/**` | Auth Service |
| `/api/users/**` | User Service |
| `/api/cards/**` | User Service |

### Public endpoints

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET /actuator/health`

All other endpoints require a valid JWT access token.

## Security and Rate Limiting

The Gateway uses Spring Security OAuth2 Resource Server to validate JWT access tokens using the Auth Service JWKS endpoint.

Redis-based rate limiting protects Auth and User Service routes. Requests are limited by authenticated user identity or client IP address when the user is not authenticated.

The default rate limit configuration is:

- **Replenish rate:** 10 tokens per second.
- **Burst capacity:** 20 tokens.

These values can be customized using environment variables.

Role-based access control is handled by the backend services.

## Configuration

Configuration files:

- `application.yaml`
- `application-local.yaml`
- `application-docker.yaml`

Main environment variables:

| Variable | Description | Default |
|---|---|---|
| `SERVER_PORT` | Gateway server port | `8080` |
| `AUTH_JWKS_URI` | Auth Service JWKS endpoint | Profile-dependent |
| `AUTH_SERVICE_URI` | Auth Service URL | Profile-dependent |
| `USER_SERVICE_URI` | User Service URL | Profile-dependent |
| `REDIS_HOST` | Redis hostname | `localhost` locally, `redis` in Docker |
| `REDIS_PORT` | Redis port | `6379` |
| `RATE_LIMIT_REPLENISH_RATE` | Tokens replenished per second | `10` |
| `RATE_LIMIT_BURST_CAPACITY` | Maximum token capacity | `20` |

Redis is required for rate limiting. When running the Gateway locally, Redis must be available at the configured host and port. When running with Docker Compose, Redis is started as a separate service, and the Gateway waits for its health check before starting.

## Running the Application

The complete Marketplace stack is managed in the [Marketplace Infrastructure](https://github.com/elyaberlet/marketplace-infra) repository.

Follow the instructions in that repository to configure environment variables and start all services using Docker Compose.

## Tests

Run tests using Maven Wrapper:

```bash
./mvnw clean test
```

## JWT Key Generation

The Auth Service requires an RSA key pair for signing and verifying JWTs.

If the keys have not been generated yet, create them in the `auth-module` directory:

```bash
mkdir -p secrets

openssl genpkey \
  -algorithm RSA \
  -out secrets/private_key.pem \
  -pkeyopt rsa_keygen_bits:2048

openssl pkey \
  -in secrets/private_key.pem \
  -pubout \
  -out secrets/public_key.pem
```

The generated files must be located at:

- `auth-module/secrets/private_key.pem`
- `auth-module/secrets/public_key.pem`

These files are required by Docker Compose and must not be committed to Git. Keep the same key pair when restarting the services.