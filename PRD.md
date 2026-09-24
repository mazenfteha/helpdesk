# Helpdesk Ticket Management API

## 1. Product Overview

A production-oriented REST API for managing customer support tickets.

The system allows customers to create and track support tickets, while support agents and administrators manage the ticket lifecycle.

The project is intentionally small in scope but should expose the engineering concerns commonly encountered in a real Spring Boot backend:

* REST API design
* Relational database modeling
* JPA/Hibernate
* Authentication and authorization
* Business rules
* Transactions
* Validation
* Pagination and filtering
* Exception handling
* Automated testing
* Dockerized PostgreSQL
* Production-oriented configuration

The goal is not to build a feature-heavy helpdesk product.

The goal is to build a backend that is small enough to understand completely while containing realistic engineering problems.

---

# 2. Technology Stack

## Backend

* Java
* Spring Boot
* Spring Web
* Spring Data JPA
* Hibernate
* Spring Security
* Bean Validation
* Flyway
* PostgreSQL

## Testing

* JUnit
* Mockito where appropriate
* Spring Boot Test
* MockMvc
* Integration tests

## Infrastructure

* Docker
* Docker Compose
* PostgreSQL container

The project should avoid introducing Redis, Kafka, microservices, Kubernetes, or other infrastructure unless a concrete learning requirement emerges.

---

# 3. User Roles

The system has three roles:

```text
CUSTOMER
AGENT
ADMIN
```

A user has exactly one role.

For the initial version, the role is represented as an enum stored on the `users` table.

## CUSTOMER

Customers can:

* Register.
* Login.
* View their own profile.
* Create tickets.
* View their own tickets.
* View details of their own tickets.
* Add comments to their own tickets where permitted.

Customers cannot:

* View other customers' tickets.
* Assign tickets.
* Change ticket status.
* Manage categories.
* Manage users.

## AGENT

Agents can:

* Login.
* View tickets assigned to themselves.
* View appropriate unassigned tickets.
* Claim unassigned tickets.
* Add comments to tickets they are allowed to work on.
* Change ticket status according to the allowed workflow.

Agents cannot:

* Assign another agent to a ticket.
* Reassign tickets owned by another agent.
* Manage users.
* Manage categories unless explicitly authorized later.

## ADMIN

Administrators can:

* Login.
* View/manage tickets.
* Assign tickets to agents.
* Reassign tickets.
* Change ticket status.
* Manage ticket categories.
* Manage users where appropriate.

---

# 4. Core Domain

The initial database contains approximately five tables.

## users

Represents system users.

Important fields:

* id
* name
* email
* password_hash
* role
* created_at
* updated_at

Requirements:

* Email must be unique.
* Password must never be stored in plaintext.
* Role must be one of the supported roles.
* Email should be normalized appropriately during registration/login.

---

## ticket_categories

Represents categories used to classify tickets.

Examples:

```text
Technical Support
Billing
Account
General
```

Important fields:

* id
* name
* created_at
* updated_at

Requirements:

* Category name should be unique.
* Category name cannot be empty.
* Categories are managed by authorized users.

---

## tickets

Represents customer support requests.

Important fields:

* id
* title
* description
* status
* priority
* customer_id
* category_id
* assigned_to
* created_at
* updated_at

Relationships:

```text
Customer ───────< Tickets
Category ───────< Tickets
Agent ──────────< Tickets
```

`assigned_to` is nullable.

A newly created ticket starts unassigned:

```text
assigned_to = NULL
```

---

## comments

Represents communication attached to a ticket.

Important fields:

* id
* content
* ticket_id
* user_id
* created_at

Relationships:

```text
Ticket ───────< Comments
User ─────────< Comments
```

A comment belongs to exactly one ticket and has exactly one author.

---

# 5. Ticket Status

Initial statuses:

```text
OPEN
IN_PROGRESS
RESOLVED
CLOSED
```

The system must not treat status as an arbitrary string.

Status transitions will be controlled by business rules.

Initial proposed workflow:

```text
OPEN
  ↓
IN_PROGRESS
  ↓
RESOLVED
  ↓
CLOSED
```

The exact allowed transitions will be finalized during implementation.

The service layer is responsible for enforcing business rules around state transitions.

Spring Security is responsible for determining whether the authenticated user has permission to attempt the operation.

