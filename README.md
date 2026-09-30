# Helpdesk Ticket Management API

A REST API for customer support tickets, built with Spring Boot. Customers open tickets, agents
claim and work on them, and admins assign tickets and manage categories.

- **Stack:** Java 21, Spring Boot 4, Spring Security (JWT), Spring Data JPA / Hibernate,
  PostgreSQL 16, Flyway
- **Docs:** [PRD](PRD.md) · [Design](docs/DESIGN.md) · [Phases](PHASES.md) · [CRUD guide](docs/CRUD_GUIDE.md)

---

## Prerequisites

- **JDK 21**
- **Docker** (Docker Desktop on Windows/macOS)
- **Git Bash** or another POSIX shell for the commands below

Maven isn't required: the project ships with the Maven wrapper (`./mvnw`).

---

## Quick start (local development)

**1. Start PostgreSQL**

```bash
docker compose up -d
```

This starts PostgreSQL 16 on **`localhost:5433`** (database, user and password: `helpdesk`).
Data lives in the `postgres_data` Docker volume and survives restarts.

**2. Run the app**

```bash
./mvnw spring-boot:run
```

With no profile set, the app runs with the **`dev`** profile, which points at the database from
step 1 and uses a development JWT secret. On startup, **Flyway** creates or updates the schema
and seeds the default ticket categories.

**3. Check it's up**

```bash
curl http://localhost:8080/api/health
# UP
```

The API listens on **`http://localhost:8080`**.

---

## Configuration

Configuration is split by **Spring profile**:

| File | Used when | Contents |
|---|---|---|
| `application.properties` | always | Shared settings. Secrets come only from environment variables, with no defaults |
| `application-dev.properties` | `dev` profile (the default for local runs) | Local database on `localhost:5433` and a development JWT secret |
| `application-prod.properties` | `prod` profile (set by the Docker image) | Production logging |

### Environment variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `DATABASE_URL` | prod | — | JDBC URL, e.g. `jdbc:postgresql://host:5432/helpdesk` |
| `DATABASE_USERNAME` | prod | — | Database user |
| `DATABASE_PASSWORD` | prod | — | Database password |
| `JWT_SECRET` | prod | — | HS256 signing key, **at least 32 bytes**. Generate with `openssl rand -base64 48` |
| `JWT_EXPIRATION` | no | `PT1H` | Token lifetime (ISO-8601 duration) |
| `PORT` | no | `8080` | HTTP port |

In the `prod` profile, the app **refuses to start** if a required variable is missing, and also
if `JWT_SECRET` is shorter than 32 bytes.

Spring Boot doesn't read `.env` files. `.env.example` lists the variables for tools that do, such
as Docker Compose. Copy it to `.env`; `.env` is git-ignored and must never be committed.

---

## Running with the prod profile

### From source

```bash
DATABASE_URL=jdbc:postgresql://localhost:5433/helpdesk \
DATABASE_USERNAME=helpdesk \
DATABASE_PASSWORD=helpdesk \
JWT_SECRET=$(openssl rand -base64 48) \
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

### As a Docker image

The `Dockerfile` is a multi-stage build: Maven compiles the jar in a JDK image, and the final image
contains only a Java 21 JRE and the jar. It runs as a non-root user with the `prod` profile active.

```bash
docker build -t helpdesk-api .

docker run --rm -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5433/helpdesk \
  -e DATABASE_USERNAME=helpdesk \
  -e DATABASE_PASSWORD=helpdesk \
  -e JWT_SECRET=$(openssl rand -base64 48) \
  helpdesk-api
```

Inside a container, `localhost` means the container itself. `host.docker.internal` reaches the
PostgreSQL port published on your machine (Docker Desktop).

The image build skips tests (`-DskipTests`): the test suite needs a running database, so tests
should run in CI before the image is built.

> A JWT is signed with `JWT_SECRET`. Tokens issued under one secret (for example in `dev`) are
> rejected under another. Log in again after switching.

---

## Users and roles

| Role | Can do |
|---|---|
| `CUSTOMER` | Create, view and edit their own tickets (while `OPEN`), comment on their own tickets |
| `AGENT` | View unassigned tickets and tickets assigned to them, claim unassigned tickets, change status and comment on their assigned tickets |
| `ADMIN` | View all tickets, assign or reassign to any agent, change any status, delete tickets, manage categories |

`POST /api/auth/register` always creates a **`CUSTOMER`**; clients can't choose a role.
To create an agent or admin, register the user through the API first (so the password is hashed
correctly), then change the role in the database:

```bash
docker exec -it helpdesk-postgres psql -U helpdesk -d helpdesk \
  -c "update users set role = 'ADMIN' where email = 'admin@example.com';"
```

Roles are stored in the JWT, so the user has to **log in again** after a role change.

---

## Using the API

### Log in and call an endpoint

```bash
B=http://localhost:8080

