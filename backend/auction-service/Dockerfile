FROM maven:3.9.11-eclipse-temurin-21 AS build
LABEL authors="Shamaev_Ivan"

WORKDIR /app

COPY pom.xml ./pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml
COPY auction-service/pom.xml ./auction-service/pom.xml
COPY bidding-service/pom.xml ./bidding-service/pom.xml

COPY auction-service/src ./auction-service/src

RUN mvn -f auction-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/auction-service/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]