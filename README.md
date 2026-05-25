# Task Management API

A simplified Task Management REST API built with **Java 21 + Spring Boot 3.3**,
organised using **Domain-Driven Design (DDD)** layering and developed test-first.

Data is held in an in-memory store — no external database is required.

---

## 1. Tech stack

| Concern        | Choice                                |
| -------------- | ------------------------------------- |
| Language       | Java 21 (compiles & runs on JDK 21+)  |
| Framework      | Spring Boot 3.3.5                     |
| Build          | Apache Maven                          |
| Validation     | `spring-boot-starter-validation`      |
| Testing        | JUnit 5, Mockito, AssertJ, MockMvc    |
| Persistence    | In-memory `ConcurrentHashMap`         |

---

## 2. Prerequisites

- **JDK 21 or higher** (verified on JDK 25)
- **Apache Maven 3.9+**

Verify your installation:

```bash
java -version
mvn -version
```

The project includes the standard Spring Boot Maven plugin; no other tooling
is required.

---

## 3. Build & run

From the project root:

```bash
# Build (compiles + runs all unit + integration tests)
mvn clean verify

# Run the application
mvn spring-boot:run
```

The service starts on **`http://localhost:8080`**.

Alternatively, build an executable jar and run it directly:

```bash
mvn clean package
java -jar target/task-management-api-1.0.0.jar
```

---

## 4. Running the tests

```bash
mvn test
```

The suite contains **33 tests** across four files:

| File                                            | Type        | Scope                                          |
| ----------------------------------------------- | ----------- | ---------------------------------------------- |
| `TaskTest`                                      | Unit        | Domain entity invariants                       |
| `InMemoryTaskRepositoryTest`                    | Unit        | Repository sort / filter / paginate behaviour  |
| `TaskServiceTest`                               | Unit (mocks)| Service logic with a mocked repository         |
| `TaskControllerIntegrationTest`                 | Integration | End-to-end HTTP via Spring's `MockMvc`         |

---

## 5. Project structure (DDD layering)

```
src/main/java/com/indiasatcom/taskmanagement
├── TaskManagementApplication.java
├── domain
│   ├── model           # Task aggregate + value objects + enum
│   ├── repository      # TaskRepository PORT (interface)
│   └── exception       # Domain exceptions
├── application
│   ├── dto             # Request / response records
│   └── service         # TaskService — orchestrates use cases
├── infrastructure
│   └── persistence     # InMemoryTaskRepository (adapter)
└── interfaces
    └── rest            # TaskController + GlobalExceptionHandler
```

Key design points:

- **Domain layer is pure** — no Spring annotations on `Task`, `TaskId`,
  `TaskStatus`, or `TaskRepository`. Invariants (non-blank title, non-past
  due date) are enforced inside the entity, not in controllers.
- **Repository is a port** (`TaskRepository`); the in-memory implementation
  lives in the infrastructure layer and is wired by Spring via `@Repository`.
- **DTOs are separate** from entities — JSON shape is decoupled from the
  domain model.
- **Errors are translated centrally** in `GlobalExceptionHandler`.

---

## 6. Domain model

| Field        | Type           | Required | Notes                                       |
| ------------ | -------------- | -------- | ------------------------------------------- |
| `id`         | `string` (UUID)| auto     | Generated server-side                       |
| `title`      | `string`       | ✅       | Non-blank, trimmed                          |
| `description`| `string`       | optional |                                             |
| `status`     | enum           | optional | `PENDING` (default), `IN_PROGRESS`, `DONE`  |
| `due_date`   | `date` (ISO)   | ✅       | Must not be in the past                     |

---

## 7. REST API

Base URL: `http://localhost:8080`

### Create task — `POST /tasks`

Request:
```json
{
  "title": "Write report",
  "description": "Quarterly summary",
  "status": "PENDING",
  "due_date": "2026-12-01"
}
```

Response: `201 Created`
```json
{
  "id": "ab9a8d3c-...",
  "title": "Write report",
  "description": "Quarterly summary",
  "status": "PENDING",
  "due_date": "2026-12-01"
}
```

Validation errors return `400 Bad Request` with a structured body:
```json
{
  "timestamp": "2026-05-24T14:32:11Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": ["title: title is required"]
}
```

### Get task — `GET /tasks/{id}`

Returns the task, or `404 Not Found` when the id is unknown.

### Update task — `PUT /tasks/{id}`

Partial update — only non-null fields in the body are applied.

```json
{ "status": "IN_PROGRESS" }
```

Returns the updated task, or `404` if not found.

### Delete task — `DELETE /tasks/{id}`

Returns `204 No Content` on success, or `404` if not found.

### List tasks — `GET /tasks`

Returns a **JSON array** of tasks, **sorted by `due_date` ascending**:

```json
[
  {
    "id": "...",
    "title": "...",
    "description": "...",
    "status": "PENDING",
    "due_date": "2026-06-15"
  }
]
```

Query parameters (all optional — bonus features):

