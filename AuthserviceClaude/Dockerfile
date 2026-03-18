FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY gradle gradle
COPY gradlew gradlew.bat ./
COPY gradle.properties settings.gradle.kts build.gradle.kts ./
RUN ./gradlew dependencies --no-daemon
COPY src src
RUN ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*-all.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
