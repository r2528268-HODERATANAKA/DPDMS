# auth-service

DPDMS authentication service - issues the signed JWTs every other DPDMS service trusts.
Owned by **Sean**. Standalone Spring Boot 4.1.1 / Java 17 service in the team's
`flood-service` layout (`controller / service / repository / model / dto / exception`).

- Port: `8100`
- Database: MySQL `dpdms_auth` (team credentials convention: root / Gr@nd$0n)
- Tech: JPA, JJWT 0.12.6, BCrypt (`spring-boot-starter-security`), Lombok

---

## TOKEN CONTRACT (what the other services code against)

`POST /api/v1/auth/login` returns a signed **HS256** JWT. Every claim below is stable.

| Claim | Present on | Meaning |
|---|---|---|
| `sub` | all | username |
| `role` | all | `WARD_RECORDER` \| `PROVINCIAL_SUPERVISOR` \| `PROVINCIAL_ADMIN` |
| `name` | all | full name - hazard services use it as the `reviewedBy` stamp |
| `ward` | WARD_RECORDER only | the ONE ward this account may write to |
| `hazard` | RECORDER + SUPERVISOR | the ONE hazard this account is scoped to |
| `iss` | all | always `dpdms-auth-service` |
| `iat`/`exp` | all | issued-at / expiry (default 720 minutes = 12 h) |

Decoded payload example:

```json
{
  "iss": "dpdms-auth-service",
  "sub": "ward4.fire",
  "role": "WARD_RECORDER",
  "ward": "Ward 4",
  "hazard": "fire",
  "name": "Ward 4 Fire Recorder",
  "iat": 1758300000,
  "exp": 1758343200
}
```

### Rules every teammate's service must follow

1. Read the same secret from env `JWT_SECRET` (min 32 chars) - a token signed with a
   different secret is rejected.
2. Read `Authorization: Bearer <token>`; validate signature + expiry.
   (Or call `GET /api/v1/auth/validate` and read the decoded scope - no secret needed.)
3. Map claims to checks: recorder -> own ward AND own hazard; supervisor -> own hazard,
   all wards; admin -> everything.
4. **401** = missing/invalid/expired token. **403** = valid token, out of scope.
5. `/internal/**` is service-to-service only - never route it through the gateway.

### Role -> what a token allows

| Role | ward | hazard | Allowed |
|---|---|---|---|
| `WARD_RECORDER` | concrete | concrete | create/edit/delete records of that hazard in that ward |
| `PROVINCIAL_SUPERVISOR` | - | concrete | approve / reject / request-corrections for that hazard |
| `PROVINCIAL_ADMIN` | - | - | manage accounts; review/capture across the province |

**FR-SCOPE-01:** a `WARD_RECORDER` account must be pinned to one concrete ward AND one
hazard; wildcards (`""`, blank, `"*"`, `"ALL"`) are rejected at account creation with **400**.

---

## Endpoints

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/v1/auth/login` | public | `{"username","password"}` -> `LoginResponse`; 401 wrong/disabled |
| GET | `/api/v1/auth/me` | Bearer | current profile |
| GET | `/api/v1/auth/validate` | Bearer | decoded scope of the token |
| GET | `/api/v1/auth/users` | Bearer + PROVINCIAL_ADMIN | list accounts (no password hashes) |
| POST | `/api/v1/auth/users` | Bearer + PROVINCIAL_ADMIN | create account; 400 bad scope, 403 non-admin, 409 duplicate |
| PUT | `/api/v1/auth/users/{id}/status` | Bearer + PROVINCIAL_ADMIN | enable/disable |
| GET | `/internal/recipients?ward=&hazard=` | internal | alert-service fan-out lookup |

Seeded demo accounts: `admin` / `Admin@123`; `*.supervisor` / `Super@123`;
`ward4.fire`, `ward11.fire`, `ward12.fire`, `ward13.fire` / `Ward@123`.

## Running

```bash
./mvnw spring-boot:run     # needs MySQL (root / Gr@nd$0n); creates dpdms_auth itself
./mvnw test                # 22 unit tests, no database required
```

Config: `dpdms.jwt.secret` (min 32 chars) and `dpdms.jwt.expiration-minutes` (default 720).
