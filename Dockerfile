# Dockerfile pour le Backend Spring Boot
FROM maven:3.9.6-openjdk-8 AS builder

WORKDIR /app

# Copier les fichiers du projet
COPY pom.xml .
COPY src ./src

# Compiler et builder l'application
RUN mvn clean package -DskipTests

# Stage 2 : Runtime
FROM openjdk:8-jre-slim

WORKDIR /app

# Copier le JAR compilé depuis le builder
COPY --from=builder /app/target/*.jar app.jar

# Exposer le port 8080
EXPOSE 8080

# Lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]
