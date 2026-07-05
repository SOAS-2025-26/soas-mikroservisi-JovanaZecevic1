FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY naming-server/ naming-server/
RUN mvn -f naming-server/pom.xml -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/naming-server/target/naming-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
