# DPDMS

**Rushinga Provincial Disaster Monitoring & Management System** — a microservices
backend built by Group 2 (University of Zimbabwe, OOP module, lecturer Mr. N. Zanamwe).

One independent Spring Boot service per hazard, plus auth, reporting, alerts,
a dashboard aggregator, an API gateway and a service registry. Every service
follows the SAME structure (see `flood-service` — the pattern Tanaka set in the
first commit — all other services are exact twins of it).

## The team and who owns what

| Member | Owns end-to-end | Supporting responsibility |
|--------|-----------------|---------------------------|
| **Tanaka** (leader) | `flood-service` | `api-gateway` (Spring Cloud Gateway) |
| **Oliver** | `drought-service` | `discovery-server` (Eureka) + `dashboard-service` lead |
| **Sean** | `fire-service` | `auth-service` (JWT, RBAC, accounts) |
| **Rejoice** | `zoonotic-disease-service` | `report-service` (PDF, DOCX, XLSX, CSV) |
| **Todzani** | `mining-accident-service` | `alert-service` (email, WhatsApp, Telegram) |

`dashboard-service` is the one deliberately shared component: Oliver leads it and
every member plugs their own hazard feed into it during integration.

## Services at a glance

| Service | Port | Database | Owner |
|---------|------|----------|-------|
| auth-service | 8080 | dpdms_auth | Sean |
| flood-service | 8081 | dpdms_flood | Tanaka |
| drought-service | 8082 | dpdms_drought | Oliver |
| fire-service | 8083 | dpdms_fire | Sean |
| zoonotic-disease-service | 8084 | dpdms_zoonotic | Rejoice |
| mining-accident-service | 8085 | dpdms_mining | Todzani |
| report-service | 8086 | none (live fetch) | Rejoice |
| alert-service | 8087 | dpdms_alert | Todzani |
| dashboard-service | 8088 | none (live fetch) | Oliver (lead) |
| api-gateway | 8888 | none | Tanaka |
| discovery-server | 8761 | none | Oliver |

## Quick start (3 commands, needs Docker)

```bash
cp .env.example .env
docker compose up -d --build
docker compose ps          # wait until everything is Up
```

Then:
- Registry dashboard: http://localhost:8761
- Everything through the gateway: `http://localhost:8888/api/...`
- Demo walkthrough: see `docs/00-STUDY-GUIDE.md` (also works without Docker — see DEPLOYMENT.md)

## How a request flows (the one picture to remember)

```
Frontend / Postman
      |  Authorization: Bearer <jwt>
      v
api-gateway :8888      <- verifies the JWT, injects X-User-Name/Role/Ward/Hazard headers
      |  lb://<service> (asks discovery-server who/where that service is)
      v
hazard service         <- enforces ward & hazard scoping, runs the approval workflow
      |  on APPROVE, best-effort POST /api/alerts/send
      v
alert-service :8087    <- fans out to EMAIL / WHATSAPP / TELEGRAM, logs every attempt
```

More detail: `docs/01-ARCHITECTURE.md` • Endpoints: `docs/03-API-REFERENCE.md`
• Databases: `docs/02-DATABASE.md` • Diagrams: `docs/diagrams/`

## The shared incident model (read this before editing)

Every hazard service stores the SAME core metadata, plus its own indicators:

```
ward, district, province, occurredAt, reporter, severity, status,
latitude, longitude,                    <- GPS at point of entry
reviewedBy, reviewedAt, reviewNotes,    <- audit trail
createdAt,
<5-6 hazard-specific indicators>        <- e.g. peakWaterLevelMetres for floods
```

Workflow: `PENDING -> APPROVED / REJECTED / CORRECTIONS_REQUESTED`.
Approved records are locked; editing a returned record resubmits it as PENDING.

## Build & test any service without Docker

```bash
# needs Java 17+ and MySQL running with root password Gr@nd$0n (see DEPLOYMENT.md)
mvn -f fire-service/pom.xml spring-boot:run
mvn -f fire-service/pom.xml test        # tests run on in-memory H2, NO MySQL needed
```

## Where each member should start

1. Read `docs/members/<your-name>.md` — your part explained file by file.
2. Run the system (Docker section above) and click through YOUR service's endpoints.
3. Read `docs/00-STUDY-GUIDE.md` — the 3-day mastery plan for the whole system.
