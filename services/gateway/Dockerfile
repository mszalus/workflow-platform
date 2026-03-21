FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .
COPY buildSrc/ buildSrc/
COPY libs/ libs/
COPY services/ services/
RUN chmod +x gradlew && ./gradlew :services:gateway:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appuser && adduser -S appuser -G appuser

WORKDIR /app
COPY --from=build /app/services/gateway/build/libs/*.jar app.jar

RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
