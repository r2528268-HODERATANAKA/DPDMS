# DPDMS API reference (with copy-paste curl)

Two ways to call any endpoint:

1. **Through the gateway (the real way)** — `http://localhost:8888/...` with a
   `Authorization: Bearer <token>` header. The gateway checks the token and
   adds the `X-User-*` headers for you.
2. **Direct to a service (dev/testing)** — e.g. `http://localhost:8083/...`,
   adding the `X-User-*` headers yourself. Great for demos when you want to
   show the scoping rules in isolation.

Login once and save the token:

```bash
TOKEN=$(curl -s -X POST http://localhost:8888/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"tanaka","password":"Passw0rd!"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
echo $TOKEN
```

Seed accounts (create the first admin with the SQL snippet in
`docs/members/SEAN.md`, then create everyone else through the API):

| Username | Role | ward | hazard |
|----------|------|------|--------|
| admin | PROVINCIAL_ADMIN | – | – |
| tanaka | WARD_RECORDER | Mudzi | flood |
| oliver | PROVINCIAL_SUPERVISOR | – | drought |
| sean | WARD_RECORDER | Mudzi | fire |
| rejoice | WARD_RECORDER | Kanyemba | zoonotic |
| todzani | PROVINCIAL_SUPERVISOR | – | mining |

(All with password `Passw0rd!` if you use the seed snippet.)

---

## auth-service (Sean) — `/api/auth`

| Method & path | Who | Purpose |
|---------------|-----|---------|
| `POST /api/auth/login` | everyone | get a JWT |
| `GET /api/auth/validate` | everyone (Bearer token) | check a token, see its claims |
| `POST /api/auth/users` | PROVINCIAL_ADMIN | create an account |
| `GET /api/auth/users` | PROVINCIAL_ADMIN | list accounts |

```bash
# login
curl -X POST http://localhost:8888/api/auth/login -H "Content-Type: application/json" \
  -d '{"username":"tanaka","password":"Passw0rd!"}'

# validate a token
curl http://localhost:8888/api/auth/validate -H "Authorization: Bearer $TOKEN"

# admin creates a ward recorder (FR-SCOPE-01: concrete ward+hazard enforced)
curl -X POST http://localhost:8888/api/auth/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"username":"sean","password":"Passw0rd!","fullName":"Sean M.","role":"WARD_RECORDER","ward":"Mudzi","hazard":"fire"}'
# a recorder with ward "ALL" is rejected with 400 - that is the FR-SCOPE-01 rule working
```

## Hazard services — `/api/floods`, `/api/droughts`, `/api/fires`, `/api/zoonotics`, `/api/minings`

Identical shape in all five (shown for fires; swap the prefix and fields):

| Method & path | Who | Purpose |
|---------------|-----|---------|
| `POST /api/fires` | ward recorder | create (status forced to PENDING) |
| `GET /api/fires` | everyone | APPROVED-only feed (reports/dashboard/alerts) |
| `GET /api/fires/ward/{ward}` | recorder view | all records of one ward incl. PENDING |
| `GET /api/fires/{id}` | everyone | one record |
| `PUT /api/fires/{id}` | ward recorder | edit non-approved (resubmits as PENDING) |
| `DELETE /api/fires/{id}` | ward recorder | delete |
| `PATCH /api/fires/{id}/approve` | supervisor | approve |
| `PATCH /api/fires/{id}/reject` | supervisor | reject (body: `{"reason":"..."}`) |
| `PATCH /api/fires/{id}/request-corrections` | supervisor | send back (body: `{"notes":"..."}`) |

