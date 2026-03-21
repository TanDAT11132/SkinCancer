FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

ENV PORT=8001
ENV APP_UPLOAD_ROOT_DIR=/tmp/uploads

COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8001
CMD ["sh", "-c", "java -jar /app/app.jar --server.port=${PORT}"]
