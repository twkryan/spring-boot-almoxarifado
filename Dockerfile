# ---- Etapa 1: compilacao ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

# ---- Etapa 2: execucao ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Banco H2 em arquivo: mantenha /app/data num volume para nao perder os dados
VOLUME /app/data

COPY --from=build /build/target/almox-ti.jar ./almox-ti.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "almox-ti.jar"]