curl -X POST $B/api/auth/register -H "Content-Type: application/json" \
  -d '{"name":"Ahmed","email":"ahmed@example.com","password":"secret123"}'

TOKEN=$(curl -s -X POST $B/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"ahmed@example.com","password":"secret123"}' \
  | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')

curl $B/api/auth/me -H "Authorization: Bearer $TOKEN"
```

Every endpoint except `health`, `register` and `login` needs the header
`Authorization: Bearer <accessToken>`. Tokens expire after 1 hour by default.

### Endpoints

**Auth**

| Method | Path | Who | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | public | Register a customer → `201` |
| `POST` | `/api/auth/login` | public | Returns `accessToken`, `tokenType`, `expiresIn` |
| `GET` | `/api/auth/me` | authenticated | The current user |

**Tickets**

| Method | Path | Who | Description |
|---|---|---|---|
| `POST` | `/api/tickets` | CUSTOMER | Create a ticket (`title`, `description`, `categoryId`, optional `priority`, default `MEDIUM`) → `201` |
| `GET` | `/api/tickets` | authenticated | List the tickets visible to the caller, newest first |
| `GET` | `/api/tickets/{id}` | authenticated | One ticket (`404` if missing or not visible to the caller) |
| `PUT` | `/api/tickets/{id}` | CUSTOMER (owner) | Replace `title`, `description`, `categoryId`, `priority`; only while `OPEN` |
| `DELETE` | `/api/tickets/{id}` | ADMIN | Delete a ticket and its comments → `204` |
| `PATCH` | `/api/tickets/{id}/status` | AGENT (assignee), ADMIN | Change the status: `{"status":"IN_PROGRESS"}` |
| `POST` | `/api/tickets/{id}/claim` | AGENT | Claim an unassigned ticket |
| `PATCH` | `/api/tickets/{id}/assignment` | ADMIN | Assign or reassign: `{"agentId":"<uuid>"}` |

**Comments**

| Method | Path | Who | Description |
|---|---|---|---|
| `POST` | `/api/tickets/{ticketId}/comments` | ticket owner, assigned agent, ADMIN | Add a comment (`content`) → `201` |
| `GET` | `/api/tickets/{ticketId}/comments` | anyone who can view the ticket | Comments, oldest first |

**Categories**

| Method | Path | Who | Description |
|---|---|---|---|
| `GET` | `/api/categories` | authenticated | All categories, sorted by name |
| `GET` | `/api/categories/{id}` | authenticated | One category |
| `POST` | `/api/categories` | ADMIN | Create (`name`, unique, case-insensitive) → `201` |
| `PUT` | `/api/categories/{id}` | ADMIN | Rename |
| `DELETE` | `/api/categories/{id}` | ADMIN | Delete → `204`; `409` while tickets use it |

**Other:** `GET /api/health` and `GET /actuator/health` (public).

### Ticket status workflow

```text
OPEN ──► IN_PROGRESS ──► RESOLVED ──► CLOSED
  │           ▲              │
  │           └── reopen ────┘
  └─────────────────────────────────► CLOSED   (duplicate / spam)
```

`CLOSED` is final: no status changes, comments or assignments. Any other transition returns `409`.

### Errors

Errors from the API's controllers share one JSON format:

```json
{
  "timestamp": "2026-09-30T10:15:30.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Ticket not found",
  "path": "/api/tickets/00000000-0000-0000-0000-000000000000"
}
```

Validation errors (`400`) add a `fieldErrors` object, e.g.
`"fieldErrors": {"title": "must not be blank"}`. Stack traces are never returned; unexpected errors
are logged on the server and return a generic `500`.

`401` and `403` responses produced by Spring Security (missing or invalid token, wrong role for a
URL) have an empty body. The `WWW-Authenticate` header describes `401` errors.

---

## Database

- The schema is managed **only by Flyway** (`src/main/resources/db/migration`). Hibernate just
  validates it on startup (`ddl-auto=validate`).
- To change the schema, add a new migration, e.g. `V3__add_something.sql`. **Never edit a migration
  that has already run**: Flyway stores a checksum per file and fails on startup if it changes.
- Connect with psql:

  ```bash
  docker exec -it helpdesk-postgres psql -U helpdesk -d helpdesk
  ```

- Reset everything, **deleting all data**:

  ```bash
  docker compose down -v
  ```

---

## Tests

```bash
./mvnw test
```

The current test suite only checks that the application context starts. It uses the `dev`
profile, so PostgreSQL from the quick start must be running.

---

## Known limitations

- `GET /api/tickets` isn't paginated yet and returns every visible ticket.
- The ticket and comment lists run one extra query per related user and category (N+1).
- The first admin has to be created manually (see [Users and roles](#users-and-roles)).
- `docker-compose.yml` starts only PostgreSQL; the app runs from source or with `docker run`.
- Automated test coverage is minimal.
