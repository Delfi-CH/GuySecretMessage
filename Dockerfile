# =========================
# Teil 1: Build Stage
# =========================

# start with Eclipse Temurin JDK 21
FROM eclipse-temurin:21-jdk AS builder

# install maven and git
RUN apt-get update && apt-get install -y maven git

# Set working directory
WORKDIR /App

# Clone project
RUN git clone https://github.com/GuyNeeman/SecretMessage.git

# Set working directory
WORKDIR /App/SecretMessage

# Build the app
RUN mvn -DskipTests package


# =========================
# Teil 2: Runtime Stage
# =========================

# small runtime image
FROM alpine:latest

# install Java Runtime 21
RUN apk add --no-cache openjdk21-jre

# Set working directory
WORKDIR /App

# copy jar from first image
COPY --from=builder /App/SecretMessage/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Start application
ENTRYPOINT ["java", "-jar", "app.jar"]
