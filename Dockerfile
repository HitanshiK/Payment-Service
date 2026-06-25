# ---- Build stage ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# Copy the Gradle wrapper + build scripts first so dependency resolution is cached
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Copy sources and build the boot jar (tests run in CI, not in the image build)
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# Run as an unprivileged user
RUN useradd -r -u 1001 appuser
COPY --from=build /workspace/build/libs/*.jar app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
