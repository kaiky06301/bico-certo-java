# Bico Certo API (Java 21 + Spring Boot)
#   docker build -t bicocerto-api .
#   docker run -p 8080:8080 --env-file .env bicocerto-api

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
# usuario sem privilegios de root
RUN useradd --create-home bicocerto
USER bicocerto
COPY --from=build --chown=bicocerto /app/target/*.jar app.jar
EXPOSE 8080
# memoria contida para caber no plano gratuito (512 MB)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