| Param    | Type    | Default | Meaning                                                |
| -------- | ------- | ------- | ------------------------------------------------------ |
| `status` | enum    | —       | Filter to a single status (`PENDING` / `IN_PROGRESS` / `DONE`) |
| `page`   | int     | `0`     | Zero-based page index                                  |
| `size`   | int     | `100`   | Page size                                              |

Example: `GET /tasks?status=PENDING&page=0&size=10`

---

## 8. Sample requests (curl)

```bash
# Create
curl -X POST http://localhost:8080/tasks \
  -H 'Content-Type: application/json' \
  -d '{"title":"Buy milk","due_date":"2026-12-01"}'

# Get
curl http://localhost:8080/tasks/<id>

# List (sorted by due_date)
curl http://localhost:8080/tasks

# List with filter + pagination
curl 'http://localhost:8080/tasks?status=PENDING&page=0&size=5'

# Update (partial)
curl -X PUT http://localhost:8080/tasks/<id> \
  -H 'Content-Type: application/json' \
  -d '{"status":"DONE"}'

# Delete
curl -X DELETE -i http://localhost:8080/tasks/<id>
```

---

## 9. Bonus features implemented

- ✅ Pagination on `GET /tasks` (`page`, `size`)
- ✅ Filtering on `GET /tasks` by `status`
- ✅ Date validation — `due_date` must be a valid date and not in the past

---

## 10. Notes

- Because the store is in-memory, **all data is lost on restart**.
- The store is concurrency-safe (`ConcurrentHashMap`), so the service is
  safe to hit from multiple threads.
- Replacing the in-memory adapter with a JPA / JDBC implementation only
  requires providing a new `TaskRepository` bean — no other layer changes.

---

## 11. AI usage disclosure

Per the "Fair use of AI" clause in the challenge brief, this section records
how AI assistance was used to build this submission.

### Tool

Claude Code (Anthropic), Sonnet model, running in an agentic CLI session with
file-edit and shell-execution tools.

### Approach

I treated the AI as a pair-programming collaborator working from a fully
specified brief. The session followed four phases:

1. **Specification** — provided the full problem statement verbatim and chose
   the stack (Java + Spring Boot).
2. **Implementation** — the model produced the DDD-layered code, tests, and
   README in a single pass, running `mvn test` and live `curl` smoke tests
   against the running app to verify behaviour end-to-end.
3. **Spec audit & correction** — I asked the model to re-read the brief and
   verify the implementation matched it line-by-line. This caught one real
   deviation (the list endpoint was returning a paged envelope instead of the
   plain JSON array the brief required) which was then corrected, with tests
   and README updated to match.
4. **Test logging polish** — added INFO-level "Input / Result" log lines to
   every test so the suite output reads as a self-documenting trace of what
   each test sent in and what came back.

### Prompts used (verbatim)

**Prompt 1 — initial implementation:**

> [pasted the full problem statement from the challenge brief]
>
> Let me know if you have any issues. Use JAVA spring boot.

**Prompt 2 — spec compliance audit:**

> check one more time is the problem statement completely matched with the
> changes

**Prompt 3 — test logging:**

> add some loggers in the test case
>
> (follow-up) No not this just like Info logs, no debug logs, also tell what
> is the value passed and what's the result you got

(A handful of short conversational follow-ups — "continue", port-conflict
diagnostics, "is everything correct" sanity checks — were used to keep the
session moving but did not change the design.)

### What I verified myself

- Read the final code in every layer before submitting
- Ran `mvn test` locally — 33 tests pass
- Booted the app and hit every endpoint with curl to confirm the behaviour
  matched the brief
- Re-read the brief one more time after the audit fix to confirm full
  compliance

### What the AI did vs. what I did

| Activity | AI | Me |
|---|---|---|
| Architectural decisions (DDD layering, ports/adapters) | Proposed | Approved |
| Code generation (entities, service, controller, repo) | Wrote | Reviewed |
| Test design and implementation | Wrote | Reviewed |
| Running the build and tests | Executed | Verified output |
| Spec-compliance check | Performed on request | Initiated and reviewed |
| Test logging strategy (INFO-only Input/Result format) | Implemented | Specified the format |
| Stack choice (Java + Spring Boot) | — | Decided |
| Final go/no-go on submission | — | Decided |

### Why I used a single prompt (token-cost rationale)

Estimated token usage for producing this same deliverable:

| Strategy | Prompts | Est. total tokens | Relative cost |
|---|---|---|---|
| **Single prompt (what I used)** | 1 + 2 follow-ups | **~55K – 70K** | **1×** |
| 4 mega-prompts (one per DDD layer) | 4 | ~180K – 220K | ~3× |
| 10 step-by-step prompts | 10 | ~380K – 520K | **~6–8×** |

Each prompt re-sends ~30K tokens of system context, so step-by-step pays
that overhead 10× for the same output. A single prompt was the right call
because the brief was complete, the task fit in one context window, and
designing all layers in one pass yields more consistent naming and error
handling than building them in isolation.
