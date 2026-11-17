# =====================================================================
# STAGE 1: Build Stage (Maven + Eclipse Temurin JDK 17 Alpine)
# =====================================================================
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /workspace

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -Dcheckstyle.skip=true -B

# =====================================================================
# STAGE 2: Runtime Stage (Minimal Eclipse Temurin JRE 17 Alpine)
# =====================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Security Hardening: Run microservice as a non-root banking user
RUN addgroup -S pivotgroup && adduser -S pivotuser -G pivotgroup

COPY --from=builder /workspace/target/*.jar app.jar
RUN chown -R pivotuser:pivotgroup /app

USER pivotuser

EXPOSE 8082

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]