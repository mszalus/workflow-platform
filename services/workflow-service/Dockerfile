FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY ../../gradlew .
COPY ../../gradle gradle
COPY ../../build.gradle.kts .
COPY ../../settings.gradle.kts .
COPY ../../gradle.properties .
COPY ../../buildSrc buildSrc
COPY ../../libs libs
COPY services/workflow-service services/workflow-service
RUN chmod +x gradlew && ./gradlew :services:workflow-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/services/workflow-service/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
