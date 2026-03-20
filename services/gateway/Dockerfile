FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY gradle/ gradle/
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY libs/ libs/
COPY services/gateway/ services/gateway/

RUN ./gradlew :services:gateway:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre

RUN groupadd --system appuser && useradd --system --gid appuser appuser

WORKDIR /app

COPY --from=build /workspace/services/gateway/build/libs/*.jar app.jar

RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
