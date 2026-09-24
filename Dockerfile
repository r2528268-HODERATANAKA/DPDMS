# ============================================================
# ONE Dockerfile for every DPDMS Spring Boot service.
#
# Build any single service, e.g.:
#   docker build --build-arg MODULE=flood-service -t dpdms/flood-service .
# docker-compose.yml does this automatically for all services.
#
# Stage 1 (build):   Maven + JDK 17 compile the service into a jar
# Stage 2 (runtime): small JRE 17 image that only runs the jar
# ============================================================

# ---------- Stage 1: build ----------
FROM maven:3.9.9-eclipse-temurin-17 AS build
ARG MODULE
WORKDIR /build

# Copy the pom first so Docker can cache the dependency download layer
COPY ${MODULE}/pom.xml ${MODULE}/pom.xml
RUN mvn -ntp -f ${MODULE}/pom.xml dependency:go-offline || true

# Copy the sources and package (tests are skipped here; CI/`mvn test` covers them)
COPY ${MODULE}/src ${MODULE}/src
RUN mvn -ntp -f ${MODULE}/pom.xml package -DskipTests

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:17-jre
ARG MODULE
COPY --from=build /build/${MODULE}/target/*.jar /app/app.jar
# Run as a non-root user (good practice on a VPS)
RUN useradd -r dpdms && chown -R dpdms /app
USER dpdms
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
