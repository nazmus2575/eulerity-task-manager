# Eulerity Task Manager

A Java 17 Spring Boot REST API for a personal task manager. The app supports core task CRUD operations, an AI-powered task suggestion endpoint using Gemini, H2 in-memory persistence, validation, structured error responses, tests, and a simple browser UI for reviewers who do not want to use Postman.

## Tech Stack

- Java 17
- Spring Boot
- Maven
- Spring Web
- Spring Data JPA
- Jakarta Validation
- H2 in-memory database
- Gemini API integration through `RestTemplate`
- JUnit, Mockito, Spring MockMvc

## Run Locally

From the project root:

```bash
./mvnw spring-boot:run
```

Then open:

```text
http://localhost:8080/
```

The frontend is served from:

```text
src/main/resources/static/index.html
```

## Gemini API Key

The AI endpoint reads the Gemini API key from configuration. Do not hardcode keys in the source code.

Set the environment variable before running:

```bash
export GEMINI_API_KEY="your-gemini-api-key"
```

The app maps it in [application.properties](src/main/resources/application.properties):

```properties
gemini.api.key=${GEMINI_API_KEY:}
gemini.model=gemini-2.0-flash
```

If no API key is configured, the normal CRUD API and UI still run, but `POST /tasks/suggest` returns a structured server configuration error.

## Task Model

Each task includes:

```json
{
  "id": 1,
  "title": "Submit quarterly report",
  "description": "Send report to finance",
  "dueDate": "2026-06-05",
  "priority": "MEDIUM",
  "status": "TODO"
}
```

Rules:

- `id` is auto-generated.
- `title` is required.
- `description` is optional.
- `dueDate` uses ISO date format: `yyyy-MM-dd`.
- `priority` can be `LOW`, `MEDIUM`, or `HIGH`.
- `status` can be `TODO`, `IN_PROGRESS`, or `DONE`.
- New tasks default to `status = TODO`.
- Missing priority defaults to `MEDIUM`.

## REST API

### Create Task

```http
POST /tasks
Content-Type: application/json
```

Request:

```json
{
  "title": "Submit quarterly report",
  "description": "Send report to finance",
  "dueDate": "2026-06-05",
  "priority": "HIGH"
}
```

Response:

```json
{
  "id": 1,
  "title": "Submit quarterly report",
  "description": "Send report to finance",
  "dueDate": "2026-06-05",
  "priority": "HIGH",
  "status": "TODO"
}
```

### List Tasks

```http
GET /tasks
```

### Get Task By ID

```http
GET /tasks/{id}
```

### Update Task

```http
PUT /tasks/{id}
Content-Type: application/json
```

Request:

```json
{
  "title": "Submit revised quarterly report",
  "description": "Include updated forecast",
  "dueDate": "2026-06-06",
  "priority": "HIGH",
  "status": "IN_PROGRESS"
}
```

### Delete Task

```http
DELETE /tasks/{id}
```

Returns `204 No Content` when successful.

## AI-Powered Endpoint

### Suggest Task From Plain Language

```http
POST /tasks/suggest
Content-Type: text/plain
```

Request body:

```text
remind me to submit the quarterly report before Friday
```

Example response:

```json
{
  "id": null,
  "title": "Submit quarterly report",
  "description": "Submit the quarterly report before Friday",
  "dueDate": "2026-06-05",
  "priority": "MEDIUM",
  "status": "TODO"
}
```

This endpoint does not save the task. It only returns a structured suggestion that can be reviewed and then submitted through `POST /tasks`.

The Gemini prompt is designed to force JSON-only output. The service also sets Gemini's response MIME type to `application/json`:

```java
"generationConfig", Map.of(
    "responseMimeType", "application/json",
    "temperature", 0.1
)
```

The system prompt instructs the model to return exactly this task shape:

```json
{
  "id": null,
  "title": "short required task title",
  "description": "optional longer description or null",
  "dueDate": "ISO-8601 date in yyyy-MM-dd format or null",
  "priority": "LOW | MEDIUM | HIGH",
  "status": "TODO"
}
```

## Error Responses

The API uses `@RestControllerAdvice` to return structured JSON errors.

Example validation error:

```json
{
  "timestamp": "2026-06-02T10:15:30",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/tasks",
  "validationErrors": {
    "title": "Title is required"
  }
}
```

Example not-found error:

```json
{
  "timestamp": "2026-06-02T10:15:30",
  "status": 404,
  "error": "Not Found",
  "message": "Task not found with id: 99",
  "path": "/tasks/99",
  "validationErrors": null
}
```

## Frontend UI

The project includes a small static UI at:

```text
http://localhost:8080/
```

It supports:

- Viewing all current tasks
- Creating a task
- Sending a plain-language task idea to `/tasks/suggest`
- Displaying the AI-generated structured task suggestion
- Copying the AI suggestion into the create-task form

## H2 Database

The app uses an in-memory H2 database. Data resets when the app restarts.

Configured in [application.properties](src/main/resources/application.properties):

```properties
spring.datasource.url=jdbc:h2:mem:taskmanager;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
```

H2 console:

```text
http://localhost:8080/h2-console
```

JDBC URL:

```text
jdbc:h2:mem:taskmanager
```

## Tests

Run:

```bash
./mvnw test
```

Included tests:

- `TaskServiceImplTest`: happy-path unit tests for service-layer CRUD methods.
- `TaskControllerIntegrationTest`: Spring Boot + MockMvc integration test covering create, list, get, update, delete.
- `TaskSuggestControllerTest`: controller test for `POST /tasks/suggest` with `GeminiService` mocked so no real external API call happens during builds.

## Project Structure

```text
src/
  main/
    java/com/eulerity/taskmanager/
      TaskManagerApplication.java
      controller/
        TaskController.java
      exception/
        ApiError.java
        GlobalExceptionHandler.java
        TaskNotFoundException.java
      model/
        Priority.java
        Task.java
        TaskStatus.java
      repository/
        TaskRepository.java
      service/
        GeminiService.java
        TaskService.java
        TaskServiceImpl.java
    resources/
      application.properties
      static/
        index.html
  test/
    java/com/eulerity/taskmanager/
      controller/
        TaskControllerIntegrationTest.java
        TaskSuggestControllerTest.java
      service/
        TaskServiceImplTest.java
```

## Notes

- API keys are intentionally not committed.
- The AI endpoint is stateless and does not persist suggestions.
- CRUD endpoints persist tasks in H2 for the lifetime of the application process.
- The included frontend is intentionally simple and meant for quick reviewer exploration.
