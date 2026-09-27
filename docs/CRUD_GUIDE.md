# How to Build a CRUD in This Project

A mental model and a repeatable recipe for adding create/read/update/delete to any entity,
based on the ticket implementation.

---

## 1. The Request's Journey (Runtime View)

What happens on every request, using `POST /api/tickets` as the example:

```text
 CLIENT
   │  POST /api/tickets
   │  Authorization: Bearer eyJ...
   │  { "title": "...", "categoryId": "..." }
   ▼
┌──────────────────────────────────────────────────────────────┐
│ SECURITY FILTER CHAIN                  (SecurityConfig)      │
│  • Is there a valid JWT?            no → 401                 │
│  • Does the ROLE allow this URL?    no → 403                 │
│  → puts the user in the SecurityContext                      │
└──────────────────────────────────────────────────────────────┘
   ▼
┌──────────────────────────────────────────────────────────────┐
│ CONTROLLER                              (TicketController)   │
│  • JSON → Request DTO                  (@RequestBody)        │
│  • Validate the DTO           (@Valid) fail → 400            │
│  • Get the current user from the JWT   (CurrentUser.from)    │
│  • Call ONE service method                                   │
│  • Choose the HTTP status              (@ResponseStatus 201) │
└──────────────────────────────────────────────────────────────┘
   ▼   CreateTicketRequest + customerId
┌──────────────────────────────────────────────────────────────┐
│ SERVICE                  (TicketService)   @Transactional    │
│  ┌── transaction starts ───────────────────────────────────┐ │
│  │ • Load related entities (category, user)   → 400 / 404  │ │
│  │ • Resource authorization (canView)         → 404        │ │
│  │ • Business rules (status=OPEN, default priority)        │ │
│  │ • DTO → Entity, call the repository                     │ │
│  │ • Entity → Response DTO (lazy fields are still OK here) │ │
│  └── transaction commits (SQL is flushed) ─────────────────┘ │
└──────────────────────────────────────────────────────────────┘
   ▼   Ticket entity                    ▲  Ticket entity
┌──────────────────────────────────────────────────────────────┐
│ REPOSITORY                     (TicketRepository interface)  │
│  • save / findById / delete / derived queries / @Query       │
│  • Spring Data generates the implementation (a proxy)        │
└──────────────────────────────────────────────────────────────┘
   ▼   SQL (Hibernate)                  ▲  rows
┌──────────────────────────────────────────────────────────────┐
│ DATABASE                      (PostgreSQL, schema by Flyway) │
│  • The final guard: PK, FK, UNIQUE, NOT NULL, CHECK          │
└──────────────────────────────────────────────────────────────┘

   ◄── TicketResponse DTO → JSON → 201 Created ◄──
```

### The boundary rule

```text
 JSON  ⇄  DTO   ⇄   Controller  ⇄  Service  ⇄  Entity  ⇄  Repository  ⇄  DB
           └──── web layer sees only DTOs ────┘└──── entities never leave the service ────┘
```

Why entities never leave the service: `spring.jpa.open-in-view=false` closes the persistence
session when the `@Transactional` service method returns. Touching a lazy relation
(`ticket.getCustomer().getName()`) after that throws `LazyInitializationException`.
Mapping entity → DTO inside the service avoids this and guarantees that fields such as
`passwordHash` can never be serialized by accident.

---

## 2. Who Is Responsible for What

| Layer | Responsible for | Never does |
|---|---|---|
| **Security config** | Authentication, and *"can this role call this URL?"* | Load data or check ownership |
| **Controller** | HTTP: the path, JSON, `@Valid`, status codes, extracting the current user | Business rules, repositories, entities |
| **Service** | Business rules, ownership, transactions, DTO ⇄ entity mapping | Know about HTTP, JWTs or JSON |
| **Repository** | Database access | Business logic |
| **Entity** | Mirror the table, lifecycle callbacks (`@PrePersist`, `@PreUpdate`) | Get serialized to JSON |
| **DTO** | The API contract: what clients may send and receive | Contain logic beyond simple mapping |
| **Database** | Enforce data integrity as the last line of defense | Hold business logic the app can't see |

