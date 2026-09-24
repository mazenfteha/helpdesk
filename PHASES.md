# Helpdesk Ticket Management API — Development Phases

## Rules for AI Coding Agents

Before implementing any phase:

1. Read the existing project structure.
2. Read this file.
3. Identify the current phase.
4. Implement ONLY the requested phase.
5. Do not implement future-phase functionality.
6. Do not introduce dependencies without justification.
7. Preserve existing behavior.
8. Add/update tests for meaningful changes.
9. Do not refactor unrelated code.
10. Explain important architectural decisions after implementation.

A phase is complete only when its Definition of Done is satisfied.

---

# PHASE 0 — Requirements & Domain Design

## Objective

Finalize the product requirements and domain model before implementation.

## Technical Concepts

* Domain modeling
* Entities and relationships
* Business rules
* Ownership
* Authorization boundaries
* API responsibilities
* Database constraints

## Tasks

* Review the PRD.
* Finalize entities.
* Finalize relationships.
* Finalize roles.
* Finalize ticket statuses.
* Finalize status transitions.
* Finalize assignment rules.
* Finalize initial API contract.
* Identify important validation rules.
* Identify important database constraints.
* Identify initial indexes.

## Database Changes

No implementation migrations yet.

## Testing

No automated tests yet.

## Definition of Done

* Domain model is documented.
* Business rules are documented.
* API responsibilities are clear.
* Authorization rules are documented.
* No major unresolved domain decisions remain.

---

# PHASE 1 — Spring Boot Project Setup

## Objective

Create the Spring Boot application and establish the project foundation.

## Technical Concepts

* Spring Boot
* Dependency Injection
* Application configuration
* Project structure
* Profiles
* Maven/Gradle
* Spring application lifecycle

## Tasks

* Create Spring Boot project.
* Configure dependencies.
* Establish package structure.
* Create application configuration.
* Add development profile.
* Verify application startup.
* Add basic health/startup verification.

## Database Changes

None.

## Testing

* Application context/startup test.

## Definition of Done

* Application starts successfully.
* Project structure is clear.
* Configuration works.
* Basic test suite runs successfully.

---

# PHASE 2 — PostgreSQL + Docker + Flyway

## Objective

Connect the application to PostgreSQL and manage schema through Flyway.

## Technical Concepts

* PostgreSQL
* Docker Compose
* Database configuration
* Flyway
* Database migrations
* JPA configuration

## Tasks

* Create PostgreSQL Docker Compose configuration.
* Configure environment variables.
* Connect Spring Boot to PostgreSQL.
* Configure Flyway.
* Create initial migrations.
* Create database constraints.
* Verify migrations.
* Create initial entities where appropriate.

## Database Changes

Create:

* users
* ticket_categories
* tickets
* comments

Additional supporting schema only if required by the finalized design.

## Testing

* Database connectivity test.
* Migration verification.

## Definition of Done

* PostgreSQL runs through Docker.
* Spring Boot connects successfully.
* Flyway manages schema creation.
* Application does not rely on automatic production schema creation.

---

# PHASE 3 — Authentication

## Objective

Implement user registration, login, password hashing, and authenticated requests.

## Technical Concepts

* Spring Security
* Authentication
* Security context
* Password hashing
* Authentication filters
* JWT/session strategy
* Current authenticated user

## Endpoints

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
```

## Tasks

* Implement user registration.
* Hash passwords securely.
* Implement login.
* Implement authentication mechanism.
* Protect private endpoints.
* Resolve current authenticated user.
* Prevent password hash exposure.

## Database Changes

Use the users table from Phase 2.

## Testing

* Registration success.
* Duplicate email.
* Invalid credentials.
* Protected endpoint without authentication.
* Authenticated request.
* Password hash is never returned.

## Definition of Done

A user can register, authenticate, and access protected endpoints.

---

# PHASE 4 — Core Ticket Management

## Objective

Implement ticket creation and retrieval.

## Endpoints

```text
POST /api/tickets
GET  /api/tickets
GET  /api/tickets/{id}
```

## Technical Concepts

* Controllers
* Services
* Repositories
* DTOs
* JPA entities
* Relationships
* HTTP status codes

## Tasks

* Create ticket entity.
* Create repository.
* Create request/response DTOs.
* Implement service.
* Implement controller.
* Implement customer ownership.
* Prevent clients from supplying ownership from arbitrary user IDs.

## Database Changes

Tickets and relationships as defined by the domain model.

## Testing

* Create ticket.
* Get own ticket.
* Get ticket.
* Missing ticket.
* Unauthorized access.
* Invalid request.

## Definition of Done

Customers can create and retrieve tickets securely.

---

# PHASE 5 — Comments & Relationships

## Objective

Implement ticket comments and deepen JPA relationship handling.

## Endpoints

```text
POST /api/tickets/{ticketId}/comments
GET  /api/tickets/{ticketId}/comments
```

## Technical Concepts

* `@ManyToOne`
* `@OneToMany`
* Lazy loading
* DTO mapping
* Entity lifecycle
* N+1 query risks

## Tasks

* Implement comments.
* Associate comments with tickets/users.
* Enforce access rules.
* Avoid exposing entities directly.
* Investigate generated SQL.
* Identify potential N+1 behavior.

## Testing

* Create comment.
* List comments.
* Invalid ticket.
* Unauthorized comment.
* Ownership/access checks.

## Definition of Done

Comments work with correct relationships and authorization.

---

# PHASE 6 — Assignment & Authorization

## Objective

Implement role-specific ticket management.

## Assignment Rules

```text
ADMIN
  → assign/reassign to eligible agents

