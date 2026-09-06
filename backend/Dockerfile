FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
RUN useradd --create-home --shell /bin/false appuser
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/storage/resumes && chown -R appuser:appuser /app
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
