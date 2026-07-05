FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY service-library/ service-library/
RUN mvn -f service-library/pom.xml -q clean install -DskipTests

COPY currency-exchange-service/ currency-exchange-service/
RUN mvn -f currency-exchange-service/pom.xml -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/currency-exchange-service/target/currency-exchange-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