**Two kinds of authorization:**

- **Role authorization** (*"may an AGENT call `POST /api/tickets`?"*) → `SecurityConfig`, answers with `403`.
- **Resource authorization** (*"may Ahmed see ticket #123?"*) → the service, because it needs
  data. Answers with `404`, so that callers can't find out which IDs exist.

---

## 3. The Recipe

### Step 0: Think before you code

Answer these on paper before writing any file:

```text
 1. DATA         What fields? Which are required? Lengths? Unique?
 2. RELATIONS    What does it belong to? What depends on it?
 3. WHO          For each operation: which ROLES may do it?   (→ SecurityConfig)
 4. WHOSE        Can they touch ANY record, or only THEIR OWN? (→ service check)
 5. CLIENT VS    Which fields does the client send?
    SERVER       Which does the server set? (id, owner, status, timestamps)
 6. RULES        Which states and changes are allowed? What's a conflict?
 7. DELETE       Hard delete? Blocked while in use? Cascade? (be conservative)
```

### Steps 1–9: Build it bottom-up

```text
 ① Migration (V<n>__...sql)  table + constraints + indexes     ← the DB is the truth
         ▼
 ② Entity                    mirrors the table exactly; @PrePersist / @PreUpdate
         ▼
 ③ Repository                interface extends JpaRepository<X, UUID>
         ▼
 ④ DTOs                      CreateXRequest, UpdateXRequest, XResponse (+ static from())
         ▼
 ⑤ Exceptions                XNotFoundException (404), conflicts (409) ...
         ▼
 ⑥ Service                   one method per operation, @Transactional, rules + mapping
         ▼
 ⑦ Controller                one endpoint per operation, thin
         ▼
 ⑧ SecurityConfig            role rules per URL + HTTP method
         ▼
 ⑨ Verify                    happy path + every "no" path (400/401/403/404/409)
```

Why bottom-up: each layer depends only on the ones below it, so the code compiles at every
step and you never write code that calls something you haven't built yet.

Flyway rule: a migration that has already run is **immutable**. Every schema or data change
goes into a new `V<n+1>__description.sql`. Editing an applied migration causes a checksum
mismatch at startup.

---

## 4. The Five Operations Side by Side

| | **Create** | **Read one** | **Read many** | **Update (PUT)** | **Delete** |
|---|---|---|---|---|---|
| **HTTP** | `POST /x` | `GET /x/{id}` | `GET /x` | `PUT /x/{id}` | `DELETE /x/{id}` |
| **Success** | `201` + body | `200` + body | `200` + list/page | `200` + body | `204`, no body |
| **Request DTO** | `CreateXRequest` | none | query params | `UpdateXRequest` | none |
| **Transaction** | `@Transactional` | `readOnly = true` | `readOnly = true` | `@Transactional` | `@Transactional` |
| **Repository** | `save(new)` | `findById` | query method / `@Query` | `findById`, then setters; **no `save()`** | `findById` → `delete` |
| **Typical "no"** | 400 validation, 400 bad reference | 404 missing or not yours | *(filter by owner in the query)* | 404, 400, 409 | 404, 409 in use |

### Create

```java
@Transactional
public XResponse create(CreateXRequest request, UUID ownerId) {
    // load references → 400/404 if missing
    // new X(), copy CLIENT fields from the DTO, set SERVER fields (owner, status...)
    return XResponse.from(repository.save(x));
}
```

The owner comes from the JWT (a method parameter), **never** from the request body.

### Read one

