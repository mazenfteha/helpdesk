# Helpdesk Ticket Management API — Phase 0 Design

## 1. Project Overview

The Helpdesk Ticket Management API is a small but production-oriented Spring Boot backend.

The system allows customers to create and track support tickets, while agents and administrators manage those tickets.

The project is intentionally small enough to understand deeply while covering important backend engineering concepts:

- Spring Boot
- Dependency Injection and IoC
- REST APIs
- PostgreSQL
- JPA/Hibernate
- Flyway migrations
- Spring Security
- Authentication and authorization
- DTOs and validation
- Transactions
- Pagination and filtering
- Global exception handling
- Unit, repository, controller, and integration testing
- Docker and production configuration

The goal is not to build a large product. The goal is to build a realistic backend and understand the engineering decisions behind it.

---

## 2. Core Users and Roles

The system has three roles:

- `CUSTOMER`
- `AGENT`
- `ADMIN`

Roles are stored directly on the `users` table as an enum/string value.

### CUSTOMER

Customers can:

- Register and log in
- Create tickets
- View their own tickets
- View comments on tickets they are allowed to access
- Add comments to their tickets

Customers cannot:

- Assign tickets
- Change ticket status
- Access other customers' tickets

### AGENT

Agents can:

- View tickets available to them according to the ticket access rules
- Claim an unassigned ticket for themselves
- Work on assigned tickets
- Change ticket status
- Add comments

An agent cannot:

- Assign a ticket to another agent
- Reassign another agent's ticket
- Claim a ticket that is already assigned

### ADMIN

Administrators can:

- View all tickets
- Assign tickets to agents
- Reassign tickets
- Change ticket status
- Manage categories
- Add comments

---

## 3. Database Design

The initial database contains four tables:

1. `users`
2. `ticket_categories`
3. `tickets`
4. `comments`

Keeping roles inside `users` allows the project to stay small without introducing a separate roles table.

---

## 4. users

Stores authentication and user information.

### Columns

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | Primary key, NOT NULL |
| `name` | VARCHAR(100) | NOT NULL |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE |
| `password_hash` | VARCHAR(255) | NOT NULL |
| `role` | VARCHAR/enum | NOT NULL |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

Allowed roles:

- `CUSTOMER`
- `AGENT`
- `ADMIN`

Passwords are never stored in plaintext. Only a password hash is stored.

Password hashes must never be returned in API DTOs.

The application will also need an explicit email normalization policy, such as storing emails in lowercase.

---

## 5. ticket_categories

Stores ticket categories.

### Columns

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | Primary key, NOT NULL |
| `name` | VARCHAR(100) | NOT NULL, UNIQUE |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

Example categories:

- Technical Support
- Billing
- Account
- General

Categories should not be deleted in a way that accidentally deletes tickets.

---

## 6. tickets

Stores support tickets.

### Columns

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | Primary key, NOT NULL |
| `title` | VARCHAR(200) | NOT NULL |
| `description` | TEXT | NOT NULL |
| `status` | VARCHAR/enum | NOT NULL |
| `priority` | VARCHAR/enum | NOT NULL |
| `customer_id` | UUID | NOT NULL, FK → `users.id` |
| `category_id` | UUID | NOT NULL, FK → `ticket_categories.id` |
| `assigned_to` | UUID | NULL, FK → `users.id` |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

Allowed statuses:

- `OPEN`
- `IN_PROGRESS`
- `RESOLVED`
- `CLOSED`

Allowed priorities:

- `LOW`
- `MEDIUM`
- `HIGH`
- `URGENT`

### Ticket creation

A new ticket:

- Starts with `OPEN`
- Has no assigned agent (`assigned_to = NULL`)
- Gets its customer from the authenticated user
- Gets its category from the request

The client must not be allowed to choose the `customer_id`.

The application should not trust a client-provided user ID for ownership.

---

## 7. comments

Stores comments associated with tickets.

### Columns

| Column | Type | Constraints |
|---|---|---|
| `id` | UUID | Primary key, NOT NULL |
| `content` | TEXT | NOT NULL |
| `ticket_id` | UUID | NOT NULL, FK → `tickets.id` |
| `user_id` | UUID | NOT NULL, FK → `users.id` |
| `created_at` | TIMESTAMP | NOT NULL |

Comments are initially treated as immutable records.

There is no `updated_at` in version 1.

The comment author comes from the authenticated user rather than from the request body.

---

## 8. Relationships

### User → Tickets as customer

One user can create many tickets.

```text
users 1 ──────── N tickets
              customer_id
```

### User → Tickets as assigned agent

One agent can be assigned many tickets.

```text
users 1 ──────── N tickets
              assigned_to
```

This is a second relationship between the same two tables.

### User → Comments

One user can create many comments.

```text
users 1 ──────── N comments
```

### Category → Tickets

One category can contain many tickets.

```text
ticket_categories 1 ──────── N tickets
```

### Ticket → Comments

One ticket can contain many comments.

