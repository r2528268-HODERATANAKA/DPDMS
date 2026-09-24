# fire-service

Records **veld/fire incidents** reported by ward recorders in Rushinga district, runs them through the provincial approval workflow, and notifies alert-service on approval.

**Owner:** Sean   |   **Port:** 8083   |   **Database:** dpdms_fire (auto-created)   |   **Base URL:** `/api/fires`

## Files in this service (what each one does)

| File | What it does |
|------|--------------|
| `pom.xml` | Maven build file. Same Spring Boot 4.1.1 starters as flood-service, plus H2 (test-only, so `mvn test` runs without MySQL). |
| `src/main/java/com/dpdms/fire_service/FireServiceApplication.java` | Entry point (`@SpringBootApplication`). Starts the service on port 8083. |
| `src/main/resources/application.properties` | Port, MySQL connection (`dpdms_fire`), JPA settings, and the alert-service URL. |
| `model/FireIncident.java` | The JPA entity = one row in table `fire_incidents`. Lombok `@Data`/`@Builder` generate getters/setters. Validation annotations guard every field. |
| `model/Severity.java` | Enum `LOW / MEDIUM / HIGH / CRITICAL`. |
| `model/IncidentStatus.java` | Enum `PENDING / APPROVED / REJECTED / CORRECTIONS_REQUESTED` (the approval workflow). |
| `model/FireStatus.java` | Enum `ACTIVE, CONTAINED`. |

| `repository/FireIncidentRepository.java` | Spring Data interface. Derived queries like `findByStatus`, `findByWard` - no SQL needed. |
| `service/FireIncidentService.java` | The business rules: ward/hazard scoping (403), the PENDING -> APPROVED / REJECTED / CORRECTIONS workflow, approved-record locking, audit fields. |
| `service/AlertNotifier.java` | Fires an HTTP POST to alert-service whenever an incident is APPROVED (best-effort, never blocks the approval). |
| `controller/FireIncidentController.java` | REST endpoints under `/api/fires`. Reads caller identity from `X-User-*` headers (put there by the gateway from the JWT). |
| `exception/ForbiddenOperationException.java` | Marker exception -> HTTP 403. |
| `exception/ResourceNotFoundException.java` | Marker exception -> HTTP 404. |
| `exception/GlobalExceptionHandler.java` | `@RestControllerAdvice` that converts exceptions into clean JSON errors (403 / 404 / 400 field map). |
| `src/test/resources/application.properties` | Test-only config: swaps MySQL for in-memory H2 while testing. |
| `test/.../FireIncidentServiceTest.java` | 13 unit tests for every business rule (scoping, workflow, audit). |
| `test/.../FireServiceApplicationTests.java` | Spring context load test (runs on H2). |

## How the scoping works (FR-SCOPE-01)

- Every request carries `X-User-Ward` and `X-User-Hazard` (from the caller's JWT via the gateway).
- The service rejects the request with **403** if the caller's hazard is not `fire`.
- Ward recorders may only touch records of **their own ward**; anything else is a **403**.
- Newly created records are always forced to `PENDING`.
- `APPROVED` records are locked; editing a returned record resubmits it as `PENDING`.

## Endpoints

| Method & path | Purpose | Headers needed |
|---------------|---------|----------------|
| `POST /api/fires` | Record a new fire incident (status forced to PENDING) | `X-User-Ward`, `X-User-Hazard` |
| `GET /api/fires` | Approved-only feed (used by dashboard & report services) | none |
| `GET /api/fires/ward/{ward}` | All records of one ward (recorder's own view) | none |
| `GET /api/fires/{id}` | One record by id | none |
| `PUT /api/fires/{id}` | Edit a non-approved record (resubmits as PENDING) | `X-User-Ward`, `X-User-Hazard` |
| `DELETE /api/fires/{id}` | Remove a record | `X-User-Ward`, `X-User-Hazard` |
| `PATCH /api/fires/{id}/approve` | Supervisor approves | `X-User-Name`, `X-User-Hazard` |
| `PATCH /api/fires/{id}/reject` | Supervisor rejects (`{"reason": "..."}` in body) | `X-User-Name`, `X-User-Hazard` |
| `PATCH /api/fires/{id}/request-corrections` | Supervisor sends back for fixes (`{"notes": "..."}`) | `X-User-Name`, `X-User-Hazard` |

## Try it locally (needs MySQL running; see root `DEPLOYMENT.md`)

```bash
mvn spring-boot:run
```

Create one (recorder headers):

```bash
curl -X POST http://localhost:8083/api/fires \
  -H "Content-Type: application/json" \
  -H "X-User-Ward: Mudzi" -H "X-User-Hazard: fire" \
  -d '{
    "ward": "Mudzi", "district": "Mudzi", "province": "Mashonaland Central",
    "occurredAt": "2026-09-20T14:30:00", "reporter": "T. Chirwa",
    "severity": "HIGH", "latitude": -17.4, "longitude": 31.6,
    "areaBurnedHectares": 45.5, "suspectedCause": "Land clearing fire that spread",
    "injuries": 1, "fatalities": 0, "structuresDestroyed": 2, "fireStatus": "ACTIVE"
  }'
```

Approve it (supervisor headers):

```bash
curl -X PATCH http://localhost:8083/api/fires/1/approve \
  -H "X-User-Name: Mrs Moyo" -H "X-User-Hazard: fire"
```

## Run the tests

```bash
mvn test          # 14 tests: 13 business-rule unit tests + context load (H2, no MySQL needed)
```
