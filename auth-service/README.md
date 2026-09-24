# auth-service

Login + JWT issuing + user account management for the whole DPDMS system.

**Owner:** Sean   |   **Port:** 8080   |   **Database:** `dpdms_auth` (auto-created)

## Files in this service (what each one does)

| File | What it does |
|------|--------------|
| `pom.xml` | Boot 4.1.1 starters + **JJWT 0.12.6** (JWT) + **spring-security-crypto** (BCrypt only — no filter chain) + MySQL + H2 for tests. |
| `AuthServiceApplication.java` | Entry point (port 8080). |
| `model/Role.java` | The 3 roles with a comment each explaining what they may do. |
| `model/UserAccount.java` | JPA entity `user_accounts`. Stores the **BCrypt hash**, never the password. `ward`/`hazard` implement FR-SCOPE-01. |
| `repository/UserAccountRepository.java` | `findByUsername`, `existsByUsernameIgnoreCase`. |
| `service/JwtTokenService.java` | Signs and verifies HS256 tokens. Secret must be ≥ 32 chars (startup fails fast otherwise). Adds `ward`/`hazard` claims only when present. |
| `service/AuthService.java` | The rules: login (401 on bad creds / deactivated), token validation, account creation (admin-only, FR-SCOPE-01 wildcard rejection, duplicate username → 409, BCrypt hashing). |
| `controller/AuthController.java` | `POST /api/auth/login`, `GET /api/auth/validate`, `POST/GET /api/auth/users` (admin — reads `X-User-Role` header injected by the gateway). |
| `dto/` | `LoginRequest`, `LoginResponse`, `CreateUserRequest`, `UserResponse` (never leaks the hash), `ValidationResponse`. |
| `exception/` | `InvalidCredentialsException` → 401, `ForbiddenOperationException` → 403, `DuplicateResourceException` → 409, plus a handler that maps any `JwtException` → 401. |
| `src/test/resources/application.properties` | Tests run on in-memory H2 with a test secret. |
| `JwtTokenServiceTest.java` (7 tests) | round-trip claims, admin token has no ward/hazard, expiry window, wrong key rejected, tampered token rejected, garbage rejected, short secret rejected. |
| `AuthServiceTest.java` (7 tests) | login ok / wrong password / unknown user / deactivated, validate with a REAL issued token, admin-only creation, wildcard ward rejected (FR-SCOPE-01), BCrypt hash actually stored. |

## The token contract (the one table to memorise)

| Claim | Value | Used for |
|-------|-------|----------|
| `sub` | username | who is calling |
| `role` | `WARD_RECORDER` / `PROVINCIAL_SUPERVISOR` / `PROVINCIAL_ADMIN` | what they may do |
| `name` | full name | becomes `reviewedBy` on approvals |
| `ward` | e.g. `Mudzi` | recorders only — ward scoping |
| `hazard` | `flood`/`drought`/`fire`/`zoonotic`/`mining` | recorders + supervisors — hazard scoping |
| `iss` | `dpdms-auth-service` | issuer check in gateway + here |
| `exp` | issue + 480 min | the gateway rejects expired tokens with 401 |

**The secret must be identical in auth-service and api-gateway**
(`dpdms.jwt.secret` — set `DPDMS_JWT_SECRET` in `.env` for both).

## Roles × endpoints matrix

| Endpoint | WARD_RECORDER | PROVINCIAL_SUPERVISOR | PROVINCIAL_ADMIN |
|----------|:---:|:---:|:---:|
| POST `/api/auth/login` | ✅ | ✅ | ✅ |
| GET `/api/auth/validate` | ✅ | ✅ | ✅ |
| POST `/api/auth/users` | ❌ 403 | ❌ 403 | ✅ |
| GET `/api/auth/users` | ❌ 403 | ❌ 403 | ✅ |

## Bootstrapping the first admin

Nobody can create accounts before an admin exists, so seed one directly:

```sql
-- run once against dpdms_auth (the hash is BCrypt of "Passw0rd!")
INSERT INTO user_accounts (username, password_hash, full_name, role, active, created_at)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Provincial Admin', 'PROVINCIAL_ADMIN', true, NOW());
```

Then create everyone else through the API (see `docs/03-API-REFERENCE.md`).
That BCrypt hash above is the standard hash for `Passw0rd!` — change it for
anything real.

## Try it

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Passw0rd!"}'
```

## If the teacher asks

- **Where is the password stored?** Only as a BCrypt hash (`$2a$10$...`); the
  hash includes its own salt. `spring-security-crypto` gives us BCrypt without
  any servlet security machinery.
- **How would a stolen token be limited?** 8-hour expiry; the gateway strips
  fake `X-User-*` headers; deactivating the account makes `validate` fail
  immediately even if the token is still unexpired.
- **Why 400 for ward "ALL"?** FR-SCOPE-01: wildcards at account-creation time
  would silently widen a recorder's access, so we reject them early instead of
  patching scoping bugs later.
