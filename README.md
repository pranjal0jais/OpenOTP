# OTP Service

A production-grade One-Time Password (OTP) microservice built with Java Spring Boot. The service handles secure OTP generation, email delivery, and verification with production-level security and reliability patterns.

---

## Quick Start

```bash
git clone <repository-url>

cd otp-service

cp .env.example .env

docker compose up --build
```

---

## Self-Hosted

This service is designed to be self-hosted using Docker Compose. No manual installation of Redis or RabbitMQ is required.

---

## Features

- Send a 6-digit OTP to any email address
- Verify the OTP within a 5-minute expiry window
- HMAC-SHA256 signing to detect tampered OTP records
- API key authentication on all endpoints
- Async email delivery via RabbitMQ with retry and dead-letter support
- Redis-based OTP storage with native TTL expiry
- Rate limiting, resend cooldown, and attempt limiting
- HTML email templates via Thymeleaf
- Structured logging and unit test coverage

---

## Containerized Infrastructure

The service is fully containerized using Docker Compose. The following services run automatically:

- OTP Service (Spring Boot)
- Redis
- RabbitMQ

---

## Architecture

```
Client → Spring Boot API → Redis (OTP storage)

Spring Boot API → RabbitMQ → Email Consumer → SMTP Server
```

---

## Technology Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.x |
| OTP Storage | Redis |
| Message Broker | RabbitMQ |
| Email Delivery | Gmail SMTP |
| Email Templates | Thymeleaf |
| Rate Limiting | Bucket4j 8.18.0 |
| Security | Spring Security |
| HMAC Signing | javax.crypto.Mac (HmacSHA256) |
| Testing | JUnit 5 + Mockito |

---

## Project Structure

```
com.pranjal.otp_service
├── config          # Redis, RabbitMQ, Security, App configuration
├── controller      # REST endpoints
├── service         # OtpService, EmailService, RateLimiterService
├── dto             # Request/response DTOs, RedisOtpRecord, OtpEmailMessage
├── repository      # RedisOtpRepository
├── exception       # Custom exceptions + GlobalExceptionHandler
└── utility         # OtpGenerator, OtpSignatureUtility
```

---

## API Endpoints

All endpoints require the `X-API-KEY` header.

### Send OTP

```
POST /api/send
Content-Type: application/json
X-API-KEY: <your-api-key>

{
  "email": "user@example.com"
}
```

### Verify OTP

```
POST /api/verify
Content-Type: application/json
X-API-KEY: <your-api-key>

{
  "email": "user@example.com",
  "otpCode": "123456"
}
```

---

## Security Policies

| Policy | Detail |
|---|---|
| OTP Expiry | 5 minutes via Redis native TTL |
| Resend Cooldown | 50 seconds between requests for the same email |
| Attempt Limit | Blocked after 3 failed verification attempts |
| Rate Limit | Max 3 send requests per 10 minutes per email |
| API Authentication | `X-API-KEY` header — constant-time comparison |
| HMAC Signing | HmacSHA256 — tamper detection on stored records |

---

## Error Reference

| HTTP Status | Scenario |
|---|---|
| 400 | Wrong OTP submitted or invalid request fields |
| 403 | HMAC signature verification failed |
| 404 | No active OTP found for the given email |
| 429 | Max attempts, cooldown active, or rate limit exceeded |
| 500 | Email delivery failure — routed to Dead Letter Queue |

---

## Environment Variables

```bash
cp .env.example .env
```

Edit `.env` with your values:

```env
MAIL_USERNAME=
MAIL_PASSWORD=

REDIS_PASSWORD=

RABBITMQ_USERNAME=
RABBITMQ_PASSWORD=

OTP_EXPIRY_MINUTES=5
OTP_RESEND_COOLDOWN_SECONDS=50
OTP_MAX_ATTEMPTS=3

HMAC_SECRET_KEY=
API_KEY=
```

---

## Running the Application

### Prerequisites

- Docker
- Docker Compose

> Java 21+ only required for local development outside Docker.

### Start the Complete Stack

```bash
docker compose up --build
```

This starts the Spring Boot application, Redis, and RabbitMQ.

| Service | URL |
|---|---|
| Application | http://localhost:8080 |
| RabbitMQ Dashboard | http://localhost:15672 |

---

## Docker Commands

### Start

```bash
docker compose up --build
```

### Stop

```bash
docker compose down
```

### View Logs

```bash
docker compose logs -f
```

---

## Tests

```bash
./mvnw test
```

Covers `OtpSignatureUtility` (4 tests) and `OtpService` (8 tests) using Mockito.