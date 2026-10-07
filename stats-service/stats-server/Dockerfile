FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY stats-service/pom.xml stats-service/pom.xml
COPY stats-service/stats-dto/pom.xml stats-service/stats-dto/pom.xml
COPY stats-service/stats-client/pom.xml stats-service/stats-client/pom.xml
COPY stats-service/stats-server/pom.xml stats-service/stats-server/pom.xml
COPY main-service/pom.xml main-service/pom.xml

COPY stats-service/stats-dto/src stats-service/stats-dto/src
COPY stats-service/stats-server/src stats-service/stats-server/src

RUN mvn clean package -pl stats-service/stats-server -am -DskipTests


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/stats-service/stats-server/target/stats-server-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]