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
# india-satcom
