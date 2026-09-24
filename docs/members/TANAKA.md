# Tanaka's part — flood-service + api-gateway

You are the leader: your flood-service set THE pattern every other member
followed, and your gateway is the single door the whole system goes through.

## flood-service (port 8081, db dpdms_flood)

Already complete (your commit `f2cf779 "Done flood service"`). Its README now
documents it file-by-file — read it again with the teacher in mind. Key points
you defend:

- The `NOTE ON HEADERS` comment in your controller planned the JWT/gateway swap
  — it is now implemented: the gateway injects exactly the headers you chose.
- The `TODO` in your `approve` method planned the alert hook — implemented in
  the other four services (`AlertNotifier`); if you want it in flood too, copy
  the two classes from drought-service and add one line in approve (documented
  in drought-service README).

## api-gateway (port 8888, no database)

| File | One-sentence explanation |
|------|--------------------------|
| `pom.xml` | Boot 4.0.2 + Spring Cloud 2025.1.1 (gateway-webflux + eureka-client + JJWT). The 4.0.2 pin is because Spring Cloud 2025.1.x officially supports Boot 4.0.x — comment in the pom explains it. |
| `ApiGatewayApplication.java` | Entry point; routes are pure configuration. |
| `filter/JwtChecker.java` | Verifies signature/expiry/issuer with the SAME secret as auth-service. |
| `filter/JwtForwardFilter.java` | The GlobalFilter on every request: login passes through; other /api/** needs Bearer; claims become X-User-Name/Role/Ward/Hazard; client-sent X-User-* headers are stripped first (anti-faking); bad token → 401 before any service is contacted. |
| `application.properties` | The 9 routes (`/api/floods/**` → `lb://flood-service`, etc.) + Eureka address + shared secret. |
| `ApiGatewayApplicationTests.java` | Context load test (Eureka disabled, MVC-free test starter — reactive gateway refuses Spring MVC on the classpath). |

## Your demo (5 minutes)

1. Call `GET :8888/api/floods` with NO token → 401 (proof the door is guarded).
2. Login → repeat with the Bearer token → 200 (proof the gateway decodes identity).
3. Show a tampered token (change 3 characters) → 401.
4. Open `http://localhost:8761` — your gateway + all services on the Eureka board.
5. Show the route table in `application.properties` — 9 lines, one per service.

## If the teacher asks YOU

- **Why is there a gateway at all?** One public door: services stay private on
  the VPS (firewall), JWT is verified once, the frontend needs only one URL.
- **What does `lb://` mean?** "Load balance to whoever Eureka says is
  `flood-service` right now" — instances can move/ports can change freely.
- **Why strip incoming X-User-* headers?** Otherwise anyone could fake their
  identity with a header; only claims from a VERIFIED token may set them.
- **Why is the gateway on a different Spring Boot version?** Spring Cloud
  2025.1.x (the release train with Eureka/Gateway for Boot 4) officially
  supports 4.0.x; services are standalone so mixing is safe and documented.