```bash
# recorder creates a fire incident (direct call - note the manual headers)
curl -X POST http://localhost:8083/api/fires \
  -H "Content-Type: application/json" \
  -H "X-User-Ward: Mudzi" -H "X-User-Hazard: fire" \
  -d '{
    "ward":"Mudzi","district":"Mudzi","province":"Mashonaland Central",
    "occurredAt":"2026-09-20T14:30:00","reporter":"T. Chirwa",
    "severity":"HIGH","latitude":-17.4,"longitude":31.6,
    "areaBurnedHectares":45.5,"suspectedCause":"Land clearing fire that spread",
    "injuries":1,"fatalities":0,"structuresDestroyed":2,"fireStatus":"ACTIVE"
  }'

# same call through the gateway, as the frontend does it (identity comes from the token)
curl -X POST http://localhost:8888/api/fires \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{ ...same body... }'

# supervisor approves (fires the alert notification as a side effect)
curl -X PATCH http://localhost:8888/api/fires/1/approve -H "Authorization: Bearer $SUPERVISOR_TOKEN"

# wrong ward -> 403 (try it - this is a marking-guide behaviour)
curl -X POST http://localhost:8083/api/fires -H "X-User-Ward: Bindura" -H "X-User-Hazard: fire" \
  -H "Content-Type: application/json" -d '{ ...any valid body... }'
```

Field cheat-sheet per hazard (all mandatory):

| Hazard | Own fields (after the shared block) |
|--------|--------------------------------------|
| flood | `peakWaterLevelMetres`, `riverBasin`, `householdsDisplaced`, `areaFloodedHectares`, `inundationDurationDays` |
| drought | `rainfallDeficitMm`, `consecutiveDryDays`, `affectedHouseholds`, `livestockDeaths`, `cropDamageHectares`, `waterSourceCondition` |
| fire | `areaBurnedHectares`, `suspectedCause`, `injuries`, `fatalities`, `structuresDestroyed`, `fireStatus` |
| zoonotic | `diseaseName`, `suspectedAnimalSpecies`, `humanCases`, `humanDeaths`, `animalsAffected`, `outbreakStatus` |
| mining | `mineName`, `accidentType`, `casualties`, `rescued`, `mineOperationalStatus`, `description` |

## report-service (Rejoice) — `/api/reports`

| Method & path | Purpose |
|---------------|---------|
| `GET /api/reports/{hazard}?format=csv\|pdf\|xlsx\|docx` | download the approved incidents of one hazard as a file (format defaults to pdf) |

```bash
curl -OJ "http://localhost:8888/api/reports/flood?format=pdf"        # flood-report-2026-09-24.pdf
curl -OJ "http://localhost:8888/api/reports/mining?format=xlsx" -H "Authorization: Bearer $TOKEN"
```

## alert-service (Todzani) — `/api/alerts`

| Method & path | Purpose |
|---------------|---------|
| `POST /api/alerts/send` | send one alert through all channels (hazard services call this on approval) |
| `POST /api/alerts/scan` | pull approved incidents from all 5 hazards, alert any never-alerted ones |
| `GET /api/alerts` | full alert log, newest first |
| `GET /api/alerts/hazard/{hazard}` | log for one hazard |
| `GET /api/alerts/config` | which channels are configured |

```bash
curl -X POST http://localhost:8888/api/alerts/send -H "Content-Type: application/json" \
  -d '{"hazard":"flood","incidentId":1,"ward":"Mudzi","district":"Mudzi","severity":"HIGH","message":"Demo alert"}'
curl -X POST http://localhost:8888/api/alerts/scan
curl http://localhost:8888/api/alerts
```

## dashboard-service (Oliver + group) — `/api/dashboard`

| Method & path | Purpose |
|---------------|---------|
| `GET /api/dashboard/summary` | approved counts per hazard + by severity + 5 latest |
| `GET /api/dashboard/health` | UP/DOWN per hazard service |

```bash
curl http://localhost:8888/api/dashboard/summary
curl http://localhost:8888/api/dashboard/health
```

## Error contract (all services behave the same)

| Status | When |
|--------|------|
| 400 | validation failed (JSON body contains a field->message map) or bad scope values |
| 401 | missing/invalid/expired token (gateway) or bad credentials (auth) |
| 403 | caller is outside their ward/hazard scope, or non-admin touched admin endpoints |
| 404 | unknown id / unknown hazard |
| 409 | duplicate username |
| 502 | report-service could not reach a hazard service |