AGENT
  → claim unassigned tickets

AGENT
  → cannot assign to another agent

CUSTOMER
  → cannot assign
```

## Status Rules

Only:

```text
AGENT
ADMIN
```

may change status.

The service must validate status transitions.

## Technical Concepts

* Spring Security authorization
* Roles
* Authorities
* Security context
* Ownership
* Business authorization
* Service-layer authorization

## Tasks

* Implement assignment endpoint.
* Implement claim behavior.
* Implement role restrictions.
* Implement ownership/assignment checks.
* Implement status transition rules.
* Separate security checks from business rules.

## Testing

Test role and resource combinations, including:

* Customer attempting assignment.
* Agent claiming unassigned ticket.
* Agent claiming assigned ticket.
* Agent assigning another agent.
* Admin assigning agent.
* Customer changing status.
* Agent changing status.
* Invalid status transition.

## Definition of Done

Role and resource authorization rules are enforced.

---

# PHASE 7 — Pagination & Filtering

## Objective

Make ticket listing useful and scalable.

## Technical Concepts

* `Pageable`
* Sorting
* Query methods
* Dynamic filtering
* SQL query generation
* Indexing
* Query performance

## Tasks

* Add pagination.
* Add appropriate sorting.
* Add selected filters.
* Define maximum page size.
* Inspect generated queries.
* Add indexes based on access patterns.

## Testing

* Pagination.
* Sorting.
* Each supported filter.
* Invalid filter.
* Maximum page size behavior.

## Definition of Done

Ticket listing supports controlled pagination/filtering without unnecessary query complexity.

---

# PHASE 8 — Validation & Global Error Handling

## Objective

Create consistent API validation and error behavior.

## Technical Concepts

* Bean Validation
* `@Valid`
* Exception handlers
* HTTP status codes
* Error DTOs
* Domain exceptions

## Tasks

* Validate request DTOs.
* Create global exception handler.
* Create meaningful domain exceptions.
* Define error response structure.
* Handle common Spring exceptions.
* Prevent sensitive internal information from leaking.

## Testing

* Validation errors.
* Not found.
* Forbidden.
* Unauthorized.
* Conflict.
* Invalid state transition.

## Definition of Done

API errors are consistent, meaningful, and safe.

---

# PHASE 9 — Testing & Integration

## Objective

Build confidence in the application through multiple testing levels.

## Technical Concepts

* Unit tests
* Mockito
* MockMvc
* Spring Boot Test
* Repository tests
* Integration testing
* Test isolation

## Tasks

* Review existing tests.
* Add missing unit tests.
* Add controller tests.
* Add repository tests where useful.
* Add critical integration tests.
* Test authentication/authorization flows.
* Test important database behavior.

## Definition of Done

Critical business behavior is covered by meaningful automated tests.

---

# PHASE 10 — Docker & Production Configuration

## Objective

Make the application reproducible and production-oriented.

## Technical Concepts

* Docker
* Docker Compose
* Environment variables
* Spring profiles
* Configuration management
* Container networking

## Tasks

* Containerize Spring Boot.
* Configure application/database networking.
* Externalize configuration.
* Create appropriate development/production configuration.
* Verify migrations inside deployment flow.
* Document startup process.

## Definition of Done

The application can be started reproducibly using documented configuration.

---

# PHASE 11 — Code Review & Refactoring

## Objective

Review the project as a senior engineer would before merging.

## Review Areas

* Package structure
* Naming
* Dependency injection
* Controller responsibilities
* Service responsibilities
* Repository queries
* DTO design
* Entity design
* Transactions
* Security
* Exception handling
* Tests
* Logging
* Configuration
* Database constraints
* Query performance

## Tasks

* Identify code smells.
* Remove unnecessary abstractions.
* Review transaction boundaries.
* Investigate N+1 queries.
* Review API consistency.
* Review security boundaries.
* Improve documentation where necessary.

## Definition of Done

The project has undergone a structured production-oriented code review.

---

# PHASE 12 — Final Interview Review

## Objective

Turn the project into interview knowledge rather than simply a GitHub repository.

## Topics

### Spring

* Dependency Injection
* IoC
* Beans
* Bean lifecycle
* `@Component`
* `@Service`
* `@Repository`
* `@RestController`

### JPA/Hibernate

* Entity lifecycle
* Relationships
* Lazy/eager loading
* N+1
* Persistence context
* Dirty checking
* Transactions

### Security

* Authentication flow
* Security filter chain
* Password hashing
* JWT/authentication mechanism
* Roles
* Authorization
* Ownership

### API

* REST design
* DTOs
* Validation
* Status codes
* Pagination
* Error handling

### Database

* Relationships
* Constraints
* Indexes
* Transactions
* Concurrency

### Testing

* Unit vs integration
* Mocking
* Repository testing
* API testing

## Definition of Done

You can explain the architecture, implementation decisions, trade-offs, and important Spring internals without relying on the codebase as a script.
