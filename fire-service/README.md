# fire-service

DPDMS fire hazard microservice. Owned by **Sean**. Standalone Spring Boot 4.1.1 / Java 17
service in the team's `flood-service` layout, owning the `dpdms_fire` schema.

- Port: `8203`
- Security: validates the auth-service JWT itself (`JwtAuthFilter` + `AuthContext`) and
  enforces the (ward, hazard) scope on every write. 401 = bad token, 403 = out of scope.
- Events: publishes `incident.approved.fire` to RabbitMQ exchange `incident.events`.

## Entity

Shared metadata (identical to flood-service): `ward, district, province, occurredAt,
reporter, severity, status, latitude, longitude` + audit (`reviewedBy, reviewedAt, reviewNotes`).

| # | Fire indicator | Field |
|---|---|---|
| 1 | area burned | `areaBurnedHectares` |
| 2 | suspected cause | `suspectedCause` |
| 3 | injuries / fatalities | `injuries`, `fatalities` |
| 4 | structures destroyed | `structuresDestroyed` |
| 5 | active / contained | `fireStatus` = ACTIVE \| CONTAINED |

`severity`: `LOW | MEDIUM | HIGH | CRITICAL` (implements `SeverityScale`).
`status`: `PENDING | APPROVED | REJECTED | CORRECTIONS_REQUESTED` (state machine enforced
inside the entity - illegal transitions -> 409).

## Endpoints (gateway route `/api/v1/fire/**`)

```
POST   /api/v1/fire/incidents                          capture (ward recorder, own ward)
GET    /api/v1/fire/incidents                          list (role-scoped)
GET    /api/v1/fire/incidents/approved                 approved-only feed (dashboard/report)
GET    /api/v1/fire/incidents/ward/{ward}              a ward's records
GET    /api/v1/fire/incidents/{id}                     single
PUT    /api/v1/fire/incidents/{id}                     edit + resubmit (own ward, while editable)
DELETE /api/v1/fire/incidents/{id}                     delete (own ward)
POST   /api/v1/fire/incidents/{id}/approve             (fire supervisor / admin)
POST   /api/v1/fire/incidents/{id}/reject              (fire supervisor / admin)
POST   /api/v1/fire/incidents/{id}/request-corrections (fire supervisor / admin)
```

Approval stamps reviewer + comment + time, then publishes `incident.approved.fire`;
if the broker is down, approval still succeeds (alerting is asynchronous by design, FR-ALR).

## Tests

```bash
./mvnw test     # 24 tests, no database required
```

Includes `FireScopeEnforcementTest` - the guide's required test: **a mismatched
supervisor's token must get 403** (a flood supervisor's real JWT approving a fire
incident), plus hazard/ward scoping, 401, and the approved-only feed.
