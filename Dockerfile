# Стадия 1: сборка (build)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests  # пропускаем тесты для скорости

# Стадия 2: запуск (runtime)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Render передаёт PORT через переменную окружения
ENV PORT=8080
EXPOSE ${PORT}

ENTRYPOINT ["java", "-jar", "app.jar"]