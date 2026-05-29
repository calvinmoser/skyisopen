# Angular
FROM node:20.9-alpine AS build-frontend
WORKDIR /usr/src/app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm install
COPY frontend/. .
RUN npm run build

# Spring Boot
FROM maven:3.9-eclipse-temurin-17 AS build-backend
WORKDIR /usr/src/app
COPY backend/pom.xml .
RUN mvn -B dependency:go-offline
COPY backend/. .
COPY --from=build-frontend /usr/src/app/backend/target ./target
RUN mvn package -DskipTests

# Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /usr/src/app
COPY --from=build-backend /usr/src/app/target/skyisopen-*.jar app.jar
CMD ["java", "-jar", "./app.jar"]