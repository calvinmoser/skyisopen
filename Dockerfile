# Angular
FROM node:20.9-alpine AS build-frontend
WORKDIR /usr/src/app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm install
COPY frontend/. .
RUN npm run build

# Maven (attempt to prevent downloading libraries everytime)
FROM maven:3.5-jdk-8 as build-maven
COPY backend/pom.xml .
RUN mvn -B dependency:go-offline

# Spring Boot
FROM eclipse-temurin:17-jdk-alpine AS build-backend
WORKDIR /usr/src/app
COPY backend/. .
COPY --from=build-maven /root/.m2 /root/.m2
COPY --from=build-frontend /usr/src/app/backend/target ./target
RUN ./mvnw package

# Run
FROM eclipse-temurin:17-jre-alpine
ARG SKYISOPEN_VERSION
ENV SKYISOPEN_VERSION ${SKYISOPEN_VERSION}
WORKDIR /usr/src/app
COPY --from=build-backend /usr/src/app/target/skyisopen-${SKYISOPEN_VERSION}.jar .
CMD ["sh", "-c", "java -jar ./skyisopen-${SKYISOPEN_VERSION}.jar"]