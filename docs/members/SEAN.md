# Sean's part — fire-service + auth-service

You own two services: **fire-service** (your hazard, CRUD + workflow) and
**auth-service** (security for the whole group). Together they are the most
security-sensitive slice — expect questions here.

## fire-service (port 8083, db dpdms_fire)

An exact twin of Tanaka's flood-service: same layers, same rules — your fields
are `areaBurnedHectares`, `suspectedCause`, `injuries`, `fatalities`,
`structuresDestroyed` and the `FireStatus` enum (ACTIVE/CONTAINED).

| File | One-sentence explanation |
|------|--------------------------|
| `model/FireIncident.java` | One row in `fire_incidents`: shared metadata + GPS + your 5 fire indicators + audit trail; validation annotations guard every field. |
| `model/FireStatus.java` | ACTIVE while burning, CONTAINED once under control. |
| `repository/FireIncidentRepository.java` | Spring Data derived queries (findByStatus, findByWard...) — no SQL written by hand. |
| `service/FireIncidentService.java` | The rules: create forces PENDING, ward/hazard 403 guards run BEFORE any DB lookup, approved records locked, edits resubmit as PENDING, reviews stamp reviewedBy/reviewedAt. |
| `service/AlertNotifier.java` | On approve → best-effort POST to alert-service (3 s timeout, try/catch) — fulfils the flood-service TODO. |
| `controller/FireIncidentController.java` | 9 endpoints under `/api/fires`; reads X-User-* headers (injected by the gateway from the JWT). |
| `exception/GlobalExceptionHandler.java` | Turns 403/404/400 into clean JSON for every endpoint. |
| `FireIncidentServiceTest.java` | 13 unit tests: each business rule has one — including the "wrong-hazard supervisor rejected at the door" test the guide requires. |

## auth-service (port 8080, db dpdms_auth)

| File | One-sentence explanation |
|------|--------------------------|
| `model/UserAccount.java` + `Role.java` | One user row; BCrypt hash only (never the password); 3 roles documented in comments. |
| `service/JwtTokenService.java` | Issues/verifies HS256 tokens (JJWT 0.12.6); secret ≥32 chars enforced at startup; adds ward/hazard claims only when present. |
| `service/AuthService.java` | login (401 on bad creds/deactivated), validate, createAccount (admin-only 403, wildcard ward/hazard → 400 per FR-SCOPE-01, duplicate username → 409, BCrypt on save). |
| `controller/AuthController.java` | `/api/auth/login`, `/api/auth/validate`, `/api/auth/users` (admin). |
| `dto/` | Request/response shapes; `UserResponse` never exposes the hash. |
| `exception/GlobalExceptionHandler.java` | Maps JwtException → 401 too. |
| Tests (14) | 7 JWT tests (round-trip, tamper, wrong key, short secret...) + 7 service tests incl. FR-SCOPE-01 wildcard rejection and "hash is really BCrypt". |

## Your demo (5 minutes)

1. `demo-requests.http` steps 0-6: login → recorder creates fire → recorder
   tries to approve (403) → supervisor approves → edit approved (403).
2. Show the alert log after approval: `GET :8888/api/alerts` — proof your
   approve hook reached alert-service.
3. Run the tests live: `mvn -f fire-service/pom.xml test` then auth.

## If the teacher asks YOU

- **Why headers and not the token in each service?** The gateway verifies the
  JWT once and injects `X-User-*`; services stay simple and testable with
  plain headers; swapping to token-claims later touches only the controllers.
- **How is the password safe?** BCrypt hash with per-hash salt; login compares
  hash-to-hash; `UserResponse` cannot leak it by construction.
- **What stops a Zoonotic supervisor approving a fire?** `enforceHazardScope`
  runs before `findById` — the unit test `approveBySupervisorOfDifferentHazardRejectedAtTheDoor`
  uses `verifyNoInteractions(repository)` to prove no lookup even happens.
- **What does FR-SCOPE-01 change at account creation?** A recorder needs a
  concrete ward + hazard; "ALL"/"*"/blank → HTTP 400 (tested).
