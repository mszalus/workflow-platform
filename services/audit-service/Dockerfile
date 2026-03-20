FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY ../../gradlew .
COPY ../../gradle gradle
COPY ../../build.gradle.kts .
COPY ../../settings.gradle.kts .
COPY ../../gradle.properties .
COPY ../../buildSrc buildSrc
COPY ../../libs libs
COPY services/audit-service services/audit-service
RUN chmod +x gradlew && ./gradlew :services:audit-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/services/audit-service/build/libs/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
