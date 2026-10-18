FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY services services
COPY incident-simulator incident-simulator
COPY copilot copilot
ARG MODULE
ARG MODULE_ROOT=services
RUN mvn -q -pl ${MODULE_ROOT}/${MODULE} -am package -DskipTests

FROM eclipse-temurin:21-jre
ARG MODULE
ARG MODULE_ROOT=services
WORKDIR /app
COPY --from=build /workspace/${MODULE_ROOT}/${MODULE}/target/*-0.1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
