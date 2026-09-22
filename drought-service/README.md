# drought-service

Records **drought situations** reported by ward recorders (rainfall deficit, dry spells, water source condition), runs them through the provincial approval workflow, and notifies alert-service on approval.

**Owner:** Oliver   |   **Port:** 8082   |   **Database:** dpdms_drought (auto-created)   |   **Base URL:** `/api/droughts`

## Files in this service (what each one does)

| File | What it does |
|------|--------------|
| `pom.xml` | Maven build file. Same Spring Boot 4.1.1 starters as flood-service, plus H2 (test-only, so `mvn test` runs without MySQL). |
| `src/main/java/com/dpdms/drought_service/DroughtServiceApplication.java` | Entry point (`@SpringBootApplication`). Starts the service on port 8082. |
| `src/main/resources/application.properties` | Port, MySQL connection (`dpdms_drought`), JPA settings, and the alert-service URL. |
| `model/DroughtIncident.java` | The JPA entity = one row in table `drought_incidents`. Lombok `@Data`/`@Builder` generate getters/setters. Validation annotations guard every field. |
| `model/Severity.java` | Enum `LOW / MEDIUM / HIGH / CRITICAL`. |
| `model/IncidentStatus.java` | Enum `PENDING / APPROVED / REJECTED / CORRECTIONS_REQUESTED` (the approval workflow). |
| `model/WaterSourceCondition.java` | Enum `NORMAL, STRESSED, CRITICAL, DRY`. |

| `repository/DroughtIncidentRepository.java` | Spring Data interface. Derived queries like `findByStatus`, `findByWard` - no SQL needed. |
| `service/DroughtIncidentService.java` | The business rules: ward/hazard scoping (403), the PENDING -> APPROVED / REJECTED / CORRECTIONS workflow, approved-record locking, audit fields. |
| `service/AlertNotifier.java` | Fires an HTTP POST to alert-service whenever an incident is APPROVED (best-effort, never blocks the approval). |
| `controller/DroughtIncidentController.java` | REST endpoints under `/api/droughts`. Reads caller identity from `X-User-*` headers (put there by the gateway from the JWT). |
| `exception/ForbiddenOperationException.java` | Marker exception -> HTTP 403. |
| `exception/ResourceNotFoundException.java` | Marker exception -> HTTP 404. |
| `exception/GlobalExceptionHandler.java` | `@RestControllerAdvice` that converts exceptions into clean JSON errors (403 / 404 / 400 field map). |
| `src/test/resources/application.properties` | Test-only config: swaps MySQL for in-memory H2 while testing. |
| `test/.../DroughtIncidentServiceTest.java` | 13 unit tests for every business rule (scoping, workflow, audit). |
| `test/.../DroughtServiceApplicationTests.java` | Spring context load test (runs on H2). |

## How the scoping works (FR-SCOPE-01)

- Every request carries `X-User-Ward` and `X-User-Hazard` (from the caller's JWT via the gateway).
- The service rejects the request with **403** if the caller's hazard is not `drought`.
- Ward recorders may only touch records of **their own ward**; anything else is a **403**.
- Newly created records are always forced to `PENDING`.
- `APPROVED` records are locked; editing a returned record resubmits it as `PENDING`.

## Endpoints

| Method & path | Purpose | Headers needed |
|---------------|---------|----------------|
| `POST /api/droughts` | Record a new drought incident (status forced to PENDING) | `X-User-Ward`, `X-User-Hazard` |
| `GET /api/droughts` | Approved-only feed (used by dashboard & report services) | none |
| `GET /api/droughts/ward/{ward}` | All records of one ward (recorder's own view) | none |
| `GET /api/droughts/{id}` | One record by id | none |
| `PUT /api/droughts/{id}` | Edit a non-approved record (resubmits as PENDING) | `X-User-Ward`, `X-User-Hazard` |
| `DELETE /api/droughts/{id}` | Remove a record | `X-User-Ward`, `X-User-Hazard` |
| `PATCH /api/droughts/{id}/approve` | Supervisor approves | `X-User-Name`, `X-User-Hazard` |
| `PATCH /api/droughts/{id}/reject` | Supervisor rejects (`{"reason": "..."}` in body) | `X-User-Name`, `X-User-Hazard` |
| `PATCH /api/droughts/{id}/request-corrections` | Supervisor sends back for fixes (`{"notes": "..."}`) | `X-User-Name`, `X-User-Hazard` |

## Try it locally (needs MySQL running; see root `DEPLOYMENT.md`)

```bash
mvn spring-boot:run
```

Create one (recorder headers):

```bash
curl -X POST http://localhost:8082/api/droughts \
  -H "Content-Type: application/json" \
  -H "X-User-Ward: Mudzi" -H "X-User-Hazard: drought" \
  -d '{
    "ward": "Mudzi", "district": "Mudzi", "province": "Mashonaland Central",
    "occurredAt": "2026-09-15T08:00:00", "reporter": "O. Nyathi",
    "severity": "CRITICAL", "latitude": -17.4, "longitude": 31.6,
    "rainfallDeficitMm": 62.5, "consecutiveDryDays": 34,
    "affectedHouseholds": 320, "livestockDeaths": 18,
    "cropDamageHectares": 120.0, "waterSourceCondition": "CRITICAL"
  }'
```

Approve it (supervisor headers):

```bash
curl -X PATCH http://localhost:8082/api/droughts/1/approve \
  -H "X-User-Name: Mrs Moyo" -H "X-User-Hazard: drought"
```

## Run the tests

```bash
mvn test          # 14 tests: 13 business-rule unit tests + context load (H2, no MySQL needed)
```
