# flood-service

The FIRST service of the project — the pattern every other DPDMS service is
modelled on. Tanaka (leader) built this one completely.

**Owner:** Tanaka   |   **Port:** 8081   |   **Database:** `dpdms_flood` (auto-created)

## Files in this service (what each one does)

| File | What it does |
|------|--------------|
| `pom.xml` | Spring Boot 4.1.1, Java 17, starters: data-jpa, validation, webmvc (+ Boot 4 test starters), MySQL driver, Lombok. |
| `FloodServiceApplication.java` | Entry point (`@SpringBootApplication`). |
| `src/main/resources/application.properties` | Port 8081, MySQL `dpdms_flood` with `createDatabaseIfNotExist=true`, `ddl-auto=update`, SQL logging. |
| `model/FloodIncident.java` | JPA entity `flood_incidents`. Shared metadata (ward/district/province/occurredAt/reporter/severity/status) + GPS + 5 flood indicators (peakWaterLevelMetres, riverBasin, householdsDisplaced, areaFloodedHectares, inundationDurationDays) + audit (reviewedBy/reviewedAt/reviewNotes) + createdAt. Lombok `@Data @Builder` generates accessors. |
| `model/Severity.java` | LOW / MEDIUM / HIGH / CRITICAL. |
| `model/IncidentStatus.java` | PENDING / APPROVED / REJECTED / CORRECTIONS_REQUESTED — the workflow. |
| `repository/FloodIncidentRepository.java` | Spring Data interface: `findByStatus`, `findByWardAndStatus`, `findByWard`, `findByDistrict` — derived queries, no SQL. |
| `service/FloodIncidentService.java` | The business rules: create forces PENDING; ward/hazard scoping (403); approved records locked; editing resubmits as PENDING; approve/reject/request-corrections stamp the audit fields. The `TODO` in `approve` marks where alert-service hooks in (implemented in the other four services — see drought-service's `AlertNotifier`). |
| `controller/FloodIncidentController.java` | REST under `/api/floods` (create, approved feed, ward view, by id, update, delete, approve, reject, request-corrections). Reads `X-User-Ward` / `X-User-Hazard` / `X-User-Name` — the NOTE comment explains the JWT/gateway swap plan (implemented, see api-gateway). |
| `exception/ForbiddenOperationException.java` | → HTTP 403 |
| `exception/ResourceNotFoundException.java` | → HTTP 404 |
| `exception/GlobalExceptionHandler.java` | `@RestControllerAdvice`: clean JSON errors, validation failures as a field→message map (400). |
| `FloodServiceApplicationTests.java` | Context load test. NOTE: this service's pom has no H2, so this test needs MySQL running (the other services include H2 for tests). |

## Why this service matters to the team

Every other service (drought, fire, zoonotic, mining) is a **twin** of this one:
same layers, same naming, same scoping rules, same workflow — only the
hazard-specific indicator columns differ. That consistency was a deliberate
team decision: review one, understand five.

## Try it

```bash
curl -X POST http://localhost:8081/api/floods \
  -H "Content-Type: application/json" \
  -H "X-User-Ward: Mudzi" -H "X-User-Hazard: flood" \
  -d '{
    "ward":"Mudzi","district":"Mudzi","province":"Mashonaland Central",
    "occurredAt":"2026-09-21T06:00:00","reporter":"T. Nyamapfene",
    "severity":"HIGH","latitude":-16.9,"longitude":31.9,
    "peakWaterLevelMetres":3.2,"riverBasin":"Mazowe",
    "householdsDisplaced":48,"areaFloodedHectares":90.0,"inundationDurationDays":4
  }'

curl -X PATCH http://localhost:8081/api/floods/1/approve \
  -H "X-User-Name: Mrs Moyo" -H "X-User-Hazard: flood"
```
