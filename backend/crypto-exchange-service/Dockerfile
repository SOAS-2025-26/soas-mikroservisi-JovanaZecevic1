FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY service-library/ service-library/
RUN mvn -f service-library/pom.xml -q clean install -DskipTests

COPY crypto-exchange-service/ crypto-exchange-service/
RUN mvn -f crypto-exchange-service/pom.xml -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/crypto-exchange-service/target/crypto-exchange-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8400
ENTRYPOINT ["java", "-jar", "app.jar"]
