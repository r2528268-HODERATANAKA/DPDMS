# Oliver's part — drought-service + discovery-server + dashboard-service

You own a hazard slice, the infrastructure piece (Eureka) and the group's
integration checkpoint (dashboard). Three small services instead of two big
ones — each is genuinely simple.

## drought-service (port 8082, db dpdms_drought)

An exact twin of flood-service. Your fields: `rainfallDeficitMm`,
`consecutiveDryDays`, `affectedHouseholds`, `livestockDeaths`,
`cropDamageHectares`, `waterSourceCondition` (NORMAL/STRESSED/CRITICAL/DRY).

| File | One-sentence explanation |
|------|--------------------------|
| `model/DroughtIncident.java` | Row in `drought_incidents`: shared metadata + GPS + your drought indicators + audit trail. |
| `model/WaterSourceCondition.java` | Condition of the ward's main water sources. |
| `service/DroughtIncidentService.java` | PENDING on create, 403 scoping before any DB call, approved locked, reviews audited — identical rules to flood. |
| `service/AlertNotifier.java` | On approve → best-effort POST to alert-service (this is the class Tanaka's flood TODO planned — flood can copy it verbatim). |
| `controller/DroughtIncidentController.java` | 9 endpoints under `/api/droughts`. |
| `DroughtIncidentServiceTest.java` | 13 unit tests, one per rule. |

## discovery-server (port 8761)

| File | One-sentence explanation |
|------|--------------------------|
| `pom.xml` | Boot 4.0.2 + `spring-cloud-starter-netflix-eureka-server` (Spring Cloud 2025.1.1 — version note in the pom). |
| `DiscoveryServerApplication.java` | `@EnableEurekaServer` — the whole logic is that annotation. |
| `application.properties` | Port 8761; does not register with itself. |

**Your line when demoing:** "This is the phonebook. Every service registers
here by name; the gateway asks it where `flood-service` lives today."

## dashboard-service (port 8088)

| File | One-sentence explanation |
|------|--------------------------|
| `client/HazardDataFetcher.java` | Calls the five approved feeds; dead service → empty list (the dashboard never breaks). |
| `service/DashboardService.java` | Per-hazard approved counts, by-severity counts, 5 latest approved (newest first), health UP/DOWN map. |
| `controller/DashboardController.java` | `GET /api/dashboard/summary`, `GET /api/dashboard/health`. |
| `DashboardServiceTest.java` | 4 tests incl. "dead hazard degrades to zero" and "latest-5 sorted newest first". |

## Your demo (5 minutes)

1. `GET :8888/api/dashboard/health` — every member's service shows UP because
   they all registered with YOUR discovery server.
2. Stop one service (`docker compose stop fire-service`) → refresh health:
   fire shows DOWN, everything else still works — resilience you built.
3. Create + approve a drought incident → `GET :8888/api/dashboard/summary`
   shows the drought count go up.

## If the teacher asks YOU

- **Why does the system need Eureka?** Without it the gateway needs hardcoded
  host:port per service; with it, services find each other by name and the
  dashboard/gateway survive instances moving or dying.
- **Why does the dashboard have no database?** It is a live aggregation; storing
  counts would just make them stale.
- **Who feeds the dashboard?** Every member — that is why it is the group's
  integration checkpoint in the guide.
