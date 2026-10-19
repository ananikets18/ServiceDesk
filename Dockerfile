# ---------------------------------------------------------------------
# Build stage: compile the Spring Boot app into a runnable jar.
# ---------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy the Maven wrapper and pom.xml first. Docker caches this layer, so
# dependencies are only re-downloaded when the pom changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline || true

# Now copy the source and build the jar. Tests run in CI, not here.
COPY src/ src/
RUN ./mvnw -B -DskipTests package

# ---------------------------------------------------------------------
# Run stage: a small JRE image that only holds the built jar.
# ---------------------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