---

# 6. Ticket Priority

Initial priorities:

```text
LOW
MEDIUM
HIGH
URGENT
```

Priority is separate from status.

For example:

```text
status   = IN_PROGRESS
priority = HIGH
```

A customer's ticket may be created with a supported default priority if the API design decides that customers should not control priority directly.

This will be decided during API design.

---

# 7. Ticket Assignment Rules

## New ticket

When a customer creates a ticket:

```text
assigned_to = NULL
```

## Admin assignment

An administrator can assign an unassigned or assigned ticket to an eligible agent.

Example conceptual operation:

```text
Admin
  ↓
Assign Ticket #123
  ↓
Agent #42
```

## Agent claim

An agent can claim an unassigned ticket.

Requirement:

```text
assigned_to == NULL
```

The agent becomes the assignee.

An agent cannot use the assignment API to assign the ticket to another agent.

An agent cannot reassign another agent's ticket.

## Invalid assignment

The system must reject cases such as:

* Assigning a ticket to a customer.
* Assigning a ticket to a nonexistent user.
* Assigning a ticket to an invalid role.
* Claiming an already assigned ticket.
* Unauthorized assignment attempts.

---

# 8. Ticket Ownership

The system distinguishes between:

### Customer ownership

The customer who created the ticket.

```text
ticket.customer_id
```

### Agent assignment

The support agent currently responsible for the ticket.

```text
ticket.assigned_to
```

These are intentionally different concepts.

Example:

```text
Customer: Ahmed
Agent: Sara

Ticket #100

customer_id = Ahmed
assigned_to = Sara
```

Sara is responsible for handling the ticket, but Ahmed remains its customer/creator.

---

# 9. Comments

Users may add comments according to their role and relationship with the ticket.

The service must verify access before creating a comment.

The API should not blindly trust:

```text
user_id
```

from the request body.

The authenticated user should be obtained from the security context.

This is an important security requirement.

---

# 10. Authentication

The system supports:

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
```

Authentication should include:

* Password hashing.
* Secure credential verification.
* Authentication mechanism appropriate for a REST API.
* Protected endpoints.
* Access to the currently authenticated user.

The exact authentication implementation will be designed during the authentication phase.

---

# 11. Authorization

Authorization occurs at multiple levels.

## Role authorization

Example:

```text
CUSTOMER → cannot change ticket status
AGENT    → can change ticket status
ADMIN    → can change ticket status
```

## Resource authorization

Example:

A customer may be authenticated but still cannot access:

```text
GET /api/tickets/999
```

if ticket `999` belongs to another customer.

Therefore:

```text
Authentication
    ↓
Who is the user?

Authorization
    ↓
Does their role permit this operation?

Ownership/access check
    ↓
Can they operate on THIS resource?
```

---

# 12. API Requirements

The API should follow REST conventions.

Initial endpoint groups:

## Authentication

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
```

## Tickets

```text
POST   /api/tickets
GET    /api/tickets
GET    /api/tickets/{id}
```

Specific operations may use dedicated endpoints rather than turning every field into a generic update operation.

Potential operations:

```text
PATCH /api/tickets/{id}/status
PATCH /api/tickets/{id}/assignment
```

The final API contract will be designed before implementation.

## Comments

```text
POST /api/tickets/{ticketId}/comments
GET  /api/tickets/{ticketId}/comments
```

## Categories

```text
POST   /api/categories
GET    /api/categories
PATCH  /api/categories/{id}
DELETE /api/categories/{id}
```

The final endpoint set may be adjusted during design.

---

# 13. Pagination and Filtering

Ticket listing must support pagination.

Example:

```text
GET /api/tickets?page=0&size=20
```

Relevant filters may include:

```text
status
priority
category
assignedTo
```

We should only implement filters that have clear business value.

Pagination should use Spring Data's pagination mechanisms where appropriate.

We will also discuss:

* Offset pagination.
* Sorting.
* Maximum page size.
* Count queries.
* Query performance.

---

# 14. Validation

Request DTOs should validate incoming data.

Examples:

* Required fields.
* String length.
* Valid email.
* Valid enum values.
* Password requirements.
* Non-null identifiers where required.

Validation failures should produce a consistent API error response.

