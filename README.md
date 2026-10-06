# Event Ticketing Platform

A ticket booking system split into three Spring Boot microservices with a React frontend. The interesting part is checkout: many people can try to grab the same seat at the same moment, and the system guarantees each seat is sold exactly once and nobody is charged twice.

## Services

| Service | What it does | Data |
|---|---|---|
| **user-service** | Sign up, log in, issues JWTs | PostgreSQL `users` |
| **event-service** | Create and list events, generates the seat map (A1, A2, ... ) | PostgreSQL `events` |
| **booking-service** | Seat holds, checkout, payments, availability | PostgreSQL `bookings` + Redis |
| **gateway** | nginx serving the React app and routing `/api/*` to each service | |

Each service has its own database and verifies the same JWT, so they can be deployed and scaled independently.

## How double booking is prevented

Checkout is two steps:

1. **Hold.** The user picks seats and booking-service locks each one in Redis with `SET seat:{event}:{seat} {holdId} NX PX 300000`. It is all or nothing: if any seat is already locked, the seats it already grabbed are released and the user gets a 409. Seats are locked in sorted order so overlapping requests behave predictably. Locks expire on their own after 5 minutes if the user walks away.
2. **Confirm.** The client sends the hold id plus an `Idempotency-Key` header (generated once per checkout). The service checks the hold still owns every lock, charges the payment with the same idempotency key, then saves the booking.

Safety nets:

- **Redis locks** stop two users from checking out the same seat at the same time, across every booking-service instance.
- **Release is compare-and-delete** (a small Lua script), so an expired hold can never free a seat that now belongs to someone else.
- **Unique (event, seat) constraint** in Postgres means the database itself refuses to sell a seat twice, even in a worst case where a lock expired at the wrong moment. If that happens the payment is refunded.
- **Idempotency keys** mean a double click or a retried request returns the original booking instead of charging again. A unique (user, key) constraint covers the case where two identical requests race each other.

`ConcurrentHoldTest` fires 50 users at the same seat at the same moment and checks exactly one wins.

## Run it

```bash
docker compose up --build
```

Open http://localhost:8080, create an account, create an event, and book seats. Open a second browser as another user to see seats show up as "being booked" and "sold".

## Tests

```bash
mvn verify
```

- user-service: registration, validation, login, token checks
- event-service: seat map generation, validation, auth
- booking-service: full hold → confirm flow, all-or-nothing holds, sold seats, idempotent confirm (charged once), ownership checks, releasing holds, and the 50-thread concurrency test

## API

| Method | Path | Notes |
|---|---|---|
| POST | `/api/users/register`, `/api/users/login` | returns a JWT |
| GET | `/api/events` | upcoming events |
| POST | `/api/events` | create an event (login required) |
| GET | `/api/events/{id}` | event with seat map |
| GET | `/api/bookings/events/{id}/availability` | sold and held seats |
| POST | `/api/bookings/holds` | `{eventId, seats[]}` |
| DELETE | `/api/bookings/holds/{id}` | give seats back |
| POST | `/api/bookings` | `{holdId}` + `Idempotency-Key` header |
| GET | `/api/bookings/me` | your bookings |

## Deployment (AWS ECS)

- `infra/` (Terraform) creates ECR repositories, an ECS Fargate cluster with one service per microservice, an Application Load Balancer with path-based routing, Cloud Map service discovery (booking-service calls event-service by name), CloudWatch log groups, and deployment circuit breakers that roll back a bad release automatically. Postgres (RDS) and Redis (ElastiCache) endpoints and a Secrets Manager secret are passed in as variables.
- `.github/workflows/ci-cd.yml` runs all tests and builds the images on every push. On `main`, once AWS is configured, it pushes images to ECR and rolls out the ECS services.

## Tech

Java 21, Spring Boot 3, Spring Data JPA, PostgreSQL, Redis, JWT (jjwt), JUnit 5, MockMvc, React + TypeScript (Vite), Docker, nginx, Terraform, GitHub Actions.