```text
tickets 1 ──────── N comments
```

---

## 9. Important Database Rules

The database should enforce data integrity where appropriate.

Examples:

- Primary keys are required
- Foreign keys are required where relationships are mandatory
- `users.email` is unique
- `ticket_categories.name` is unique
- Required fields are `NOT NULL`
- `tickets.assigned_to` is nullable
- Stable string values should be used for status/priority rather than ordinal numeric values

The database cannot conveniently enforce every business rule.

For example, the foreign key can verify that `assigned_to` references an existing user, but the application still needs to verify that the selected user has the `AGENT` role.

---

## 10. Assignment Rules

There are two assignment operations.

### Admin assignment

An admin can:

- Assign an unassigned ticket to an agent
- Reassign a ticket from one agent to another agent

The target user must be an eligible agent.

### Agent claim

An agent can claim a ticket only when:

```text
assigned_to == NULL
```

The ticket becomes assigned to the authenticated agent.

An agent cannot silently take ownership of another agent's ticket.

This rule is enforced in the service/business layer.

---

## 11. Status Rules

Only:

- `AGENT`
- `ADMIN`

can request a ticket status change.

A customer cannot change status.

Authorization answers:

> Is this user allowed to perform a status change?

The business logic separately answers:

> Is this particular status transition valid?

The exact transition matrix will be finalized before implementation.

The initial workflow is:

```text
OPEN
  ↓
IN_PROGRESS
  ↓
RESOLVED
  ↓
CLOSED
```

The service layer will own transition validation.

---

## 12. Authentication and Ownership

Three concepts must remain separate:

### Authentication

Who is the user?

### Authorization

Is this role allowed to perform this operation?

### Ownership

Is this particular resource owned by or accessible to this user?

Example:

A customer may be authenticated and have permission to view tickets, but that does not mean they can view every ticket in the database.

---

## 13. API Contract

### Authentication

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
```

### Tickets

```text
POST   /api/tickets
GET    /api/tickets
GET    /api/tickets/{id}
PATCH  /api/tickets/{id}/status
PATCH  /api/tickets/{id}/assignment
POST   /api/tickets/{id}/claim
```

### Comments

```text
POST /api/tickets/{ticketId}/comments
GET  /api/tickets/{ticketId}/comments
```

### Categories

```text
POST   /api/categories
GET    /api/categories
PATCH  /api/categories/{id}
DELETE /api/categories/{id}
```

The API contract may evolve during implementation when we discover a better design.

---

## 14. Ticket Listing

The main ticket listing endpoint will support pagination.

Example:

```text
GET /api/tickets?page=0&size=20
```

Possible filters:

- status
- priority
- category
- assigned agent

Possible sorting will be explicitly controlled rather than exposing arbitrary database fields.

Access rules will depend on the authenticated user's role.

### CUSTOMER

Can see their own tickets.

### ADMIN

Can see all tickets.

### AGENT

Can see tickets relevant to their work, including tickets assigned to them and tickets that are available to claim according to the final access policy.

---

## 15. Validation

Input validation will cover at least:

### User

- Name required
- Valid email
- Password requirements

### Ticket

- Title required
- Description required
- Category required
- Valid priority

### Comment

- Content required
- Reasonable length limits

Exact limits will be finalized during implementation.

---

## 16. HTTP Status Strategy

Initial API status conventions:

| Situation | Status |
|---|---:|
| Successful creation | `201 Created` |
| Successful read/update | `200 OK` |
| Successful deletion | `204 No Content` |
| Invalid request | `400 Bad Request` |
| Not authenticated | `401 Unauthorized` |
| Not authorized | `403 Forbidden` |
| Resource not found | `404 Not Found` |
| Business/data conflict | `409 Conflict` |

The exact validation/error strategy can be refined when global error handling is implemented.

---

## 17. Transaction Boundaries

Operations that change important state should have clear transaction boundaries.

Examples:

- Create ticket
- Claim ticket
- Assign/reassign ticket
- Change ticket status
- Add comment where multiple database operations are involved

Special attention is required for ticket claiming because two agents could attempt to claim the same unassigned ticket concurrently.

The implementation should prevent an invalid double-claim.

---

## 18. Indexing Strategy

Initial indexes:

- Unique index on `users.email`
- Index on `tickets.customer_id`
- Index on `tickets.assigned_to`
- Potential index on `tickets.status`
- Potential index on `tickets.category_id`

Composite indexes should not be added blindly.

They should be introduced when actual query/access patterns justify them.

---

## 19. Deletion Strategy

We should avoid cascading deletion of important business data.

In particular:

- Deleting a user should not automatically delete their tickets.
- Deleting a category should not delete its tickets.
- Ticket/comment deletion needs deliberate treatment.

For version 1, destructive operations should be conservative.

The exact category and user deletion policy will be finalized during implementation.

---

## 20. Security Rules

Important security boundaries:

1. Registration must not allow the client to create an arbitrary role.
2. A normal registration should create a `CUSTOMER`.
3. Passwords must be securely hashed.
4. Password hashes must never appear in API responses.
5. Customer ownership must come from the authenticated identity.
6. Comment authorship must come from the authenticated identity.
7. Customers cannot change ticket status.
8. Customers cannot assign tickets.
9. Agents cannot assign tickets to other agents.
10. Agents can claim only unassigned tickets.
11. Admins can assign and reassign tickets.

---

## 21. Testing Strategy

The project should eventually contain several levels of tests.

### Unit tests

Business rules such as:

- Status transitions
- Assignment rules
- Claim rules
- Authorization decisions where appropriate

### Repository/data tests

Verify:

- Persistence
- Queries
- Relationships
- Constraints

### Controller/API tests

Verify:

- HTTP status codes
- Request validation
- Response shape
- Authentication/authorization behavior

### Integration tests

Verify realistic flows such as:

```text
Register
  ↓