Entities should not be used as request DTOs simply to avoid creating DTOs.

---

# 15. Error Handling

The API should provide centralized exception handling.

Examples:

```text
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Entity
```

We will decide which status codes are appropriate for each scenario rather than using one generic error code everywhere.

The API should return a consistent error structure containing useful information such as:

* timestamp
* HTTP status
* error type
* message
* request path

Sensitive internal information must not leak into API responses.

---

# 16. Database Requirements

PostgreSQL is the primary database.

Database schema changes must be managed through Flyway migrations.

The application should not depend on Hibernate automatically creating production schema.

Important database constraints should be enforced at the database level where appropriate.

Examples:

* Primary keys.
* Foreign keys.
* Unique email.
* Unique category name.
* Non-null required fields.

Indexes should be introduced based on actual query/access patterns rather than added indiscriminately.

---

# 17. Transactions

Transactions should be placed around business operations that require atomicity.

Examples may include:

```text
Assign ticket
Change ticket state
Create related records
```

The exact boundaries will be determined while implementing the business logic.

We will explicitly investigate:

* What `@Transactional` does.
* Transaction boundaries.
* Rollback behavior.
* Read vs write transactions.
* Lazy loading inside/outside transactions.
* Concurrent requests.

---

# 18. DTO and Entity Separation

The API should use DTOs for external request/response contracts where appropriate.

Example conceptual flow:

```text
HTTP Request
    ↓
Request DTO
    ↓
Controller
    ↓
Service
    ↓
Entity
    ↓
Repository
    ↓
Database
```

Entities should not automatically become API responses.

This protects the API contract from being tightly coupled to the database model and helps prevent accidental exposure of fields such as password hashes.

---

# 19. Logging

The application should contain useful structured/application logging around important operations and failures.

Examples:

* Authentication failures where appropriate.
* Ticket creation.
* Ticket assignment.
* Status transitions.
* Unexpected exceptions.

Logs must not contain:

* Passwords.
* Authentication secrets.
* Tokens.
* Sensitive personal information unnecessarily.

---

# 20. Configuration

Environment-specific values should not be hardcoded.

Examples:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
```

Configuration should support useful profiles such as:

```text
dev
test
prod
```

Secrets should come from environment/configuration rather than source code.

---

# 21. Docker

PostgreSQL should initially run through Docker Compose.

Development flow:

```text
Spring Boot Application
        │
        │
        ▼
Docker PostgreSQL
```

Later, the Spring Boot application itself may be containerized.

We will learn:

* Docker networking.
* Port mapping.
* Environment variables.
* Database persistence.
* Application/database configuration.

---

# 22. Testing Requirements

Testing is part of each feature.

We will use multiple levels of testing.

### Unit tests

Focus on isolated business logic.

Examples:

* Valid status transition.
* Invalid status transition.
* Assignment rules.
* Ownership rules.

### Repository/data tests

Verify persistence behavior and queries.

### Controller/API tests

Verify:

* HTTP status.
* Validation.
* Authentication.
* Authorization.
* Response contract.

### Integration tests

Verify important flows across:

```text
HTTP
 ↓
Controller
 ↓
Service
 ↓
Repository
 ↓
Database
```

We will deliberately discuss what should and should not be mocked.

---

# 23. Non-Goals

The first version will NOT include:

* Microservices.
* Kafka.
* Redis.
* WebSockets.
* File uploads.
* Email notifications.
* Real-time notifications.
* Complex permission-management UI.
* Kubernetes.
* Distributed tracing.
* Full frontend application.

These may be discussed as future extensions but are outside the learning project's core scope.

---

# 24. Definition of Done

The project is complete when:

* Users can authenticate.
* Roles are enforced.
* Customers can create and access their own tickets.
* Agents can work with tickets according to assignment rules.
* Admins can manage assignments.
* Ticket status transitions are validated.
* Comments work with proper authorization.
* Categories are managed securely.
* Pagination/filtering works.
* Validation is implemented.
* Errors have consistent responses.
* Database schema is managed through Flyway.
* PostgreSQL runs through Docker.
* Configuration uses environment variables/profiles.
* Important business rules have tests.
* Integration tests cover critical flows.
* Application logs are useful and safe.
* The application can be run locally from documented instructions.
* We can explain the architecture and major design decisions in an interview.