```java
@Transactional(readOnly = true)
public XResponse get(UUID id, CurrentUser user) {
    X x = repository.findById(id)
            .filter(e -> canView(e, user))      // "not found" and "not yours" → same exception
            .orElseThrow(XNotFoundException::new);
    return XResponse.from(x);
}
```

### Read many

```text
 ❌ findAll() → filter in Java      loads the WHOLE table into memory
 ✅ query with WHERE owner = ?      the database filters, returns only what's needed
```

The access rules in `canView` have to be expressed again as queries (one per role).
Those two places must stay in sync. Pagination (`Pageable`, `Page<T>`) comes in Phase 7.

### Update: dirty checking

```java
@Transactional
public XResponse update(UUID id, UpdateXRequest request, CurrentUser user) {
    X x = repository.findById(id)          // ① the entity is now MANAGED by the persistence context
            .filter(e -> canEdit(e, user))
            .orElseThrow(XNotFoundException::new);

    x.setName(request.name());             // ② just change fields...

    repository.flush();                    // ③ optional: only needed so that @PreUpdate runs
                                           //    before we build the response (fresh updatedAt)
    return XResponse.from(x);              // ④ no save() call
}                                          // ⑤ at commit, Hibernate detects the change → UPDATE
```

Hibernate keeps a snapshot of every entity it loads. At flush or commit it compares each managed
entity with its snapshot and writes an `UPDATE` for anything that changed. `@PreUpdate` runs
at that moment, **not** when you call the setter, which is why step ③ exists.

**PUT vs PATCH:**

- `PUT` **replaces** the whole editable resource. The client sends every editable field,
  so a missing field is a validation error.
- `PATCH` changes **only** what is sent.
- Important business actions get **dedicated** endpoints (`PATCH /api/tickets/{id}/status`,
  `PATCH /api/tickets/{id}/assignment`) instead of being hidden inside a generic update, so each
  one gets its own rules and authorization. A generic `PUT` must never change those fields.

### Delete

```java
@Transactional
public void delete(UUID id) {
    X x = repository.findById(id).orElseThrow(XNotFoundException::new);
    // check "in use" rules → 409
    repository.delete(x);
}
```

Controller: `@DeleteMapping("/{id}")` + `@ResponseStatus(HttpStatus.NO_CONTENT)`, returning `void`.

Think about **what else disappears**: foreign keys with `ON DELETE CASCADE` delete child rows
in the database, and Hibernate doesn't know that happened. Without a cascade, the foreign key
blocks the delete (→ handle it as `409`). The alternative is a **soft delete** (a `deleted_at`
column), which keeps history for audits.

---

## 5. The Recipe Applied to Tickets

| Recipe step | Ticket |
|---|---|
| ① Migration | `V1` → `tickets` table, FKs, CHECKs, indexes; `V2` seeds categories |
| ② Entity | `Ticket` + `@PrePersist` / `@PreUpdate` |
| ③ Repository | `TicketRepository` (derived queries + `@Query`) |
| ④ DTOs | `CreateTicketRequest`, `UpdateTicketRequest`, `TicketResponse`, `UserSummary`, `CategorySummary` |
| ⑤ Exceptions | `InvalidCategoryException`, `TicketNotFoundException`, `TicketNotEditableException` |
| ⑥ Service | `createTicket`, `getTicket`, `listTickets`, `updateTicket`, `deleteTicket` + `canView` |
| ⑦ Controller | `POST`, `GET /{id}`, `GET`, `PUT /{id}`, `DELETE /{id}` |
| ⑧ Security | `POST` / `PUT` → `CUSTOMER`; `DELETE` → `ADMIN`; `GET` → any authenticated user |
| ⑨ Verify | curl checks per step (automated tests in Phase 9) |

---

## 6. Practice Exercise: Category CRUD

`POST / GET / PATCH / DELETE /api/categories`, admin only for writes.
Work through the recipe from step 0 to step 9 on your own. The tricky question for step 0:
*what happens when you delete a category that tickets still use?*
