# Built from the repo root so the parent pom is available:
#   docker build -f booking-service/Dockerfile .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY user-service/pom.xml user-service/
COPY event-service/pom.xml event-service/
COPY booking-service/pom.xml booking-service/
RUN mvn -q -B -pl booking-service -am dependency:go-offline
COPY booking-service/src booking-service/src
RUN mvn -q -B -pl booking-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /src/booking-service/target/booking-service-1.0.0.jar app.jar
USER app
EXPOSE 8080
ENV PORT=8080
HEALTHCHECK CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