Login
  ↓
Create ticket
  ↓
Admin assigns agent
  ↓
Agent changes status
  ↓
Agent adds comment
  ↓
Customer reads ticket
```

---

## 22. Production-Oriented Concerns

During later phases we will explicitly review:

- Secure authentication
- Authorization boundaries
- DTO/entity separation
- Transaction boundaries
- Database constraints
- Query performance
- N+1 queries
- Pagination
- Validation
- Consistent error responses
- Logging
- Configuration management
- Secrets
- Environment variables
- Profiles
- Testing
- Docker deployment

---

## 23. Non-Goals

The first version intentionally does NOT include:

- Microservices
- Kafka
- Redis
- WebSockets
- File uploads
- Email notifications
- Real-time notifications
- Kubernetes
- Distributed tracing
- Complex permission management UI
- Frontend application

These technologies may be useful in other systems, but adding them here would distract from the Spring Boot/backend fundamentals we are trying to master.

---

# Phase Plan

## Phase 0 — Requirements & Domain Design

Finalize:

- Domain model
- Database model
- Roles
- Ownership rules
- Assignment rules
- Status rules
- API responsibilities
- Validation rules
- Database constraints
- Indexing strategy

No implementation yet.

## Phase 1 — Spring Boot Project Setup

Learn and implement:

- Java/Spring Boot project structure
- Maven
- Spring Boot startup
- IoC
- Dependency Injection
- ApplicationContext
- Component scanning
- Configuration
- Basic REST endpoint
- Basic application test

## Phase 2 — PostgreSQL + Docker + Flyway

Implement:

- PostgreSQL
- Docker Compose
- Environment configuration
- Flyway
- Initial database migrations
- Database constraints
- JPA entities
- Database connectivity

## Phase 3 — Authentication

Implement:

- Spring Security
- Registration
- Login
- Password hashing
- Authentication context
- Current user
- Authentication protection

## Phase 4 — Core Ticket Management

Implement:

- Ticket creation
- Ticket retrieval
- Customer ownership
- Controllers
- Services
- Repositories
- DTOs
- JPA mapping

## Phase 5 — Comments & Relationships

Learn and implement:

- Entity relationships
- Many-to-one
- One-to-many
- Lazy loading
- DTO mapping
- N+1 risks
- Ticket comments

## Phase 6 — Assignment & Authorization

Implement:

- Admin assignment
- Admin reassignment
- Agent claim
- Ownership rules
- Role authorization
- Status transition rules

## Phase 7 — Pagination & Filtering

Implement:

- Pageable
- Sorting
- Filtering
- Query methods
- Dynamic queries where justified
- Index review
- Query performance

## Phase 8 — Validation & Global Error Handling

Implement:

- Bean Validation
- `@Valid`
- Global exception handling
- Domain exceptions
- Consistent error DTOs
- Safe error messages

## Phase 9 — Testing & Integration

Implement/review:

- JUnit
- Mockito
- MockMvc
- Spring Boot tests
- Repository tests
- Integration tests
- Security tests
- Full business flows

## Phase 10 — Docker & Production Configuration

Implement:

- Spring Boot Docker image
- Docker networking
- Environment variables
- Profiles
- Production configuration
- Database migrations during deployment

## Phase 11 — Code Review & Refactoring

Review:

- Architecture
- Package structure
- Dependency injection
- Controllers
- Services
- Repositories
- DTOs
- Entities
- Transactions
- Security
- Error handling
- Tests
- Logging
- Database constraints
- Query performance

## Phase 12 — Final Interview Review

Review the project through interview questions covering:

- Spring vs Spring Boot
- IoC/DI
- Beans
- ApplicationContext
- Annotations
- JPA/Hibernate
- Entity lifecycle
- Persistence context
- Dirty checking
- Lazy/eager loading
- N+1
- Transactions
- Spring Security
- Authentication
- Authorization
- REST
- DTOs
- Validation
- HTTP status codes
- Pagination
- Database indexes
- Concurrency
- Testing
- Docker
- Production trade-offs
