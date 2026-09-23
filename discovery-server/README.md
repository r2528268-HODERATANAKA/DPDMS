# discovery-server

The **Eureka service registry** — the "phonebook" of DPDMS.

**Owner:** Oliver   |   **Port:** 8761   |   **Dashboard:** http://localhost:8761

## What it does

Every DPDMS service registers itself with Eureka when it starts (using its
`spring.application.name`, e.g. `flood-service`). When the api-gateway needs to
forward a request to `/api/floods/**`, it asks Eureka: "where is a live
instance of flood-service?" and forwards the request there (`lb://flood-service`).
That is the whole job — no database, no business logic.

## Files

| File | What it does |
|------|--------------|
| `pom.xml` | Same Spring Boot 4.1.1 parent as the other services + `spring-cloud-starter-netflix-eureka-server` (version managed by the Spring Cloud 2025.1.1 BOM). |
| `DiscoveryServerApplication.java` | Entry point. `@EnableEurekaServer` turns the app into a registry. |
| `application.properties` | Port 8761; `register-with-eureka=false` / `fetch-registry=false` because this app IS the registry. |

## If the teacher asks

- **Why do we need this?** Without it the gateway would need a hard-coded
  `localhost:port` for every service. With Eureka, services find each other by
  *name*, they can move ports/hosts, and dead instances disappear from the registry.
- **How do services find it?** `eureka.client.service-url.defaultZone=http://localhost:8761/eureka`
  (in Docker this becomes `http://discovery:8761/eureka` — see the root `docker-compose.yml`).
- **Where do I see it working?** Open http://localhost:8761 — every registered
  DPDMS service is listed there with its status.
