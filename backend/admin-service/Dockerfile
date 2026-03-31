FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY common/ common/
COPY admin-service/ admin-service/
RUN gradle :admin-service:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl && addgroup -S app && adduser -S app -G app
COPY --from=builder /app/admin-service/build/libs/*.jar app.jar
RUN chown -R app:app /app
USER app
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]
