# Build stage
FROM gradle:8.0-jdk21 as builder
WORKDIR /app
COPY . .
RUN gradle :server:shadowJar -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/server/build/libs/server-all.jar app.jar

# Environment variables
ENV SUPPORTDESK_SERVER_HOST=0.0.0.0
ENV SUPPORTDESK_SERVER_PORT=8080

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
