FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY service-library/ service-library/
RUN mvn -f service-library/pom.xml -q clean install -DskipTests
COPY util/ util/
RUN mvn -f util/pom.xml -q clean install -DskipTests

COPY currency-conversion-service/ currency-conversion-service/
RUN mvn -f currency-conversion-service/pom.xml -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/currency-conversion-service/target/currency-conversion-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8100
ENTRYPOINT ["java", "-jar", "app.jar"]
