# ---- build (compila e roda os testes com H2) ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B clean package

# ---- runtime ----
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/shared/files/products
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
