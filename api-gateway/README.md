# api-gateway

The **single front door** of the DPDMS backend. Nothing else needs to be exposed
to users — every request goes through here (port 8888).

**Owner:** Tanaka (leader)   |   **Port:** 8888   |   No database.

## What it does on every request

1. **Checks the JWT** issued by auth-service (`Authorization: Bearer <token>`),
   except for `POST /api/auth/login` which must work without a token.
2. **Forwards the caller's identity** as `X-User-Name`, `X-User-Role`,
   `X-User-Ward`, `X-User-Hazard` headers — exactly what the hazard services
   read in their controllers (the leader's flood-service NOTE planned this).
3. **Strips any caller-sent X-User-* headers first**, so identity cannot be faked.
4. **Routes by path prefix** to the right microservice, discovered by name via
   Eureka (`lb://flood-service` etc.).
5. Bad/missing token -> **401** before any downstream service is contacted.

## Files

| File | What it does |
|------|--------------|
| `pom.xml` | Boot 4.1.1 + `spring-cloud-starter-gateway-server-webflux` + `eureka-client` + JJWT (same library/auth secret as auth-service). |
| `ApiGatewayApplication.java` | Entry point (no `@EnableEurekaClient` needed — presence of the dependency + properties is enough in current Spring Cloud). |
| `filter/JwtChecker.java` | Verifies signature/expiry/issuer with the shared secret. |
| `filter/JwtForwardFilter.java` | The `GlobalFilter` doing steps 1-5 above for every request. |
| `application.properties` | Port, Eureka address, shared secret, and the 9 routes (one per service). |
| `src/test/resources/application.properties` | Test config: Eureka disabled, test secret. |

## Route table (also in application.properties)

| Path | Service |
|------|---------|
| `/api/auth/**` | auth-service |
| `/api/floods/**` | flood-service |
| `/api/droughts/**` | drought-service |
| `/api/fires/**` | fire-service |
| `/api/zoonotics/**` | zoonotic-disease-service |
| `/api/minings/**` | mining-accident-service |
| `/api/reports/**` | report-service |
| `/api/alerts/**` | alert-service |
| `/api/dashboard/**` | dashboard-service |

## If the teacher asks

- **Why a gateway at all?** One public entry point -> services stay private;
  one place for JWT verification; the frontend only needs to know one URL.
- **Why headers instead of parsing the token in every service?** The parsing is
  done ONCE here; services stay simple and can still run standalone in dev by
  passing the headers manually (curl/Postman).
- **What if the token is expired?** The gateway returns 401 immediately —
  the request never reaches the hazard services.
