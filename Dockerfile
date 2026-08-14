# ── Build stage ──────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Cache dependencies layer separately for faster rebuilds
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Run stage ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

# Install curl for healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Railway injects PORT env var; fall back to 8081 for local Docker runs
EXPOSE 8081

ENTRYPOINT ["java", \
  "-Xmx512m", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
