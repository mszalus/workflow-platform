FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY ../../gradlew .
COPY ../../gradle gradle
COPY ../../build.gradle.kts .
COPY ../../settings.gradle.kts .
COPY ../../gradle.properties .
COPY ../../buildSrc buildSrc
COPY ../../libs libs
COPY services/notification-service services/notification-service
RUN chmod +x gradlew && ./gradlew :services:notification-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/services/notification-service/build/libs/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
