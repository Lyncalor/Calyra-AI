FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace
COPY mvnw mvnw
COPY .mvn .mvn
COPY pom.xml pom.xml
RUN ./mvnw -q -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
ENV TZ=Europe/Berlin

COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD wget -qO- http://localhost:8080/health || exit 1
ENTRYPOINT ["java","-jar","/app/app.jar"]
