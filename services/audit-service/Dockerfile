FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .
COPY gradle.properties .
COPY buildSrc/ buildSrc/
COPY libs/ libs/
COPY services/ services/
RUN chmod +x gradlew && ./gradlew :services:audit-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appuser && adduser -S appuser -G appuser
WORKDIR /app
COPY --from=build /app/services/audit-service/build/libs/*.jar app.jar
RUN chown -R appuser:appuser /app
USER appuser
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
