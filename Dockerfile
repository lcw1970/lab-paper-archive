FROM gradle:9.7.1-jdk21-alpine AS build
WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/build/libs/*.jar app.jar
RUN mkdir -p /app/logs /data/papers && chown -R app:app /app /data/papers

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
